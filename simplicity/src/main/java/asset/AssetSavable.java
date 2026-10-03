package asset;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AssetSavable<T> implements Savable {

    private final Asset<T> asset;

    public AssetSavable(Asset<T> asset) {
        this.asset = asset;
    }

    /** True if this asset can be saved at all: it has a file (not the jar) and its extension has a saver. */
    public static boolean canSave(Asset<?> asset) {
        return asset.hasFile() && asset.getPoolHandler().saverFor(asset.getExtension()) != null;
    }

    @Override
    public String displayName() {
        return asset.getPrefixedPath();
    }

    @Override
    public void save() throws IOException {
        if (!asset.hasFile()) throw new IOException("'" + displayName() + "' is in a read-only pool");
        // get() returns the placeholder when the asset isn't loaded: saving that would overwrite the real file
        if (!asset.isLoaded()) throw new IOException("'" + displayName() + "' isn't loaded");

        T raw = asset.get();
        byte[] bytes;
        try {
            bytes = asset.getPoolHandler().saverFor(asset.getExtension(), raw).encode(raw);
        } catch (IllegalStateException e) {   // no saver for the extension, or it writes another type
            throw new IOException(e.getMessage(), e);
        }

        // write next to the file, then swap: a failed write never leaves a half-written asset
        Path file = asset.getAbsolutePath();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.createDirectories(file.getParent());
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // the pool has one handle per path, so "the same asset" means the same handle
    @Override
    public boolean equals(Object o) {
        return o instanceof AssetSavable<?> other && other.asset == asset;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(asset);
    }
}
