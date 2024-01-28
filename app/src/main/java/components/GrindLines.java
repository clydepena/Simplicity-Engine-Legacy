package components;

import org.joml.Vector2f;
import org.joml.Vector4f;

import renderer.DebugDraw;
import simplicity.Camera;
import simplicity.Window;
import util.Settings;


public class GrindLines extends Component {
    

    @Override
    public void editorUpdate(float dt) {
        Camera camera = Window.getScene().camera();

        Vector2f cameraPos = camera.getPosition();
        Vector2f projectionSize = camera.getProjectionSize();
        
        float firstX = ((int) (cameraPos.x / Settings.GRID_WIDTH) - 1) * Settings.GRID_WIDTH;
        float firstY = ((int) (cameraPos.y / Settings.GRID_HEIGHT) - 1) * Settings.GRID_HEIGHT;

        int numVtLines = (int) (projectionSize.x * camera.getZoom() / Settings.GRID_WIDTH) + 2;
        int numHzLines = (int) (projectionSize.y * camera.getZoom() / Settings.GRID_HEIGHT) + 2;

        float width = (int) (projectionSize.x * camera.getZoom()) + Settings.GRID_WIDTH * 2;
        float height = (int) (projectionSize.y * camera.getZoom()) + Settings.GRID_HEIGHT * 2;

        int maxLines = Math.max(numVtLines, numHzLines);
        Vector4f color = new Vector4f(0.4f, 0.4f, 0.4f, 0.5f);

        for(int i = 0; i < maxLines; i++) {
            float x = firstX + (Settings.GRID_WIDTH * i);
            float y = firstY + (Settings.GRID_HEIGHT * i);

            if(i < numVtLines) {
                DebugDraw.addLine2D(new Vector2f(x, firstY), new Vector2f(x, firstY + height), color, 1);
            }

            if(i < numHzLines) {
                DebugDraw.addLine2D(new Vector2f(firstX, y), new Vector2f(firstX + width, y), color, 1);
            }
        }
    }
}
