package editor.panels;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;

import components.Sprite;
import components.SpriteRenderer;
import components.Spritesheet;
import editor.SImGui;
import editor.SimplicityEditorContext;
import imgui.ImGui;
import imgui.type.ImInt;
import observers.events.Event;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;
import util.Resources;
import util.Settings;

/**
 * Sprites from sprite sheets, as a grid of buttons: a click places a sprite object in the middle of the view,
 * on the grid, selected, as one undoable step. For now it lists the engine's sheets.
 */
public class SpritePalettePanel extends SimplicityPanel {

    private static final float PIXELS_PER_UNIT = 64.0f;   // a 16 px tile is 0.25 units, one grid cell
    private static final float BUTTON_SCALE = 2.0f;       // buttons show sprites at twice their pixel size

    private static final String[] SHEET_PATHS = {
        "engine:" + Resources.SPRITESHEET_TILES_SHEET,
        "engine:" + Resources.SPRITESHEET_OBJ_SHEET,
    };

    private final String[] sheetNames = new String[SHEET_PATHS.length];
    private final List<Spritesheet> sheets = new ArrayList<>();   // null where a sheet couldn't be loaded
    private final ImInt current = new ImInt(0);

    public SpritePalettePanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        for (int i = 0; i < SHEET_PATHS.length; i++) {
            String path = SHEET_PATHS[i];
            sheetNames[i] = path.substring(path.lastIndexOf('/') + 1);
            Spritesheet sheet = null;
            try {
                sheet = editorContext.assetPoolHandler.resolve(path, Spritesheet.class);
            } catch (RuntimeException e) {
                logger.Logger.error("Sprite palette: can't load '" + path + "': " + e.getMessage());
            }
            sheets.add(sheet);
        }
    }

    @Override
    public void onRender(RenderContext renderContext) {
        ImGui.begin("Sprites");
        updateCalc();

        ImGui.combo("Sheet", current, sheetNames, sheetNames.length);
        Spritesheet sheet = sheets.get(current.get());
        if (sheet == null) {
            ImGui.textDisabled("This sheet couldn't be loaded (see the log)");
            ImGui.end();
            return;
        }

        boolean canEdit = editorContext.canEdit();
        if (!canEdit) ImGui.beginDisabled();

        float framePadding = ImGui.getStyle().getFramePaddingX() * 2;
        Sprite clicked = null;
        for (int i = 0; i < sheet.size(); i++) {
            Sprite sprite = sheet.getSprite(i);
            float w = sprite.getWidth() * BUTTON_SCALE, h = sprite.getHeight() * BUTTON_SCALE;
            if (i > 0) SImGui.sameLineIfFits(w + framePadding);   // wraps to a new row at the window's edge
            if (SImGui.spriteButton("sprite" + i, sprite, w, h)) clicked = sprite;
        }

        if (!canEdit) ImGui.endDisabled();
        ImGui.end();

        if (clicked != null) place(clicked, sheetNames[current.get()]);
    }

    /** A new object showing a copy of the sprite (the sheet's own sprite stays untouched by later edits). */
    private void place(Sprite sprite, String sheetName) {
        Sprite copy = new Sprite();
        copy.setTexture(sprite.getTextureAsset());
        Vector2f[] uv = sprite.getTexCoords();
        Vector2f[] uvCopy = new Vector2f[uv.length];
        for (int i = 0; i < uv.length; i++) uvCopy[i] = new Vector2f(uv[i]);
        copy.setTexCoords(uvCopy);
        copy.setWidth(sprite.getWidth());
        copy.setHeight(sprite.getHeight());

        String name = sheetName.replace(".sheet", "") + " sprite";
        GameObject go = editorContext.world.createGameObject(name);
        SpriteRenderer renderer = new SpriteRenderer();
        renderer.setSprite(copy);
        go.addComponent(renderer);
        go.transform.scale.set(sprite.getWidth() / PIXELS_PER_UNIT, sprite.getHeight() / PIXELS_PER_UNIT);

        Camera view = editorContext.world.renderCamera();
        if (view != null) {
            Vector2f center = view.viewportToWorld(0.5f, 0.5f);
            // positions are centres: snap to the middle of a grid cell
            float gx = Settings.GRID_WIDTH, gy = Settings.GRID_HEIGHT;
            go.transform.position.set((float) Math.floor(center.x / gx) * gx + gx / 2, (float) Math.floor(center.y / gy) * gy + gy / 2);
        }
        editorContext.addObjects("Place '" + name + "'", List.of(go));
    }

    @Override
    public void onUpdate(float dt) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void onEvent(Event event) {
    }
}
