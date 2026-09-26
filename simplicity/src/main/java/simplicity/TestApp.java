package simplicity;

import editor.SimplicityEditor;
import scenes.World2DLayer;
import scenes.WorldTestLayer;

public class TestApp extends Application{

    public TestApp() {
        super("Test");

        pushLayer(new World2DLayer());
        pushLayer(new WorldTestLayer("saves/level.json"));
        pushLayer(new SimplicityEditor());
        // pushLayer(new TestLayerTriangle());
    }

    @Override
    protected void onClose() {
        System.out.println(title + " closed");
    }
}
