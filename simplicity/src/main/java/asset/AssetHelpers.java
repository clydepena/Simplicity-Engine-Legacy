package asset;
import java.nio.file.Path;
import java.util.StringJoiner;

public class AssetHelpers {
    private AssetHelpers() {}

    /*
     * Prefixes are handled on plain strings: Path.of("engine:x.png") throws on Windows,
     * where ':' is only allowed right after a drive letter.
     */

    /**
     * "engine:images/x.png" -> "engine", "images/x.png" -> "".
     * A prefix is 2+ letters, digits or '_' starting with a letter, so a drive letter ("C:/...") is never one.
     */
    public static String extractPrefix(String ref) {
        int colonIndex = ref.indexOf(':');
        if (colonIndex < 2) return "";
        if (!Character.isLetter(ref.charAt(0))) return "";
        for (int i = 1; i < colonIndex; i++) {
            char c = ref.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return "";
        }
        return ref.substring(0, colonIndex);
    }

    /** "engine:images/x.png" -> "images/x.png"; unprefixed refs are returned as they are. */
    public static String stripPrefix(String ref) {
        String prefix = extractPrefix(ref);
        return prefix.isEmpty() ? ref : ref.substring(prefix.length() + 1);
    }

    /** "images/x.png" + "engine" -> "engine:images/x.png"; an empty prefix adds nothing. */
    public static String addPrefix(String path, String prefix) {
        return prefix.isEmpty() ? path : prefix + ":" + path;
    }

    public static Path getRelativePath(Path target, Path root) {
        Path normalized = target.normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized);
        }
        throw new IllegalArgumentException("not project-relative: " + target.toString());
    }

    /**
     * An unprefixed path -> the pool key: '/' separators, no "." or "..".
     * Absolute paths are made relative to root; a pool without a root (the jar) rejects them.
     */
    public static String normalizeKey(String key, Path root) {
        Path p = Path.of(key.replace('\\', '/')).normalize();
        if (p.isAbsolute() || p.getRoot() != null) {
            if (root == null) throw new IllegalArgumentException("absolute path in a pool without a folder: " + key);
            p = getRelativePath(p, root);
        }
        if (p.startsWith("..")) throw new IllegalArgumentException("outside the project: " + key);
        StringJoiner newKey = new StringJoiner("/");
        for (Path part : p) newKey.add(part.toString());
        String normalized = newKey.toString();
        if (normalized.isEmpty()) throw new IllegalArgumentException("empty asset path: '" + key + "'");
        return normalized;
    }
}
