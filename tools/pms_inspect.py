#!/usr/bin/env python3
"""pms_inspect.py - read big Excel workbooks (xlsx/xlsm) WITHOUT loading them whole.

Why: the PMS file is ~57 MB; sheet PMS-Overall is ~380 MB of XML. openpyxl/pandas hang on it.
This tool streams the XML straight out of the zip. It never writes to the workbook.

Usage (columns like AB or AB:AK, rows like 1-9):
  python pms_inspect.py sheets  FILE
  python pms_inspect.py head    FILE SHEET [--rows 1-9] [--cols A-AX]
  python pms_inspect.py nonzero FILE SHEET --col AB [--name-col U] [--minrow 8]
  python pms_inspect.py edges   FILE SHEET            # cross-sheet references (full scan, slow on PMS-Overall)
  python pms_inspect.py xlsb-head FILE SHEET [--rows 1-9] [--cols A-AX]   # needs: pip install pyxlsb
Notes:
  * Shared formulas: only the first cell of a shared group holds text; the others print "(shared)".
  * 'head' stops reading as soon as it passes the last requested row, so it is fast even on PMS-Overall.
"""
import sys, re, html, zipfile, codecs, argparse, collections, signal
try: signal.signal(signal.SIGPIPE, signal.SIG_DFL)   # no traceback when piped into head
except (AttributeError, ValueError): pass

def col2n(c):
    n = 0
    for ch in c: n = n * 26 + ord(ch) - 64
    return n

def n2col(n):
    s = ''
    while n: n, r = divmod(n - 1, 26); s = chr(65 + r) + s
    return s

def parse_range(txt, kind):
    if not txt: return (1, 10**9)
    a, _, b = txt.replace('-', ':').partition(':')
    b = b or a
    if kind == 'row': return (int(a), int(b))
    return (col2n(a), col2n(b))

class Book:
    def __init__(self, path):
        self.z = zipfile.ZipFile(path)
        wb = self.z.read('xl/workbook.xml').decode('utf-8', 'replace')
        rels = self.z.read('xl/_rels/workbook.xml.rels').decode('utf-8', 'replace')
        rid = {}
        for m in re.finditer(r'<Relationship ([^>]*)/>', rels):
            a = m.group(1)
            rid[re.search(r'Id="([^"]+)"', a).group(1)] = re.search(r'Target="([^"]+)"', a).group(1)
        self.sheets = []
        for m in re.finditer(r'<sheet ([^>]*)/>', wb):
            a = m.group(1)
            name = html.unescape(re.search(r'name="([^"]*)"', a).group(1))
            target = rid[re.search(r'r:id="([^"]*)"', a).group(1)].lstrip('/')
            target = target if target.startswith('xl/') else 'xl/' + target
            st = re.search(r'state="([^"]*)"', a)
            self.sheets.append((name, target, st.group(1) if st else 'visible'))
        self.sst = None

    def shared_strings(self):
        if self.sst is None:
            try: t = self.z.read('xl/sharedStrings.xml').decode('utf-8', 'replace')
            except KeyError: t = ''
            self.sst = [html.unescape(''.join(re.findall(r'<t[^>]*>(.*?)</t>', si, flags=re.S)))
                        for si in re.findall(r'<si>(.*?)</si>', t, flags=re.S)]
        return self.sst

    def target(self, sheet):
        for n, t, _ in self.sheets:
            if n == sheet: return t
        raise SystemExit('sheet not found: %r. Use the "sheets" command.' % sheet)

    def rows(self, sheet, max_row=10**9):
        """yield (row_number, row_xml); stops after max_row."""
        dec = codecs.getincrementaldecoder('utf-8')('replace')
        carry = ''
        with self.z.open(self.target(sheet)) as f:
            while True:
                raw = f.read(8 * 1024 * 1024)
                data = carry + dec.decode(raw, final=not raw)
                if raw:
                    k = data.rfind('</row>')
                    if k < 0: carry = data; continue
                    carry, data = data[k + 6:], data[:k + 6]
                else: carry = ''
                for m in re.finditer(r'<row r="(\d+)"[^>]*>(.*?)</row>', data, flags=re.S):
                    r = int(m.group(1))
                    if r > max_row: return
                    yield r, m.group(2)
                if not raw: return

CELL = re.compile(r'<c r="([A-Z]+)(\d+)"([^>]*?)(?:/>|>(.*?)</c>)', re.S)
FRE = re.compile(r'<f([^>]*?)(?:/>|>(.*?)</f>)', re.S)
VRE = re.compile(r'<v>(.*?)</v>', re.S)

def cells(book, rowxml):
    sst = book.shared_strings()
    for m in CELL.finditer(rowxml):
        col, row, attrs, inner = m.group(1), int(m.group(2)), m.group(3), m.group(4)
        if inner is None: continue
        v = VRE.search(inner); f = FRE.search(inner)
        val = None
        if v:
            val = v.group(1)
            if 't="s"' in attrs: val = sst[int(val)]
            else:
                val = html.unescape(val)
                try: val = float(val)
                except ValueError: pass
        ftxt = None
        if f: ftxt = html.unescape(f.group(2)) if f.group(2) else '(shared)'
        yield col, row, val, ftxt

def cmd_sheets(a):
    b = Book(a.file)
    for n, t, st in b.sheets:
        info = b.z.getinfo(t).file_size
        print('%-22s %-28s %-8s %8.1f MB xml' % (n, t, st, info / 1e6))

def cmd_head(a):
    b = Book(a.file); r0, r1 = parse_range(a.rows, 'row'); c0, c1 = parse_range(a.cols, 'col')
    for r, x in b.rows(a.sheet, r1):
        if r < r0: continue
        for col, row, val, f in cells(b, x):
            if not (c0 <= col2n(col) <= c1): continue
            if val in (None, '') and not f: continue
            print('%s%d: %s%s' % (col, row, str(val)[:80], ('   [=%s]' % f[:100]) if f else ''))

def cmd_nonzero(a):
    b = Book(a.file); n = 0
    names = {}; vals = {}
    for r, x in b.rows(a.sheet):
        if r < a.minrow: continue
        for col, row, val, f in cells(b, x):
            if col == a.col and isinstance(val, float) and abs(val) > 1e-9: vals[row] = val
            if a.name_col and col == a.name_col and val not in (None, ''): names[row] = val
    for r in sorted(vals):
        print('row %d | %s | %s' % (r, str(names.get(r, ''))[:50], round(vals[r], 6))); n += 1
    print('rows with non-zero %s: %d' % (a.col, n))

def cmd_edges(a):
    b = Book(a.file); cnt = collections.Counter(); ex = {}
    ref = re.compile(r"(?:'((?:[^']|'')+)'|([A-Za-z_][A-Za-z0-9_.]*))!")
    for r, x in b.rows(a.sheet):
        for col, row, val, f in cells(b, x):
            if f and f != '(shared)':
                for p, q in ref.findall(f):
                    nm = (p.replace("''", "'") if p else q)
                    cnt[(col, nm)] += 1; ex.setdefault((col, nm), '%s%d: %s' % (col, row, f[:90]))
    agg = collections.defaultdict(int)
    for (c, nm), v in cnt.items(): agg[nm] += v
    for nm, v in sorted(agg.items(), key=lambda kv: -kv[1]):
        cols = sorted({c for (c, n2) in cnt if n2 == nm}, key=lambda c: (len(c), c))
        print('-> %-22s %6d formulas  cols %s' % (nm, v, ','.join(cols)[:60]))

def cmd_xlsb(a):
    from pyxlsb import open_workbook
    r0, r1 = parse_range(a.rows, 'row'); c0, c1 = parse_range(a.cols, 'col')
    with open_workbook(a.file) as wb, wb.get_sheet(a.sheet) as ws:
        for row in ws.rows(sparse=True):
            if not row: continue
            r = row[0].r + 1
            if r > r1: break
            if r < r0: continue
            for c in row:
                if c.v in (None, '') or not (c0 <= c.c + 1 <= c1): continue
                print('%s%d: %s' % (n2col(c.c + 1), r, str(c.v)[:80]))

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = p.add_subparsers(dest='cmd', required=True)
    s = sp.add_parser('sheets'); s.add_argument('file'); s.set_defaults(fn=cmd_sheets)
    for name, fn in (('head', cmd_head), ('xlsb-head', cmd_xlsb)):
        s = sp.add_parser(name); s.add_argument('file'); s.add_argument('sheet')
        s.add_argument('--rows', default='1-9'); s.add_argument('--cols', default='A-AX'); s.set_defaults(fn=fn)
    s = sp.add_parser('nonzero'); s.add_argument('file'); s.add_argument('sheet'); s.add_argument('--col', required=True)
    s.add_argument('--name-col'); s.add_argument('--minrow', type=int, default=8); s.set_defaults(fn=cmd_nonzero)
    s = sp.add_parser('edges'); s.add_argument('file'); s.add_argument('sheet'); s.set_defaults(fn=cmd_edges)
    a = p.parse_args(); a.fn(a)

if __name__ == '__main__':
    main()
