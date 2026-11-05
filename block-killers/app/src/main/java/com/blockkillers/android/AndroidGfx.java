package com.blockkillers.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.blockkillers.core.Gfx;

/** {@link Gfx} implemented on a hardware-accelerated Android Canvas. Reuses Paint/Path/RectF objects. */
final class AndroidGfx implements Gfx {
    private Canvas c;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final Typeface bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private final Typeface normal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Rect srcR = new android.graphics.Rect();
    private final RectF dstR = new RectF();
    private int lastTint = 0, lastMode = -1;
    private android.graphics.ColorFilter filter;

    AndroidGfx() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    void setCanvas(Canvas canvas) { c = canvas; }

    @Override public void clear(int argb) { c.drawColor(argb); }
    @Override public void save() { c.save(); }
    @Override public void restore() { c.restore(); }
    @Override public void translate(float x, float y) { c.translate(x, y); }
    @Override public void scale(float s) { c.scale(s, s); }
    @Override public void rotate(float radians) { c.rotate((float) Math.toDegrees(radians)); }

    @Override public void circle(float x, float y, float r, int col) { fill.setColor(col); c.drawCircle(x, y, r, fill); }
    @Override public void ring(float x, float y, float r, float w, int col) { stroke.setColor(col); stroke.setStrokeWidth(w); c.drawCircle(x, y, r, stroke); }
    @Override public void arc(float x, float y, float r, float start, float sweep, int col) {
        fill.setColor(col);
        rf.set(x - r, y - r, x + r, y + r);
        c.drawArc(rf, (float) Math.toDegrees(start), (float) Math.toDegrees(sweep), true, fill);
    }
    @Override public void rect(float l, float t, float r, float b, int col) { fill.setColor(col); c.drawRect(l, t, r, b, fill); }
    @Override public void roundRect(float l, float t, float r, float b, float rad, int col) { fill.setColor(col); c.drawRoundRect(l, t, r, b, rad, rad, fill); }
    @Override public void strokeRoundRect(float l, float t, float r, float b, float rad, float w, int col) {
        stroke.setColor(col); stroke.setStrokeWidth(w); c.drawRoundRect(l, t, r, b, rad, rad, stroke);
    }
    @Override public void line(float x1, float y1, float x2, float y2, float w, int col) {
        stroke.setColor(col); stroke.setStrokeWidth(w); c.drawLine(x1, y1, x2, y2, stroke);
    }
    private void buildPath(float[] pts, int n) {
        path.rewind();
        path.moveTo(pts[0], pts[1]);
        for (int i = 1; i < n; i++) path.lineTo(pts[i * 2], pts[i * 2 + 1]);
        path.close();
    }
    @Override public void poly(float[] pts, int n, int col) { buildPath(pts, n); fill.setColor(col); c.drawPath(path, fill); }
    @Override public void strokePoly(float[] pts, int n, float w, int col) { buildPath(pts, n); stroke.setColor(col); stroke.setStrokeWidth(w); c.drawPath(path, stroke); }
    @Override public void text(String s, float x, float y, float size, int col, int align, boolean b) {
        text.setTextSize(size);
        text.setColor(col);
        text.setTypeface(b ? bold : normal);
        text.setTextAlign(align == ALIGN_CENTER ? Paint.Align.CENTER : (align == ALIGN_RIGHT ? Paint.Align.RIGHT : Paint.Align.LEFT));
        c.drawText(s, x, y, text);
    }
    @Override public void image(Object img, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, float alpha, int mode, int tint) {
        if (!(img instanceof android.graphics.Bitmap)) return;
        srcR.set((int) sx, (int) sy, (int) (sx + sw), (int) (sy + sh));
        dstR.set(dx, dy, dx + dw, dy + dh);
        bmp.setAlpha((int) (Math.max(0f, Math.min(1f, alpha)) * 255));
        if (mode == TINT_NONE) bmp.setColorFilter(null);
        else {
            if (mode != lastMode || tint != lastTint) {
                filter = new android.graphics.PorterDuffColorFilter(tint, mode == TINT_FLASH ? android.graphics.PorterDuff.Mode.SRC_ATOP : android.graphics.PorterDuff.Mode.MULTIPLY);
                lastMode = mode; lastTint = tint;
            }
            bmp.setColorFilter(filter);
        }
        c.drawBitmap((android.graphics.Bitmap) img, srcR, dstR, bmp);
    }

    @Override public float textWidth(String s, float size, boolean b) {
        text.setTextSize(size); text.setTypeface(b ? bold : normal);
        return text.measureText(s);
    }
}
