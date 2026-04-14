package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Airbrush / spray tool. Scatters random dots in a circle around the pointer.
 */
public class SprayTool implements Tool {
    private BufferedImage beforeSnapshot;
    private Graphics2D layerG2;
    private final Random rng = new Random();
    private boolean painting = false;
    private Layer activeLayer;

    /** Dots per event (higher = denser spray). */
    private static final int DOTS_PER_EVENT = 12;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;
        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);
        layerG2 = activeLayer.getImage().createGraphics();
        layerG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        painting = true;
        spray(p, ctx);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (!painting || layerG2 == null) return;
        spray(p, ctx);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (!painting) return;
        painting = false;
        if (layerG2 != null) { layerG2.dispose(); layerG2 = null; }

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
        painting = false;
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

    private void spray(Point center, DrawContext ctx) {
        int radius = Math.max(5, (int)(ctx.size / 2));
        int dotSize = Math.max(1, (int)(ctx.size / 20));

        layerG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                ctx.opacity * 0.4f)); // each dot is semi-transparent
        layerG2.setColor(ctx.color);

        for (int i = 0; i < DOTS_PER_EVENT; i++) {
            // Random point inside circle using rejection sampling
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dist = Math.sqrt(rng.nextDouble()) * radius;
            int dx = (int)(Math.cos(angle) * dist);
            int dy = (int)(Math.sin(angle) * dist);
            layerG2.fillOval(center.x + dx - dotSize / 2,
                             center.y + dy - dotSize / 2,
                             dotSize, dotSize);
        }
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Spray"; }
}
