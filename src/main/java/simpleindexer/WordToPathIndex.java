package simpleindexer;

import com.sun.nio.file.SensitivityWatchEventModifier;
import simpleindexer.fs.*;
import simpleindexer.valuestorages.ValueStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static simpleindexer.utils.IndexerUtils.checkNotNull;

/**
 * Primitive implementation of <a href="http://en.wikipedia.org/wiki/Inverted_index">inverted index</a>.
 * <p>
 * Base idea of this implementation is <b>distributed indexing</b> (<a href="http://en.wikipedia.org/wiki/MapReduce">
 *     MapReduce</a> concept).
 * There is a big (main) index represented as {@link simpleindexer.Index} uses implementation of {@link simpleindexer.IndexStorage}
 * for storing indexed data. When {@link simpleindexer.fs.FSEvent} occurs, it will be caught by
 * {@link simpleindexer.fs.FSWatcher}. Then {@link simpleindexer.fs.FSWatcher} calls
 * {@link simpleindexer.fs.FSEventDispatcher#dispatch(simpleindexer.fs.FSEvent)}, which in turn distribute this event among its
 * {@link simpleindexer.fs.FSEventListener}. In this implementation there is special {@link simpleindexer.fs.FSEventListener}
 * which used to submitting {@link java.lang.Runnable tasks} for index update to {@link java.util.concurrent.ExecutorService executor}.
 * Each task use {@link simpleindexer.TextFileIndexer} to extract words from {@link simpleindexer.fs.FileWrapper}. Also you can
 * implement your own {@link simpleindexer.DataIndexer} using custom {@link simpleindexer.tokenizer.Tokenizer} to parse file on your own way.
 * Then extracted data is merged into {@link simpleindexer.Index index}.
 * <p>
 * Also you can customize index behavior by specifying {@link simpleindexer.WordToPathIndex.IndexProperties}.
 *
 *
 * @see simpleindexer.DataIndexer
 * @see simpleindexer.WordToPathIndex.IndexProperties
 * @see simpleindexer.fs.FSEventListener
 * @see simpleindexer.Index
 * @see simpleindexer.IndexStorage
 * @see simpleindexer.fs.FileWrapper
 * @see simpleindexer.fs.FSEvent
 * @see simpleindexer.fs.FSEventDispatcher
 * @see simpleindexer.fs.FSWatcher
 * @see simpleindexer.tokenizer.Tokenizer
 *
 * @author Ivan Arbuzov
 */
public class WordToPathIndex {

    private final Logger log = LoggerFactory.getLogger(WordToPathIndex.class);

    private final WatchEvent.Kind[] EVENTS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};
    private final WatchService watchService;
    private final Set<Path> pendingInconsistentPaths = new ConcurrentSkipListSet<>();
    private final ReentrantReadWriteLock pendingLock = new ReentrantReadWriteLock();
    private final LinkedBlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>();
    private final IndexProperties properties;
    private final FSWatcher fsWatcher;
    private FSRegistrar fsRegistrar;
    private ExecutorService executor;
    private Index<String, String, FileWrapper> index;
    private volatile boolean isTerminated;

    /**
     * Creates index by specified file system, {@link simpleindexer.WordToPathIndex.IndexProperties properties} and
     * {@link Path path} for initial content.
     *
     * @param fileSystem file system for {@link java.nio.file.WatchService} instance.
     * @param properties properties for {@link simpleindexer.WordToPathIndex.IndexProperties} instance.
     * @param path which content will be used for indexing by default.
     * @throws IOException
     */
    public WordToPathIndex(@NotNull final FileSystem fileSystem, @NotNull IndexProperties properties, Path path) throws IOException {
        log.info("Initializing index...");
        this.watchService = checkNotNull(fileSystem, "fileSystem").newWatchService();
        this.properties = checkNotNull(properties, "properties");
        int nThreads = properties.getIndexingThreadsCountProperty();
        executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, executorQueue);
        FSEventDispatcher<FSEventListener> fsEventDispatcher = new FSEventDispatcher<>();
        fsWatcher = new FSWatcher(watchService, fsEventDispatcher);
        fsRegistrar = new FSRegistrar(new FSRegistrar.Registrar() {
            @Override
            public WatchKey register(@NotNull Path root) throws IOException {
                checkNotNull(root, "root");
                checkIsRunning();
                return root.register(watchService, EVENTS, SensitivityWatchEventModifier.HIGH);
            }
        });
        fsEventDispatcher.addListener(new Submitter());
        index = new StringStringIndex(new TextFileIndexer(), new IndexStorageImpl());
        fsWatcher.start();
        if (path != null)
            submitUpdateTaskRecursive(path);
        isTerminated = false;
        log.info("Index initialized.");
    }

    /**
     * Creates index by specified file system and {@link Path path} for initial content.
     *
     * @param fileSystem file system for {@link java.nio.file.WatchService} instance.
     * @param path which content will be used for indexing by default.
     * @throws IOException
     */
    public WordToPathIndex(@NotNull final FileSystem fileSystem, Path path) throws IOException {
        this(fileSystem, new IndexProperties(), path);
    }

    /**
     * Creates empty index by specified file system and {@link simpleindexer.WordToPathIndex.IndexProperties properties}.
     *
     * @param fileSystem file system for {@link java.nio.file.WatchService} instance.
     * @param properties properties for {@link simpleindexer.WordToPathIndex.IndexProperties} instance.
     * @throws IOException if such exception was thrown from {@link java.nio.file.FileSystem#newWatchService()}.
     * @see simpleindexer.WordToPathIndex.IndexProperties
     */
    public WordToPathIndex(@NotNull final FileSystem fileSystem, @NotNull IndexProperties properties) throws IOException {
        this(fileSystem, properties, null);
    }

    /**
     * Creates empty index by specified file system and {@link java.util.Properties properties} which are used
     * for creation {@link simpleindexer.WordToPathIndex.IndexProperties}.
     *
     * @param fileSystem file system for {@link java.nio.file.WatchService} instance.
     * @param properties properties for {@link java.util.Properties} instance.
     * @throws IOException if such exception was thrown from {@link java.nio.file.FileSystem#newWatchService()}.
     * @see simpleindexer.WordToPathIndex.IndexProperties
     */
    public WordToPathIndex(@NotNull final FileSystem fileSystem, @NotNull Properties properties) throws IOException {
        this(fileSystem, new IndexProperties(properties));
    }

    /**
     * Creates empty index by specified file system with default {@link simpleindexer.WordToPathIndex.IndexProperties}
     *
     * @param fileSystem file system for {@link java.nio.file.WatchService} instance.
     * @throws IOException if such exception was thrown from {@link java.nio.file.FileSystem#newWatchService()}.
     * @see simpleindexer.WordToPathIndex.IndexProperties
     */
    public WordToPathIndex(@NotNull final FileSystem fileSystem) throws IOException {
        this(fileSystem, new IndexProperties());
    }

    /**
     * Start watching path specified {@code root}.
     * Does nothing if either {@code root} is already watched or {@code root} is directory.
     * Operation is thread-safe.
     *
     * @param root start watching to.
     */
    public void startWatch(String root) {
        startWatch(Paths.get(root));
    }

    /**
     * Start watching path specified {@code root}.
     * Does nothing if either {@code root} is already watched or {@code root} is directory.
     * Operation is thread-safe.
     *
     * @param root start watching to.
     */
    public void startWatch(Path root) {
        log.info("Start watching {}", root);
        if (!Files.isDirectory(root)) {
            log.warn("Can't start watch path {}: it isn't a directory.", root);
            return;
        }
        if (fsRegistrar.isRegistered(root)) {
            log.warn("Path {} is already registered.", root);
            return;
        }
        submitUpdateTaskRecursive(root);
    }

    /**
     * Stop watching path specified by {@code root}.
     * Does nothing if either {@code root} is already watched or {@code root} is directory.
     * Operation is thread-safe.
     *
     * @param root stop watching to.
     */
    public void stopWatch(String root) {
        stopWatch(Paths.get(root));
    }

    /**
     * Stop watching path specified by {@code root}.
     * Does nothing if either {@code root} is already watched or {@code root} is directory.
     * Operation is thread-safe.
     *
     * @param root stop watching to.
     */
    public void stopWatch(Path root) {
        if (!fsRegistrar.isRegistered(root)) {
            log.warn("Path {} isn't registered. Nothing to stop.", root);
            return;
        }
        log.info("Stop watching {}", root);
        List<Path> removed = fsRegistrar.unregisterAll(root);
        for (Path p : removed) {
            log.debug("Submit removed path {}", p);
            submitRemoveTask(p);
        }
    }

    /**
     * Return {@link java.util.List list} of paths represented as {@link String} such that
     * corresponding files contain given {@code word}.
     *<p>
     * If
     * <pre>
     *     {@link simpleindexer.WordToPathIndex.IndexProperties#isBlockRequestProperty()} == true
     * </pre>
     * , than it will blocked until there is at least one pending task for index update,
     * otherwise result corresponding instantaneous state of index will return.
     * <p>
     * Operation is thread-safe.
     *
     * @param word to search for
     * @return {@link List}<{@link java.lang.String}>
     * @throws java.lang.InterruptedException if waiting while all updates will be performed is interrupted.
     */
    public List<String> getPathsByWord(final String word) throws InterruptedException, IndexException {
        checkIsRunning();
        log.info("GET: {}", word);
        try {
            ValueStorage<String> vs;
            if (properties.isBlockRequestProperty()) {
                while(!executorQueue.isEmpty()) {
                    checkIsRunning();
                    Thread.sleep(10); // terrible stub!
                }
            }
            vs = index.get(word);
            if (vs == null) {
                return Collections.emptyList();
            }
            return vs.asList();
        } catch (IndexException e) {
            log.error("Index exception while GET query: {}", e);
        } catch (IndexIllegalStateException e) {
            throw new IndexException(e);
        }
        return Collections.emptyList();
    }

    /**
     * Shutdown index.
     * <p>
     * This method performs {@link java.util.concurrent.ExecutorService#shutdownNow() executor.shutdownNow()},
     * {@link simpleindexer.fs.FSWatcher#stop()} and {@link Index#clear()}.
     * @throws IndexException
     */
    public void shutdown() throws IndexException {
        log.info("Shutdown indexer...");
        if (!isTerminated) {
            synchronized (this) {
                if (!isTerminated) {
                    isTerminated = true;
                } else {
                    log.warn("Index is already stopped.");
                    return;
                }
            }
        } else {
            log.warn("Index is already stopped.");
            return;
        }
        executor.shutdownNow();
        fsWatcher.stop();
        index.clear();
        log.info("Index is stopped.");
    }


    private boolean moveToPending(Path path) {
        if (!pendingInconsistentPaths.contains(path)) {
            pendingLock.writeLock().lock();
            try {
                if (!pendingInconsistentPaths.contains(path)) {
                    pendingInconsistentPaths.add(path);
                    return true;
                }
            } finally {
                pendingLock.writeLock().unlock();
            }
        }
        return false;
    }

    private boolean removeFromPending(Path path) {
        if (pendingInconsistentPaths.contains(path)) {
            pendingLock.writeLock().lock();
            try {
                if (pendingInconsistentPaths.contains(path)) {
                    pendingInconsistentPaths.remove(path);
                    return true;
                }
            } finally {
                pendingLock.writeLock().unlock();
            }
        }
        return false;
    }

    private Runnable updateTask(final FileWrapper file) {
        return new Runnable() {
            @Override
            public void run() {
                if (!removeFromPending(file.getPath())) {
                    log.warn("File already removed from pending: {}", file);
                    return;
                }
                try {
                    index.update(file);
                } catch (IndexException e) {
                    log.error("Exception while indexing file {}: {}", file, e);
                }
            }
        };
    }

    private Runnable removeTask(final FileWrapper file) {
        return new Runnable() {
            @Override
            public void run() {
                if (!removeFromPending(file.getPath())) {
                    log.warn("File already removed from pending: {}", file);
                    return;
                }
                try {
                    index.remove(file);
                } catch (IndexException e) {
                    log.error("Exception while removing file from index {}: {}", file, e);
                }
            }
        };
    }

    private void submitUpdateTask(Path path) {
        checkIsRunning();
        if (!moveToPending(path)) {
            return;
        }
        log.debug("Submit to update {}.", path);
        try {
            executor.submit(updateTask(new FileWrapper(path, properties.getMaxAvailableFileSizeProperty())));
        } catch (RejectedExecutionException e) {
            log.error(e.toString());
            removeFromPending(path);
        }
    }

    private void submitRemoveTask(Path path) {
        checkIsRunning();
        if (!moveToPending(path)) {
            log.warn("File already scheduled: {}", path);
            return;
        }
        log.debug("Submit remove {}", path);
        try {
            executor.submit(removeTask(new FileWrapper(path, properties.getMaxAvailableFileSizeProperty())));
        } catch (RejectedExecutionException e) {
            log.error(e.toString());
            removeFromPending(path);
        }
    }

    private void submitUpdateTaskRecursive(final Path path) {
        checkIsRunning();
        try {
            if (!Files.isDirectory(path)) {
                log.debug("Path {} is not a dir. Skip its recursive update.", path);
                return;
            }
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visit file {}", filePath);
                    fsRegistrar.register(filePath);
                    submitUpdateTask(filePath);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visit dir {}", dir);
                    fsRegistrar.register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error while recursive updating {}: {}", path, e);
        }
    }

    private void checkIsRunning() {
        if (isTerminated) {
            synchronized (this) {
                if (isTerminated) {
                    throw new IndexIllegalStateException("Index is terminated.");
                }
            }
        }
    }

    private class Submitter implements FSEventListener {

        @Override
        public void onFileCreated(final Path path) throws IOException {
            checkIsRunning();
            fsRegistrar.register(path);
            submitUpdateTask(path);
        }

        @Override
        public void onFileModified(final Path path) throws IOException {
            checkIsRunning();
            fsRegistrar.register(path);
            submitUpdateTask(path);
        }

        @Override
        public void onDirectoryCreated(final Path path) {
            checkIsRunning();
            submitUpdateTaskRecursive(path);
        }

        @Override
        public void onDirectoryModified(final Path path) {
            // do nothing
            log.debug("Directory modified {}", path);
        }

        @Override
        public void onDeleted(final Path path) {
            checkIsRunning();
            log.debug("Delete {}", path);
            List<Path> removed = fsRegistrar.unregisterAll(path);
            for (Path p : removed) {
                log.debug("Submit removed path {}", p);
                submitUpdateTask(p);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Class representing convenient access to index properties.
     */
    public static class IndexProperties {

        /**
         * Threads count used by {@link java.util.concurrent.ExecutorService executor} while executing updating tasks.
         */
        public final static String INDEXING_THREADS_COUNT_PROPERTY = "simpleindexer.threads.count";
        /**
         * Whether request {@link simpleindexer.WordToPathIndex#getPathsByWord(String)} will blocking.
         */
        public final static String BLOCK_REQUEST_PROPERTY = "simpleindexer.block.request";
        /**
         * Maximum allowed for indexing file size in bytes.
         * @see simpleindexer.fs.FileWrapper
         */
        public final static String MAX_AVAILABLE_FILE_SIZE_PROPERTY = "simpleindexer.max.file.size";

        private int indexingThreadsCountProperty;
        private boolean blockRequestProperty;
        private long maxAvailableFileSizeProperty;

        public IndexProperties(@NotNull Properties properties) {
            checkNotNull(properties, "properties");
            this.indexingThreadsCountProperty = Integer.parseInt(properties.getProperty(
                    INDEXING_THREADS_COUNT_PROPERTY,
                    String.valueOf(2 * Runtime.getRuntime().availableProcessors())));
            this.blockRequestProperty = Boolean.parseBoolean(properties.getProperty(
                    BLOCK_REQUEST_PROPERTY, "true"));
            this.maxAvailableFileSizeProperty = Long.parseLong(properties.getProperty(
                    MAX_AVAILABLE_FILE_SIZE_PROPERTY, String.valueOf(30 * 1024 * 1024L)));
        }

        public IndexProperties() {
            this(new Properties());
        }

        public int getIndexingThreadsCountProperty() {
            return indexingThreadsCountProperty;
        }

        public boolean isBlockRequestProperty() {
            return blockRequestProperty;
        }

        public long getMaxAvailableFileSizeProperty() {
            return maxAvailableFileSizeProperty;
        }
    }
}