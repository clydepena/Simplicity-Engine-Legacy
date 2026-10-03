package editor.undo;

import java.lang.reflect.Field;
import java.util.Objects;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Field values as the inspector and FieldCommand handle them: mutable values (JOML vectors) are copied, so a
 * command's "before" can't change behind its back, and are set in place, so other holders of the vector see it.
 */
public final class FieldValues {

    private FieldValues() {}

    /** A copy of a mutable value; immutable ones (primitives, String, enums) are returned as they are. */
    public static Object copy(Object value) {
        if (value instanceof Vector2f v) return new Vector2f(v);
        if (value instanceof Vector3f v) return new Vector3f(v);
        if (value instanceof Vector4f v) return new Vector4f(v);
        return value;
    }

    public static boolean same(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static Object get(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("can't read " + field.getName() + ": " + e.getMessage(), e);
        }
    }

    /** Sets the field to (a copy of) value; vectors are set in place when the field already holds one. */
    public static void set(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            Object current = field.get(target);
            if (current instanceof Vector2f c && value instanceof Vector2f v) c.set(v);
            else if (current instanceof Vector3f c && value instanceof Vector3f v) c.set(v);
            else if (current instanceof Vector4f c && value instanceof Vector4f v) c.set(v);
            else field.set(target, copy(value));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("can't set " + field.getName() + ": " + e.getMessage(), e);
        }
    }
}
