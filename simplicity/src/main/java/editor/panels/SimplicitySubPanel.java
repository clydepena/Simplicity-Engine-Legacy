package editor.panels;

import editor.SimplicityEditor;
import editor.SimplicityEditorContext;

public abstract class SimplicitySubPanel<P extends SimplicityPanel> extends EditorSubPanel<SimplicityEditor, SimplicityEditorContext, P> {

        public SimplicitySubPanel(P parentPanel) {
            super(parentPanel);
        }
    }
