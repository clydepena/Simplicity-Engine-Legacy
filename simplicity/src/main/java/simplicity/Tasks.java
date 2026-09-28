package simplicity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/**
 * The engine's only threads besides the main one. Work runs on a worker; its result (or error)
 * is handed back through a queue that the main thread empties once per frame, so callbacks
 * always run on the main thread and may use OpenGL, the asset pools and the event system.
 * Workers must not touch any of those.
 */
public final class Tasks {

    private Tasks() {}

    private static final int CORES = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final ExecutorService platformWorkers = new ThreadPoolExecutor(
        CORES,
        CORES,
        10, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        Thread.ofPlatform().name("cpu-worker-", 0).daemon(true).factory()   // daemon: never keeps the app alive on exit
    );
    private static final ExecutorService virtualWorkers = Executors.newVirtualThreadPerTaskExecutor();

    private static final Queue<Runnable> mainThread = new ConcurrentLinkedQueue<>();
    private static volatile Thread mainThreadRef;

    /** Records the calling thread as the main thread. Call it first thing in main(). */
    public static void init() {
        mainThreadRef = Thread.currentThread();
    }

    public static <R> void submitAsync(Supplier<R> work, Consumer<R> onComplete, Consumer<Throwable> onError) {
        platformWorkers.execute(wrap(work, onComplete, onError));
    }

    public static <R> void submitAsyncVirtual(Supplier<R> work, Consumer<R> onComplete, Consumer<Throwable> onError) {
        virtualWorkers.execute(wrap(work, onComplete, onError));
    }

    public static void submitAsync(Runnable work, Consumer<Throwable> onError) {
        platformWorkers.execute(wrap(work, onError));
    }

    public static <R> void submitAsyncVirtual(Runnable work, Consumer<Throwable> onError) {
        virtualWorkers.execute(wrap(work, onError));
    }

    public static <R> void submitAsync(Runnable work) {
        platformWorkers.execute(wrap(work));
    }

    public static <R> void submitAsyncVirtual(Runnable work) {
        virtualWorkers.execute(wrap(work));
    }   public static void onMainThread(Runnable work) {
        mainThread.add(work);
    }

    public static void runMainThreadJobs() {
        Runnable job;
        while ((job = mainThread.poll()) != null) job.run();
    }

    public static boolean isMainThread() {
        return Thread.currentThread() == mainThreadRef;
    }

    public static boolean isInitialized() {
        return mainThreadRef != null;
    }

    public static void shutdown() {
        platformWorkers.shutdown();
        virtualWorkers.shutdown();
        try {
            platformWorkers.awaitTermination(5, TimeUnit.SECONDS);
            virtualWorkers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

  
    private static <R> Runnable wrap(Supplier<R> work, Consumer<R> onComplete, Consumer<Throwable> onError) {
        return () -> {
            try {
                R result = work.get();
                if (onComplete != null) mainThread.add(() -> onComplete.accept(result));
            } catch (Throwable t) {
                if (onError != null) mainThread.add(() -> onError.accept(t));
                else mainThread.add(() -> System.err.println("background task failed: " + t));
            }
        };
    }

    private static Runnable wrap(Runnable work, Consumer<Throwable> onError) {
        return () -> {
            try {
                work.run();
            } catch (Throwable t) {
                if (onError != null) mainThread.add(() -> onError.accept(t));
                else mainThread.add(() -> System.err.println("background task failed: " + t));
            }
        };
    }

    private static Runnable wrap(Runnable work) {
        return () -> {
            try {
                work.run();
            } catch (Throwable t) {
                mainThread.add(() -> System.err.println("background task failed: " + t));
            }
        };
    }
}
