package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Click to place a text input, Enter to commit.
 */
public class TextTool implements Tool {
    private JTextField textField;
    private Point canvasPoint;
    private JComponent canvas;
    private DrawContext savedCtx;
    private BufferedImage beforeSnapshot;

    // Font settings (can be changed via ToolOptionsBar)
    private String fontName = "SansSerif";
    private int fontStyle = Font.PLAIN;

    public void setFontName(String name) { this.fontName = name; }
    public void setFontStyle(int style) { this.fontStyle = style; }
    public String getFontName() { return fontName; }
    public int getFontStyle() { return fontStyle; }

    public void setCanvasComponent(JComponent canvas) {
        this.canvas = canvas;
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        if (textField != null) commit(ctx);

        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;

        savedCtx = ctx;
        canvasPoint = p;
        beforeSnapshot = DrawStrokeCommand.snapshot(layer);

        if (canvas instanceof TextToolListener) {
            ((TextToolListener) canvas).showTextField(p, ctx, this);
        }
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {}

    @Override
    public void onRelease(Point p, DrawContext ctx) {}

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    public void commit(DrawContext ctx) {
        if (textField == null || canvasPoint == null) return;
        String text = textField.getText();
        dismissField();

        if (text == null || text.isEmpty()) { beforeSnapshot = null; return; }

        Layer layer = (ctx != null ? ctx : savedCtx).doc.getLayerModel().getActive();
        if (layer == null) { beforeSnapshot = null; return; }

        DrawContext dc = ctx != null ? ctx : savedCtx;
        Graphics2D g2 = layer.getImage().createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(dc.color);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dc.opacity));
        int fontSize = Math.max(8, (int)(dc.size * 3));
        g2.setFont(new Font(fontName, fontStyle, fontSize));
        g2.drawString(text, canvasPoint.x, canvasPoint.y);
        g2.dispose();

        BufferedImage after = DrawStrokeCommand.snapshot(layer);
        dc.doc.getHistory().push(new DrawStrokeCommand(layer, beforeSnapshot, after));
        dc.doc.getLayerModel().fireLayersChanged();
        dc.doc.markDirty();
        beforeSnapshot = null;
    }

    public void dismissField() {
        if (textField != null && canvas != null) {
            canvas.remove(textField);
            canvas.revalidate();
            canvas.repaint();
        }
        textField = null;
    }

    public void setTextField(JTextField field) { this.textField = field; }

    public interface TextToolListener {
        void showTextField(Point canvasPoint, DrawContext ctx, TextTool tool);
    }

    @Override
    public void cancel(DrawContext ctx) {
        dismissField();
        beforeSnapshot = null;
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); }

    @Override
    public String getName() { return "Text"; }
}
