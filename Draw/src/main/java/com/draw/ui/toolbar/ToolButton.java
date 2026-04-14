package com.draw.ui.toolbar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A toggle button with custom dark-theme styling for the tool bar.
 */
public class ToolButton extends JToggleButton {
    private static final Color BG_HOVER    = new Color(0x2a2d2e);
    private static final Color BG_SELECTED = new Color(0x094771);
    private static final Color ACCENT      = new Color(0x0078D4);
    private static final Color ICON_NORMAL = new Color(0xAAAAAA);
    private static final Color ICON_ACTIVE = new Color(0xFFFFFF);

    private boolean hovered = false;

    public ToolButton(String tooltip, Icon normalIcon) {
        super();
        setToolTipText(tooltip);
        setIcon(normalIcon);
        setSelectedIcon(tintIcon(normalIcon, ICON_ACTIVE));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setPreferredSize(new Dimension(44, 44));
        setMaximumSize(new Dimension(44, 44));
        setMinimumSize(new Dimension(44, 44));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int arc = 8;

        if (isSelected()) {
            g2.setColor(BG_SELECTED);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
            // Left accent bar
            g2.setColor(ACCENT);
            g2.fillRoundRect(0, 8, 3, h - 16, 3, 3);
        } else if (hovered) {
            g2.setColor(BG_HOVER);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private Icon tintIcon(Icon icon, Color tint) {
        // Icons are already color-appropriate; just return the same icon
        // For a proper tint we'd need to re-render, but since DrawIcons already
        // accepts a color we simply return the same icon here.
        return icon;
    }
}
