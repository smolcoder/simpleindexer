package simpleindexer.fs;

import gnu.trove.map.hash.THashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static simpleindexer.utils.IndexerUtils.checkNotNull;

/**
 * Registrar for all paths that are registered for watching in index.
 * Implementation is thread-safe.
 *
 * @author Ivan Arbuzov
 * 10/11/14.
 */
public class FSRegistrar {

    /**
     * Register paths as watched.
     */
    public static interface Registrar {
        /**
         * Register {@code paths} as watched.
         *
         * @param root to register
         * @return {@link java.nio.file.WatchKey} corresponding the {@code path}
         * @throws IOException
         */
        public WatchKey register(@NotNull Path root) throws IOException;
    }

    private final Logger log = LoggerFactory.getLogger(FSRegistrar.class);

    private final Map<Path, FSEntry> fs = new THashMap<>();
    private final ReentrantReadWriteLock fsLock = new ReentrantReadWriteLock();
    private final Registrar registrar;

    /**
     * Create FSRegistrar which will register paths with {@code registrar}.
     *
     * @param registrar for register paths
     */
    public FSRegistrar(@NotNull Registrar registrar) {
        this.registrar = checkNotNull(registrar, "registrar");
    }

    public Lock readLock() {
        return fsLock.readLock();
    }

    public Lock writeLock() {
        return fsLock.writeLock();
    }

    /**
     * Register {@code path} as watched.
     *
     * @param root to register as watched
     * @throws IOException
     */
    public void register(final Path root) throws IOException {
        if (isRegistered(root)) {
            return;
        }
        log.debug("register {}", root);
        try {
            writeLock().lock();
            if (Files.isDirectory(root)) {
                fs.put(root, new FSEntry(root, registrar.register(root)));
                if (fs.containsKey(root.getParent()))
                    fs.get(root.getParent()).getChildren().add(root.getFileName());
            } else {
                fs.put(root, new FSEntry(root));
                fs.get(root.getParent()).getChildren().add(root.getFileName());
            }

        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Unregister all paths stats with {@code prefix}.
     *
     * @param prefix of all paths should be unregistered
     * @return {@link java.util.List} of {@link java.nio.file.Path paths} that was unregistered.
     */
    public List<Path> unregisterAll(final Path prefix) {
        List<Path> removed = new ArrayList<>();
        if (!isRegistered(prefix)) return removed;
        if (fs.containsKey(prefix)) {
            writeLock().lock();
            try {
                if (fs.containsKey(prefix)) {
                    removed.addAll(unregisterAllHelper(prefix));
                }
            } finally {
                writeLock().unlock();
            }
        }
        return removed;
    }

    /**
     * Check is {@code path} already registered as watched path.
     *
     * @param path to check
     * @return {@code true} if {@code path} is registered, {@code false} otherwise.
     */
    public boolean isRegistered(Path path) {
        try {
            readLock().lock();
            return fs.containsKey(path);
        } finally {
            readLock().unlock();
        }
    }

    /*
     * Unregister prefix/* from fs.
     * Remove prefix from its parent.
     * Reset prefix if it is directory.
     */
    private List<Path> unregisterAllHelper(final Path prefix) {
        log.debug("unregister {}", prefix);
        List<Path> removed = new ArrayList<>();
        FSEntry node = fs.get(prefix);
        if (!node.isDirectory() || node.getChildren().isEmpty()) {
            fs.remove(prefix);
            if (fs.containsKey(prefix.getParent()))
                fs.get(prefix.getParent()).getChildren().remove(prefix.getFileName());
            removed.add(prefix);
            return removed;
        }
        // to avoid ConcurrentModificationException
        List<Path> children = new ArrayList<>(node.getChildren());
        for (Path c : children) {
            removed.addAll(unregisterAllHelper(Paths.get(prefix.toString(), c.toString())));
        }
        node.getKey().cancel();
        fs.remove(prefix);
        if (fs.containsKey(prefix.getParent()))
            fs.get(prefix.getParent()).getChildren().remove(prefix.getFileName());
        return removed;
    }


    private class FSEntry {
        private final Path root;
        private Set<Path> children;
        private WatchKey key;
        private boolean isDir = false;

        public FSEntry(@NotNull Path root, @NotNull WatchKey key) {
            this.key = checkNotNull(key);
            this.children = new HashSet<>();
            this.isDir = true;
            this.root = checkNotNull(root);
        }

        public FSEntry(@NotNull Path root) {
            this.root = checkNotNull(root);
            this.isDir = false;
        }

        public boolean isDirectory() {
            return isDir;
        }

        public WatchKey getKey() {
            return this.key;
        }

        public Set<Path> getChildren() {
            return this.children;
        }

        public String toString() {
            if (isDir) {
                return root + ": " + children;
            }
            return root.toString();
        }
    }
}
