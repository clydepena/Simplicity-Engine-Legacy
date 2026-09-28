package asset;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class AssetPoolHandler implements AssetPoolInterface {

    @FunctionalInterface
    public interface FileReadingCallback {
        /** @param root the pool's folder, null for a pool without one; @param key e.g. "images/x.png" */
        public byte[] readAsBytes(Path root, String key) throws IOException;

        /** Loose files under the pool's folder. */
        public static final FileReadingCallback FILE_SYSTEM = (root, key) -> Files.readAllBytes(root.resolve(key));

        /** Resources inside the jar (or build/resources when run from the IDE). */
        public static final FileReadingCallback CLASSPATH = (root, key) -> {
            try (InputStream in = AssetPoolHandler.class.getResourceAsStream("/" + key)) {
                if (in == null) throw new FileNotFoundException("no resource '" + key + "'");
                return in.readAllBytes();
            }
        };
    }

    private Map<String, AssetPool> assetPools = new HashMap<>();
    // kept here so pools created later (a newly opened project) get them too; close() keeps them
    private final Map<String, AssetLoader<?, ?>> loaders = new HashMap<>();   // by lowercase extension
    private final Map<String, AssetSaver<?>> savers = new HashMap<>();        // by lowercase extension
    private final Map<Class<?>, Object> placeholders = new HashMap<>();
    private static AssetPoolHandler instance;

    private AssetPoolHandler() {}

    public static AssetPoolHandler GetInstance() {
        if (instance == null) {
            instance = new AssetPoolHandler();
        }
        return instance;
    }

    @Override
    public <T> Asset<T> get(String path, Class<T> type) {
        return resolveAssetPool(path).get(path, type);
    }

    @Override
    public <T> void acquire(Asset<T> asset) {
        asset.getPool().acquire(asset);
    }

    @Override
    public <T> void acquireAsync(Asset<T> asset) {
        asset.getPool().acquireAsync(asset);
    }

    @Override
    public <T> T resolve(String path, Class<T> type) {
        return resolveAssetPool(path).resolve(path, type);
    }

    @Override
    public <T> Asset<T> adopt(String path, Class<T> type, T object) {
        return resolveAssetPool(path).adopt(path, type, object);
    }

    /**
     * Same prefix: a move inside that pool. Different prefixes: the handles leave the old pool and join the new one.
     * Both pools must have a folder; the jar can't be moved into or out of (copy an engine asset instead).
     */
    @Override
    public int move(String oldRef, String newRef) {
        AssetPool from = resolveAssetPool(oldRef);
        AssetPool to = resolveAssetPool(newRef);
        if (from == to) return from.move(oldRef, newRef);

        from.requireWritable();
        to.requireWritable();
        Map<String, String> renames = from.collectMoves(from.toKey(oldRef), to.toKey(newRef));
        to.checkFree(renames.values(), Set.of());   // nothing leaves the target pool; a failed check changes nothing
        for (Asset<?> asset : from.takeOut(renames.keySet())) {
            to.putIn(asset, renames.get(asset.getPath()));   // getPath() is still the old key here
        }
        return renames.size();
    }

    @Override
    public int markMissing(String path) {
        return resolveAssetPool(path).markMissing(path);
    }

    @Override
    public int reload(String path) {
        return resolveAssetPool(path).reload(path);
    }

    @Override
    public int unloadAll() {
        int count = 0;
        for (AssetPool pool : assetPools.values()) count += pool.unloadAll();
        return count;
    }

    /** Closes every pool, the engine's too: call it before the GL context is destroyed. Loaders and placeholders are kept. */
    @Override
    public void close() {
        for (AssetPool pool : new ArrayList<>(assetPools.values())) pool.close();   // a copy: each close() unregisters itself
    }

    /** Used by every pool, now and created later; a later loader for the same extension replaces the earlier one. */
    @Override
    public void addAssetLoader(AssetLoader<?, ?> assetLoader) {
        for (String ext : assetLoader.supportedExtensions()) {
            loaders.put(ext.toLowerCase(Locale.ROOT), assetLoader);
        }
    }

    /** A later saver for the same extension replaces the earlier one. */
    public void addAssetSaver(AssetSaver<?> assetSaver) {
        for (String ext : assetSaver.supportedExtensions()) {
            savers.put(ext.toLowerCase(Locale.ROOT), assetSaver);
        }
    }

    /**
     * Registers a codec in every role it has: as a loader, a saver, or both.
     * @throws IllegalArgumentException if it's neither
     */
    public void addAssetCodec(Object codec) {
        boolean known = false;
        if (codec instanceof AssetLoader<?, ?> loader) { addAssetLoader(loader); known = true; }
        if (codec instanceof AssetSaver<?> saver) { addAssetSaver(saver); known = true; }
        if (!known) {
            throw new IllegalArgumentException(codec.getClass().getSimpleName() + " is neither an AssetLoader nor an AssetSaver");
        }
    }

    /** Used by every pool, now and created later. Not owned: never disposed by a pool. */
    @Override
    public <T> void setPlaceholder(Class<T> type, T placeholder) {
        placeholders.put(type, placeholder);
    }

    /** The loader for an extension ("png", as Asset.getExtension() gives it; any case), or null if the engine can't load it. */
    public AssetLoader<?, ?> loaderFor(String extension) {
        return loaders.get(extension.toLowerCase(Locale.ROOT));
    }

    /** The saver for an extension ("sheet", as Asset.getExtension() gives it), or null if files of that type can't be saved. */
    public AssetSaver<?> saverFor(String extension) {
        return savers.get(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * The saver for an extension, checked against the object to save, e.g. saverFor("sheet", sheet).
     * @throws IllegalStateException if the extension has no saver, or its saver writes another type
     */
    public <T> AssetSaver<T> saverFor(String extension, T object) {
        AssetSaver<?> saver = saverFor(extension);
        if (saver == null) {
            throw new IllegalStateException("'." + extension + "' files can't be saved: no saver for them");
        }
        if (!saver.savedType().isInstance(object)) {
            throw new IllegalStateException("a " + object.getClass().getSimpleName() + " can't be saved as '." + extension
                + "': that saver writes " + saver.savedType().getSimpleName());
        }
        @SuppressWarnings("unchecked")   // savedType() was checked against the object just above
        AssetSaver<T> typed = (AssetSaver<T>) saver;
        return typed;
    }

    <T> T placeholderFor(Class<T> type) {
        return type.cast(placeholders.get(type));
    }
    
    /**
     * @param keyPrefix "" for the project pool, e.g. "engine" for "engine:..." refs
     * @param workingDir the pool's absolute folder, or null for a pool without one (the jar)
     */
    public AssetPool createAssetPool(String keyPrefix, String workingDir, FileReadingCallback callback) {
        if (assetPools.containsKey(keyPrefix)) {
            throw new IllegalStateException("an asset pool with prefix '" + keyPrefix + "' already exists");
        }
        AssetPool assetPool = new AssetPool(workingDir, keyPrefix, this) {
            @Override
            protected byte[] readAsBytes(String key) throws IOException {
                return callback.readAsBytes(root, key);
            }
        };
        assetPools.put(keyPrefix, assetPool);
        return assetPool;
    }

    /** Called by AssetPool.close(). */
    void unregister(AssetPool pool) {
        assetPools.remove(pool.getPrefix(), pool);   // only if that prefix still maps to this pool
    }

    protected AssetPool resolveAssetPool(String path) {
        String prefix = AssetHelpers.extractPrefix(path);
        AssetPool pool = assetPools.get(prefix);
        if (pool == null) {
            throw new IllegalArgumentException(prefix.isEmpty()
                ? "no project asset pool for '" + path + "'"
                : "no asset pool for prefix '" + prefix + "' in '" + path + "'");
        }
        return pool;
    }
}
