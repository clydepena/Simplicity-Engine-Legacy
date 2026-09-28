package codecs;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import asset.Asset;
import asset.AssetLoader;
import asset.AssetPoolHandler;
import asset.AssetSaver;
import components.Spritesheet;
import renderer.Texture;

/**
 * A .sheet file: a texture cut into an even grid of sprites.
 *
 *   { "texture": "images/TilesSpritesheet.png", "spriteWidth": 32, "spriteHeight": 32, "numSprites": 48, "spacing": 0 }
 *
 * The texture is a path like any asset reference ("engine:..." for an engine texture); it's loaded when the sheet is.
 */
public class SpriteSheetCodec implements AssetLoader<Spritesheet, SpriteSheetCodec.SheetData>, AssetSaver<Spritesheet> {

    public static final List<String> EXTENSIONS = List.of("sheet");

    /** The file's contents, in both directions. The sprites themselves aren't stored: they're cut from these numbers. */
    public record SheetData(String texture, int spriteWidth, int spriteHeight, int numSprites, int spacing) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public List<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public Class<Spritesheet> savedType() {
        return Spritesheet.class;
    }

    /** Worker: JSON -> numbers and the texture's path. Nothing is looked up here. */
    @Override
    public SheetData decode(byte[] bytes) {
        SheetData data;
        try {
            data = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), SheetData.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("not a valid sheet file: " + e.getMessage(), e);
        }
        if (data == null) throw new IllegalArgumentException("empty sheet file");
        if (data.texture() == null || data.texture().isBlank()) throw new IllegalArgumentException("no \"texture\"");
        if (data.spriteWidth() <= 0 || data.spriteHeight() <= 0) {
            throw new IllegalArgumentException("sprite size must be positive, got " + data.spriteWidth() + "x" + data.spriteHeight());
        }
        if (data.numSprites() < 0) throw new IllegalArgumentException("negative \"numSprites\"");
        if (data.spacing() < 0) throw new IllegalArgumentException("negative \"spacing\"");
        return data;
    }

    /** Main thread: loads the texture (a dependency: its own handle, never disposed by the sheet), then cuts the sprites. */
    @Override
    public Spritesheet finish(SheetData data, AssetPoolHandler handler) {
        Asset<Texture> texture = handler.get(data.texture(), Texture.class);
        handler.acquire(texture);
        if (!texture.isLoaded()) {
            // a sheet can't be cut without its image's size; the texture's own error was already logged
            throw new IllegalStateException("its texture '" + data.texture() + "' couldn't be loaded");
        }
        Texture image = texture.get();
        int fits = Spritesheet.capacity(image.getWidth(), image.getHeight(), data.spriteWidth(), data.spriteHeight(), data.spacing());
        if (data.numSprites() > fits) {
            throw new IllegalStateException(data.numSprites() + " sprites of " + data.spriteWidth() + "x" + data.spriteHeight()
                + " don't fit in '" + data.texture() + "' (" + image.getWidth() + "x" + image.getHeight() + ": room for " + fits + ")");
        }
        return new Spritesheet(texture, data.spriteWidth(), data.spriteHeight(), data.numSprites(), data.spacing());
    }

    /** Main thread: the sheet -> its file. The texture is written as its path. */
    @Override
    public byte[] encode(Spritesheet sheet) {
        Asset<Texture> texture = sheet.getTextureAsset();
        if (texture == null) {
            throw new IllegalStateException("this sheet was built from a raw Texture (legacy), so its texture has no path to save");
        }
        SheetData data = new SheetData(texture.getPrefixedPath(),
            sheet.getSpriteWidth(), sheet.getSpriteHeight(), sheet.size(), sheet.getSpacing());
        return GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
    }
}
