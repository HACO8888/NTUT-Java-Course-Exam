package com.draw2.ui;

import com.draw2.tool.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ToolBar extends JPanel {

    // Ordered map: display name → tool instance
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    private final ButtonGroup group = new ButtonGroup();
    private Consumer<Tool> toolChangeCallback = t -> {};
    private Consumer<Integer> sizeChangeCallback = s -> {};

    private JToggleButton filledBtn;
    private JSpinner sizeSpinner;

    // Tool instances (owned here, shared with caller via setToolChangeCallback)
    public final PencilTool   pencil   = new PencilTool();
    public final BrushTool    brush    = new BrushTool();
    public final EraserTool   eraser   = new EraserTool();
    public final FillTool     fill     = new FillTool();
    public final LineTool     line     = new LineTool();
    public final RectangleTool rect    = new RectangleTool();
    public final EllipseTool  ellipse  = new EllipseTool();
    public final TextTool     text     = new TextTool();
    public final SprayTool    spray    = new SprayTool();
    public final EyedropperTool eyedropper = new EyedropperTool();

    public ToolBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(66, 0));

        // Register tools in classic Paint order
        tools.put("鉛筆",  pencil);
        tools.put("筆刷",  brush);
        tools.put("橡皮擦", eraser);
        tools.put("油漆桶", fill);
        tools.put("直線",  line);
        tools.put("矩形",  rect);
        tools.put("橢圓",  ellipse);
        tools.put("噴漆",  spray);
        tools.put("文字",  text);
        tools.put("滴管",  eyedropper);

        JPanel grid = new JPanel(new GridLayout(0, 2, 2, 2));
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        String[] icons = {"✏", "🖌", "⬜", "🪣", "╱", "▭", "⬭", "💨", "T", "💧"};
        int i = 0;
        for (Map.Entry<String, Tool> e : tools.entrySet()) {
            JToggleButton btn = makeToolButton(icons[i++], e.getKey(), e.getValue());
            grid.add(btn);
            if (e.getKey().equals("鉛筆")) btn.setSelected(true); // default
        }

        add(grid, BorderLayout.NORTH);

        // Options below the tool grid
        JPanel options = buildOptionsPanel();
        add(options, BorderLayout.CENTER);
    }

    private JToggleButton makeToolButton(String icon, String name, Tool tool) {
        JToggleButton btn = new JToggleButton(icon);
        btn.setToolTipText(name);
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setFont(btn.getFont().deriveFont(14f));
        btn.setFocusable(false);
        group.add(btn);
        btn.addActionListener(e -> toolChangeCallback.accept(tool));
        return btn;
    }

    private JPanel buildOptionsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("選項"));

        // Filled toggle (for Rect / Ellipse)
        filledBtn = new JToggleButton("填充");
        filledBtn.setFont(filledBtn.getFont().deriveFont(10f));
        filledBtn.setFocusable(false);
        filledBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        p.add(filledBtn);
        p.add(Box.createVerticalStrut(4));

        // Stroke size spinner
        p.add(new JLabel("大小"));
        sizeSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 30, 1));
        sizeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        sizeSpinner.addChangeListener(e ->
            sizeChangeCallback.accept((Integer) sizeSpinner.getValue()));
        p.add(sizeSpinner);

        return p;
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void setToolChangeCallback(Consumer<Tool> cb)  { toolChangeCallback  = cb; }
    public void setSizeChangeCallback(Consumer<Integer> cb){ sizeChangeCallback = cb; }

    public boolean isFilled() { return filledBtn.isSelected(); }

    public int getStrokeSize() { return (Integer) sizeSpinner.getValue(); }

    public void addFilledListener(Runnable cb) {
        filledBtn.addActionListener(e -> cb.run());
    }

    /** Programmatically select a tool button by tool name. */
    public void selectTool(String name) {
        // Iterate group buttons — not easy, so we re-trigger from the map
        tools.forEach((n, t) -> {
            if (n.equals(name)) toolChangeCallback.accept(t);
        });
    }
}
