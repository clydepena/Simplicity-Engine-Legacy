package asset;

public final class Asset<T> {

    public enum State { NOT_LOADED, LOADING, LOADED, MISSING }

    private String path;
    private final Class<T> type;
    private T rawAsset;
    private State state = State.NOT_LOADED;
    private AssetPool assetPool;

    Asset(String path, Class<T> type, AssetPool assetPool) {
        this.path = path;
        this.type = type;
        this.assetPool = assetPool;
    }

    /** The loaded asset, or its pool's current placeholder for the type while there's none (may be null). */
    public T get() {
        return rawAsset != null ? rawAsset : assetPool.placeholder(type);
    }

    /*
     * Paths, for "engine:images/tiles/grass.png". All are worked out from the current key, so they follow a move().
     */

    /** The pool key, without prefix: "images/tiles/grass.png". */
    public String getPath() {
        return path;
    }

    /** The path as saved in files, so it finds the right pool again: "engine:images/tiles/grass.png" (no prefix in the project pool). */
    public String getPrefixedPath() {
        return assetPool.prefixPath(path);
    }

    /** The pool's prefix: "engine", or "" for the project pool. */
    public String getPrefix() {
        return assetPool.getPrefix();
    }

    /** The last part of the path: "grass.png". */
    public String getFileName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** The file name without its extension: "grass" ("archive.tar.gz" -> "archive.tar"). */
    public String getName() {
        String fileName = getFileName();
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);   // a leading dot (".gitignore") is part of the name
    }

    /** The extension without the dot, lowercased, as loaders are registered: "png"; "" if there's none. */
    public String getExtension() {
        String fileName = getFileName();
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? "" : fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    /** The folder inside the pool: "images/tiles", or "" at the pool's top level. */
    public String getDirectory() {
        int slash = path.lastIndexOf('/');
        return slash == -1 ? "" : path.substring(0, slash);
    }

    /** True if the asset lives in a folder on disk (so it has a file and can be saved); false for the jar. */
    public boolean hasFile() {
        return assetPool.getRootDir() != null;
    }

    /** Where the file is on disk: "C:/Games/MyGame/images/tiles/grass.png"; null for an asset in the jar (see hasFile()). */
    public java.nio.file.Path getAbsolutePath() {
        java.nio.file.Path root = assetPool.getRootDir();
        return root == null ? null : root.resolve(path);
    }

    public AssetPool getPool() {
        return assetPool;
    }

    public AssetPoolHandler getPoolHandler() {
        return assetPool.getHandler();
    }

    public Class<T> getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public boolean isLoaded() {
        return state == State.LOADED;
    }

    void setRawAsset(T asset) {
        this.rawAsset = asset;
    }

    void setPath(String path) {
        this.path = path;
    }

    void setPool(AssetPool assetPool) {
        this.assetPool = assetPool;
    }

    // which load is the current one: a load that finishes after a newer one started (reload, or acquire() taking over) is discarded
    private int loadTicket = 0;
    private boolean loadingAsync = false;

    /** Marks the asset LOADING and returns this load's ticket; any earlier load still running becomes stale. */
    int beginLoad(boolean async) {
        state = State.LOADING;
        loadingAsync = async;
        return ++loadTicket;
    }

    /** True while the load with this ticket is still the one the asset is waiting for. */
    boolean isCurrentLoad(int ticket) {
        return state == State.LOADING && loadTicket == ticket;
    }

    boolean isLoadingAsync() {
        return state == State.LOADING && loadingAsync;
    }

    void setState(State state) {
        this.state = state;
    }

    /**
     * Frees the loaded data and moves to next (NOT_LOADED or MISSING), so the state always matches the data.
     * Package-private: only the pool decides when shared data dies. Placeholders belong to the pool, so they're never disposed here.
     */
    void dispose(State next) {
        if (next == State.LOADED || next == State.LOADING) {
            throw new IllegalArgumentException("a disposed asset has no data, so it can't be " + next);
        }
        if (rawAsset instanceof Disposable d) d.dispose();
        rawAsset = null;
        state = next;
    }
}
