package com.blockkillers.core;

/** Platform-independent drawing API. Implemented by Android Canvas and by Java2D (desktop tests). */
public interface Gfx {
    int ALIGN_LEFT = 0, ALIGN_CENTER = 1, ALIGN_RIGHT = 2;

    void clear(int argb);
    void save();
    void restore();
    void translate(float x, float y);
    void scale(float s);
    void rotate(float radians);

    void circle(float x, float y, float r, int c);
    void ring(float x, float y, float r, float width, int c);
    /** Filled pie slice; angles in radians, 0 = right, clockwise positive (screen coords). */
    void arc(float x, float y, float r, float start, float sweep, int c);
    void rect(float l, float t, float r, float b, int c);
    void roundRect(float l, float t, float r, float b, float rad, int c);
    void strokeRoundRect(float l, float t, float r, float b, float rad, float width, int c);
    void line(float x1, float y1, float x2, float y2, float width, int c);
    /** Filled polygon from pts = {x0,y0,x1,y1,...}; n = number of points. */
    void poly(float[] pts, int n, int c);
    void strokePoly(float[] pts, int n, float width, int c);
    void text(String s, float x, float y, float size, int c, int align, boolean bold);
    float textWidth(String s, float size, boolean bold);
    /** Image tint modes for {@link #image}. */
    int TINT_NONE = 0, TINT_MULTIPLY = 1, TINT_FLASH = 2;
    /** Draws a sub-rectangle of a platform image into a destination rectangle (current transform applies). */
    void image(Object img, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh, float alpha, int tintMode, int tint);
}
