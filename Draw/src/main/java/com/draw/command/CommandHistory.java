package com.draw.command;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayDeque;
import java.util.Deque;

public class CommandHistory {
    private static final int MAX_HISTORY = 50;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void execute(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        if (undoStack.size() > MAX_HISTORY) {
            // Remove oldest entry (bottom of deque)
            ((ArrayDeque<Command>) undoStack).removeLast();
        }
        redoStack.clear();
        fireChange();
    }

    /** Push a command that has already been executed (e.g. paint strokes done live). */
    public void push(Command cmd) {
        undoStack.push(cmd);
        if (undoStack.size() > MAX_HISTORY) {
            ((ArrayDeque<Command>) undoStack).removeLast();
        }
        redoStack.clear();
        fireChange();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        fireChange();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        fireChange();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        fireChange();
    }

    private void fireChange() {
        pcs.firePropertyChange("canUndo", null, canUndo());
        pcs.firePropertyChange("canRedo", null, canRedo());
    }

    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }
}
