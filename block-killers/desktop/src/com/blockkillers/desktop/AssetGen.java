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

    /** Zombie head in a red crosshair (original design). */
    static void drawEmblem(Gfx g, float cx, float cy, float s) {
        float r = s * 0.2f;
        g.circle(cx, cy + r * 0.15f, r * 1.05f, 0xFF8FA37A);                     // head
        g.arc(cx, cy + r * 0.15f, r * 1.07f, (float) Math.PI, (float) Math.PI, 0xFF6E7F5C); // scalp shadow
        g.circle(cx - r * 0.38f, cy + r * 0.05f, r * 0.2f, 0xFFFFE066);          // eyes
        g.circle(cx + r * 0.38f, cy + r * 0.05f, r * 0.2f, 0xFFFFE066);
        g.rect(cx - r * 0.42f, cy + r * 0.55f, cx + r * 0.42f, cy + r * 0.72f, 0xFF3D0507); // mouth
        for (int i = -1; i <= 1; i++) g.rect(cx + i * r * 0.25f - r * 0.05f, cy + r * 0.55f, cx + i * r * 0.25f + r * 0.05f, cy + r * 0.66f, 0xFFE0E0C0);
        g.circle(cx + r * 0.55f, cy - r * 0.45f, r * 0.18f, 0xFF7A0E12);         // wound
        float cr = s * 0.32f;
        g.ring(cx, cy, cr, s * 0.025f, 0xFFE53935);                               // crosshair
        g.line(cx - cr * 1.25f, cy, cx - cr * 0.7f, cy, s * 0.025f, 0xFFE53935);
        g.line(cx + cr * 0.7f, cy, cx + cr * 1.25f, cy, s * 0.025f, 0xFFE53935);
        g.line(cx, cy - cr * 1.25f, cx, cy - cr * 0.7f, s * 0.025f, 0xFFE53935);
        g.line(cx, cy + cr * 0.7f, cx, cy + cr * 1.25f, s * 0.025f, 0xFFE53935);
    }

    static BufferedImage icon(int px, boolean round) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        DesktopGfx g = new DesktopGfx(gr, px, px);
        float m = px * 0.04f;
        if (round) g.circle(px / 2f, px / 2f, px / 2f - m, 0xFF101A10);
        else g.roundRect(m, m, px - m, px - m, px * 0.18f, 0xFF101A10);
        if (round) g.ring(px / 2f, px / 2f, px / 2f - m - px * 0.01f, px * 0.02f, 0x66E53935);
        else g.strokeRoundRect(m + px * 0.01f, m + px * 0.01f, px - m - px * 0.01f, px - m - px * 0.01f, px * 0.17f, px * 0.02f, 0x66E53935);
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
        g.text("ZOMBIE", w / 2f + 3, 400 + 3, 110, 0xFF000000, Gfx.ALIGN_CENTER, true);
        g.text("ZOMBIE", w / 2f, 400, 110, 0xFF8BC34A, Gfx.ALIGN_CENTER, true);
        g.text("KILLER", w / 2f + 3, 510 + 3, 110, 0xFF000000, Gfx.ALIGN_CENTER, true);
        g.text("KILLER", w / 2f, 510, 110, 0xFFE53935, Gfx.ALIGN_CENTER, true);
        gr.dispose();
        return img;
    }
}
