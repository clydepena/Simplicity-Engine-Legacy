package asset;

public interface AssetPoolInterface {
    public <T> Asset<T> get(String path, Class<T> type);
    public <T> void acquire(Asset<T> asset);
    /** Loads in the background: the file read and decode run on a worker, the rest on the main thread. Returns at once. */
    public <T> void acquireAsync(Asset<T> asset);
    public <T> T resolve(String path, Class<T> type);
    /** An object just saved to path becomes that path's LOADED asset, without reading the file back. */
    public <T> Asset<T> adopt(String path, Class<T> type, T object);
    public void addAssetLoader(AssetLoader<?, ?> assetLoader);
    /**
     * Re-keys cached handles after a file or folder was moved on disk; the files themselves aren't touched.
     * @return how many handles moved (0 when nothing referenced the path yet)
     */
    public int move(String oldPath, String newPath);
    /**
     * The file or folder was deleted: its handles become MISSING and show the placeholder; the entries stay.
     * @return how many handles were marked
     */
    public int markMissing(String path);
    /**
     * The file or folder changed or came back: its handles become NOT_LOADED and load again on the next acquire().
     * @return how many handles were reset
     */
    public int reload(String path);
    /**
     * Frees every asset's data; handles stay valid and load again on the next acquire().
     * @return how many handles were reset
     */
    public int unloadAll();
    /** Frees every asset and closes (a pool), or closes every pool (the handler). Closing twice does nothing. */
    public void close();
    /** What get() returns for assets of this type while they have no data. */
    public <T> void setPlaceholder(Class<T> type, T placeholder);
}
