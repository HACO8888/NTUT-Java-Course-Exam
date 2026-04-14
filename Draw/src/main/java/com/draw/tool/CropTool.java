package com.draw.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Crop tool — drag to define a crop rectangle.
 * Press Enter or click "Apply Crop" to crop all layers to the selection.
 * Overlay shows dimmed exterior + rule-of-thirds grid + corner handles.
 */
public class CropTool implements Tool {

    private Rectangle cropRect;
    private Point pressPoint;

    public Rectangle getCropRect() { return cropRect; }

    public void clearCrop() {
        cropRect = null;
        pressPoint = null;
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        pressPoint = p;
        cropRect = new Rectangle(p.x, p.y, 0, 0);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (pressPoint == null) return;
        int x = Math.min(pressPoint.x, p.x);
        int y = Math.min(pressPoint.y, p.y);
        int w = Math.abs(p.x - pressPoint.x);
        int h = Math.abs(p.y - pressPoint.y);
        cropRect = new Rectangle(x, y, w, h);
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        // crop rect is finalized by drag; user confirms with Enter or Apply button
        if (cropRect != null && (cropRect.width < 4 || cropRect.height < 4)) {
            cropRect = null;
        }
    }

    @Override
    public void cancel(DrawContext ctx) {
        cropRect = null;
        pressPoint = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        Graphics2D g2 = overlay.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        if (cropRect != null && cropRect.width > 1 && cropRect.height > 1) {
            int iw = overlay.getWidth(), ih = overlay.getHeight();

            // Dim area outside crop
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRect(0,                          0,                iw, cropRect.y);
            g2.fillRect(0,                          cropRect.y,       cropRect.x, cropRect.height);
            g2.fillRect(cropRect.x + cropRect.width, cropRect.y,      iw - cropRect.x - cropRect.width, cropRect.height);
            g2.fillRect(0,                          cropRect.y + cropRect.height, iw, ih - cropRect.y - cropRect.height);

            // Rule-of-thirds lines
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.setStroke(new BasicStroke(0.5f));
            int w3 = cropRect.width / 3, h3 = cropRect.height / 3;
            g2.drawLine(cropRect.x + w3,     cropRect.y, cropRect.x + w3,     cropRect.y + cropRect.height);
            g2.drawLine(cropRect.x + 2*w3,   cropRect.y, cropRect.x + 2*w3,   cropRect.y + cropRect.height);
            g2.drawLine(cropRect.x, cropRect.y + h3,     cropRect.x + cropRect.width, cropRect.y + h3);
            g2.drawLine(cropRect.x, cropRect.y + 2*h3,   cropRect.x + cropRect.width, cropRect.y + 2*h3);

            // Crop border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(cropRect.x, cropRect.y, cropRect.width, cropRect.height);

            // Corner handles
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int hs = 10;
            int cx = cropRect.x, cy = cropRect.y, cw = cropRect.width, ch = cropRect.height;
            // Top-left
            g2.drawLine(cx,        cy,        cx + hs,   cy);
            g2.drawLine(cx,        cy,        cx,        cy + hs);
            // Top-right
            g2.drawLine(cx+cw-hs,  cy,        cx + cw,   cy);
            g2.drawLine(cx + cw,   cy,        cx + cw,   cy + hs);
            // Bottom-left
            g2.drawLine(cx,        cy+ch-hs,  cx,        cy + ch);
            g2.drawLine(cx,        cy + ch,   cx + hs,   cy + ch);
            // Bottom-right
            g2.drawLine(cx+cw,     cy+ch-hs,  cx + cw,   cy + ch);
            g2.drawLine(cx+cw-hs,  cy + ch,   cx + cw,   cy + ch);
        }
        g2.dispose();
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Crop"; }
}
