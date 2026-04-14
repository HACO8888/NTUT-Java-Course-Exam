package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Click to add vertices; double-click to close and commit the polygon.
 */
public class PolygonTool implements Tool {
    private final List<Point> vertices = new ArrayList<>();
    private Point mousePos;
    private BufferedImage beforeSnapshot;
    private boolean filled = false;

    public void setFilled(boolean filled) { this.filled = filled; }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        if (ctx.clickCount >= 2 && vertices.size() >= 2) {
            commitPolygon(ctx);
            return;
        }

        if (vertices.isEmpty()) {
            Layer layer = ctx.doc.getLayerModel().getActive();
            if (layer != null) beforeSnapshot = DrawStrokeCommand.snapshot(layer);
        }
        vertices.add(p);
        mousePos = p;
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) { mousePos = p; }

    @Override
    public void onRelease(Point p, DrawContext ctx) { mousePos = p; }

    private void commitPolygon(DrawContext ctx) {
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null || vertices.size() < 2) { reset(); return; }

        int[] xs = vertices.stream().mapToInt(pt -> pt.x).toArray();
        int[] ys = vertices.stream().mapToInt(pt -> pt.y).toArray();

        Graphics2D g2 = layer.getImage().createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity));
        if (filled) g2.fillPolygon(xs, ys, xs.length);
        else g2.drawPolygon(xs, ys, xs.length);
        g2.dispose();

        BufferedImage after = DrawStrokeCommand.snapshot(layer);
        ctx.doc.getHistory().push(new DrawStrokeCommand(layer, beforeSnapshot, after));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        reset();
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        Graphics2D g2 = overlay.createGraphics();
        clearOverlay(g2, overlay);
        if (vertices.isEmpty()) { g2.dispose(); return; }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity * 0.8f));

        // Draw existing edges
        for (int i = 1; i < vertices.size(); i++) {
            Point a = vertices.get(i - 1), b = vertices.get(i);
            g2.drawLine(a.x, a.y, b.x, b.y);
        }
        // Draw tentative edge to mouse
        if (mousePos != null && !vertices.isEmpty()) {
            Point last = vertices.get(vertices.size() - 1);
            float[] dash = {4f, 4f};
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, dash, 0f));
            g2.drawLine(last.x, last.y, mousePos.x, mousePos.y);
        }
        // Draw vertex handles
        g2.setStroke(new BasicStroke(1.5f));
        for (Point v : vertices) {
            g2.setColor(Color.WHITE);
            g2.fillOval(v.x - 3, v.y - 3, 6, 6);
            g2.setColor(ctx.color);
            g2.drawOval(v.x - 3, v.y - 3, 6, 6);
        }
        g2.dispose();
    }

    @Override
    public void cancel(DrawContext ctx) { reset(); }

    private void reset() { vertices.clear(); mousePos = null; beforeSnapshot = null; }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Polygon"; }

    private void clearOverlay(Graphics2D g2, BufferedImage overlay) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    }
}
