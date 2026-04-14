package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Linear gradient fill tool.
 * Drag from start to end point; on release applies a linear gradient
 * from the foreground (primary) color to the secondary color across the layer.
 */
public class GradientTool implements Tool {

    private Point startPoint;
    private Point endPoint;
    private BufferedImage beforeSnapshot;
    private Layer activeLayer;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        activeLayer = ctx.doc.getLayerModel().getActive();
        if (activeLayer == null) return;
        beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);
        startPoint = p;
        endPoint = p;
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (startPoint == null) return;
        endPoint = p;
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (startPoint == null || activeLayer == null) return;
        endPoint = p;

        if (startPoint.distance(endPoint) < 2) {
            reset();
            return;
        }

        applyGradient(ctx);
        ctx.doc.getHistory().push(new DrawStrokeCommand(activeLayer,
                beforeSnapshot, DrawStrokeCommand.snapshot(activeLayer)));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        reset();
    }

    @Override
    public void cancel(DrawContext ctx) { reset(); }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        Graphics2D g2 = overlay.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        if (startPoint != null && endPoint != null && !startPoint.equals(endPoint)) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw gradient preview line
            GradientPaint gp = new GradientPaint(
                    startPoint.x, startPoint.y, ctx.color,
                    endPoint.x, endPoint.y, ctx.secondaryColor);
            g2.setPaint(gp);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);

            // Start handle
            g2.setPaint(null);
            g2.setColor(ctx.color);
            g2.fillOval(startPoint.x - 6, startPoint.y - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(startPoint.x - 6, startPoint.y - 6, 12, 12);

            // End handle
            g2.setColor(ctx.secondaryColor);
            g2.fillOval(endPoint.x - 6, endPoint.y - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawOval(endPoint.x - 6, endPoint.y - 6, 12, 12);
        }
        g2.dispose();
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Gradient"; }

    private void applyGradient(DrawContext ctx) {
        int w = activeLayer.getImage().getWidth();
        int h = activeLayer.getImage().getHeight();

        Color c1 = new Color(ctx.color.getRed(), ctx.color.getGreen(), ctx.color.getBlue(),
                Math.max(0, Math.min(255, (int)(ctx.opacity * 255))));
        Color c2 = new Color(ctx.secondaryColor.getRed(), ctx.secondaryColor.getGreen(),
                ctx.secondaryColor.getBlue(),
                Math.max(0, Math.min(255, (int)(ctx.opacity * 255))));

        GradientPaint paint = new GradientPaint(
                startPoint.x, startPoint.y, c1,
                endPoint.x,   endPoint.y,   c2);

        Graphics2D g2 = activeLayer.getImage().createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    private void reset() {
        startPoint = null;
        endPoint = null;
        beforeSnapshot = null;
        activeLayer = null;
    }
}
