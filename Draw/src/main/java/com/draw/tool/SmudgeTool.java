package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Smudge tool — smears pixels by carrying a "finger buffer" of sampled color
 * across the canvas. Opacity controls smear strength; higher = stronger smear.
 */
public class SmudgeTool implements Tool {

    private BufferedImage beforeSnapshot;
    private Layer activeLayer;
    private Point lastPoint;
    private int[] fingerBuffer;   // sampled pixels under brush
    private int bufferDiameter;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;
        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);
        lastPoint = p;
        sampleFinger(p, ctx);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (activeLayer == null || lastPoint == null) return;
        float dx = p.x - lastPoint.x;
        float dy = p.y - lastPoint.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int)(dist / Math.max(1f, ctx.size * 0.2f)));
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            int ix = (int)(lastPoint.x + dx * t);
            int iy = (int)(lastPoint.y + dy * t);
            applySmudge(new Point(ix, iy), ctx);
        }
        lastPoint = p;
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (activeLayer == null) return;
        BufferedImage after = DrawStrokeCommand.snapshot(activeLayer);
        ctx.doc.getHistory().push(new DrawStrokeCommand(activeLayer, beforeSnapshot, after));
        beforeSnapshot = null;
        activeLayer = null;
        lastPoint = null;
        fingerBuffer = null;
        ctx.doc.markDirty();
    }

    @Override
    public void cancel(DrawContext ctx) {
        if (activeLayer != null && beforeSnapshot != null) {
            activeLayer.restoreFrom(new Layer("snap", beforeSnapshot));
            ctx.doc.getLayerModel().fireLayersChanged();
        }
        beforeSnapshot = null;
        activeLayer = null;
        lastPoint = null;
        fingerBuffer = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    /** Sample canvas pixels under the brush into the finger buffer. */
    private void sampleFinger(Point p, DrawContext ctx) {
        int radius = Math.max(1, (int)(ctx.size / 2));
        int diameter = radius * 2;
        bufferDiameter = diameter;
        fingerBuffer = new int[diameter * diameter];

        BufferedImage img = activeLayer.getImage();
        int imgW = img.getWidth(), imgH = img.getHeight();

        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                int px = p.x - radius + x;
                int py = p.y - radius + y;
                fingerBuffer[y * diameter + x] =
                        (px >= 0 && px < imgW && py >= 0 && py < imgH)
                        ? img.getRGB(px, py) : 0;
            }
        }
    }

    private void applySmudge(Point p, DrawContext ctx) {
        if (fingerBuffer == null || activeLayer == null) return;
        int radius = Math.max(1, (int)(ctx.size / 2));
        int diameter = radius * 2;
        BufferedImage img = activeLayer.getImage();
        int imgW = img.getWidth(), imgH = img.getHeight();
        float strength = Math.min(1f, ctx.opacity);

        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                double ddx = x - radius;
                double ddy = y - radius;
                double dist = Math.sqrt(ddx * ddx + ddy * ddy) / radius;
                if (dist > 1.0) continue;

                float falloff = (float)(1.0 - dist * dist); // smooth falloff
                float alpha = falloff * strength;
                if (alpha < 0.01f) continue;

                int cpx = p.x - radius + x;
                int cpy = p.y - radius + y;
                if (cpx < 0 || cpx >= imgW || cpy < 0 || cpy >= imgH) continue;

                int bufIdx = y * diameter + x;
                if (bufIdx < 0 || bufIdx >= fingerBuffer.length) continue;

                int canvasPixel = img.getRGB(cpx, cpy);
                int fingerPixel = fingerBuffer[bufIdx];

                // Paint finger color onto canvas
                int newCanvas = lerpPixel(canvasPixel, fingerPixel, alpha);
                img.setRGB(cpx, cpy, newCanvas);

                // Finger picks up a bit of the original canvas color
                fingerBuffer[bufIdx] = lerpPixel(fingerPixel, canvasPixel, 0.2f * falloff);
            }
        }
    }

    private static int lerpPixel(int a, int b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        int aa = (a >>> 24) & 0xFF, ba = (b >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF, br = (b >> 16) & 0xFF;
        int ag = (a >> 8)  & 0xFF, bg = (b >> 8)  & 0xFF;
        int ab =  a        & 0xFF, bb =  b        & 0xFF;
        int ti = (int)(t * 256);
        int ra = aa + ((ba - aa) * ti >> 8);
        int rr = ar + ((br - ar) * ti >> 8);
        int rg = ag + ((bg - ag) * ti >> 8);
        int rb = ab + ((bb - ab) * ti >> 8);
        return (clamp(ra) << 24) | (clamp(rr) << 16) | (clamp(rg) << 8) | clamp(rb);
    }

    private static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Smudge"; }
}
