package simplicity;

import scenes.NewImGuiLayer;
import scenes.World2DLayer;
import scenes.WorldTestLayer;

public class TestApp extends Application{

    public TestApp() {
        super("Test");

        // pushLayer(new TestLayerTriangle());
        pushLayer(new World2DLayer());
        pushLayer(new WorldTestLayer("saves/level.json"));
        pushLayer(new NewImGuiLayer());
    }

    @Override
    protected void onClose() {
        System.out.println(title + " closed");
    }
}
