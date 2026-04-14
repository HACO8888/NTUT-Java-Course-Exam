package com.draw.ui.toolbar;

import com.draw.tool.*;
import com.draw.tool.CropTool;
import com.draw.tool.SmudgeTool;
import com.draw.util.DrawIcons;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vertical tool selection bar on the left side.
 */
public class ToolBar extends JPanel {
    private final ButtonGroup group = new ButtonGroup();
    private Consumer<Tool> toolChangeCallback;

    // Tools
    private final RectSelectTool selectTool  = new RectSelectTool();
    private final PencilTool   pencilTool   = new PencilTool();
    private final BrushTool    brushTool    = new BrushTool();
    private final EraserTool   eraserTool   = new EraserTool();
    private final SprayTool    sprayTool    = new SprayTool();
    private final SmudgeTool   smudgeTool   = new SmudgeTool();
    private final CropTool     cropTool     = new CropTool();
    private final LineTool     lineTool     = new LineTool();
    private final RectangleTool rectTool    = new RectangleTool();
    private final EllipseTool  ellipseTool  = new EllipseTool();
    private final PolygonTool  polyTool     = new PolygonTool();
    private final GradientTool gradientTool = new GradientTool();
    private final FillTool     fillTool     = new FillTool();
    private final TextTool     textTool     = new TextTool();
    private EyedropperTool eyedropperTool;  // set externally

    /** Maps tool display name → (button, tool) for keyboard shortcut lookup. */
    private final Map<String, ToolEntry> toolEntries = new LinkedHashMap<>();

    private static final class ToolEntry {
        final ToolButton button;
        Tool tool;
        ToolEntry(ToolButton button, Tool tool) { this.button = button; this.tool = tool; }
    }

    public ToolBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0x252526));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x3c3c3c)));
        setPreferredSize(new Dimension(48, Integer.MAX_VALUE));

        add(Box.createVerticalStrut(8));

        Color ic = new Color(0xAAAAAA);
        // Selection / Move
        addEntry("Select",      "Select (V)",      DrawIcons.select(ic),    selectTool);
        addSep();
        // Drawing
        addEntry("Pencil",      "Pencil (P)",      DrawIcons.pencil(ic),    pencilTool);
        addEntry("Brush",       "Brush (B)",       DrawIcons.brush(ic),     brushTool);
        addEntry("Eraser",      "Eraser (E)",      DrawIcons.eraser(ic),    eraserTool);
        addEntry("Spray",       "Spray (S)",       DrawIcons.spray(ic),     sprayTool);
        addEntry("Smudge",      "Smudge (M)",      DrawIcons.smudge(ic),    smudgeTool);
        addSep();
        // Shapes + Gradient
        addEntry("Line",        "Line (L)",        DrawIcons.line(ic),      lineTool);
        addEntry("Rectangle",   "Rectangle (R)",   DrawIcons.rectangle(ic), rectTool);
        addEntry("Ellipse",     "Ellipse (O)",     DrawIcons.ellipse(ic),   ellipseTool);
        addEntry("Polygon",     "Polygon (W)",     DrawIcons.polygon(ic),   polyTool);
        addEntry("Gradient",    "Gradient (U)",    DrawIcons.gradient(ic),  gradientTool);
        addSep();
        // Utility
        addEntry("Crop",        "Crop (C)",        DrawIcons.crop(ic),      cropTool);
        addEntry("Fill",        "Fill (G)",        DrawIcons.fill(ic),      fillTool);
        addEntry("Text",        "Text (T)",        DrawIcons.text(ic),      textTool);
        addEntry("Eyedropper",  "Eyedropper (I)",  DrawIcons.eyedropper(ic), null); // null = set after

        add(Box.createVerticalGlue());

        // Select pencil by default
        toolEntries.get("Pencil").button.setSelected(true);
    }

    public RectSelectTool getSelectTool() { return selectTool; }
    public CropTool getCropTool() { return cropTool; }

    public void setToolChangeCallback(Consumer<Tool> cb) { this.toolChangeCallback = cb; }

    public TextTool     getTextTool()       { return textTool; }
    public EyedropperTool getEyedropperTool() { return eyedropperTool; }
    public Tool getActiveTool() {
        for (ToolEntry e : toolEntries.values()) {
            if (e.button.isSelected()) return e.tool;
        }
        return pencilTool;
    }

    /** Called from MainWindow after eyedropper is configured. */
    public void setEyedropperTool(EyedropperTool et) {
        this.eyedropperTool = et;
        ToolEntry entry = toolEntries.get("Eyedropper");
        if (entry != null) {
            entry.tool = et;
        }
    }

    /** Select tool by name (for keyboard shortcuts). */
    public void selectTool(String name) {
        ToolEntry entry = toolEntries.get(name);
        if (entry == null) return;
        entry.button.setSelected(true);
        Tool tool = entry.tool;
        if (tool != null && toolChangeCallback != null) toolChangeCallback.accept(tool);
    }

    public Tool getDefaultTool() { return pencilTool; }

    private void addEntry(String key, String tooltip, Icon icon, Tool tool) {
        ToolButton btn = new ToolButton(tooltip, icon);
        btn.setAlignmentX(CENTER_ALIGNMENT);
        group.add(btn);
        add(btn);
        add(Box.createVerticalStrut(2));
        toolEntries.put(key, new ToolEntry(btn, tool));

        btn.addActionListener(e -> {
            Tool t = toolEntries.get(key).tool;
            if (t != null && toolChangeCallback != null) toolChangeCallback.accept(t);
        });
    }

    private void addSep() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(0x3c3c3c));
        sep.setMaximumSize(new Dimension(32, 1));
        sep.setAlignmentX(CENTER_ALIGNMENT);
        add(Box.createVerticalStrut(4));
        add(sep);
        add(Box.createVerticalStrut(4));
    }
}
