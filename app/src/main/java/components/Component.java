package components;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import editor.SImGui;
import imgui.ImGui;
import imgui.type.ImInt;
import simplicity.GameObject;

public abstract class Component {
    
    private static int ID_COUNTER = 0;
    private int uid = -1;

    public transient GameObject gameObject = null;

    public void start() {
        
    }

    public void update(float dt) {

    }

    public void editorUpdate(float dt) {
        
    }

    public void imgui() {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for(Field field : fields) {
                boolean isTransient = Modifier.isTransient(field.getModifiers());
                if(isTransient) {
                    continue;
                }
                boolean isPrivate = Modifier.isPrivate(field.getModifiers());
                if(isPrivate) {
                    field.setAccessible(true);
                }

                Class type = field.getType();
                Object value = field.get(this);
                String name = field.getName();

                // System.out.println(name);

                if(type == int.class) {
                    int val = (int) value;
                    field.set(this, SImGui.dragInt(name, val));
                } else if(type == float.class) {
                    float val = (float) value;
                    field.set(this, SImGui.dragFloat(name, val));
                } else if(type == boolean.class) {
                    boolean val = (boolean) value;
                    if(ImGui.checkbox(name + ": ", val)) {
                        field.set(this, !val);
                    }
                } else if(type == Vector2f.class) {
                    Vector2f val = (Vector2f) value;
                    SImGui.drawVec2fControl(name, val);
                } else if(type == Vector3f.class) {
                    Vector3f val = (Vector3f) value;
                    float[] imVec3f = {val.x, val.y, val.z};
                    if(ImGui.dragFloat3(name + ": ", imVec3f)) {
                        val.set(imVec3f[0], imVec3f[1], imVec3f[2]);
                    }
                } else if(type == Vector4f.class) {
                    Vector4f val = (Vector4f) value;
                    float[] imVec4f = {val.x, val.y, val.z, val.w};
                    if(ImGui.dragFloat4(name + ": ", imVec4f)) {
                        val.set(imVec4f[0], imVec4f[1], imVec4f[2], imVec4f[3]);
                    }
                } else if (type.isEnum()) {
                    String[] enumVals = getEnumValues(type);
                    String enumType = ((Enum)value).name();
                    ImInt index = new ImInt(indexOf(enumType, enumVals));
                    if(ImGui.combo(field.getName(), index, enumVals, enumVals.length)) {
                        field.set(this, type.getEnumConstants()[index.get()]);
                    }
                }

                if(isPrivate) {
                    field.setAccessible(false);
                }
            }
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void generateId() {
        if(this.uid == -1) {
            this.uid = ID_COUNTER++;
        }
    }

    public int getUid() {
        return this.uid;
    }

    public static void init(int maxId) {
        ID_COUNTER = maxId;
    }

    private <T extends Enum<T>> String[] getEnumValues(Class<T> enumType) {
        String[] enumValues = new String[enumType.getEnumConstants().length];
        int i = 0;
        for (T enumIntValue : enumType.getEnumConstants()) {
            enumValues[i] = enumIntValue.name();
            i++;
        }
        return enumValues;
    }

    private int indexOf(String str, String[] strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (str.equals(strArr[i])) {
                return i;
            }
        }
        return -1;
    }

    public void destroy() {
    }


}
