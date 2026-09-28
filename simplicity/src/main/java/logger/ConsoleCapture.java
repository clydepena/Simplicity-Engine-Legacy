package logger;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class ConsoleCapture {
    private static PrintStream originalOut = System.out, originalErr = System.err;

    /** Call once, as early as possible (e.g. first thing in main): only later output is captured. */
    public static void install(Consumer<String> onOut, Consumer<String> onErr) {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(new LineCapture(originalOut, onOut), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LineCapture(originalErr, onErr), true, StandardCharsets.UTF_8));
    }

    /** The real console, bypassing the capture. */
    public static PrintStream out() { return originalOut; }
    public static PrintStream err() { return originalErr; }
}