package editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import simplicity.GameObject;

public class EditorSelection {
    
    public enum Mode { REPLACE, ADD, TOGGLE, SUBTRACT }

    public List<GameObject> selectedGameObjects = new ArrayList<>();

    public EditorSelection() {}

    public boolean isSingleSelection() {
        return isNoSelection() ? false: selectedGameObjects.stream().filter(Objects::nonNull).count() == 1;
    }

    public boolean isNoSelection() {
        return selectedGameObjects == null ? true : selectedGameObjects.stream().noneMatch(Objects::nonNull);
    }

    /** How a pick changes the selection. */

    public void apply(Mode mode, Collection<GameObject> objects) {
        switch (mode) {
            case REPLACE -> set(objects);
            case ADD -> add(objects);
            case TOGGLE -> toggle(objects);
            case SUBTRACT -> remove(objects);
        }
    }

    public void set(Collection<GameObject> objects) {
        list().clear();
        add(objects);
    }

    public void add(Collection<GameObject> objects) {
        List<GameObject> list = list();
        for (GameObject go : objects) {
            if (go != null && !list.contains(go)) list.add(go);
        }
    }

    public void remove(Collection<GameObject> objects) {
        list().removeAll(objects);
    }

    public void toggle(Collection<GameObject> objects) {
        List<GameObject> list = list();
        for (GameObject go : objects) {
            if (go != null && !list.remove(go)) list.add(go);
        }
    }

    public void clear() {
        list().clear();
    }

    private List<GameObject> list() {
        if (selectedGameObjects == null) selectedGameObjects = new ArrayList<>();
        return selectedGameObjects;
    }
}
