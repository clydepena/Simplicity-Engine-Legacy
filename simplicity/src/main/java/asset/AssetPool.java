package asset;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import asset.Asset.State;

public abstract class AssetPool implements AssetPoolInterface {

    protected final Path root;
    protected final String prefixIdentifier;
    protected final AssetPoolHandler handler;
    protected Map<String, Asset<?>> assets = new HashMap<>();
    protected Map<String, AssetLoader<?>> loaders = new HashMap<>();
    protected Map<Class<?>, Object> placeholders = new HashMap<>();
    
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
        String prefix = AssetHelpers.extractPrefix(path);
        if (!prefix.isEmpty() && !prefix.equals(prefixIdentifier)) {
            throw new IllegalArgumentException("'" + path + "' does not belong to the '" + prefixIdentifier + "' pool");
        }
        String key = AssetHelpers.normalizeKey(AssetHelpers.stripPrefix(path), root);
        Asset<?> existing = assets.get(key);
        if (existing != null) {
            if (existing.getType() != type) {
                throw new IllegalArgumentException("'" + key + "' is a " + existing.getType().getSimpleName() + ", not a " + type.getSimpleName());
            }
            @SuppressWarnings("unchecked")
            Asset<T> found = (Asset<T>) existing;
            return found;
        }

        Asset<T> created = new Asset<>(key, type, placeholder(type), this);
        assets.put(key, created);
        return created;
    }

    @Override
    public <T> void acquire(Asset<T> asset) {
        switch (asset.getState()) {
            case LOADED, MISSING -> { return; }
            case LOADING -> {
                System.err.println("Asset dependency cycle through '" + asset.getPath() + "'");
                return;
            }
            case NOT_LOADED -> { }
        }

        @SuppressWarnings("unchecked")       // the loader's output is checked against the asset's type below
        AssetLoader<T> loader = (AssetLoader<T>) loaders.get(getExtension(asset.getPath()));
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

        asset.setState(State.LOADING);
        Object result;
        try {
            result = loader.load(data, handler);
        } catch (RuntimeException e) {
            markMissing(asset, "failed to load '" + asset.getPath() + "': " + e.getMessage());
            return;
        }
        if (!asset.getType().isInstance(result)) {
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

    public <T> void addAssetLoader(AssetLoader<T> assetLoader) {
        List<String> extensions = assetLoader.supportedExtensions();
        for (String ext : extensions) {
            loaders.put(ext.toLowerCase(Locale.ROOT), assetLoader);
        }
    }

    public String prefixPath(String path) {
        return AssetHelpers.addPrefix(path, prefixIdentifier);
    }

    protected <T> T placeholder(Class<T> type) {
        return type.cast(placeholders.get(type));
    }

    protected static void markMissing(Asset<?> asset, String reason) {
        asset.setState(State.MISSING);
        System.err.println(reason);
    }

    protected String getExtension(String path) {
        int lastDotIndex = path.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : path.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
