package editor;

import asset.AssetPool;
import asset.AssetPoolHandler;
import asset.UnsavedChanges;
import scenes.World2DLayer;

public class SimplicityEditorContext extends editor.EditorContext<SimplicityEditor> {
    public Project project;
    public World2DLayer world;
    public EditorSelection gameObjectSelection;
    public AssetPoolHandler assetPoolHandler;
    public AssetPool projectAssets, engineResources;
    public EditorIcons icons;
    public UnsavedChanges unsavedChanges;   // new for each opened project
    public WorldSavable worldSavable;       // the open world; null if it has no file

    public SimplicityEditorContext(SimplicityEditor editorLayer) {
        super(editorLayer);
    }
}