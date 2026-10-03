package components;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;

import asset.Asset;
import renderer.Texture;

public class Spritesheet {

    private Asset<Texture> textureAsset;
    private List<Sprite> sprites;
    private int spriteWidth, spriteHeight, spacing;

    // Legacy (util.AssetPool, moved to to-refactor/depreciated): a sheet on a raw Texture. Sprites now hold an
    // Asset<Texture>, so a raw Texture can't be handed to them; build sheets from the pool or load a .sheet file.
    // public Spritesheet(Texture texture, int spriteWidth, int spriteHeight, int numSprites, int spacing) { ... }

    /** A sheet on a pool texture: can be saved (SpriteSheetCodec), since it knows its texture's path. The texture must be loaded. */
    public Spritesheet(Asset<Texture> textureAsset, int spriteWidth, int spriteHeight, int numSprites, int spacing) {
        this.sprites = new ArrayList<>();
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.spacing = spacing;
        this.textureAsset = textureAsset;

        Texture texture = textureAsset.get();   // its size is needed to cut the sprites
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
            sprite.setTexture(textureAsset);
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

    /** The texture's handle. */
    public Asset<Texture> getTextureAsset() {
        return textureAsset;
    }

    public Texture getTexture() {
        return textureAsset.get();
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
