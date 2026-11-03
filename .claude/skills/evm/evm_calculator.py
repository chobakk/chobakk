"""
Earned Value Management (EVM) Calculator
=========================================
Computes standard ANSI/EIA-748 EVM metrics from a WBS cost CSV,
generates a structured markdown report, and saves five PNG charts.

Supported input formats (auto-detected):
  Snapshot  — one row per WBS element, current period cumulative values
  Time-phased — multiple rows per WBS element, one row per reporting period

Usage (CLI):
  python evm_calculator.py data.csv
  python evm_calculator.py data.csv --output /path/to/report/

Usage (module):
  from evm_calculator import run_evm_analysis
  report = run_evm_analysis("data.csv", output_dir="/tmp/evm/")
"""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

import matplotlib
matplotlib.use("Agg")
import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# ─── Thresholds ──────────────────────────────────────────────────────────────
# Aligned with capital project industry norms (PMI, AACE RP 89R-03)
CPI_GREEN = 0.95
CPI_AMBER = 0.85
SPI_GREEN = 0.95
SPI_AMBER = 0.85

# Escalation trigger: SPI below this for ≥2 consecutive periods → Project Director
ESCALATION_SPI = 0.85
ESCALATION_CPI = 0.85

CHART_DPI = 150
CHART_STYLE = "seaborn-v0_8-whitegrid"

# RAG colour map (used consistently across all charts and report)
RAG_COLOURS = {"GREEN": "#2ecc71", "AMBER": "#f39c12", "RED": "#e74c3c", "GREY": "#95a5a6"}

MILLIONS = 1_000_000


# ─── Data model ──────────────────────────────────────────────────────────────

@dataclass(frozen=True)
class EVMSnapshot:
    """All EVM inputs and derived metrics for one WBS element at one period."""

    wbs_code: str
    description: str
    bac: float       # Budget at Completion
    bcws: float      # Budgeted Cost of Work Scheduled  (Planned Value, PV)
    bcwp: float      # Budgeted Cost of Work Performed  (Earned Value, EV)
    acwp: float      # Actual Cost of Work Performed    (Actual Cost, AC)
    period: str = ""

    # ── Variances ────────────────────────────────────────────────────────────

    @property
    def cv(self) -> float:
        """Cost Variance = BCWP − ACWP  (positive = under budget)."""
        return self.bcwp - self.acwp

    @property
    def sv(self) -> float:
        """Schedule Variance = BCWP − BCWS  (positive = ahead of schedule)."""
        return self.bcwp - self.bcws

    @property
    def cv_pct(self) -> Optional[float]:
        return (self.cv / self.bcwp * 100) if self.bcwp else None

    @property
    def sv_pct(self) -> Optional[float]:
        return (self.sv / self.bcws * 100) if self.bcws else None

    # ── Performance indices ──────────────────────────────────────────────────

    @property
    def cpi(self) -> Optional[float]:
        """Cost Performance Index = BCWP / ACWP  (>1 = under budget)."""
        return self.bcwp / self.acwp if self.acwp else None

    @property
    def spi(self) -> Optional[float]:
        """Schedule Performance Index = BCWP / BCWS  (>1 = ahead)."""
        return self.bcwp / self.bcws if self.bcws else None

    @property
    def pc(self) -> Optional[float]:
        """Percent complete = BCWP / BAC × 100."""
        return (self.bcwp / self.bac * 100) if self.bac else None

    # ── Forecasts ────────────────────────────────────────────────────────────

    @property
    def eac_cpi(self) -> Optional[float]:
        """EAC assuming current CPI continues for remaining work."""
        return self.bac / self.cpi if self.cpi else None

    @property
    def eac_composite(self) -> Optional[float]:
        """EAC weighted by both CPI and SPI — conservative, common for LCPs."""
        if self.cpi and self.spi:
            return self.acwp + (self.bac - self.bcwp) / (self.cpi * self.spi)
        return None

    @property
    def eac_planned(self) -> float:
        """EAC assuming remaining work performed at original planned rate."""
        return self.acwp + (self.bac - self.bcwp)

    @property
    def etc(self) -> Optional[float]:
        """Estimate to Complete (using CPI method)."""
        return (self.eac_cpi - self.acwp) if self.eac_cpi else None

    @property
    def vac(self) -> Optional[float]:
        """Variance at Completion = BAC − EAC_CPI."""
        return (self.bac - self.eac_cpi) if self.eac_cpi else None

    @property
    def tcpi(self) -> Optional[float]:
        """To-Complete Performance Index = (BAC−BCWP) / (BAC−ACWP).
        Required CPI for all remaining work to finish within BAC.
        """
        remaining_budget = self.bac - self.acwp
        remaining_work = self.bac - self.bcwp
        return remaining_work / remaining_budget if remaining_budget else None

    # ── RAG status ───────────────────────────────────────────────────────────

    def cost_rag(self) -> str:
        if self.cpi is None:
            return "GREY"
        if self.cpi >= CPI_GREEN:
            return "GREEN"
        if self.cpi >= CPI_AMBER:
            return "AMBER"
        return "RED"

    def schedule_rag(self) -> str:
        if self.spi is None:
            return "GREY"
        if self.spi >= SPI_GREEN:
            return "GREEN"
        if self.spi >= SPI_AMBER:
            return "AMBER"
        return "RED"

    def overall_rag(self) -> str:
        priority = {"RED": 0, "AMBER": 1, "GREEN": 2, "GREY": 3}
        return min(self.cost_rag(), self.schedule_rag(), key=lambda r: priority[r])


# ─── Input loading ────────────────────────────────────────────────────────────

REQUIRED_COLS = {"wbs_code", "wbs_description", "bac", "bcws_cum", "bcwp_cum", "acwp_cum"}
TIMEPHASED_COL = "period"


def _validate_columns(df: pd.DataFrame, path: Path) -> None:
    missing = REQUIRED_COLS - set(df.columns)
    if missing:
        raise ValueError(
            f"CSV '{path.name}' is missing required columns: {sorted(missing)}\n"
            f"Required: {sorted(REQUIRED_COLS)}"
        )


def _is_timephased(df: pd.DataFrame) -> bool:
    if TIMEPHASED_COL not in df.columns:
        return False
    return df["wbs_code"].duplicated().any()


def load_snapshot(path: Path) -> list[EVMSnapshot]:
    """Load a snapshot CSV (one row per WBS element, current period)."""
    df = pd.read_csv(path)
    _validate_columns(df, path)
    period = df["period"].iloc[0] if "period" in df.columns else ""
    return [
        EVMSnapshot(
            wbs_code=row["wbs_code"],
            description=row["wbs_description"],
            bac=float(row["bac"]),
            bcws=float(row["bcws_cum"]),
            bcwp=float(row["bcwp_cum"]),
            acwp=float(row["acwp_cum"]),
            period=str(period),
        )
        for _, row in df.iterrows()
    ]


def load_timephased(path: Path) -> dict[str, list[EVMSnapshot]]:
    """Load a time-phased CSV. Returns {period: [EVMSnapshot, ...]}."""
    df = pd.read_csv(path)
    _validate_columns(df, path)
    if TIMEPHASED_COL not in df.columns:
        raise ValueError("Time-phased CSV must include a 'period' column.")

    periods: dict[str, list[EVMSnapshot]] = {}
    for period, group in df.groupby(TIMEPHASED_COL, sort=True):
        periods[str(period)] = [
            EVMSnapshot(
                wbs_code=row["wbs_code"],
                description=row["wbs_description"],
                bac=float(row["bac"]),
                bcws=float(row["bcws_cum"]),
                bcwp=float(row["bcwp_cum"]),
                acwp=float(row["acwp_cum"]),
                period=str(period),
            )
            for _, row in group.iterrows()
        ]
    return periods


# ─── Rollup ───────────────────────────────────────────────────────────────────

def rollup(snapshots: list[EVMSnapshot], label: str = "PROJECT TOTAL") -> EVMSnapshot:
    """Sum WBS elements to produce a project-level EVMSnapshot."""
    return EVMSnapshot(
        wbs_code="TOTAL",
        description=label,
        bac=sum(s.bac for s in snapshots),
        bcws=sum(s.bcws for s in snapshots),
        bcwp=sum(s.bcwp for s in snapshots),
        acwp=sum(s.acwp for s in snapshots),
        period=snapshots[0].period if snapshots else "",
    )


# ─── Report text ─────────────────────────────────────────────────────────────

def _fmt_eur(value: Optional[float], decimals: int = 1) -> str:
    if value is None:
        return "N/A"
    return f"EUR {value / MILLIONS:,.{decimals}f}M"


def _fmt_idx(value: Optional[float]) -> str:
    return f"{value:.3f}" if value is not None else "N/A"


def _fmt_pct(value: Optional[float]) -> str:
    return f"{value:+.1f}%" if value is not None else "N/A"


def _rag_badge(rag: str) -> str:
    icons = {"GREEN": "🟢", "AMBER": "🟡", "RED": "🔴", "GREY": "⚪"}
    return icons.get(rag, "⚪")


def _escalation_flags(total: EVMSnapshot, timephased_totals: list[EVMSnapshot]) -> list[str]:
    """Return list of escalation warnings based on thresholds."""
    flags: list[str] = []

    if total.cpi is not None and total.cpi < ESCALATION_CPI:
        flags.append(
            f"**ESCALATE — Cost**: Project CPI={total.cpi:.3f} is below {ESCALATION_CPI}. "
            "Notify Project Director."
        )

    if total.spi is not None and total.spi < ESCALATION_SPI:
        flags.append(
            f"**ESCALATE — Schedule**: Project SPI={total.spi:.3f} is below {ESCALATION_SPI}. "
            "Notify Project Director."
        )

    # Check for 2+ consecutive periods of SPI below threshold
    if len(timephased_totals) >= 2:
        recent = [t for t in timephased_totals[-2:] if t.spi is not None]
        if len(recent) == 2 and all(t.spi < ESCALATION_SPI for t in recent):
            last_periods = f"{timephased_totals[-2].period} and {timephased_totals[-1].period}"
            flags.append(
                f"**ESCALATE — Sustained Schedule Underperformance**: SPI below "
                f"{ESCALATION_SPI} for {last_periods}. Requires Project Director memo."
            )

    if total.tcpi is not None and total.tcpi > 1.10:
        flags.append(
            f"**WARNING — TCPI**: Required future performance index is {total.tcpi:.3f}. "
            "A TCPI >1.10 is generally considered unachievable without re-baselining."
        )

    return flags


def generate_report(
    snapshots: list[EVMSnapshot],
    total: EVMSnapshot,
    timephased_totals: list[EVMSnapshot],
    charts: list[str],
) -> str:
    lines: list[str] = []
    period_label = total.period or "Current period"

    lines += [
        f"# Earned Value Management Report — {period_label}",
        "",
        "---",
        "",
        "## Executive Summary",
        "",
    ]

    # EAC range
    eac_values = [v for v in [total.eac_cpi, total.eac_composite, total.eac_planned] if v]
    eac_range = (
        f"{_fmt_eur(min(eac_values))} – {_fmt_eur(max(eac_values))}"
        if len(eac_values) >= 2
        else (_fmt_eur(eac_values[0]) if eac_values else "N/A")
    )

    lines += [
        f"| Metric | Value |",
        f"|--------|-------|",
        f"| BAC (Budget at Completion) | {_fmt_eur(total.bac)} |",
        f"| BCWS (Planned Value) | {_fmt_eur(total.bcws)} |",
        f"| BCWP (Earned Value) | {_fmt_eur(total.bcwp)} |",
        f"| ACWP (Actual Cost) | {_fmt_eur(total.acwp)} |",
        f"| % Complete | {_fmt_pct(total.pc)[1:] if total.pc else 'N/A'} |",
        f"| CPI | {_fmt_idx(total.cpi)} {_rag_badge(total.cost_rag())} |",
        f"| SPI | {_fmt_idx(total.spi)} {_rag_badge(total.schedule_rag())} |",
        f"| Cost Variance | {_fmt_eur(total.cv)} ({_fmt_pct(total.cv_pct)}) |",
        f"| Schedule Variance | {_fmt_eur(total.sv)} ({_fmt_pct(total.sv_pct)}) |",
        f"| EAC range | {eac_range} |",
        f"| VAC (Variance at Completion) | {_fmt_eur(total.vac)} |",
        f"| TCPI (required future perf.) | {_fmt_idx(total.tcpi)} |",
        "",
    ]

    # Escalation flags
    flags = _escalation_flags(total, timephased_totals)
    if flags:
        lines += ["## ⚠️ Escalation Flags", ""]
        for flag in flags:
            lines.append(f"- {flag}")
        lines.append("")

    # WBS breakdown
    lines += [
        "## WBS Performance Breakdown",
        "",
        "| WBS | Description | BAC | BCWP | ACWP | CV | CPI | SPI | Cost | Sched |",
        "|-----|-------------|-----|------|------|----|-----|-----|------|-------|",
    ]
    for s in snapshots:
        lines.append(
            f"| {s.wbs_code} | {s.description} | {_fmt_eur(s.bac)} | "
            f"{_fmt_eur(s.bcwp)} | {_fmt_eur(s.acwp)} | "
            f"{_fmt_eur(s.cv)} | {_fmt_idx(s.cpi)} | {_fmt_idx(s.spi)} | "
            f"{_rag_badge(s.cost_rag())} | {_rag_badge(s.schedule_rag())} |"
        )
    lines += [""]

    # EAC methods
    lines += [
        "## EAC Forecast Methods",
        "",
        "| Method | Formula | EAC | ETC | VAC |",
        "|--------|---------|-----|-----|-----|",
        f"| CPI (perf. continues) | BAC ÷ CPI | {_fmt_eur(total.eac_cpi)} | "
        f"{_fmt_eur(total.etc)} | {_fmt_eur(total.vac)} |",
        f"| Composite CPI×SPI | ACWP + (BAC−BCWP)÷(CPI×SPI) | "
        f"{_fmt_eur(total.eac_composite)} | N/A | N/A |",
        f"| Planned rate | ACWP + (BAC−BCWP) | {_fmt_eur(total.eac_planned)} | N/A | N/A |",
        "",
        "> **Recommended EAC for LCPs**: Composite method (CPI×SPI) is most conservative "
        "and appropriate when schedule pressure can drive cost overruns.",
        "",
    ]

    # Charts
    if charts:
        lines += ["## Charts", ""]
        for chart in charts:
            label = Path(chart).stem.replace("_", " ").title()
            lines.append(f"- `{chart}` — {label}")
        lines.append("")

    return "\n".join(lines)


# ─── Visualisations ───────────────────────────────────────────────────────────

def _apply_style() -> None:
    try:
        plt.style.use(CHART_STYLE)
    except OSError:
        pass  # Fall back to matplotlib default if style unavailable


def chart_scurve(
    period_totals: list[EVMSnapshot],
    output_dir: Path,
) -> str:
    """S-curve: cumulative BCWS, BCWP, ACWP over time."""
    _apply_style()
    periods = [t.period for t in period_totals]
    bcws = [t.bcws / MILLIONS for t in period_totals]
    bcwp = [t.bcwp / MILLIONS for t in period_totals]
    acwp = [t.acwp / MILLIONS for t in period_totals]

    fig, ax = plt.subplots(figsize=(12, 6), constrained_layout=True)
    x = range(len(periods))

    ax.plot(x, bcws, "b--o", linewidth=2, label="BCWS (Planned Value)", markersize=5)
    ax.plot(x, bcwp, "g-o", linewidth=2.5, label="BCWP (Earned Value)", markersize=5)
    ax.plot(x, acwp, "r-s", linewidth=2, label="ACWP (Actual Cost)", markersize=5)

    # Shade cost variance region
    ax.fill_between(x, bcwp, acwp, where=[a > b for a, b in zip(acwp, bcwp)],
                    alpha=0.15, color="red", label="Cost Overrun")
    ax.fill_between(x, bcwp, acwp, where=[b >= a for a, b in zip(acwp, bcwp)],
                    alpha=0.10, color="green", label="Cost Saving")

    ax.set_xticks(list(x))
    ax.set_xticklabels(periods, rotation=45, ha="right")
    ax.set_xlabel("Reporting Period")
    ax.set_ylabel("EUR Million (cumulative)")
    ax.set_title("S-Curve — Cumulative Cost Performance", fontsize=14, fontweight="bold")
    ax.legend(loc="upper left")
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"€{v:,.0f}M"))

    path = output_dir / "01_s_curve.png"
    fig.savefig(path, dpi=CHART_DPI, bbox_inches="tight")
    plt.close(fig)
    return str(path)


def chart_indices_trend(
    period_totals: list[EVMSnapshot],
    output_dir: Path,
) -> str:
    """CPI and SPI trend over reporting periods."""
    _apply_style()
    periods = [t.period for t in period_totals]
    cpis = [t.cpi if t.cpi else np.nan for t in period_totals]
    spis = [t.spi if t.spi else np.nan for t in period_totals]

    fig, ax = plt.subplots(figsize=(12, 5), constrained_layout=True)
    x = range(len(periods))

    ax.plot(x, cpis, "g-o", linewidth=2.5, label="CPI (Cost)", markersize=6)
    ax.plot(x, spis, "b-s", linewidth=2.5, label="SPI (Schedule)", markersize=6)

    ax.axhline(1.0, color="black", linewidth=1.0, linestyle="-", alpha=0.5, label="1.0 (on target)")
    ax.axhline(CPI_GREEN, color=RAG_COLOURS["AMBER"], linewidth=1, linestyle="--", alpha=0.7,
               label=f"Amber threshold ({CPI_GREEN})")
    ax.axhline(CPI_AMBER, color=RAG_COLOURS["RED"], linewidth=1, linestyle=":", alpha=0.7,
               label=f"Red threshold ({CPI_AMBER})")

    ax.fill_between(x, CPI_GREEN, 1.0 + 0.5,
                    alpha=0.05, color="green")
    ax.fill_between(x, CPI_AMBER, CPI_GREEN,
                    alpha=0.05, color="orange")
    ax.fill_between(x, 0, CPI_AMBER,
                    alpha=0.05, color="red")

    ax.set_xticks(list(x))
    ax.set_xticklabels(periods, rotation=45, ha="right")
    ax.set_ylim(0.5, 1.3)
    ax.set_xlabel("Reporting Period")
    ax.set_ylabel("Performance Index")
    ax.set_title("CPI & SPI Trend", fontsize=14, fontweight="bold")
    ax.legend(loc="lower left", fontsize=9)

    path = output_dir / "02_indices_trend.png"
    fig.savefig(path, dpi=CHART_DPI, bbox_inches="tight")
    plt.close(fig)
    return str(path)


def chart_wbs_variance(
    snapshots: list[EVMSnapshot],
    output_dir: Path,
) -> str:
    """Horizontal bar chart of Cost Variance per WBS element."""
    _apply_style()
    labels = [f"{s.wbs_code}\n{s.description}" for s in snapshots]
    values = [s.cv / MILLIONS for s in snapshots]
    colours = [RAG_COLOURS[s.cost_rag()] for s in snapshots]

    fig, ax = plt.subplots(figsize=(10, max(4, len(snapshots) * 0.8)), constrained_layout=True)
    y = range(len(snapshots))

    bars = ax.barh(list(y), values, color=colours, edgecolor="white", height=0.6)

    for bar, val in zip(bars, values):
        sign = "+" if val >= 0 else ""
        ax.text(
            bar.get_width() + (max(abs(v) for v in values) * 0.01),
            bar.get_y() + bar.get_height() / 2,
            f"{sign}€{val:,.1f}M",
            va="center", fontsize=9,
        )

    ax.axvline(0, color="black", linewidth=1.2)
    ax.set_yticks(list(y))
    ax.set_yticklabels(labels, fontsize=9)
    ax.set_xlabel("Cost Variance (EUR Million)")
    ax.set_title("Cost Variance by WBS Package", fontsize=14, fontweight="bold")

    legend_patches = [
        mpatches.Patch(color=RAG_COLOURS["GREEN"], label="GREEN (CPI ≥ 0.95)"),
        mpatches.Patch(color=RAG_COLOURS["AMBER"], label="AMBER (0.85 ≤ CPI < 0.95)"),
        mpatches.Patch(color=RAG_COLOURS["RED"],   label="RED (CPI < 0.85)"),
    ]
    ax.legend(handles=legend_patches, loc="lower right", fontsize=8)

    path = output_dir / "03_wbs_variance.png"
    fig.savefig(path, dpi=CHART_DPI, bbox_inches="tight")
    plt.close(fig)
    return str(path)


def chart_eac_comparison(
    snapshots: list[EVMSnapshot],
    total: EVMSnapshot,
    output_dir: Path,
) -> str:
    """Grouped bar: BAC vs three EAC methods for each WBS package + total."""
    _apply_style()
    items = [*snapshots, total]
    labels = [f"{s.wbs_code}" for s in items[:-1]] + ["TOTAL"]

    bac_vals     = [s.bac / MILLIONS for s in items]
    eac_cpi_vals = [(s.eac_cpi or s.bac) / MILLIONS for s in items]
    eac_comp     = [(s.eac_composite or s.bac) / MILLIONS for s in items]
    eac_plan     = [s.eac_planned / MILLIONS for s in items]

    x = np.arange(len(labels))
    width = 0.2

    fig, ax = plt.subplots(figsize=(max(10, len(items) * 1.8), 6), constrained_layout=True)

    ax.bar(x - 1.5 * width, bac_vals,     width, label="BAC",              color="#3498db", alpha=0.9)
    ax.bar(x - 0.5 * width, eac_cpi_vals, width, label="EAC (CPI)",        color="#e74c3c", alpha=0.9)
    ax.bar(x + 0.5 * width, eac_comp,     width, label="EAC (CPI×SPI)",    color="#e67e22", alpha=0.9)
    ax.bar(x + 1.5 * width, eac_plan,     width, label="EAC (Planned rate)",color="#2ecc71", alpha=0.9)

    ax.set_xticks(x)
    ax.set_xticklabels(labels, rotation=30, ha="right", fontsize=9)
    ax.set_ylabel("EUR Million")
    ax.set_title("EAC Forecast Comparison vs BAC", fontsize=14, fontweight="bold")
    ax.legend(fontsize=9)
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda v, _: f"€{v:,.0f}M"))

    path = output_dir / "04_eac_comparison.png"
    fig.savefig(path, dpi=CHART_DPI, bbox_inches="tight")
    plt.close(fig)
    return str(path)


def chart_quadrant(
    snapshots: list[EVMSnapshot],
    output_dir: Path,
) -> str:
    """CPI vs SPI scatter — four-quadrant performance map, bubble = BAC."""
    _apply_style()
    fig, ax = plt.subplots(figsize=(8, 7), constrained_layout=True)

    max_bac = max(s.bac for s in snapshots)
    for s in snapshots:
        if s.cpi is None or s.spi is None:
            continue
        size = (s.bac / max_bac) * 1200 + 100
        colour = RAG_COLOURS[s.overall_rag()]
        ax.scatter(s.spi, s.cpi, s=size, c=colour, alpha=0.75, edgecolors="white", linewidth=1.5)
        ax.annotate(
            s.wbs_code,
            (s.spi, s.cpi),
            textcoords="offset points",
            xytext=(6, 4),
            fontsize=8,
        )

    ax.axhline(1.0, color="black", linewidth=1.2, alpha=0.6)
    ax.axvline(1.0, color="black", linewidth=1.2, alpha=0.6)

    _quadrant_label(ax, 0.55, 1.15, "Under budget\nBehind schedule", "#3498db")
    _quadrant_label(ax, 1.20, 1.15, "Under budget\nAhead of schedule", "#2ecc71")
    _quadrant_label(ax, 0.55, 0.60, "Over budget\nBehind schedule", "#e74c3c")
    _quadrant_label(ax, 1.20, 0.60, "Over budget\nAhead of schedule", "#f39c12")

    ax.set_xlim(0.4, 1.4)
    ax.set_ylim(0.5, 1.3)
    ax.set_xlabel("SPI (Schedule Performance Index)", fontsize=11)
    ax.set_ylabel("CPI (Cost Performance Index)", fontsize=11)
    ax.set_title("Cost / Schedule Performance Quadrant\n(bubble size proportional to BAC)", fontsize=13, fontweight="bold")

    legend_patches = [
        mpatches.Patch(color=RAG_COLOURS["GREEN"], label="GREEN"),
        mpatches.Patch(color=RAG_COLOURS["AMBER"], label="AMBER"),
        mpatches.Patch(color=RAG_COLOURS["RED"],   label="RED"),
    ]
    ax.legend(handles=legend_patches, loc="lower right", fontsize=9)

    path = output_dir / "05_cpi_spi_quadrant.png"
    fig.savefig(path, dpi=CHART_DPI, bbox_inches="tight")
    plt.close(fig)
    return str(path)


def _quadrant_label(ax, x: float, y: float, text: str, colour: str) -> None:
    ax.text(x, y, text, fontsize=7.5, color=colour, alpha=0.6,
            ha="center", va="center", style="italic",
            bbox=dict(boxstyle="round,pad=0.2", facecolor="white", alpha=0.0))


# ─── Orchestrator ─────────────────────────────────────────────────────────────

def run_evm_analysis(csv_path: str, output_dir: str = "./evm-output/") -> str:
    """
    Full EVM analysis pipeline.

    Parameters
    ----------
    csv_path  : path to snapshot or time-phased CSV
    output_dir: directory for PNG charts and report markdown

    Returns
    -------
    str — formatted markdown report (also saved to output_dir/evm-report.md)
    """
    path = Path(csv_path)
    if not path.exists():
        raise FileNotFoundError(f"CSV not found: {path}")

    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)

    raw = pd.read_csv(path)
    _validate_columns(raw, path)
    timephased = _is_timephased(raw)

    charts: list[str] = []
    timephased_totals: list[EVMSnapshot] = []

    if timephased:
        period_data = load_timephased(path)
        all_periods = sorted(period_data.keys())
        # Build per-period totals for trend charts
        for p in all_periods:
            timephased_totals.append(rollup(period_data[p], label=p))

        # Latest period is the "current" snapshot
        latest_period = all_periods[-1]
        snapshots = period_data[latest_period]
        total = rollup(snapshots, label=latest_period)

        charts.append(chart_scurve(timephased_totals, out))
        if len(all_periods) >= 2:
            charts.append(chart_indices_trend(timephased_totals, out))
    else:
        snapshots = load_snapshot(path)
        total = rollup(snapshots)

    charts.append(chart_wbs_variance(snapshots, out))
    charts.append(chart_eac_comparison(snapshots, total, out))
    charts.append(chart_quadrant(snapshots, out))

    report = generate_report(snapshots, total, timephased_totals, charts)

    report_path = out / "evm-report.md"
    report_path.write_text(report, encoding="utf-8")

    print(report)
    print(f"\nCharts saved to: {out}/")
    print(f"Report saved to: {report_path}")
    return report


# ─── CLI ─────────────────────────────────────────────────────────────────────

def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="EVM Calculator — ANSI/EIA-748 metrics from a WBS cost CSV",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("csv", help="Path to snapshot or time-phased EVM CSV")
    parser.add_argument(
        "--output", "-o",
        default="./evm-output/",
        help="Output directory for charts and report (default: ./evm-output/)",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = _parse_args()
    try:
        run_evm_analysis(args.csv, args.output)
    except (FileNotFoundError, ValueError) as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
