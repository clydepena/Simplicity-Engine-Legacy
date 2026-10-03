package editor.undo;

import java.lang.reflect.Field;

import components.Component;

/**
 * One field changed in the inspector: target.field went from before to after. The target can be a component, the
 * game object (its name) or an object inside a component (an animation frame); owner is the component that holds
 * it, told about the change so it can react (Component.onFieldsChanged), or null.
 */
public final class FieldCommand implements EditorCommand {

    /** Changes to the same field this close together become one undo step (a colour picker reports every frame). */
    private static final long MERGE_WINDOW_NANOS = 500_000_000L;

    private final Object target;
    private final Field field;
    private final Component owner;
    private final Object before;
    private Object after;
    private long lastChange = System.nanoTime();

    public FieldCommand(Object target, Field field, Component owner, Object before, Object after) {
        this.target = target;
        this.field = field;
        this.owner = owner;
        this.before = FieldValues.copy(before);
        this.after = FieldValues.copy(after);
    }

    @Override
    public String name() {
        return "Change " + field.getName();
    }

    @Override
    public void undo() {
        apply(before);
    }

    @Override
    public void redo() {
        apply(after);
    }

    private void apply(Object value) {
        FieldValues.set(target, field, value);
        if (owner != null) owner.onFieldsChanged();
    }

    @Override
    public boolean mergeWith(EditorCommand next) {
        if (!(next instanceof FieldCommand other)) return false;
        if (other.target != target || !other.field.equals(field)) return false;
        if (other.lastChange - lastChange > MERGE_WINDOW_NANOS) return false;
        after = FieldValues.copy(other.after);
        lastChange = other.lastChange;
        return true;
    }
}
