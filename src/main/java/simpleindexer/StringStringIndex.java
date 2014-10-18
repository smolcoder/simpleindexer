package simpleindexer;

import gnu.trove.map.hash.THashMap;
import simpleindexer.exceptions.IndexException;
import simpleindexer.fs.FileWrapper;
import simpleindexer.valuestorages.ValueStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link Index} for key-type {@link String}, value-type {@link String}
 * and data-type {@link simpleindexer.fs.FileWrapper}.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */

public class StringStringIndex implements Index<String, String, FileWrapper> {
    private static final Logger log = LoggerFactory.getLogger(StringStringIndex.class);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private DataIndexer<String, Void, FileWrapper> dataIndexer;

    private IndexStorage<String, String> indexStorage;

    private Map<String, Set<String>> fileToKeys = new THashMap<>();

    public StringStringIndex(DataIndexer<String, Void, FileWrapper> dataIndexer, IndexStorage<String, String> indexStorage) {
        this.dataIndexer = dataIndexer;
        this.indexStorage = indexStorage;
    }

    @Override
    @Nullable
    public ValueStorage<String> get(String key) throws IndexException {
        lock.readLock().lock();
        try {
            ValueStorage<String> vs = indexStorage.get(key);
            if (vs != null) {
                return vs.copy();
            }
            return null;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() throws IndexException {
        indexStorage.clear();
    }

    @Override
    public void update(FileWrapper file) throws IndexException {
        lock.writeLock().lock();
        try {
            String path = file.getPath().toString();
            removeHelper(path);
            if (!Files.isRegularFile(file.getPath())) {
                return;
            }
            Map<String, Void> newData = dataIndexer.index(file);
            for (String k : newData.keySet()) {
                indexStorage.add(k, path);
            }
            fileToKeys.put(file.getPath().toString(), newData.keySet());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(FileWrapper file) throws IndexException {
        lock.writeLock().lock();
        try {
            log.debug("remove from index {}", file);
            removeHelper(file.getPath().toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeHelper(String path) throws IndexException {
        Set<String> oldKeys = fileToKeys.get(path);
        if (oldKeys != null && !oldKeys.isEmpty()) {
            log.debug("remove old keys from {}", path);
            for (String k : oldKeys) {
                indexStorage.remove(k, path);
            }
        }
    }
}
