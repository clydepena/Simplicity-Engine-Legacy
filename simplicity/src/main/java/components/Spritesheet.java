package components;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;

import asset.Asset;
import renderer.Texture;

public class Spritesheet {

    private Texture texture;
    private Asset<Texture> textureAsset;   // null for sheets built by the legacy code from a raw Texture
    private List<Sprite> sprites;
    private int spriteWidth, spriteHeight, spacing;

    /** A sheet on a pool texture: can be saved (SpriteSheetCodec), since it knows its texture's path. The texture must be loaded. */
    public Spritesheet(Asset<Texture> texture, int spriteWidth, int spriteHeight, int numSprites, int spacing) {
        this(texture.get(), spriteWidth, spriteHeight, numSprites, spacing);
        this.textureAsset = texture;
    }

    /** Legacy (util.AssetPool): a sheet on a raw Texture. Works for drawing, but can't be saved. */
    public Spritesheet(Texture texture, int spriteWidth, int spriteHeight, int numSprites, int spacing) {
        this.sprites = new ArrayList<>();
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.spacing = spacing;

        this.texture = texture;
        int currentX = 0;
        int currentY = texture.getHeight() - spriteHeight;
        for(int i = 0; i < numSprites; i++) {
            float topY = (currentY + spriteHeight) / (float) texture.getHeight();
            float rightX = (currentX + spriteWidth) / (float) texture.getWidth();
            float leftX = currentX / (float) texture.getWidth();
            float bottomY = currentY / (float) texture.getHeight();

            Vector2f[] texCoords = {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
            };

            Sprite sprite = new Sprite();
            sprite.setTexture(this.texture);
            sprite.setTexCoords(texCoords);
            sprite.setWidth(spriteWidth);
            sprite.setHeight(spriteHeight);

            this.sprites.add(sprite);

            currentX += spriteWidth + spacing;
            if(currentX >= texture.getWidth()) {
                currentX = 0;
                currentY -= spriteHeight + spacing;
            }
        }
    }

    /**
     * How many sprites the constructor can cut before one would reach outside the texture.
     * It walks exactly like the constructor, which only starts a new row once x passes the edge:
     * with a width that isn't a multiple of the sprite size, a row's last sprite would stick out.
     */
    public static int capacity(int textureWidth, int textureHeight, int spriteWidth, int spriteHeight, int spacing) {
        int count = 0;
        int x = 0;
        int y = textureHeight - spriteHeight;
        while (y >= 0 && x + spriteWidth <= textureWidth) {
            count++;
            x += spriteWidth + spacing;
            if (x >= textureWidth) {
                x = 0;
                y -= spriteHeight + spacing;
            }
        }
        return count;
    }

    public Sprite getSprite(int index) {
        return this.sprites.get(index);
    }

    public int size() {
        return sprites.size();
    }

    /** The texture's handle, or null for a legacy sheet (built from a raw Texture). */
    public Asset<Texture> getTextureAsset() {
        return textureAsset;
    }

    public Texture getTexture() {
        return texture;
    }

    public int getSpriteWidth() {
        return spriteWidth;
    }

    public int getSpriteHeight() {
        return spriteHeight;
    }

    public int getSpacing() {
        return spacing;
    }
}
