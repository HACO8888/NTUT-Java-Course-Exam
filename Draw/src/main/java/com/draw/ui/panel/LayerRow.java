package com.draw.ui.panel;

import com.draw.model.Layer;
import com.draw.util.DrawIcons;
import com.draw.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A single row in the layers panel.
 */
public class LayerRow extends JPanel {
    private final Layer layer;
    private final JToggleButton visBtn;
    private final JLabel nameLabel;
    private final JLabel thumbnailLabel;
    private boolean selected;
    private Runnable onSelect;
    private Runnable onChange;

    private static final Color BG_NORMAL   = new Color(0x252526);
    private static final Color BG_SELECTED = new Color(0x094771);
    private static final Color BG_HOVER    = new Color(0x2a2d2e);

    public LayerRow(Layer layer) {
        this.layer = layer;
        setLayout(new BorderLayout(4, 0));
        setBackground(BG_NORMAL);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Visibility toggle
        visBtn = new JToggleButton(DrawIcons.eye(new Color(0xAAAAAA), true));
        visBtn.setSelectedIcon(DrawIcons.eye(new Color(0x555555), false));
        visBtn.setSelected(!layer.isVisible());
        visBtn.setFocusPainted(false);
        visBtn.setBorderPainted(false);
        visBtn.setContentAreaFilled(false);
        visBtn.setPreferredSize(new Dimension(24, 24));
        visBtn.addActionListener(e -> {
            layer.setVisible(!visBtn.isSelected());
            if (onChange != null) onChange.run();
        });
        add(visBtn, BorderLayout.WEST);

        // Thumbnail
        thumbnailLabel = new JLabel();
        thumbnailLabel.setPreferredSize(new Dimension(36, 36));
        thumbnailLabel.setBorder(BorderFactory.createLineBorder(new Color(0x555555)));
        updateThumbnail();
        add(thumbnailLabel, BorderLayout.EAST);

        // Name
        nameLabel = new JLabel(layer.getName());
        nameLabel.setForeground(new Color(0xCCCCCC));
        nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        add(nameLabel, BorderLayout.CENTER);

        // Double-click to rename
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) startRename();
                else { if (onSelect != null) onSelect.run(); }
            }
        });

        addMouseListener(new MouseAdapter() {
            boolean hovered = false;
            @Override public void mouseClicked(MouseEvent e) { if (onSelect != null) onSelect.run(); }
            @Override public void mouseEntered(MouseEvent e) { hovered = true; updateBg(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; updateBg(); }
            void updateBg() { setBackground(selected ? BG_SELECTED : hovered ? BG_HOVER : BG_NORMAL); }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setBackground(selected ? BG_SELECTED : BG_NORMAL);
    }

    public void setOnSelect(Runnable r) { this.onSelect = r; }
    public void setOnChange(Runnable r) { this.onChange = r; }

    public void updateThumbnail() {
        Image thumb = ImageUtil.scaledThumbnail(layer.getImage(), 36, 36);
        thumbnailLabel.setIcon(new ImageIcon(thumb));
    }

    private void startRename() {
        String newName = JOptionPane.showInputDialog(this, "Layer name:", layer.getName());
        if (newName != null && !newName.isBlank()) {
            layer.setName(newName);
            nameLabel.setText(newName);
        }
    }

    public Layer getLayer() { return layer; }
}
