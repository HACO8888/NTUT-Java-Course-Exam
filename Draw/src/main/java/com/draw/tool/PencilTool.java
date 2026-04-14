package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class PencilTool implements Tool {
    private GeneralPath path;
    private BufferedImage beforeSnapshot;
    private Graphics2D layerG2;
    private Layer activeLayer;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;

        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);

        layerG2 = activeLayer.getImage().createGraphics();
        setHints(layerG2);
        layerG2.setColor(ctx.color);
        layerG2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        layerG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity));

        path = new GeneralPath();
        path.moveTo(p.x, p.y);

        // Draw a dot on click
        layerG2.fillOval((int)(p.x - ctx.size / 2), (int)(p.y - ctx.size / 2),
                (int)ctx.size, (int)ctx.size);
        ctx.doc.getLayerModel().fireLayersChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (layerG2 == null) return;
        path.lineTo(p.x, p.y);
        layerG2.draw(path);
        path = new GeneralPath();
        path.moveTo(p.x, p.y);
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
        path = null;
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
        path = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        // Pencil draws directly on layer; no overlay needed
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "Pencil"; }

    private void setHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
