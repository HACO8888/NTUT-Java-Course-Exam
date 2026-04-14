package com.draw.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LayerModel {
    private final List<Layer> layers = new ArrayList<>();
    private int activeIndex = 0;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public LayerModel(int width, int height) {
        layers.add(new Layer("Background", width, height));
    }

    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public int getActiveIndex() { return activeIndex; }

    public Layer getActive() {
        if (layers.isEmpty()) return null;
        return layers.get(activeIndex);
    }

    public void setActiveIndex(int index) {
        if (index >= 0 && index < layers.size()) {
            int old = this.activeIndex;
            this.activeIndex = index;
            pcs.firePropertyChange("activeIndex", old, index);
        }
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
        activeIndex = layers.size() - 1;
        pcs.firePropertyChange("layers", null, layers);
    }

    public void addLayerAt(int index, Layer layer) {
        layers.add(index, layer);
        pcs.firePropertyChange("layers", null, layers);
    }

    public void deleteLayer(int index) {
        if (layers.size() <= 1) return;
        layers.remove(index);
        if (activeIndex >= layers.size()) {
            activeIndex = layers.size() - 1;
        }
        pcs.firePropertyChange("layers", null, layers);
    }

    public void moveLayerUp(int index) {
        if (index < layers.size() - 1) {
            Collections.swap(layers, index, index + 1);
            if (activeIndex == index) activeIndex = index + 1;
            else if (activeIndex == index + 1) activeIndex = index;
            pcs.firePropertyChange("layers", null, layers);
        }
    }

    public void moveLayerDown(int index) {
        if (index > 0) {
            Collections.swap(layers, index, index - 1);
            if (activeIndex == index) activeIndex = index - 1;
            else if (activeIndex == index - 1) activeIndex = index;
            pcs.firePropertyChange("layers", null, layers);
        }
    }

    public void fireLayersChanged() {
        pcs.firePropertyChange("layers", null, layers);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
