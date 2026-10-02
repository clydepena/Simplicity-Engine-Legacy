package editor.panels;

import editor.SimplicityEditor;
import editor.SimplicityEditorContext;

public abstract class SimplicityPanel extends EditorPanel<SimplicityEditor, SimplicityEditorContext> {

    public SimplicityPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
    }
}

