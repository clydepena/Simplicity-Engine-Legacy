package simplicity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import asset.AssetPoolHandler;
import asset.AssetTypeAdapterFactory;
import asset.AssetTypeAdapterFactory.Acquire;
import components.Component;
import components.ComponentDeserializer;

/**
 * The one Gson for saving and loading game objects (levels, copies): components by their type,
 * Asset<T> fields as their prefixed paths, loaded right away when read so sprites can be drawn at once.
 */
public final class GameObjectGson {

    private GameObjectGson() {}

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
        .registerTypeAdapterFactory(new AssetTypeAdapterFactory(AssetPoolHandler.GetInstance(), Acquire.NOW))
        .create();
}
