package com.draw2.tool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class TextTool implements Tool {

    private JTextField textField;
    private Point canvasPoint;
    private DrawContext pendingCtx;
    private JPanel canvasComponent;

    /** Must be called before the tool is used. */
    public void setCanvasComponent(JPanel panel) {
        this.canvasComponent = panel;
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        commitPending();
        ctx.model.pushUndo();
        canvasPoint = p;
        pendingCtx  = ctx;
        showTextField(p, ctx);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx)    {}
    @Override
    public void onRelease(Point p, DrawContext ctx) {}
    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    @Override
    public void onDeactivate() {
        commitPending();
    }

    private void showTextField(Point canvasP, DrawContext ctx) {
        if (canvasComponent == null) return;
        textField = new JTextField(20);
        textField.setFont(new Font("Dialog", Font.PLAIN, 16));
        textField.setForeground(ctx.foreColor);
        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createDashedBorder(Color.GRAY));

        // Convert canvas coords to screen coords via parent component
        // CanvasPanel handles the actual placement in screen space
        canvasComponent.putClientProperty("textField", textField);
        canvasComponent.putClientProperty("textPoint", canvasP);
        canvasComponent.putClientProperty("textCtx", ctx);
        canvasComponent.firePropertyChange("showTextField", false, true);

        textField.addActionListener(e -> commitPending());
        textField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitPending(); }
        });
    }

    public void commitPending() {
        if (textField == null || canvasPoint == null || pendingCtx == null) return;
        String text = textField.getText().trim();
        if (!text.isEmpty()) {
            Graphics2D g2 = pendingCtx.model.getCanvas().createGraphics();
            g2.setColor(pendingCtx.foreColor);
            g2.setFont(textField.getFont());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, canvasPoint.x, canvasPoint.y + fm.getAscent());
            g2.dispose();
            pendingCtx.model.fireCanvasChanged();
        }
        if (canvasComponent != null) {
            canvasComponent.firePropertyChange("hideTextField", false, true);
        }
        textField    = null;
        canvasPoint  = null;
        pendingCtx   = null;
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR); }

    @Override
    public String getName() { return "文字"; }
}
