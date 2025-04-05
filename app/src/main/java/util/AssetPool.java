package util;

import renderer.Shader;
import renderer.Texture;
import simplicity.Sound;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import components.Spritesheet;

import java.io.File;

public class AssetPool {
    private static Map<String, Shader> shaders = new HashMap<>();
    private static Map<String, Shader> shadersRes = new HashMap<>();

    private static Map<String, Texture> textures = new HashMap<>();
    private static Map<String, Texture> texturesRes = new HashMap<>();

    private static Map<String, Spritesheet> spritesheets = new HashMap<>();
    private static Map<String, Spritesheet> spritesheetsRes = new HashMap<>();

    private static Map<String, Sound> sounds = new HashMap<>();
    private static Map<String, Sound> soundsRes = new HashMap<>();

    // SOUNDS
    public static Collection<Sound> getAllSounds() {
        return sounds.values();
    }

    public static Sound getSound(String soundFile) {
        File file = new File(soundFile);
        if (sounds.containsKey(file.getAbsolutePath())) {
            return sounds.get(file.getAbsolutePath());
        } else {
            assert false : "Sound file not added '" + soundFile + "'";
        }
        return null;
    }

    public static Sound addSound(String soundFile, boolean loops) {
        File file = new File(soundFile);
        if (sounds.containsKey(file.getAbsolutePath())) {
            return sounds.get(file.getAbsolutePath());
        } else {
            Sound sound = new Sound(file.getAbsolutePath(), loops);
            AssetPool.sounds.put(file.getAbsolutePath(), sound);
            return sound;
        }
    }
    
    // SHADERS
    public static Shader getShader(String resourceName) {
        File file = new File(resourceName);
        
        // System.out.println("PATH: " + file.getAbsolutePath());
        
        if(AssetPool.shaders.containsKey(file.getAbsolutePath())) {
            return AssetPool.shaders.get(file.getAbsolutePath());
        }
        if (AssetPool.shadersRes.containsKey(resourceName)) {
            return getShaderFromRes(resourceName);
        }
        Shader shader = new Shader(resourceName);
        shader.compile();
        AssetPool.shaders.put(file.getAbsolutePath(), shader);
        return shader;
    }

    public static Shader getShaderFromRes(String resourceName) {
        
        // File file = new File(resourceName);
        
        // System.out.println("PATH: " + file.getAbsolutePath());
        
        if(AssetPool.shadersRes.containsKey(resourceName)) {
            return AssetPool.shadersRes.get(resourceName);
        } else {
            Shader shader = new Shader();
            shader.initFromRes(resourceName);
            shader.compile();
            AssetPool.shadersRes.put(resourceName, shader);
            return shader;
        }
    }

    //TEXTURES
    public static Texture getTexture(String resourceName) {
        File file = new File(resourceName);
        if(AssetPool.textures.containsKey(file.getAbsolutePath())) {
            return AssetPool.textures.get(file.getAbsolutePath());
        }
        if (AssetPool.texturesRes.containsKey(resourceName)) {
            return getTextureFromRes(resourceName);
        }
        Texture texture = new Texture();
        texture.initFromExternal(resourceName);
        AssetPool.textures.put(file.getAbsolutePath(), texture);
        return texture;
    }

    public static Texture getTextureFromRes(String resourceName) {
  
        if(AssetPool.texturesRes.containsKey(resourceName)) {
            return AssetPool.texturesRes.get(resourceName);
        } else {
            Texture texture = new Texture();
            texture.initFromRes(resourceName);
            AssetPool.texturesRes.put(resourceName, texture);
            return texture;
        }
    }

    // SPRITES
    public static void addSpritesheet(String resourceName, Spritesheet spritesheet) {
        File file = new File(resourceName);
        if(!AssetPool.spritesheets.containsKey(file.getAbsolutePath())) {
            AssetPool.spritesheets.put(file.getAbsolutePath(), spritesheet);
        }
    }

    public static void addSpritesheetToRes(String resourceName, Spritesheet spritesheet) {
        if(!AssetPool.spritesheetsRes.containsKey(resourceName)) {
            AssetPool.spritesheetsRes.put(resourceName, spritesheet);
        }
    }

    public static Spritesheet getSpritesheet(String resourceName) {
        File file = new File(resourceName);
        if(!AssetPool.spritesheets.containsKey(file.getAbsolutePath())) {
            return getSpritesheetFromRes(resourceName);
        }
        return AssetPool.spritesheets.getOrDefault(file.getAbsolutePath(), null);
    }

    public static Spritesheet getSpritesheetFromRes(String resourceName) {
        if(!AssetPool.spritesheetsRes.containsKey(resourceName)) {
            assert false : "Error: Tried to acces '" + resourceName + "' and it has not been added to asset pool.";
        }
        return AssetPool.spritesheetsRes.getOrDefault(resourceName, null);
    }

}
