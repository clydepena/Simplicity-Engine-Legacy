package components;

import org.joml.Vector2f;

import asset.Asset;
import renderer.Texture;

public class Sprite {

    private float width, height;

    // saved as its path ("engine:images/x.png") by AssetTypeAdapterFactory; older files stored the texture
    // inline ({"filepath": ...}) and are read through the same adapter
    private Asset<Texture> texture = null;
    private Vector2f[] texCoords = {
            new Vector2f(1, 1),
            new Vector2f(1, 0),
            new Vector2f(0, 0),
            new Vector2f(0, 1)
        };

    /** The texture to draw, or null; the placeholder while it isn't loaded. Asked each time, so it follows a reload. */
    public Texture getTexture() {
        return texture == null ? null : texture.get();
    }

    /** The handle, e.g. to show or change which texture this sprite uses. */
    public Asset<Texture> getTextureAsset() {
        return this.texture;
    }

    public Vector2f[] getTexCoords() {
        return this.texCoords;
    }

    public void setTexture(Asset<Texture> texture) {
        this.texture = texture;
    }

    public void setTexCoords(Vector2f[] texCoords) {
        this.texCoords = texCoords;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public int getTexId() {
        Texture t = getTexture();
        return t == null ? -1 : t.getId();
    }
}
