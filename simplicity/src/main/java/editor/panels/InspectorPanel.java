package editor.panels;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import asset.Asset;
import components.Component;
import editor.SImGui;
import editor.SimplicityEditorContext;
import editor.undo.FieldCommand;
import editor.undo.FieldValues;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import observers.events.Event;
import simplicity.Application.RenderContext;
import simplicity.GameObject;
import util.Settings;

/**
 * Shows the selected object's name and components, and edits their fields with SImGui's property grid. Fields are
 * found by reflection (saved fields only: not static, not transient), so components need no UI code of their own.
 *
 * Every change is applied right away (live), and recorded for undo once the edit ends: an "edit session" starts at
 * the first change of a field, keeping its value from before, and is committed as one FieldCommand at the end of
 * the first frame in which that field's control isn't active any more (the drag released, Enter pressed...).
 */
public class InspectorPanel extends SimplicityPanel {

    private static final int MAX_DEPTH = 4;   // nested objects shown (component > list > state > frame > sprite)
    private static final String[] PROJECT_PACKAGES = { "components.", "simplicity.", "physics2d.", "scenes." };

    /** A field being edited: its value before the first change, kept until the edit ends. */
    private record EditSession(Object target, Field field, Component owner, Object before) {
        boolean is(Object t, Field f) { return target == t && field.equals(f); }
    }

    private EditSession session = null;
    private boolean sessionActiveThisFrame = false;

    public InspectorPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
    }

    @Override
    public void onRender(RenderContext renderContext) {
        ImGui.begin("Inspector");
        updateCalc();
        sessionActiveThisFrame = false;

        List<GameObject> selected = editorContext.selectedObjects();
        if (selected.isEmpty()) {
            ImGui.textDisabled("Nothing selected");
        } else if (selected.size() > 1) {
            ImGui.textDisabled(selected.size() + " objects selected");
        } else {
            boolean playing = editorContext.playSession.isPlaying();
            if (playing) ImGui.beginDisabled();
            drawGameObject(selected.get(0));
            if (playing) ImGui.endDisabled();
        }

        // the edited control wasn't active this frame (released, or not drawn any more): the edit is finished
        if (session != null && !sessionActiveThisFrame) commitSession();
        ImGui.end();
    }

    private void drawGameObject(GameObject go) {
        if (SImGui.beginProperties("object")) {
            try {
                drawField(go, GameObject.class.getField("name"), null, 0);
            } catch (NoSuchFieldException e) {
                SImGui.readOnly("Name", go.name);
            }
            if (!go.doSerialization()) SImGui.readOnly("Saved", "no (editor-only object)");
            SImGui.endProperties();
        }

        List<Component> components = go.getAllComponenets();
        for (int i = 0; i < components.size(); i++) {
            Component c = components.get(i);
            ImGui.pushID(i);
            if (ImGui.collapsingHeader(prettyName(c.getClass().getSimpleName()), ImGuiTreeNodeFlags.DefaultOpen)) {
                if (SImGui.beginProperties("fields")) {
                    drawFields(c, c, 1);
                    SImGui.endProperties();
                }
            }
            ImGui.popID();
        }
    }

    /** Every saved field of target, its superclasses' too (but not Component's own: its id). */
    private void drawFields(Object target, Component owner, int depth) {
        for (Field field : savedFields(target.getClass())) drawField(target, field, owner, depth);
    }

    private static List<Field> savedFields(Class<?> type) {
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class && c != Component.class; c = c.getSuperclass()) chain.add(0, c);
        List<Field> fields = new ArrayList<>();
        for (Class<?> c : chain) {
            for (Field f : c.getDeclaredFields()) {
                int mods = f.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) continue;
                fields.add(f);
            }
        }
        return fields;
    }

    private void drawField(Object target, Field field, Component owner, int depth) {
        Object value = FieldValues.get(target, field);
        Class<?> type = field.getType();
        String label = prettyName(field.getName());

        ImGui.pushID(field.getName());
        Object before = FieldValues.copy(value);
        Object changedTo = null;
        boolean changed = false;

        if (type == int.class) {
            int[] v = { (int) value };
            if (changed = SImGui.dragInt(label, v)) changedTo = v[0];
        } else if (type == float.class) {
            float[] v = { (float) value };
            if (changed = SImGui.dragFloat(label, v)) changedTo = v[0];
        } else if (type == boolean.class) {
            ImBoolean v = new ImBoolean((boolean) value);
            if (changed = SImGui.checkbox(label, v)) changedTo = v.get();
        } else if (type == String.class) {
            ImString v = new ImString(value == null ? "" : (String) value, 256);
            if (changed = SImGui.inputText(label, v)) changedTo = v.get();
        } else if (type == Vector2f.class && value != null) {
            Vector2f v = new Vector2f((Vector2f) value);
            if (changed = SImGui.vec2(label, v, resetValueFor(field))) changedTo = v;
        } else if (type == Vector3f.class && value != null) {
            Vector3f v = new Vector3f((Vector3f) value);
            if (changed = SImGui.vec3(label, v, resetValueFor(field))) changedTo = v;
        } else if (type == Vector4f.class && value != null) {
            Vector4f v = new Vector4f((Vector4f) value);
            changed = field.getName().toLowerCase().contains("color") ? SImGui.color(label, v) : SImGui.vec4(label, v, resetValueFor(field));
            if (changed) changedTo = v;
        } else if (type.isEnum() && value != null) {
            Object[] constants = type.getEnumConstants();
            String[] names = new String[constants.length];
            int current = 0;
            for (int i = 0; i < constants.length; i++) {
                names[i] = ((Enum<?>) constants[i]).name();
                if (constants[i] == value) current = i;
            }
            ImInt index = new ImInt(current);
            if (changed = SImGui.combo(label, index, names)) changedTo = constants[index.get()];
        } else {
            drawReadOnlyOrNested(label, value, owner, depth);
        }

        if (changed) {
            FieldValues.set(target, field, changedTo);
            if (owner != null) owner.onFieldsChanged();
            if (session == null || !session.is(target, field)) {
                commitSession();   // another field's edit still open (e.g. Tab to the next field): finish it first
                session = new EditSession(target, field, owner, before);
            }
        }
        // SImGui draws each control as one group, so this covers every part of it (e.g. a vector's two fields)
        if (session != null && session.is(target, field) && ImGui.isItemActive()) sessionActiveThisFrame = true;
        ImGui.popID();
    }

    /** Values without a control: asset paths, nested project objects and lists of them (edited inside), others as text. */
    private void drawReadOnlyOrNested(String label, Object value, Component owner, int depth) {
        if (value == null) {
            SImGui.readOnly(label, "none");
        } else if (value instanceof Asset<?> asset) {
            SImGui.readOnly(label, asset.getPrefixedPath());
        } else if (value instanceof Collection<?> list) {
            if (depth < MAX_DEPTH && SImGui.beginPropertyGroup(label + " (" + list.size() + ")")) {
                int i = 0;
                for (Object element : list) {
                    ImGui.pushID(i);
                    drawElement("[" + i + "]", element, owner, depth + 1);
                    ImGui.popID();
                    i++;
                }
                SImGui.endPropertyGroup();
            }
        } else if (value.getClass().isArray()) {
            SImGui.readOnly(label, java.lang.reflect.Array.getLength(value) + " values");
        } else if (value instanceof Map<?, ?> map) {
            SImGui.readOnly(label, map.size() + " entries");
        } else if (isProjectType(value.getClass()) && depth < MAX_DEPTH) {
            if (SImGui.beginPropertyGroup(label)) {
                drawFields(value, owner, depth + 1);
                SImGui.endPropertyGroup();
            }
        } else {
            SImGui.readOnly(label, String.valueOf(value));
        }
    }

    private void drawElement(String label, Object element, Component owner, int depth) {
        if (element != null && isProjectType(element.getClass())) {
            if (SImGui.beginPropertyGroup(label + " " + prettyName(element.getClass().getSimpleName()))) {
                drawFields(element, owner, depth);
                SImGui.endPropertyGroup();
            }
        } else {
            SImGui.readOnly(label, String.valueOf(element));
        }
    }

    /** What a vector's axis buttons reset to: one grid cell for a scale, zero otherwise. */
    private static float resetValueFor(Field field) {
        return field.getName().toLowerCase().contains("scale") ? Settings.GRID_WIDTH : 0.0f;
    }

    /** "zIndex" -> "Z Index", "SpriteRenderer" -> "Sprite Renderer". */
    private static String prettyName(String name) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                out.append(Character.toUpperCase(c));
                continue;
            }
            char prev = name.charAt(i - 1);
            if (Character.isUpperCase(c) && (Character.isLowerCase(prev) || Character.isDigit(prev))) out.append(' ');
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isProjectType(Class<?> type) {
        String name = type.getName();
        for (String p : PROJECT_PACKAGES) if (name.startsWith(p)) return true;
        return false;
    }

    private void commitSession() {
        if (session == null) return;
        EditSession s = session;
        session = null;
        Object after = FieldValues.copy(FieldValues.get(s.target(), s.field()));
        if (FieldValues.same(s.before(), after)) return;
        if (editorContext.history != null) {
            editorContext.history.record(new FieldCommand(s.target(), s.field(), s.owner(), s.before(), after));
        }
    }

    @Override
    public void onUpdate(float dt) {
    }

    @Override
    public void destroy() {
        commitSession();
    }

    @Override
    public void onEvent(Event event) {
    }
}
