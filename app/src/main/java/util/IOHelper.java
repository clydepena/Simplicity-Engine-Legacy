package util;

import java.io.*;
import java.nio.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_CANCEL;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_FreePath;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_GetError;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OKAY;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialog_With;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_SaveDialog_With;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;
import org.lwjgl.util.nfd.NFDSaveDialogArgs;

import simplicity.Window;

public class IOHelper {
    
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
    
                this.pixels = stbi_load_from_memory(IOHelper.ResToByteBuffer(resFilepath), w, h, comp, 4);

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

    public static String openSingle(Window window, String... fileExtensions)
    {
        int handleType = window.getHandleType();
        long handleWindow = window.getHandleWin();

        try (MemoryStack stack = stackPush())
        {
            NFDFilterItem.Buffer filters = NFDFilterItem.malloc(fileExtensions.length);
            for (int i = 0; i < fileExtensions.length; i++) {
                String formatted = fileExtensions[i].charAt(0) == '.' ? fileExtensions[i].substring(1): fileExtensions[i];
                filters.get(i)
                .name(stack.UTF8(formatted))
                .spec(stack.UTF8(formatted));
            }
            PointerBuffer pp = stack.mallocPointer(1);
            return checkResult(
                NFD_OpenDialog_With(pp, NFDOpenDialogArgs.calloc(stack)
                    .filterList(filters)
                    .parentWindow(it -> it
                        .type(handleType)
                        .handle(handleWindow))),
                pp
            );
        }
    }

    public static String saveFile(Window window, String filename, String... fileExtensions) {
        int handleType = window.getHandleType();
        long handleWindow = window.getHandleWin();

        try (MemoryStack stack = stackPush()) {
            NFDFilterItem.Buffer filters = NFDFilterItem.malloc(fileExtensions.length);
            for (int i = 0; i < fileExtensions.length; i++) {
                String formatted = fileExtensions[i].charAt(0) == '.' ? fileExtensions[i].substring(1): fileExtensions[i];
                filters.get(i)
                .name(stack.UTF8(formatted))
                .spec(stack.UTF8(formatted));
            }
            PointerBuffer pp = stack.mallocPointer(1);
            return checkResult(
                NFD_SaveDialog_With(pp, NFDSaveDialogArgs.calloc(stack)
                    .filterList(filters)
                    .defaultName(stack.UTF8((filename == null ? "file": filename)))
                    .parentWindow(it -> it
                        .type(handleType)
                        .handle(handleWindow))),
                pp
            );
        }
    }

    private static String checkResult(int result, PointerBuffer path)
    {
        switch (result)
        {
            case NFD_OKAY:
                System.out.println("Success!");
                System.out.println(path.getStringUTF8(0));
                String filepath = path.getStringUTF8(0);
                NFD_FreePath(path.get(0));
                return filepath;
            case NFD_CANCEL:
                System.out.println("User pressed cancel.");
                return null;
            default: // NFD_ERROR
                System.err.format("Error: %s\n", NFD_GetError());
                return null;
        }
    }

    public static LoadedByteImg GenResImg(String filepath) {
        return new LoadedByteImg(filepath);
    }

    public static byte[] ResToByteArray(String filepath) {
        try {
            InputStream is = IOHelper.class.getResourceAsStream("/" + filepath);
            byte[] asset = is.readAllBytes();
            return asset;
        } catch (Exception e) {
            System.out.println("Could not load into byte array " + filepath);
        }

        return null;    }

    public static ByteBuffer ResToByteBuffer(String filepath) {
        try {
            InputStream is = IOHelper.class.getResourceAsStream("/" + filepath);
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
            InputStream is = IOHelper.class.getResourceAsStream("/" + filepath);
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
        return IOHelper.class.getResource("/" + filepath).getPath();
    }

}
