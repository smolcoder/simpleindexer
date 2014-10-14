package simpleindexer.fs;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for all listeners of {@link simpleindexer.fs.FSEvent}.
 * <p>
 * These listeners are registered in {@link simpleindexer.fs.FSEventDispatcher}, which calls
 * there's methods based on income {@link simpleindexer.fs.FSEvent}.
 *
 * @author Ivan Arbuzov
 * 10/5/14.
 */
public interface FSEventListener {
    /**
     * Invoked when an {@link simpleindexer.fs.FSEvent.Kind#ENTRY_CREATE} is dispatched on file with path {@code path}.
     *
     * @param path of created file
     * @throws IOException
     */
    public void onFileCreated(Path path) throws IOException;

    /**
     * Invoked when an {@link simpleindexer.fs.FSEvent.Kind#ENTRY_MODIFY} is dispatched on file with path {@code path}.
     *
     * @param path of modified file
     * @throws IOException
     */
    public void onFileModified(Path path) throws IOException;

    /**
     * Invoked when an {@link simpleindexer.fs.FSEvent.Kind#ENTRY_CREATE} is dispatched on directory with path {@code path}.
     *
     * @param path of created directory
     */
    public void onDirectoryCreated(Path path);

    /**
     * Invoked when an {@link simpleindexer.fs.FSEvent.Kind#ENTRY_MODIFY} is dispatched on directory with path {@code path}.
     *
     * @param path of modified directory
     */
    public void onDirectoryModified(Path path);

    /**
     * Invoked when an {@link simpleindexer.fs.FSEvent.Kind#ENTRY_DELETE} is dispatched on either file or directory with path
     * {@code path}.
     *
     * @param path of deleted entry
     */
    public void onDeleted(Path path);

}
