package logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

final class LineCapture extends OutputStream {
    private final OutputStream passThrough;
    private final Consumer<String> onLine;
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();

    LineCapture(OutputStream passThrough, Consumer<String> onLine) {
        this.passThrough = passThrough;
        this.onLine = onLine;
    }

    @Override public synchronized void write(int b) throws IOException {
        passThrough.write(b);
        if (b == '\n') {
            String text = line.toString(StandardCharsets.UTF_8);
            line.reset();
            onLine.accept(text);
        } else if (b != '\r') {                
            line.write(b);
        }
    }
    @Override public void flush() throws IOException { passThrough.flush(); }
}