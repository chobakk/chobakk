package com.blockkillers.desktop;

import com.blockkillers.core.Gfx;
import com.blockkillers.core.Shapes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Generates the legacy launcher icons (API 24-25) and the splash logo from code. */
public final class AssetGen {
    public static void main(String[] args) throws Exception {
        File res = new File(args.length > 0 ? args[0] : "app/src/main/res");
        String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] sizes = {48, 72, 96, 144, 192};
        for (int i = 0; i < dens.length; i++) {
            File dir = new File(res, "mipmap-" + dens[i]);
            dir.mkdirs();
            ImageIO.write(icon(sizes[i], false), "png", new File(dir, "ic_launcher.png"));
            ImageIO.write(icon(sizes[i], true), "png", new File(dir, "ic_launcher_round.png"));
        }
        File dx = new File(res, "drawable-xxhdpi");
        dx.mkdirs();
        ImageIO.write(splash(), "png", new File(dx, "splash_logo.png"));
        ImageIO.write(icon(512, false), "png", new File("docs/icon-512.png"));
        System.out.println("assets written to " + res);
    }

    static void drawEmblem(Gfx g, float cx, float cy, float s) {
        Shapes.draw(g, Shapes.SQUARE, cx - 0.30f * s, cy - 0.24f * s, 0.11f * s, 0.6f, 0xFFFF3D6E);
        Shapes.draw(g, Shapes.TRIANGLE, cx + 0.28f * s, cy - 0.26f * s, 0.12f * s, 0f, 0xFFFFB300);
        Shapes.draw(g, Shapes.PENTAGON, cx, cy + 0.30f * s, 0.11f * s, 0f, 0xFF9D6BFF);
        g.line(cx + 0.06f * s, cy - 0.07f * s, cx + 0.15f * s, cy - 0.16f * s, 0.03f * s, 0xFF80D8FF);
        g.line(cx - 0.07f * s, cy - 0.07f * s, cx - 0.14f * s, cy - 0.15f * s, 0.03f * s, 0xFF80D8FF);
        g.circle(cx, cy, 0.17f * s, 0x5540C4FF);
        g.circle(cx, cy, 0.12f * s, 0xFF40C4FF);
        g.circle(cx, cy, 0.07f * s, 0xFFFFFFFF);
    }

    static BufferedImage icon(int px, boolean round) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        DesktopGfx g = new DesktopGfx(gr, px, px);
        float m = px * 0.04f;
        if (round) g.circle(px / 2f, px / 2f, px / 2f - m, 0xFF0B1022);
        else g.roundRect(m, m, px - m, px - m, px * 0.18f, 0xFF0B1022);
        if (round) g.ring(px / 2f, px / 2f, px / 2f - m - px * 0.01f, px * 0.02f, 0x6618FFFF);
        else g.strokeRoundRect(m + px * 0.01f, m + px * 0.01f, px - m - px * 0.01f, px - m - px * 0.01f, px * 0.17f, px * 0.02f, 0x6618FFFF);
        drawEmblem(g, px / 2f, px / 2f, px * 1.25f);
        gr.dispose();
        return img;
    }

    static BufferedImage splash() {
        int w = 720, h = 560;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        DesktopGfx g = new DesktopGfx(gr, w, h);
        drawEmblem(g, w / 2f, 170, 420);
        g.text("BLOCK", w / 2f + 3, 400 + 3, 110, 0xFF000000, Gfx.ALIGN_CENTER, true);
        g.text("BLOCK", w / 2f, 400, 110, 0xFF18FFFF, Gfx.ALIGN_CENTER, true);
        g.text("KILLERS", w / 2f + 3, 510 + 3, 110, 0xFF000000, Gfx.ALIGN_CENTER, true);
        g.text("KILLERS", w / 2f, 510, 110, 0xFFFF4081, Gfx.ALIGN_CENTER, true);
        gr.dispose();
        return img;
    }
}
