package simpleindexer;

import gnu.trove.map.hash.THashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleindexer.exceptions.IndexException;
import simpleindexer.fs.FileWrapper;
import simpleindexer.valuestorages.SetValueStorage;
import simpleindexer.valuestorages.ValueStorage;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * More memory economical index implementation (but a little slower).
 *
 * Created by Arbuzov Ivan on 22/10/14.
 */
public class StringStringMemoIndex implements Index<String, String, FileWrapper> {
        private static final Logger log = LoggerFactory.getLogger(StringStringIndex.class);

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        private DataIndexer<String, Void, FileWrapper> dataIndexer;

        private Map<String, Set<String>> fileToKeys = new THashMap<>();

        public StringStringMemoIndex(DataIndexer<String, Void, FileWrapper> dataIndexer) {
            this.dataIndexer = dataIndexer;
        }

        @Override
        @Nullable
        public ValueStorage<String> get(String key) throws IndexException {
            lock.readLock().lock();
            try {
                ValueStorage<String> vs = new SetValueStorage<>();
                for (String p : fileToKeys.keySet()) {
                    if (fileToKeys.get(p) == null)
                        continue;
                    if (fileToKeys.get(p).contains(key))
                        vs.add(p);
                }
                if (vs.isEmpty()) {
                    return null;
                }
                return vs;
            }
            finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void clear() throws IndexException {
            fileToKeys.clear();
        }

        @Override
        public void update(FileWrapper file) throws IndexException {
            remove(file);
            if (!Files.isRegularFile(file.getPath())) {
                return;
            }
            Set<String> newData = dataIndexer.index(file).keySet();
            String path = file.getPath().toString();
            lock.writeLock().lock();
            try {
                fileToKeys.put(path, newData);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public void remove(FileWrapper file) throws IndexException {
            log.debug("remove from index {}", file);
            String path = file.toString();
            lock.writeLock().lock();
            try {
                fileToKeys.remove(path);
            } finally {
                lock.writeLock().unlock();
            }
        }
}
