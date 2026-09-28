package asset;

public interface AssetPoolInterface {
    public <T> Asset<T> get(String path, Class<T> type);
    public <T> void acquire(Asset<T> asset);
    public <T> T resolve(String path, Class<T> type);
}
