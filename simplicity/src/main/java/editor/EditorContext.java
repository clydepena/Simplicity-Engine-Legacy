package editor;

public abstract class EditorContext<L extends ImGuiEditorLayer> {
    public final L editorLayer;

    public EditorContext(L editorLayer) {
        this.editorLayer = editorLayer;
    }

}
