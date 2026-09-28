package asset;

/**
 * A raw asset that holds resources the garbage collector can't free (GL textures, programs, buffers).
 * The pool calls dispose() when it drops the asset's data.
 *
 * Only free what this object created itself. An asset that got another asset from the pool
 * (a sprite sheet's texture) must not dispose it: that asset belongs to its own handle.
 */
public interface Disposable {
    void dispose();
}
