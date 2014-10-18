package simpleindexer;

import gnu.trove.map.hash.THashMap;
import simpleindexer.exceptions.IndexException;
import simpleindexer.valuestorages.SetValueStorage;
import simpleindexer.valuestorages.ValueStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe implementation of {@link simpleindexer.IndexStorage}.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public class IndexStorageImpl implements IndexStorage<String, String> {

    private final Map<String, ValueStorage<String>> map = new THashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(String key, String value) throws IndexException {
        lock.writeLock().lock();
        try {
            if (!map.containsKey(key))
                map.put(key, new SetValueStorage<String>());
            map.get(key).add(value);
        } catch (Throwable e) {
            throw new IndexException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @Nullable
    public ValueStorage<String> get(String s) throws IndexException {
        lock.readLock().lock();
        try {
            return map.get(s);
        } catch (Throwable e) {
            throw new IndexException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(String key) throws IndexException {
        lock.readLock().lock();
        try {
            return map.containsKey(key);
        } catch (Throwable e) {
            throw new IndexException(e);
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public void remove(String s, String value) throws IndexException {
        lock.writeLock().lock();
        try {
            map.get(s).remove(value);
        } catch (Throwable e) {
            throw new IndexException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() throws IndexException {
        lock.writeLock().lock();
        try {
            map.clear();
        } catch (Throwable e) {
            throw new IndexException(e);
        } finally {
            lock.writeLock().lock();
        }
    }
}
