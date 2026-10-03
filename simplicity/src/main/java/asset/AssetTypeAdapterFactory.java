package asset;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Lets Gson save an Asset<T> field as its path and read it back as the shared handle:
 * "images/tiles.png", or "engine:images/editor/play.png" for another pool.
 * This is how other files refer to an asset; the asset's own file is written by its AssetSaver.
 *
 *   Gson gson = new GsonBuilder().registerTypeAdapterFactory(new AssetTypeAdapterFactory(handler, Acquire.NOW)).create();
 *
 * The field must say what it holds (Asset<Texture>): a raw Asset has no type to load it as.
 */
public final class AssetTypeAdapterFactory implements TypeAdapterFactory {

    /** What reading a path does besides finding the handle. */
    public enum Acquire {
        /** Only the handle: the owner calls acquire() itself when it needs the data. */
        NONE,
        /** Loads right away on this (main) thread: the data is there when fromJson returns. */
        NOW,
        /** Starts a background load: the placeholder shows until it's done. */
        BACKGROUND
    }

    private final AssetPoolHandler handler;
    private final Acquire acquire;

    public AssetTypeAdapterFactory(AssetPoolHandler handler, Acquire acquire) {
        this.handler = handler;
        this.acquire = acquire;
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> typeToken) {
        if (typeToken.getRawType() != Asset.class) return null;   // not ours: Gson keeps looking

        Type type = typeToken.getType();
        if (!(type instanceof ParameterizedType parameterized)) {
            throw new JsonParseException("a raw Asset field can't be read: declare what it holds, e.g. Asset<Texture>");
        }
        Class<?> assetType = TypeToken.get(parameterized.getActualTypeArguments()[0]).getRawType();

        @SuppressWarnings("unchecked")   // R is Asset<assetType>
        TypeAdapter<R> adapter = (TypeAdapter<R>) new AssetAdapter<>(assetType).nullSafe();
        return adapter;
    }

    private final class AssetAdapter<T> extends TypeAdapter<Asset<T>> {
        private final Class<T> assetType;

        AssetAdapter(Class<T> assetType) {
            this.assetType = assetType;
        }

        @Override
        public void write(JsonWriter out, Asset<T> asset) throws IOException {
            out.value(asset.getPrefixedPath());
        }

        @Override
        public Asset<T> read(JsonReader in) throws IOException {
            String path;
            if (in.peek() == JsonToken.BEGIN_OBJECT) {
                path = readLegacyObject(in);
            } else if (in.peek() == JsonToken.STRING) {
                path = in.nextString();
            } else {
                throw new JsonParseException("expected an asset path string at " + in.getPath() + ", found " + in.peek());
            }
            Asset<T> asset;
            try {
                asset = handler.get(path, assetType);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // unknown prefix, no project open, bad path, or the path is already another type
                throw new JsonParseException("can't refer to asset '" + path + "' at " + in.getPath() + ": " + e.getMessage(), e);
            }
            // a missing file is not an error here: loading it marks the handle MISSING and shows the placeholder
            switch (acquire) {
                case NOW -> handler.acquire(asset);
                case BACKGROUND -> handler.acquireAsync(asset);
                case NONE -> { }
            }
            return asset;
        }
    }

    /** Where references from files saved before the asset pools pointed: the legacy pool loaded them from the jar. */
    public static final String LEGACY_PREFIX = "engine:";

    /**
     * Files saved with the legacy util.AssetPool stored the object itself instead of a path, e.g. a sprite's
     * texture as {"filepath": "images/TilesSpritesheet.png", "width": 384, "height": 128}. Only its path is kept;
     * the next save writes the plain "engine:images/TilesSpritesheet.png".
     */
    private static String readLegacyObject(JsonReader in) {
        String at = in.getPath();
        JsonElement element = JsonParser.parseReader(in);
        JsonElement filepath = element.isJsonObject() ? element.getAsJsonObject().get("filepath") : null;
        if (filepath == null || !filepath.isJsonPrimitive()) {
            throw new JsonParseException("expected an asset path string at " + at + ", found an object without a \"filepath\"");
        }
        return LEGACY_PREFIX + filepath.getAsString();
    }
}
