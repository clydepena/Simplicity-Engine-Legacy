package asset;

public final class Asset<T> {

    public enum State { NOT_LOADED, LOADING, LOADED, MISSING }

    private String path;
    private final Class<T> type;
    private T rawAsset;
    private final T fallbackAsset;
    private State state = State.NOT_LOADED;

    Asset(String path, Class<T> type, T fallbackAsset) {
        this.path = path;
        this.type = type;
        this.fallbackAsset = fallbackAsset;
    }

    public T get() {
        return rawAsset != null ? rawAsset : fallbackAsset;
    }

    public String getPath() {
        return path;
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

    void setState(State state) {
        this.state = state;
    }
}
