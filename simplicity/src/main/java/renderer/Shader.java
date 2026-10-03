package renderer;

import org.joml.*;
import org.lwjgl.BufferUtils;

import util.IOHelper;
import asset.Disposable;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL46.*;

public class Shader implements Disposable {

    private int shaderProgramID;
    private boolean beingUsed = false;
    private String vertexSource;
    private String fragmentSource;
    private String filepath;

    public Shader() {

    }

    // Legacy loading path, only used by util.AssetPool (moved to to-refactor/depreciated). Shaders now load
    // through ShaderCodec and the asset pools; the file constructor and both initFrom methods are kept for reference.
//     public Shader(String externFilepath) {
//         initFromExternal(externFilepath);
//     }
// 
//     /** Old loading path (util.AssetPool): errors are printed, not thrown. */
//     public void initFromExternal(String filepath) {
//         this.filepath = filepath;
//         try {
//             parseShaderSource(new String(Files.readAllBytes(Paths.get(filepath))));
//         } catch (IOException | IllegalArgumentException e) {
//             System.err.println("Error: (Shader) '" + filepath + "': " + e.getMessage());
//         }
//     }
// 
//     /** Old loading path (util.AssetPool): errors are printed, not thrown. */
//     public void initFromRes(String filepath) {
//         this.filepath = filepath;
//         String source = IOHelper.ResToString(filepath);
//         if (source == null) {
//             System.err.println("Error: (Shader) could not read '" + filepath + "'");
//             return;
//         }
//         try {
//             parseShaderSource(source);
//         } catch (IllegalArgumentException e) {
//             System.err.println("Error: (Shader) '" + filepath + "': " + e.getMessage());
//         }
//     }

    /**
     * Splits a file with "#type vertex" and "#type fragment" sections into the two sources.
     * Works with LF and CRLF line endings. Pure string work: safe on any thread.
     * @throws IllegalArgumentException when a section is unknown, repeated or missing
     */
    public void parseShaderSource(String sourceCode) {
        vertexSource = null;
        fragmentSource = null;
        String source = sourceCode.replace("\r\n", "\n");

        // sections start with "#type <stage>" at the start of a line; text before the first one is ignored
        String[] sections = source.split("(?m)^#type[ \\t]+");
        for (int i = 1; i < sections.length; i++) {
            int eol = sections[i].indexOf('\n');
            String stage = (eol == -1 ? sections[i] : sections[i].substring(0, eol)).trim();
            String body = eol == -1 ? "" : sections[i].substring(eol + 1);
            switch (stage) {
                case "vertex" -> {
                    if (vertexSource != null) throw new IllegalArgumentException("two '#type vertex' sections");
                    vertexSource = body;
                }
                case "fragment" -> {
                    if (fragmentSource != null) throw new IllegalArgumentException("two '#type fragment' sections");
                    fragmentSource = body;
                }
                default -> throw new IllegalArgumentException("unknown shader stage '#type " + stage + "'");
            }
        }
        if (vertexSource == null) throw new IllegalArgumentException("no '#type vertex' section");
        if (fragmentSource == null) throw new IllegalArgumentException("no '#type fragment' section");
    }

    /**
     * Compiles and links on the GPU (main thread). Errors are printed with the GL info log.
     * @return false if compiling or linking failed; the program is then unusable and should be disposed
     */
    public boolean compile() {
        if (vertexSource == null || fragmentSource == null) {
            System.out.println("ERROR: '" + filepath + "'\n\tNothing to compile: the source wasn't parsed.");
            return false;
        }
        boolean ok = true;
        // ============================================================
        // Compile and link shaders
        // ============================================================
        int vertexID, fragmentID;

        // First load and compile the vertex shader
        vertexID = glCreateShader(GL_VERTEX_SHADER);
        // Pass the shader source to the GPU
        glShaderSource(vertexID, vertexSource);
        glCompileShader(vertexID);

        // Check for errors in compilation
        int success = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tVertex shader compilation failed.");
            System.out.println(glGetShaderInfoLog(vertexID, len));
            ok = false;
        }

        // First load and compile the vertex shader
        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);
        // Pass the shader source to the GPU
        glShaderSource(fragmentID, fragmentSource);
        glCompileShader(fragmentID);

        // Check for errors in compilation
        success = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if (success == GL_FALSE) {
            int len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tFragment shader compilation failed.");
            System.out.println(glGetShaderInfoLog(fragmentID, len));
            ok = false;
        }

        // Link shaders and check for errors
        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        glLinkProgram(shaderProgramID);

        // Check for linking errors
        success = glGetProgrami(shaderProgramID, GL_LINK_STATUS);
        if (success == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            System.out.println("ERROR: '" + filepath + "'\n\tLinking of shaders failed.");
            System.out.println(glGetProgramInfoLog(shaderProgramID, len));
            ok = false;
        }

        // the linked program keeps what it needs: the separate stage objects would otherwise stay on the GPU
        glDetachShader(shaderProgramID, vertexID);
        glDetachShader(shaderProgramID, fragmentID);
        glDeleteShader(vertexID);
        glDeleteShader(fragmentID);
        return ok;
    }

    public void use() {
        if (!beingUsed) {
            // Bind shader program
            glUseProgram(shaderProgramID);
            beingUsed = true;
        }
    }

    public void detach() {
        glUseProgram(0);
        beingUsed = false;
    }

    /** Frees the GL program; safe to call twice. */
    @Override
    public void dispose() {
        if (shaderProgramID == 0) return;
        if (beingUsed) detach();
        glDeleteProgram(shaderProgramID);
        shaderProgramID = 0;
    }

    public void uploadMat4f(String varName, Matrix4f mat4) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuffer);
        glUniformMatrix4fv(varLocation, false, matBuffer);
    }

    public void uploadMat3f(String varName, Matrix3f mat3) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(9);
        mat3.get(matBuffer);
        glUniformMatrix3fv(varLocation, false, matBuffer);
    }

    public void uploadVec4f(String varName, Vector4f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform4f(varLocation, vec.x, vec.y, vec.z, vec.w);
    }

    public void uploadVec3f(String varName, Vector3f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform3f(varLocation, vec.x, vec.y, vec.z);
    }

    public void uploadVec2f(String varName, Vector2f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform2f(varLocation, vec.x, vec.y);
    }

    public void uploadFloat(String varName, float val) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1f(varLocation, val);
    }

    public void uploadInt(String varName, int val) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1i(varLocation, val);
    }

    public void uploadTexture(String varName, int slot) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1i(varLocation, slot);
    }

    public void uploadIntArray(String varName, int[] array) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1iv(varLocation, array);
    }
}