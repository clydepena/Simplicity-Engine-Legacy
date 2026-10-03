package asset;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import asset.Asset.State;
import simplicity.Tasks;

public abstract class AssetPool implements AssetPoolInterface {

    protected Path root;
    protected final String prefixIdentifier;
    protected final AssetPoolHandler handler;
    protected Map<String, Asset<?>> assets = new HashMap<>();
    // loaders and placeholders live in the handler, shared by every pool and kept when a pool closes
    // protected Map<String, AssetLoader<?>> loaders = new HashMap<>();
    // protected Map<Class<?>, Object> placeholders = new HashMap<>();
    private boolean closed = false;

    /** The bytes of a pool key, e.g. "images/x.png" (never prefixed). */
    protected abstract byte[] readAsBytes(String key) throws IOException;

    /**
     * @param currDirectory the absolute folder keys are relative to, or null for a pool without one (the jar)
     * @param prefixIdentifier "" for the unprefixed (project) pool, e.g. "engine" for "engine:..." refs
     */
    public AssetPool(String currDirectory, String prefixIdentifier, AssetPoolHandler handler) {
        if (currDirectory != null) {
            Path temp = Path.of(currDirectory);
            if (!temp.isAbsolute() || temp.getRoot() == null) {
                throw new IllegalArgumentException("not absoloute path: " + currDirectory);
            }
            this.root = temp.normalize();
        } else {
            this.root = null;
        }
        this.prefixIdentifier = prefixIdentifier == null ? "" : prefixIdentifier;
        this.handler = handler;
    }

    public Path getRootDir() {
        return root;
    }

    public String getPrefix() {
        return prefixIdentifier;
    }

    @Override
    public <T> Asset<T> get(String path, Class<T> type) {
        if (closed) throw new IllegalStateException("the '" + prefixIdentifier + "' asset pool is closed: '" + path + "'");
        String key = toKey(path);
        Asset<?> existing = assets.get(key);
        if (existing != null) {
            if (existing.getType() != type) {
                throw new IllegalArgumentException("'" + key + "' is a " + existing.getType().getSimpleName() + ", not a " + type.getSimpleName());
            }
            @SuppressWarnings("unchecked")
            Asset<T> found = (Asset<T>) existing;
            return found;
        }

        Asset<T> created = new Asset<>(key, type, this);
        assets.put(key, created);
        return created;
    }

    @Override
    public <T> void acquire(Asset<T> asset) {
        if (closed) {
            // a handle outlived its project: whatever still holds it wasn't cleaned up
            System.err.println("asset used after its pool was closed: '" + asset.getPrefixedPath() + "'");
            return;
        }
        switch (asset.getState()) {
            case LOADED, MISSING -> { return; }
            case LOADING -> {
                // an async load in flight: this caller needs it now, so load here; the async result will be discarded
                if (!asset.isLoadingAsync()) {
                    System.err.println("Asset dependency cycle through '" + asset.getPath() + "'");
                    return;
                }
            }
            case NOT_LOADED -> { }
        }

        AssetLoader<?, ?> loader = handler.loaderFor(asset.getExtension());   // its output is checked against the asset's type below
        if (loader == null) {
            markMissing(asset, "no loader for '" + asset.getPath() + "'");
            return;
        }

        byte[] data;
        try {
            data = readAsBytes(asset.getPath());
        } catch (IOException e) {
            markMissing(asset, "can't read '" + asset.getPath() + "': " + e.getMessage());
            return;
        }

        asset.beginLoad(false);
        Object result;
        try {
            result = loader.loadAtOnce(data, handler);   // both steps on this (main) thread
        } catch (RuntimeException e) {
            markMissing(asset, "failed to load '" + asset.getPath() + "': " + e.getMessage());
            return;
        }
        attach(asset, result);
    }

    /**
     * Like acquire(), but reading the file and decode() run on a worker; finish() and everything else on the main thread.
     * Returns at once: get() shows the placeholder until the asset is LOADED. Call from the main thread.
     */
    @Override
    public <T> void acquireAsync(Asset<T> asset) {
        if (closed) {
            System.err.println("asset used after its pool was closed: '" + asset.getPrefixedPath() + "'");
            return;
        }
        if (asset.getState() != State.NOT_LOADED) return;   // loaded, missing, or a load already running: nothing to start

        AssetLoader<?, ?> loader = handler.loaderFor(asset.getExtension());
        if (loader == null) {
            markMissing(asset, "no loader for '" + asset.getPath() + "'");
            return;
        }
        startAsync(asset, loader);
    }

    // generic in R and D so decode()'s output type lines up with finish()'s input
    private <T, R, D> void startAsync(Asset<T> asset, AssetLoader<R, D> loader) {
        String key = asset.getPath();                  // captured: a move during the load doesn't change what's read
        int ticket = asset.beginLoad(true);            // main thread: a second request now finds it LOADING
        Tasks.submitAsync(
            () -> {                                    // WORKER: bytes -> decoded data only, no pools and no GL
                try {
                    return loader.decode(readAsBytes(key));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            },
            decoded -> {                               // MAIN THREAD
                if (!asset.isCurrentLoad(ticket) || asset.getPool().isClosed()) {
                    // superseded (reload, markMissing, acquire() took over) or the pool closed: drop the work
                    if (decoded instanceof Disposable d) d.dispose();
                    return;
                }
                Object result;
                try {
                    result = loader.finish(decoded, handler);
                } catch (RuntimeException e) {
                    markMissing(asset, "failed to load '" + key + "': " + e.getMessage());
                    return;
                }
                attach(asset, result);
            },
            error -> {                                 // MAIN THREAD
                if (!asset.isCurrentLoad(ticket)) return;
                Throwable cause = error instanceof UncheckedIOException ? error.getCause() : error;
                markMissing(asset, (error instanceof UncheckedIOException ? "can't read '" : "failed to load '")
                    + key + "': " + cause.getMessage());
            });
    }

    /** The last step of both acquire paths: type check, then LOADED, or MISSING and the result freed. */
    private <T> void attach(Asset<T> asset, Object result) {
        if (!asset.getType().isInstance(result)) {
            if (result instanceof Disposable d) d.dispose();   // it was created, so it must be freed
            markMissing(asset, "'" + asset.getPath() + "' loaded as "
                + (result == null ? "nothing" : result.getClass().getSimpleName()) + ", not a " + asset.getType().getSimpleName());
            return;
        }
        asset.setRawAsset(asset.getType().cast(result));
        asset.setState(State.LOADED);
    }

    @Override
    public <T> T resolve(String path, Class<T> type) {
        Asset<T> asset = get(path, type);
        acquire(asset);
        return asset.get();
    }

    /**
     * An object was just saved to path (e.g. "Save as"): makes it that path's LOADED asset without reading the file back.
     * Reuses the path's handle if one exists without data (never loaded, missing, or mid-load: a running async load is discarded).
     * Adopting the object the handle already holds does nothing.
     * @throws IllegalStateException if the handle already holds a different object: to overwrite an existing asset's file,
     *         call reload(path) after writing it instead, so everything holding the handle picks up the new contents
     */
    @Override
    public <T> Asset<T> adopt(String path, Class<T> type, T object) {
        requireWritable();                   // a saved file can't be in the jar
        if (object == null) throw new IllegalArgumentException("nothing to adopt for '" + path + "'");
        Asset<T> asset = get(path, type);    // throws if the pool is closed or the path holds another type
        if (asset.getState() == State.LOADED) {
            if (asset.get() == object) return asset;
            throw new IllegalStateException("'" + asset.getPrefixedPath() + "' already holds a loaded asset; reload it instead");
        }
        asset.beginLoad(false);              // new ticket: an async load still running for this handle becomes stale
        attach(asset, object);
        return asset;
    }

    /**
     * Re-keys cached handles after a file or folder was moved on disk; the files themselves aren't touched.
     * Loaded assets stay loaded, and every field holding a handle sees the new path.
     * @return how many handles moved (0 when nothing referenced the path yet)
     */
    @Override
    public int move(String oldPath, String newPath) {
        requireWritable();
        String from = toKey(oldPath);
        String to = toKey(newPath);
        if (from.equals(to)) return 0;
        if (to.startsWith(from + "/")) throw new IllegalArgumentException("can't move '" + from + "' into itself");

        Map<String, String> renames = collectMoves(from, to);
        checkFree(renames.values(), renames.keySet());   // a failed check changes nothing
        // take them all out first, so moved entries never overwrite each other
        for (Asset<?> asset : takeOut(renames.keySet())) putIn(asset, renames.get(asset.getPath()));
        return renames.size();
    }

    /**
     * The file or folder was deleted: drops the loaded data and marks the handles MISSING, so they show the placeholder.
     * The entries stay, so every holder keeps a valid handle and reload() can bring them back.
     * @return how many handles were marked
     */
    @Override
    public int markMissing(String path) {
        List<Asset<?>> found = assetsAt(toKey(path));
        for (Asset<?> asset : found) asset.dispose(State.MISSING);
        return found.size();
    }

    /**
     * The file or folder changed or came back: drops the loaded data and marks the handles NOT_LOADED,
     * so the next acquire() loads them again. Nothing is read here.
     * @return how many handles were reset
     */
    @Override
    public int reload(String path) {
        List<Asset<?>> found = assetsAt(toKey(path));
        for (Asset<?> asset : found) asset.dispose(State.NOT_LOADED);
        return found.size();
    }

    /**
     * Frees every asset's data; the handles stay valid and load again on the next acquire().
     * @return how many handles were reset
     */
    @Override
    public int unloadAll() {
        for (Asset<?> asset : assets.values()) asset.dispose(State.NOT_LOADED);
        return assets.size();
    }

    public void emptyAssets() {
        for (Asset<?> asset : assets.values()) {
            try {
                asset.dispose(State.MISSING);
            } catch (RuntimeException e) {
                System.err.println("failed to dispose '" + asset.getPrefixedPath() + "': " + e.getMessage());
            }
        }
        assets.clear();
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    /**
     * Frees every asset's data, drops the entries and leaves the handler, so the prefix is free for a new pool.
     * Leftover handles become MISSING and show the placeholder. Closing twice does nothing.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;                       // first, so nothing can load through this pool while it closes
        for (Asset<?> asset : assets.values()) {
            try {
                asset.dispose(State.MISSING);
            } catch (RuntimeException e) {   // one bad asset must not stop the rest from being freed
                System.err.println("failed to dispose '" + asset.getPrefixedPath() + "': " + e.getMessage());
            }
        }
        assets.clear();
        handler.unregister(this);            // last: the prefix only frees up once everything is cleaned
    }

    public boolean isClosed() {
        return closed;
    }

    /** Same as the handler's: placeholders are shared by every pool. Not owned: never disposed by a pool. */
    @Override
    public <T> void setPlaceholder(Class<T> type, T placeholder) {
        handler.setPlaceholder(type, placeholder);
    }

    /** The handle at key, or every handle inside the folder key ("tiles" must not catch "tiles2/"). */
    private List<Asset<?>> assetsAt(String key) {
        List<Asset<?>> found = new ArrayList<>();
        for (Map.Entry<String, Asset<?>> entry : assets.entrySet()) {
            String k = entry.getKey();
            if (k.equals(key) || k.startsWith(key + "/")) found.add(entry.getValue());
        }
        return found;
    }

    /*
     * The steps of a move, package-private so AssetPoolHandler can run them across two pools.
     */

    void requireWritable() {
        if (root == null) throw new UnsupportedOperationException("the '" + prefixIdentifier + "' asset pool is read-only");
    }

    /** Old key -> new key for the file itself, or everything inside the folder ("tiles" must not catch "tiles2/"). */
    Map<String, String> collectMoves(String fromKey, String toKey) {
        Map<String, String> renames = new HashMap<>();
        for (String key : assets.keySet()) {
            if (key.equals(fromKey)) renames.put(key, toKey);
            else if (key.startsWith(fromKey + "/")) renames.put(key, toKey + key.substring(fromKey.length()));
        }
        return renames;
    }

    /** Throws if a target key is taken by a handle that isn't itself leaving this pool's key. */
    void checkFree(Collection<String> targets, Set<String> leaving) {
        for (String target : targets) {
            if (assets.containsKey(target) && !leaving.contains(target)) {
                throw new IllegalStateException("can't move to '" + prefixPath(target) + "': that path is already in use");
            }
        }
    }

    /** Removes and returns the handles; their paths are still the old keys. */
    List<Asset<?>> takeOut(Set<String> keys) {
        List<Asset<?>> taken = new ArrayList<>();
        for (String key : keys) taken.add(assets.remove(key));
        return taken;
    }

    /** Files a handle under a new key in this pool (it may come from another pool). */
    void putIn(Asset<?> asset, String key) {
        asset.setPath(key);
        asset.setPool(this);
        assets.put(key, asset);
    }

    /** Same as the handler's: loaders are shared by every pool. */
    @Override
    public void addAssetLoader(AssetLoader<?, ?> assetLoader) {
        handler.addAssetLoader(assetLoader);
    }

    public String prefixPath(String path) {
        return AssetHelpers.addPrefix(path, prefixIdentifier);
    }

    /** A path or ref -> this pool's key; throws if it's prefixed for another pool. */
    protected String toKey(String path) {
        String prefix = AssetHelpers.extractPrefix(path);
        if (!prefix.isEmpty() && !prefix.equals(prefixIdentifier)) {
            throw new IllegalArgumentException("'" + path + "' does not belong to the '" + prefixIdentifier + "' pool");
        }
        return AssetHelpers.normalizeKey(AssetHelpers.stripPrefix(path), root);
    }

    protected <T> T placeholder(Class<T> type) {
        return handler.placeholderFor(type);
    }

    protected static void markMissing(Asset<?> asset, String reason) {
        asset.setState(State.MISSING);
        System.err.println(reason);
    }

    // replaced by Asset.getExtension(), which only looks at the file name ("images.v2/grass" has no extension)
    // protected String getExtension(String path) {
    //     int lastDotIndex = path.lastIndexOf('.');
    //     return (lastDotIndex == -1) ? "" : path.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    // }

    public AssetPoolHandler getHandler() {
        return handler;
    }
}
