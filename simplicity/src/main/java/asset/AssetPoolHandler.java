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

    @Override
    public <T> Asset<T> get(String path, Class<T> type) {
        return resolveAssetPool(path).get(path, type);
    }

    @Override
    public <T> void acquire(Asset<T> asset) {
        asset.getPool().acquire(asset);
    }

    @Override
    public <T> T resolve(String path, Class<T> type) {
        return resolveAssetPool(path).resolve(path, type);
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
