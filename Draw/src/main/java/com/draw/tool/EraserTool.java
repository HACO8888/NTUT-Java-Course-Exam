package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EraserTool implements Tool {
    private BufferedImage beforeSnapshot;
    private Graphics2D layerG2;
    private Point lastPoint;
    private Layer activeLayer;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;
        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);

        layerG2 = activeLayer.getImage().createGraphics();
        setHints(layerG2);
        lastPoint = p;
        erase(p, ctx);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (layerG2 == null || lastPoint == null) return;
        // Interpolate for smooth erasing
        float dx = p.x - lastPoint.x;
        float dy = p.y - lastPoint.y;
        int steps = Math.max(1, (int) Math.sqrt(dx * dx + dy * dy));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            erase(new Point((int)(lastPoint.x + dx * t), (int)(lastPoint.y + dy * t)), ctx);
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
            activeLayer.restoreFrom(new Layer("snap", beforeSnapshot));
            ctx.doc.getLayerModel().fireLayersChanged();
        }
        beforeSnapshot = null;
        activeLayer = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    private void erase(Point p, DrawContext ctx) {
        int r = Math.max(1, (int)(ctx.size / 2));
        // DST_OUT punches transparent holes into the ARGB layer
        layerG2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, ctx.opacity));
        layerG2.fillOval(p.x - r, p.y - r, r * 2, r * 2);
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Eraser"; }

    private void setHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
