package editor.undo;

import java.util.ArrayDeque;
import java.util.Deque;

import asset.Savable;
import asset.UnsavedChanges;

/**
 * The undo and redo stacks of one document (for now, the open world).
 *
 * It also decides whether the document is unsaved: it remembers how deep the undo stack was at the last save, and
 * after every change marks the document dirty, or clean again when undo/redo lands exactly back on the saved state.
 * So edits must go through record()/execute() rather than marking the document dirty directly.
 */
public final class UndoHistory {

    private static final int LIMIT = 200;

    private final Deque<EditorCommand> undoStack = new ArrayDeque<>();   // top = most recent edit
    private final Deque<EditorCommand> redoStack = new ArrayDeque<>();   // top = the next edit to redo
    private final UnsavedChanges unsavedChanges;
    private final Savable document;

    private int savedDepth = 0;      // undoStack.size() at the last save; -1 when undo/redo can't reach that state
    private boolean sealed = true;   // true: the next edit starts a new command, it can't merge into the top one

    /** @param document what these edits change; null for a document without a file (the history still works) */
    public UndoHistory(UnsavedChanges unsavedChanges, Savable document) {
        this.unsavedChanges = unsavedChanges;
        this.document = document;
    }

    /** Applies a command and records it. */
    public void execute(EditorCommand command) {
        command.redo();
        record(command);
    }

    /** Records a command whose edit was already applied (e.g. a gizmo drag, applied live while dragging). */
    public void record(EditorCommand command) {
        dropRedo();
        EditorCommand top = undoStack.peek();
        if (!sealed && top != null && top.mergeWith(command)) {
            // the top command now ends in a new state: if that was the saved one, it can't be reached any more
            if (savedDepth == undoStack.size()) savedDepth = -1;
        } else {
            undoStack.push(command);
            if (undoStack.size() > LIMIT) {
                undoStack.removeLast().discard();
                savedDepth = savedDepth > 0 ? savedDepth - 1 : -1;
            }
        }
        sealed = false;
        sync();
    }

    public void undo() {
        EditorCommand command = undoStack.poll();
        if (command == null) return;
        command.undo();
        redoStack.push(command);
        sealed = true;
        sync();
    }

    public void redo() {
        EditorCommand command = redoStack.poll();
        if (command == null) return;
        command.redo();
        undoStack.push(command);
        sealed = true;
        sync();
    }

    /** The document was just saved: the current state is the clean one. */
    public void markSaved() {
        savedDepth = undoStack.size();
        sealed = true;   // an edit after the save must not merge into the command before it
        sync();
    }

    /**
     * Forgets every command, e.g. when the objects they point to were replaced (the world rebuilt after Play).
     * The document keeps its unsaved state; if it's unsaved, no undo can make it clean again.
     */
    public void clear() {
        boolean dirty = document != null && unsavedChanges.isDirty(document);
        while (!redoStack.isEmpty()) redoStack.pop().discard();
        while (!undoStack.isEmpty()) undoStack.pop().discard();
        savedDepth = dirty ? -1 : 0;
        sealed = true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** The name of the edit undo() would revert, or null. */
    public String undoName() {
        EditorCommand top = undoStack.peek();
        return top == null ? null : top.name();
    }

    /** The name of the edit redo() would apply, or null. */
    public String redoName() {
        EditorCommand top = redoStack.peek();
        return top == null ? null : top.name();
    }

    /** A new edit after some undos: the undone edits can't be redone any more. */
    private void dropRedo() {
        if (redoStack.isEmpty()) return;
        if (savedDepth > undoStack.size()) savedDepth = -1;   // the saved state was in the dropped branch
        while (!redoStack.isEmpty()) redoStack.pop().discard();
    }

    private void sync() {
        if (document == null) return;
        if (savedDepth == undoStack.size()) unsavedChanges.markClean(document);
        else unsavedChanges.markDirty(document);
    }
}
