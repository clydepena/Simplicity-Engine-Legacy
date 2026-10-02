package editor;

import asset.AssetPool;
import asset.AssetPoolHandler;
import scenes.World2DLayer;

public class SimplicityEditorContext extends editor.EditorContext<SimplicityEditor> {
    public Project project;
    public World2DLayer world;
    public EditorSelection gameObjectSelection;
    public AssetPoolHandler assetPoolHandler;
    public AssetPool projectAssets, engineResources;
    public EditorIcons icons;

    public SimplicityEditorContext(SimplicityEditor editorLayer) {
        super(editorLayer);
    }
}