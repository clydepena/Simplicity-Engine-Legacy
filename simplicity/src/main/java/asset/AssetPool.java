package asset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import asset.Asset.State;
import logger.Logger;

public abstract class AssetPool {

    protected final Path root;
    protected Map<String, Asset<?>> assets = new HashMap<>();
    protected Map<String, AssetLoader<?>> loaders = new HashMap<>();
    protected Map<Class<?>, Object> placeholders = new HashMap<>();

    public AssetPool(String currDirectory) {
        Path temp = Path.of(currDirectory);
        if (!temp.isAbsolute() || temp.getRoot() == null) {
            throw new IllegalArgumentException("not absoloute path: " + currDirectory);
        }
        this.root = temp.normalize();
    }

    public <T> Asset<T> get(String path, Class<T> type) {
        String key = normalizeKey(path);
        Asset<?> existing = assets.get(key);
        if (existing != null) {
            if (existing.getType() != type) {
                throw new IllegalArgumentException("'" + key + "' is a " + existing.getType().getSimpleName() + ", not a " + type.getSimpleName());
            }
            @SuppressWarnings("unchecked")   // safe: the type was checked above
            Asset<T> found = (Asset<T>) existing;
            return found;
        }

        Asset<T> created = new Asset<>(key, type, placeholder(type));
        assets.put(key, created);
        return created;
    }

    public <T> void acquire(Asset<T> asset) {
        switch (asset.getState()) {
            case LOADED, MISSING -> { return; }
            case LOADING -> {
                // it's being loaded further up the call stack: an asset depends on itself through its dependencies
                Logger.error("Asset dependency cycle through '" + asset.getPath() + "'");
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
            data = readAsBytes(root.resolve(asset.getPath()));
        } catch (IOException e) {
            markMissing(asset, "can't read '" + asset.getPath() + "': " + e.getMessage());
            return;
        }

        asset.setState(State.LOADING);
        Object result;
        try {
            result = loader.load(data, this);
        } catch (RuntimeException e) {
            // a corrupt file or a failing dependency; without this the asset would stay LOADING forever
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

    private void markMissing(Asset<?> asset, String reason) {
        asset.setState(State.MISSING);
        Logger.error(reason);
    }

    protected byte[] readAsBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /** The extension without the dot, lowercased: "tiles.PNG" -> "png". */
    protected String getExtension(String path) {
        int lastDotIndex = path.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : path.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    protected boolean isRelative(Path target) {
        return target.normalize().startsWith(root);
    }

    protected Path getRelativePath(Path target) {
        Path normalized = target.normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized);
        }
        throw new IllegalArgumentException("not project-relative: " + target.toString());
    }

    protected <T> T placeholder(Class<T> type) {
        return type.cast(placeholders.get(type));
    }

    protected String normalizeKey(String key) {
        Path p = Path.of(key.replace('\\', '/')).normalize();
        if (p.isAbsolute() || p.getRoot() != null) {
            p = getRelativePath(p);
        }
        if (p.startsWith("..")) throw new IllegalArgumentException("outside the project: " + key);
        StringJoiner newKey = new StringJoiner("/");
        for (Path part : p) newKey.add(part.toString());
        String normalized = newKey.toString();
        if (normalized.isEmpty()) throw new IllegalArgumentException("empty asset path: '" + key + "'");
        return normalized;
    }
}
