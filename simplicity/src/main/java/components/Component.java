package components;

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

    /**
     * Some of this component's saved fields were changed from outside (the editor's inspector, or its undo/redo).
     * Override to react, e.g. to mark cached data as stale. Components don't draw their own editor UI: the editor's
     * inspector shows their fields.
     */
    public void onFieldsChanged() {

    }

    public void generateId() {
        if(this.uid == -1) {
            this.uid = ID_COUNTER++;
        }
    }

    /** A fresh id even if it has one, e.g. for a copy that was read back with the original's id. */
    public void generateNewId() {
        this.uid = ID_COUNTER++;
    }

    public int getUid() {
        return this.uid;
    }

    public static void init(int maxId) {
        ID_COUNTER = maxId;
    }

    public void destroy() {
    }
}
