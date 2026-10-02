package editor;

import asset.Asset;
import asset.AssetPoolHandler;
import renderer.Texture;
import util.Resources;

/**
 * The editor's icons, loaded once from the engine resources (the jar). Fields are handles: they follow a reload,
 * and show the Texture placeholder (or nothing) while missing. Draw them with SImGui.image / imageButton.
 */
public final class EditorIcons {

    public final Asset<Texture> folder, folderOpen, file, addFolder, collapseFolders, refreshFiles, play, stop;

    /** Needs the "engine" pool, the codecs registered, and a current GL context (textures upload on load). */
    public EditorIcons(AssetPoolHandler handler) {
        folder          = load(handler, Resources.Editor.SPRITE_FOLDER);
        folderOpen      = load(handler, Resources.Editor.SPRITE_FOLDER_OPEN);
        file            = load(handler, Resources.Editor.SPRITE_FILE);
        addFolder       = load(handler, Resources.Editor.SPRITE_ADDFOLDER);
        collapseFolders = load(handler, Resources.Editor.SPRITE_COLLAPSEFOLDERS);
        refreshFiles    = load(handler, Resources.Editor.SPRITE_REFRESHFILES);
        play            = load(handler, Resources.Editor.SPRITE_PLAY);
        stop            = load(handler, Resources.Editor.SPRITE_STOP);
    }

    private static Asset<Texture> load(AssetPoolHandler handler, String resource) {
        Asset<Texture> icon = handler.get("engine:" + resource, Texture.class);
        handler.acquire(icon);           // small files: loaded right away; a failure is logged and leaves it MISSING
        return icon;
    }
}
