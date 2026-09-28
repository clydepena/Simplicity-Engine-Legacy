package asset;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import simplicity.Tasks;

public interface AssetLoader<T, D> {

    public List<String> supportedExtensions();

    public D decode(byte[] bytes);
    public T finish(D decoded, AssetPoolHandler handler);

    default T loadAtOnce(byte[] bytes, AssetPoolHandler handler) {
        return finish(decode(bytes), handler);
    }

    default void loadAsync(Path file, AssetPoolHandler handler, Consumer<T> onLoaded, Consumer<Throwable> onError) {
        loadAsync(() -> {
            try {
                return Files.readAllBytes(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, handler, onLoaded, onError);
    }

    default void loadAsync(byte[] bytes, AssetPoolHandler handler, Consumer<T> onLoaded, Consumer<Throwable> onError) {
        loadAsync(() -> bytes, handler, onLoaded, onError);
    }

    private void loadAsync(Supplier<byte[]> read, AssetPoolHandler handler, Consumer<T> onLoaded, Consumer<Throwable> onError) {
        Tasks.submitAsync(
            () -> decode(read.get()),                  // worker: no GL, no pools
            decoded -> {                               // main thread
                T result;
                try {
                    result = finish(decoded, handler);
                } catch (RuntimeException e) {
                    if (onError != null) onError.accept(e);
                    else System.err.println("loading failed: " + e);
                    return;
                }
                if (onLoaded != null) onLoaded.accept(result);
                else if (result instanceof Disposable d) d.dispose();
            },
            onError);
    }
}
