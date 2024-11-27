package util;

import java.io.*;
import java.nio.*;
import static org.lwjgl.stb.STBImage.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

public class AssetUtil {
    
    public static class LoadedByteImg {

        private ByteBuffer pixels;
        private String resFilepath;
        private int width, height;

        public LoadedByteImg(String resFilepath) {

            try (MemoryStack stack = MemoryStack.stackPush()) {
                this.resFilepath = resFilepath;
                IntBuffer comp = stack.mallocInt(1);
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
    
                this.pixels = stbi_load_from_memory(AssetUtil.ResToByteBuffer(resFilepath), w, h, comp, 4);

                if (pixels == null)
                    System.out.println("Could not load " + resFilepath);

                this.width = w.get();
                this.height = h.get();
            } catch (Exception e) {
                this.width = 0;
                this.height = 0;
                this.resFilepath = null;
                this.pixels = null;
            }
        }
    
        public ByteBuffer getImg() {
            return this.pixels;
        }

        public String getresFilepath() {
            return this.resFilepath;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }

    public static LoadedByteImg GenResImg(String filepath) {
        return new LoadedByteImg(filepath);
    }


    public static ByteBuffer ResToByteBuffer(String filepath) {
        try {
            InputStream is = AssetUtil.class.getResourceAsStream("/" + filepath);
            byte[] asset = is.readAllBytes();
            ByteBuffer assetBuffer = BufferUtils.createByteBuffer(asset.length);
            assetBuffer.put(asset);
            assetBuffer.flip();

            return assetBuffer;
        } catch (Exception e) {
            System.out.println("Could not load into byte buffer " + filepath);
        }

        return null;
    }

    public static String ResToString(String filepath) {
        try {
            InputStream is = AssetUtil.class.getResourceAsStream("/" + filepath);
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int result = bis.read(); result != -1; result = bis.read()) {
                baos.write((byte) result);
            }

            return baos.toString("UTF-8");
        } catch (Exception e) {
            System.out.println("Could not load string " + filepath);
        }

        return null;
    }

    public static String GetResAbsolouteDir(String filepath) {
        return AssetUtil.class.getResource("/" + filepath).getPath();
    }

}
