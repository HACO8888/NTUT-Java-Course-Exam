package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Soft brush tool that simulates pressure via velocity-based alpha.
 *
 * Fixes applied:
 *  - Stamp image is reused (cached by size) to avoid per-stamp allocation
 *  - Hardness formula corrected: higher hardness = harder edge
 *  - cancel() restores pre-stroke snapshot
 */
public class BrushTool implements Tool {
    private BufferedImage beforeSnapshot;
    private Graphics2D layerG2;
    private Point lastPoint;
    private Layer activeLayer;
    private static final float MAX_VELOCITY = 30f;

    // Cached stamp to avoid per-stamp BufferedImage allocation
    private BufferedImage cachedStamp;
    private int cachedRadius = -1;
    private float cachedHardness = -1f;
    private int cachedColorRGB = 0;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;
        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);

        layerG2 = activeLayer.getImage().createGraphics();
        setHints(layerG2);
        lastPoint = p;
        stampBrush(p, ctx, 1.0f);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (layerG2 == null || lastPoint == null) return;
        float dx = p.x - lastPoint.x;
        float dy = p.y - lastPoint.y;
        float velocity = (float) Math.sqrt(dx * dx + dy * dy);
        float pressureAlpha = Math.max(0.2f, 1.0f - velocity / MAX_VELOCITY);

        int steps = Math.max(1, (int)(velocity / Math.max(1f, ctx.size * 0.3f)));
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            int ix = (int)(lastPoint.x + dx * t);
            int iy = (int)(lastPoint.y + dy * t);
            stampBrush(new Point(ix, iy), ctx, pressureAlpha);
        }

        lastPoint = p;
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (layerG2 == null) return;
        layerG2.dispose();
        layerG2 = null;

        if (activeLayer != null && beforeSnapshot != null) {
            BufferedImage after = DrawStrokeCommand.snapshot(activeLayer);
            ctx.doc.getHistory().push(new DrawStrokeCommand(activeLayer, beforeSnapshot, after));
        }
        beforeSnapshot = null;
        activeLayer = null;
        ctx.doc.markDirty();
    }

    @Override
    public void cancel(DrawContext ctx) {
        if (layerG2 != null) { layerG2.dispose(); layerG2 = null; }
        if (activeLayer != null && beforeSnapshot != null) {
            // Restore the layer to its pre-stroke state
            activeLayer.restoreFrom(beforeSnapshot != null
                    ? new com.draw.model.Layer("snap", beforeSnapshot) : null);
            ctx.doc.getLayerModel().fireLayersChanged();
        }
        beforeSnapshot = null;
        activeLayer = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    private void stampBrush(Point p, DrawContext ctx, float pressureAlpha) {
        int radius = Math.max(1, (int)(ctx.size / 2));
        int colorRGB = ctx.color.getRGB() & 0x00FFFFFF;

        // Rebuild stamp only when parameters change
        if (cachedStamp == null || cachedRadius != radius
                || Math.abs(cachedHardness - ctx.hardness) > 0.001f
                || cachedColorRGB != colorRGB) {

            int diameter = radius * 2;
            cachedStamp = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);

            // Corrected hardness: hardness=1 → hard edge, hardness=0 → very soft
            // sigma controls gaussian width: small sigma = hard, large = soft
            double sigma = (1.0 - ctx.hardness) * 0.5 + 0.01; // range [0.01, 0.51]

            for (int y = 0; y < diameter; y++) {
                for (int x = 0; x < diameter; x++) {
                    double dx = x - radius;
                    double dy = y - radius;
                    double dist = Math.sqrt(dx * dx + dy * dy) / radius; // 0..1
                    if (dist > 1.0) continue;
                    double falloff = Math.exp(-(dist * dist) / (2 * sigma * sigma));
                    // For hardness=1, clamp to binary (full alpha within circle)
                    if (ctx.hardness >= 0.99f) falloff = 1.0;
                    int alpha = Math.min(255, (int)(255 * falloff));
                    cachedStamp.setRGB(x, y, (alpha << 24) | colorRGB);
                }
            }
            cachedRadius = radius;
            cachedHardness = ctx.hardness;
            cachedColorRGB = colorRGB;
        }

        // Apply stamp with pressure + tool opacity
        float alpha = Math.min(1f, ctx.opacity * pressureAlpha);
        layerG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        layerG2.drawImage(cachedStamp, p.x - radius, p.y - radius, null);
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Brush"; }

    private void setHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
