package scenes;

public interface SceneInitializer {
    public abstract void init(World2DLayer world);
    public abstract void loadResources(World2DLayer world);
    // public abstract String getLevelPath();
}
