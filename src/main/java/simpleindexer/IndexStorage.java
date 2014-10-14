package simpleindexer;

import simpleindexer.valuestorages.ValueStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for data structures typically used in {@link simpleindexer.Index} for storing map from key to values.
 *
 * @param <K> the type of elements used as keys in the storage
 * @param <V> the type of elements used as values in the storage.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public interface IndexStorage<K, V> {

    /**
     * Add {@code value} to storage by specified {@code key}.
     *
     * @param key used to add value
     * @param value to add
     * @throws IndexException
     */
    public void add(K key, V value) throws IndexException;

    /**
     * Remove {@code value} from storage by specified {@code key}.
     *
     * @param key used to add value
     * @param value to remove
     * @throws IndexException
     */
    public void remove(K key, V value) throws IndexException;

    /**
     * Clear storage (remove all keys).
     *
     * @throws IndexException
     */
    public void clear() throws IndexException;

    /**
     * Return {@link simpleindexer.valuestorages.ValueStorage} by given {@code key}
     * or {@code null} if there is no such key in storage.
     *
     * @param key for extracting {@link simpleindexer.valuestorages.ValueStorage}
     * @return {@link simpleindexer.valuestorages.ValueStorage} corresponding to given {@code key}
     *      or {@code null} if there is no such key in storage.
     *
     * @throws IndexException
     */
    @Nullable
    ValueStorage<V> get(K key) throws IndexException;

    /**
     * Tests if the specified key in storage.
     *
     * @param key possible key
     * @return {@code true} if and only if the specified key
     *         exists in this storage (this fact determined by {@code equals} method),
     *         {@code false} otherwise.
     * @throws IndexException
     */
    public boolean contains(K key) throws IndexException;

}

