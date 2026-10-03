package editor.undo;

import java.util.ArrayList;
import java.util.List;

import simplicity.Transform;

/** Moved, rotated or scaled objects (a gizmo drag): each target's transform before and after. */
public final class TransformCommand implements EditorCommand {

    private final String name;
    private final List<Transform> targets;
    private final List<Transform> before = new ArrayList<>();
    private final List<Transform> after = new ArrayList<>();

    /** @param before copies of the targets' transforms before the edit, in the same order; after is read from them now */
    public TransformCommand(String name, List<Transform> targets, List<Transform> before) {
        this.name = name;
        this.targets = new ArrayList<>(targets);
        for (int i = 0; i < targets.size(); i++) {
            this.before.add(before.get(i).copy());
            this.after.add(targets.get(i).copy());
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void undo() {
        for (int i = 0; i < targets.size(); i++) before.get(i).copy(targets.get(i));
    }

    @Override
    public void redo() {
        for (int i = 0; i < targets.size(); i++) after.get(i).copy(targets.get(i));
    }
}
