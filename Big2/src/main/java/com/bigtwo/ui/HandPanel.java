package com.bigtwo.ui;

import com.bigtwo.model.Card;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class HandPanel extends JPanel {
    private static final int OVERLAP = 46;
    private final List<CardPanel> cardPanels = new ArrayList<>();
    private final boolean interactive;
    private Runnable onSelectionChange;

    public HandPanel(boolean interactive) {
        this.interactive = interactive;
        setOpaque(false);
        setLayout(null);
    }

    public void setCards(List<Card> cards, boolean faceDown) {
        removeAll();
        cardPanels.clear();

        for (int i = 0; i < cards.size(); i++) {
            final int idx = i;
            CardPanel cp = new CardPanel(cards.get(i), faceDown);
            cardPanels.add(cp);
            add(cp);

            // Right cards on top: z-order 0 = topmost, so card 0 (leftmost) gets highest index
            setComponentZOrder(cp, 0);

            if (interactive && !faceDown) {
                cp.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        cardPanels.get(idx).setSelected(!cardPanels.get(idx).isSelected());
                        if (onSelectionChange != null) onSelectionChange.run();
                    }
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cardPanels.get(idx).setHovered(true);
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        cardPanels.get(idx).setHovered(false);
                        setCursor(Cursor.getDefaultCursor());
                    }
                });
            }
        }
        revalidate();
        repaint();
    }

    public void setOnSelectionChange(Runnable r) {
        this.onSelectionChange = r;
    }

    public List<Card> getSelectedCards() {
        List<Card> selected = new ArrayList<>();
        for (CardPanel cp : cardPanels) {
            if (cp.isSelected()) selected.add(cp.getCard());
        }
        return selected;
    }

    public void clearSelection() {
        for (CardPanel cp : cardPanels) cp.setSelected(false);
        if (onSelectionChange != null) onSelectionChange.run();
    }

    @Override
    public Dimension getPreferredSize() {
        int n = cardPanels.size();
        int totalH = CardPanel.CARD_HEIGHT + CardPanel.SHADOW + CardPanel.MAX_LIFT + 4;
        if (n == 0) return new Dimension(200, totalH);
        int width = OVERLAP * (n - 1) + CardPanel.CARD_WIDTH + CardPanel.SHADOW + 8;
        return new Dimension(width, totalH);
    }

    @Override
    public void doLayout() {
        int n = cardPanels.size();
        int cardH = CardPanel.CARD_HEIGHT + CardPanel.SHADOW + CardPanel.MAX_LIFT;
        for (int i = 0; i < n; i++) {
            CardPanel cp = cardPanels.get(i);
            cp.setBounds(i * OVERLAP, 0, CardPanel.CARD_WIDTH + CardPanel.SHADOW, cardH);
        }
    }
}
