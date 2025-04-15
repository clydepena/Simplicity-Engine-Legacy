package editor;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import components.MouseControls;
import components.Sprite;
import components.Spritesheet;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTabBarFlags;
import simplicity.GameObject;
import simplicity.Prefabs;
import simplicity.Window;
import util.AssetPool;
import util.Resources;
import util.Settings;

public class SpriteSelectorWindow extends ImGuiInterface{
    
    private class SpritesTabItem {
        
        private String title;
        private Spritesheet spritesheet;
        private float sizeX = 0.3125f, sizeY = 0.2344f;

        public SpritesTabItem(String title, Spritesheet spritesheet) {
            this.title = title;
            this.spritesheet = spritesheet;
        }

        public void imgui() {
            if (ImGui.beginTabItem(title)) {        
                ImVec2 itemSpacing = new ImVec2();
                ImGui.getStyle().getItemInnerSpacing(itemSpacing);
        
                float windowX2 = winPosition.x + winSize.x;
                for(int i = 0; i < spritesheet.size(); i++) {
                    Sprite sprite = spritesheet.getSprite(i);
                    float spriteWidth = sprite.getWidth() * 1;
                    float spriteHeight = sprite.getHeight() * 1;
                    int id = sprite.getTexId();
                    Vector2f[] texCoords = sprite.getTexCoords();
        
                    ImGui.pushID(i);
                    if(ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
                        GameObject object = Prefabs.generateSpriteObject(sprite, sizeX, sizeY);
                        
                        // attach to mouse cursor
                        Window.getImGuiLayer().getEditorGameObject().getComponent(MouseControls.class).pickupObject(object);
                    }
                    ImGui.popID();
        
                    ImVec2 lastButtonPos = new ImVec2();
                    ImGui.getItemRectMax(lastButtonPos);
                    float lastButtonX2 = lastButtonPos.x;
                    float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
                    if(i + 1 < spritesheet.size() && nextButtonX2 < windowX2) {
                        ImGui.sameLine();
                    }
                }
        
                ImGui.endTabItem();
            }
        }
    }

    private boolean tmp = true;
    private List<SpritesTabItem> tabs;

    public SpriteSelectorWindow() {
        tabs = new ArrayList<>();
    }

    // remove
    private void test() {
        SpritesTabItem tab = new SpritesTabItem("BLocks", AssetPool.getSpritesheet(Resources.SPRITESHEET_TILES));
        tab.sizeX = Settings.GRID_WIDTH;
        tab.sizeY = Settings.GRID_HEIGHT;
        tabs.add(tab);

        addSpritesheet("Items", AssetPool.getSpritesheet(Resources.SPRITESHEET_OBJ));
    }
    
    @Override
    public void imgui() {
        if (tmp) {
            // ImGui.setNextWindowSize(400, 500);
            test();
            tmp = false;
        }

        ImGui.begin("Sprite Selector");
        updateCalc();

        ImGui.beginTabBar("##", 
            ImGuiTabBarFlags.AutoSelectNewTabs | 
            ImGuiTabBarFlags.Reorderable | 
            ImGuiTabBarFlags.TabListPopupButton
        );

        for (SpritesTabItem tab : tabs) {
            tab.imgui();
        }

        ImGui.endTabBar();

        ImGui.end();
    }

    public void addSpritesheet(String title, Spritesheet spritesheet) {
        tabs.add(new SpritesTabItem(title, spritesheet));
    }

    @Override
    public void destroy() {
        
    }
}
