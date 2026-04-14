package com.draw.ui.panel;

import com.draw.model.CanvasDocument;
import com.draw.model.Layer;
import com.draw.model.LayerModel;
import com.draw.util.DrawIcons;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows all layers with add/delete/move controls, opacity, and blend mode.
 */
public class LayerPanel extends JPanel {
    private final CanvasDocument doc;
    private final JPanel listPanel = new JPanel();
    private final List<LayerRow> rows = new ArrayList<>();
    private final JSlider opacitySlider = makeSlider();
    private final JComboBox<Layer.BlendMode> blendCombo = new JComboBox<>(Layer.BlendMode.values());

    public LayerPanel(CanvasDocument doc) {
        this.doc = doc;
        setBackground(new Color(0x252526));
        setLayout(new BorderLayout());

        // ---- Top controls ----
        JPanel topControls = new JPanel(new GridBagLayout());
        topControls.setBackground(new Color(0x252526));
        topControls.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(2, 0, 2, 4);

        GridBagConstraints vc = new GridBagConstraints();
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.weightx = 1.0;
        vc.insets = new Insets(2, 0, 2, 0);

        // Opacity row
        lc.gridx = 0; lc.gridy = 0; vc.gridx = 1; vc.gridy = 0;
        topControls.add(smallLabel("Opacity"), lc);
        topControls.add(opacitySlider, vc);

        // Blend mode row
        lc.gridy = 1; vc.gridy = 1;
        topControls.add(smallLabel("Blend"), lc);
        styleCombo(blendCombo);
        topControls.add(blendCombo, vc);

        add(topControls, BorderLayout.NORTH);

        // ---- Layer list ----
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(new Color(0x252526));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBackground(new Color(0x252526));
        scroll.getViewport().setBackground(new Color(0x252526));
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0x3c3c3c)));
        add(scroll, BorderLayout.CENTER);

        // ---- Buttons ----
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        btnPanel.setBackground(new Color(0x252526));
        Color ic = new Color(0xAAAAAA);
        JButton addBtn  = iconBtn(DrawIcons.layerAdd(ic), "Add Layer");
        JButton delBtn  = iconBtn(DrawIcons.layerDelete(ic), "Delete Layer");
        JButton upBtn   = textBtn("▲", "Move Up");
        JButton dnBtn   = textBtn("▼", "Move Down");
        JButton dupBtn  = textBtn("⎘", "Duplicate Layer");
        btnPanel.add(addBtn); btnPanel.add(delBtn);
        btnPanel.add(Box.createHorizontalStrut(6));
        btnPanel.add(upBtn); btnPanel.add(dnBtn);
        btnPanel.add(Box.createHorizontalStrut(6));
        btnPanel.add(dupBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // ---- Listeners ----
        addBtn.addActionListener(e -> {
            String name = "Layer " + (doc.getLayerModel().getLayers().size() + 1);
            doc.getLayerModel().addLayer(new Layer(name, doc.getWidth(), doc.getHeight()));
            rebuild();
        });

        delBtn.addActionListener(e -> {
            doc.getLayerModel().deleteLayer(doc.getLayerModel().getActiveIndex());
            rebuild();
        });

        upBtn.addActionListener(e -> {
            doc.getLayerModel().moveLayerUp(doc.getLayerModel().getActiveIndex());
            rebuild();
        });

        dnBtn.addActionListener(e -> {
            doc.getLayerModel().moveLayerDown(doc.getLayerModel().getActiveIndex());
            rebuild();
        });

        dupBtn.addActionListener(e -> {
            Layer active = doc.getLayerModel().getActive();
            if (active != null) {
                Layer dup = active.snapshot();
                dup.setName(active.getName() + " copy");
                doc.getLayerModel().addLayer(dup);
                rebuild();
            }
        });

        opacitySlider.addChangeListener(e -> {
            Layer l = doc.getLayerModel().getActive();
            if (l != null) {
                l.setOpacity(opacitySlider.getValue() / 100f);
                doc.getLayerModel().fireLayersChanged();
                refreshThumbnails();
            }
        });

        blendCombo.addActionListener(e -> {
            Layer l = doc.getLayerModel().getActive();
            if (l != null) {
                l.setBlendMode((Layer.BlendMode) blendCombo.getSelectedItem());
                doc.getLayerModel().fireLayersChanged();
            }
        });

        doc.getLayerModel().addPropertyChangeListener(e -> {
            if ("layers".equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(this::refreshThumbnails);
            }
        });

        rebuild();
    }

    public void rebuild() {
        listPanel.removeAll();
        rows.clear();
        LayerModel model = doc.getLayerModel();
        List<Layer> layers = model.getLayers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            final int idx = i;
            Layer layer = layers.get(i);
            LayerRow row = new LayerRow(layer);
            row.setSelected(idx == model.getActiveIndex());
            row.setOnSelect(() -> {
                model.setActiveIndex(idx);
                updateSelection();
                syncControls();
            });
            row.setOnChange(() -> doc.getLayerModel().fireLayersChanged());
            listPanel.add(row);
            rows.add(row);
        }
        listPanel.revalidate();
        listPanel.repaint();
        syncControls();
    }

    private void updateSelection() {
        LayerModel model = doc.getLayerModel();
        int active = model.getActiveIndex();
        for (LayerRow row : rows) {
            row.setSelected(row.getLayer() == model.getLayers().get(active));
        }
    }

    private void syncControls() {
        Layer active = doc.getLayerModel().getActive();
        if (active != null) {
            opacitySlider.setValue((int)(active.getOpacity() * 100));
            blendCombo.setSelectedItem(active.getBlendMode());
        }
    }

    private void refreshThumbnails() {
        for (LayerRow row : rows) row.updateThumbnail();
    }

    // ---- Helpers ----

    private static JSlider makeSlider() {
        JSlider s = new JSlider(0, 100, 100);
        s.setBackground(new Color(0x252526));
        s.setFocusable(false);
        return s;
    }

    private static JLabel smallLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(new Color(0xAAAAAA));
        l.setFont(l.getFont().deriveFont(11f));
        return l;
    }

    private static JButton iconBtn(Icon icon, String tip) {
        JButton b = new JButton(icon);
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBackground(new Color(0x3c3c3c));
        b.setPreferredSize(new Dimension(28, 24));
        return b;
    }

    private static JButton textBtn(String text, String tip) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        b.setBackground(new Color(0x3c3c3c));
        b.setForeground(new Color(0xCCCCCC));
        b.setFont(b.getFont().deriveFont(11f));
        b.setPreferredSize(new Dimension(28, 24));
        return b;
    }

    private static void styleCombo(JComboBox<?> c) {
        c.setBackground(new Color(0x3c3c3c));
        c.setForeground(new Color(0xCCCCCC));
        c.setFont(c.getFont().deriveFont(11f));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
    }
}
