package util;

public class Resources {

    public static final String ICON =               "images/ICON_2.png";
    public static final String ICON_SMALL =         "images/ICON_2_SMALL.png";


    public static final String MAIN_SHADER =        "shaders/default.glsl";

    public static final String SHADER_GAME_DEFAULT= "shaders/default.glsl";

    public static final String SPRITESHEET_OBJ =    "images/ObjectsSpritesheet.png";
    public static final String SPRITESHEET_TEST =   "images/spritesheetTest.png";
    public static final String SPRITESHEET_TILES =  "images/TilesSpritesheet.png";
    public static final String SPRITESHEET_OBJ_SHEET =   "images/ObjectsSpritesheet.sheet";   // the cut of SPRITESHEET_OBJ
    public static final String SPRITESHEET_TILES_SHEET = "images/TilesSpritesheet.sheet";     // the cut of SPRITESHEET_TILES

    public static final String SPRITE_BALL =        "images/TestImgBall.png";
    public static final String SPRITE_PIRATE =      "images/TestPirate.png";
    public static final String SPRITE_STAR =        "images/TestStar.png";

    public static final String FONT_OPENSANS =      "fonts/OpenSans.ttf";
    public static final String FONT_AMATICSC =      "fonts/AmaticSC.ttf";
    public static final String FONT_PIXELIFY =      "fonts/PixelifySans.ttf";
    public static final String FONT_RETHINK =       "fonts/RethinkSans-SemiBold.ttf";
    public static final String FONT_HONK =          "fonts/Honk-Regular.ttf";
    public static final String FONT_SOURCE_SANS =   "fonts/SourceSans3-Regular.ttf";   // SIL OFL, see fonts/SourceSans3-OFL.txt
    public static final String FONT_AWESOME_SOLID = "fonts/fa-solid-900.otf";          // Font Awesome 7.3.1 Free, see fonts/fontawesome-LICENSE.txt; codepoints in editor.FontAwesomeIcons

    /** Editor-only resources, kept in an editor/ subfolder of each resource type folder. */
    public static class Editor {
        public static final String IMGUI_INI =          "config/editor/imgui.ini";

        public static final String SPRITESHEET_GIZMO =     "images/editor/gizmos.png";
        public static final String SPRITESHEET_GIZMO_SHEET = "images/editor/gizmos.sheet";   // the cut of SPRITESHEET_GIZMO

        public static final String SHADER_LINE=     "shaders/editor/debugLine2D.glsl";
        public static final String SHADER_PICKING=  "shaders/editor/pickingShader.glsl";
        public static final String SHADER_SELECTION_MASK=       "shaders/editor/selectionMask.glsl";
        public static final String SHADER_SELECTION_OUTLINE=    "shaders/editor/selectionOutline.glsl";
        public static final String SHADER_SELECTION_FLAGS=      "shaders/editor/selectionFlags.glsl";

        public static final String SPRITE_FOLDER=       "images/editor/folder_icon.png";
        public static final String SPRITE_FOLDER_OPEN=  "images/editor/folder_icon_opened.png";
        public static final String SPRITE_FILE=         "images/editor/file_icon.png";
        public static final String SPRITE_ADDFOLDER=         "images/editor/add_folder.png";
        public static final String SPRITE_COLLAPSEFOLDERS=         "images/editor/collapse_folders.png";
        public static final String SPRITE_REFRESHFILES=         "images/editor/refresh_files.png";
        public static final String SPRITE_PLAY=         "images/editor/play_button.png";
        public static final String SPRITE_STOP=         "images/editor/stop_button.png";
    }
}
