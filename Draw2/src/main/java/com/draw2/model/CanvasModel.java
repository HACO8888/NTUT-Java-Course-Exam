package com.draw2.model;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayDeque;

public class CanvasModel {

    private static final int MAX_UNDO = 30;

    private BufferedImage canvas;
    private int width;
    private int height;
    private final ArrayDeque<BufferedImage> undoStack = new ArrayDeque<>();
    private final ArrayDeque<BufferedImage> redoStack = new ArrayDeque<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public CanvasModel(int width, int height) {
        this.width = width;
        this.height = height;
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        fillWhite(canvas);
    }

    // ── canvas access ────────────────────────────────────────────────────────

    public BufferedImage getCanvas() { return canvas; }
    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    // ── undo / redo ──────────────────────────────────────────────────────────

    public void pushUndo() {
        undoStack.push(snapshot(canvas));
        if (undoStack.size() > MAX_UNDO) undoStack.removeLast();
        redoStack.clear();
        pcs.firePropertyChange("history", null, null);
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.push(snapshot(canvas));
        canvas = undoStack.pop();
        width  = canvas.getWidth();
        height = canvas.getHeight();
        fireCanvasChanged();
        pcs.firePropertyChange("history", null, null);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.push(snapshot(canvas));
        canvas = redoStack.pop();
        width  = canvas.getWidth();
        height = canvas.getHeight();
        fireCanvasChanged();
        pcs.firePropertyChange("history", null, null);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    // ── image operations ─────────────────────────────────────────────────────

    public void clear() {
        fillWhite(canvas);
        fireCanvasChanged();
    }

    public void resize(int newW, int newH) {
        pushUndo();
        BufferedImage newCanvas = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        fillWhite(newCanvas);
        Graphics2D g2 = newCanvas.createGraphics();
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();
        canvas = newCanvas;
        width  = newW;
        height = newH;
        fireCanvasChanged();
        pcs.firePropertyChange("history", null, null);
    }

    public void rotateRight() {
        pushUndo();
        int newW = height, newH = width;
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        fillWhite(rotated);
        Graphics2D g2 = rotated.createGraphics();
        g2.translate(newW, 0);
        g2.rotate(Math.PI / 2);
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();
        canvas = rotated;
        width  = newW;
        height = newH;
        fireCanvasChanged();
    }

    public void rotateLeft() {
        pushUndo();
        int newW = height, newH = width;
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        fillWhite(rotated);
        Graphics2D g2 = rotated.createGraphics();
        g2.translate(0, newH);
        g2.rotate(-Math.PI / 2);
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();
        canvas = rotated;
        width  = newW;
        height = newH;
        fireCanvasChanged();
    }

    public void rotate180() {
        pushUndo();
        BufferedImage rotated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = rotated.createGraphics();
        g2.translate(width, height);
        g2.rotate(Math.PI);
        g2.drawImage(canvas, 0, 0, null);
        g2.dispose();
        canvas = rotated;
        fireCanvasChanged();
    }

    public void flipHorizontal() {
        pushUndo();
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = flipped.createGraphics();
        g2.drawImage(canvas, width, 0, -width, height, null);
        g2.dispose();
        canvas = flipped;
        fireCanvasChanged();
    }

    public void flipVertical() {
        pushUndo();
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = flipped.createGraphics();
        g2.drawImage(canvas, 0, height, width, -height, null);
        g2.dispose();
        canvas = flipped;
        fireCanvasChanged();
    }

    // ── property change ──────────────────────────────────────────────────────

    public void fireCanvasChanged() {
        pcs.firePropertyChange("canvas", null, canvas);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    public static BufferedImage snapshot(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return copy;
    }

    private static void fillWhite(BufferedImage img) {
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2.dispose();
    }
}
