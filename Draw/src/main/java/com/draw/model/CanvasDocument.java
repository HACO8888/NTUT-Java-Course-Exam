package com.draw.model;

import com.draw.command.CommandHistory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class CanvasDocument {
    private int width;
    private int height;
    private final LayerModel layerModel;
    private final CommandHistory history;
    private boolean dirty;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public CanvasDocument(int width, int height) {
        this.width = width;
        this.height = height;
        this.layerModel = new LayerModel(width, height);
        this.history = new CommandHistory();
        this.dirty = false;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public LayerModel getLayerModel() { return layerModel; }
    public CommandHistory getHistory() { return history; }
    public boolean isDirty() { return dirty; }

    public void markDirty() {
        this.dirty = true;
        pcs.firePropertyChange("dirty", false, true);
    }

    public void markClean() {
        this.dirty = false;
    }

    public void resize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
        for (Layer layer : layerModel.getLayers()) {
            layer.resize(newWidth, newHeight);
        }
        pcs.firePropertyChange("size", null, new int[]{newWidth, newHeight});
    }

    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }
}
