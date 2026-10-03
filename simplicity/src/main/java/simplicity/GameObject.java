package simplicity;

import java.util.ArrayList;
import java.util.List;


import components.Component;

public class GameObject {
    private static int ID_COUNTER = 0;
    private int uid = -1;
    public String name;
    private List<Component> components;
    public transient Transform transform;
    private boolean doSerialization = true;
    private boolean isDead = false;

    // public GameObject(String name) {
    //     this.name = name;
    //     this.zIndex = 0;
    //     this.components = new ArrayList<>();
    //     this.transform = new Transform();
    // }

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();
        this.uid = ID_COUNTER++;
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        for(Component c : components) {
            if(componentClass.isAssignableFrom(c.getClass())) {
                try {
                    return componentClass.cast(c);
                } catch(ClassCastException e) {
                    e.printStackTrace();
                    assert false : "Error: Casting component";
                }
            }
        }
        return null;
    }

    public <T extends Component> void removeComponent(Class<T> componeneClass) {
        for(int i = 0; i < components.size() ; i++) {
            Component c = components.get(i);
            if(componeneClass.isAssignableFrom(c.getClass())) {
                components.remove(i);
                return;
            }
        }
    }

    public void addComponent(Component c) {
        c.generateId();
        this.components.add(c);
        c.gameObject = this;
    }

    public void update(float dt) {
        for(int i = 0; i < components.size(); i++) {
            components.get(i).update(dt);
        }
    }

    public void editorUpdate(float dt) {
        for(int i = 0; i < components.size(); i++) {
            components.get(i).editorUpdate(dt);
        }
    }

    public void start() {
        for(int i = 0; i < components.size(); i++) {
            components.get(i).start();
        }
    }

    public static void init(int maxId) {
        ID_COUNTER = maxId;
    }

    public int getUid() {
        return this.uid;
    }

    public List<Component> getAllComponenets() {
        return this.components;
    }

    public void setSerialize(Boolean bool) {
        this.doSerialization = bool;
    }

    public boolean doSerialization() {
        return this.doSerialization;
    }

    public void destroy() {
        this.isDead = true;
        for(int i = 0; i <components.size(); i++) {
            components.get(i).destroy();
        }
    }

    public boolean isDead() {
        return this.isDead;
    }

    public void generateUid() {
        this.uid = ID_COUNTER++;
    }

    public GameObject copy() {
        // textures are Asset handles, saved as paths: reading them back gives the same shared handles,
        // so the copy needs no texture fix-up
        String objAsJson = GameObjectGson.GSON.toJson(this);
        GameObject obj = GameObjectGson.GSON.fromJson(objAsJson, GameObject.class);
        obj.generateUid();

        // the components were read back with the original's ids: give them their own
        for (Component c : obj.getAllComponenets()) {
            c.generateNewId();
        }
        return obj;
    }
}
