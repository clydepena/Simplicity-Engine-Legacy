package editor.undo;

/**
 * One edit the user can undo and redo. A command holds what it needs to go both ways (the objects it touched and
 * their values before and after), so undo() and redo() can run any number of times, in turn.
 */
public interface EditorCommand {

    /** Shown in the Edit menu: "Undo Move 3 objects". */
    String name();

    /** Puts things back as they were before the edit. */
    void undo();

    /** Applies the edit (again). UndoHistory.execute() also uses it to apply the edit the first time. */
    void redo();

    /**
     * Folds a following edit into this one, so a run of small steps undoes in one go (e.g. a colour dragged in a
     * picker, which arrives as one change per frame). Return true if next was absorbed; it's then dropped.
     */
    default boolean mergeWith(EditorCommand next) {
        return false;
    }

    /**
     * Called once when the command leaves the history for good (past the limit, a dropped redo branch, a cleared
     * history), so it can free what only it still holds, e.g. objects it deleted.
     */
    default void discard() {
    }
}
