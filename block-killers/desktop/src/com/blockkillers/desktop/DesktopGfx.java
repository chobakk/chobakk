package com.blockkillers.desktop;

import com.blockkillers.core.Gfx;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;

/** Java2D implementation of {@link Gfx}, used for screenshots and visual checks off-device. */
public final class DesktopGfx implements Gfx {
    private final Graphics2D g;
    private final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
    private final int w, h;
    private final Path2D.Float path = new Path2D.Float();

    public DesktopGfx(Graphics2D g, int w, int h) {
        this.g = g; this.w = w; this.h = h;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
    private void col(int c) { g.setColor(new Color(c, true)); }
    private void stroke(float wd) { g.setStroke(new BasicStroke(wd, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); }

    @Override public void clear(int argb) {
        AffineTransform t = g.getTransform();
        g.setTransform(new AffineTransform());
        col(argb); g.fillRect(0, 0, w, h);
        g.setTransform(t);
    }
    @Override public void save() { stack.push(g.getTransform()); }
    @Override public void restore() { g.setTransform(stack.pop()); }
    @Override public void translate(float x, float y) { g.translate(x, y); }
    @Override public void scale(float s) { g.scale(s, s); }
    @Override public void rotate(float r) { g.rotate(r); }
    @Override public void circle(float x, float y, float r, int c) { col(c); g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2)); }
    @Override public void ring(float x, float y, float r, float wd, int c) { col(c); stroke(wd); g.draw(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2)); }
    @Override public void arc(float x, float y, float r, float start, float sweep, int c) {
        col(c);
        g.fill(new Arc2D.Float(x - r, y - r, r * 2, r * 2, (float) -Math.toDegrees(start), (float) -Math.toDegrees(sweep), Arc2D.PIE));
    }
    @Override public void rect(float l, float t, float r, float b, int c) { col(c); g.fill(new Rectangle2D.Float(Math.min(l, r), Math.min(t, b), Math.abs(r - l), Math.abs(b - t))); }
    @Override public void roundRect(float l, float t, float r, float b, float rad, int c) { col(c); g.fill(new RoundRectangle2D.Float(l, t, r - l, b - t, rad * 2, rad * 2)); }
    @Override public void strokeRoundRect(float l, float t, float r, float b, float rad, float wd, int c) { col(c); stroke(wd); g.draw(new RoundRectangle2D.Float(l, t, r - l, b - t, rad * 2, rad * 2)); }
    @Override public void line(float x1, float y1, float x2, float y2, float wd, int c) { col(c); stroke(wd); g.draw(new Line2D.Float(x1, y1, x2, y2)); }
    private void build(float[] p, int n) { path.reset(); path.moveTo(p[0], p[1]); for (int i = 1; i < n; i++) path.lineTo(p[i * 2], p[i * 2 + 1]); path.closePath(); }
    @Override public void poly(float[] p, int n, int c) { build(p, n); col(c); g.fill(path); }
    @Override public void strokePoly(float[] p, int n, float wd, int c) { build(p, n); col(c); stroke(wd); g.draw(path); }
    private Font font(float size, boolean bold) { return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size); }
    @Override public void text(String s, float x, float y, float size, int c, int align, boolean bold) {
        Font f = font(size, bold);
        g.setFont(f); col(c);
        float wd = g.getFontMetrics().stringWidth(s);
        float dx = align == ALIGN_CENTER ? -wd / 2 : (align == ALIGN_RIGHT ? -wd : 0);
        g.drawString(s, x + dx, y);
    }
    @Override public float textWidth(String s, float size, boolean bold) { g.setFont(font(size, bold)); return g.getFontMetrics().stringWidth(s); }
}
