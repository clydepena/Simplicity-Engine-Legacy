package editor.panels;

import editor.EditorContext;
import editor.ImGuiEditorLayer;

public abstract class EditorSubPanel<L extends ImGuiEditorLayer, C extends EditorContext<L>, P extends EditorPanel<L, C>> extends EditorPanel<L, C> {

    public final P parentPanel;

    public EditorSubPanel(P parentPanel) {
        super(parentPanel.editorContext);
        this.parentPanel = parentPanel;
    }
}
