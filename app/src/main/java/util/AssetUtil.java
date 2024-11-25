package util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class AssetUtil {
    
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
