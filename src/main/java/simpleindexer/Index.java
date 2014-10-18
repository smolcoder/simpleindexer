package simpleindexer;

import simpleindexer.exceptions.IndexException;
import simpleindexer.valuestorages.ValueStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for index implementation.
 *
 * @param <K> the type of elements used as keys in the index
 * @param <V> the type of elements used as values in the index.
 * @param <D> the type of elements used to extract data from.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public interface Index<K, V, D> {

    /**
     * Return {@link simpleindexer.valuestorages.ValueStorage} of values by given {@code key} if there is such data,
     * {@code null} otherwise.
     *
     * @param key key to search in index
     * @return {@link simpleindexer.valuestorages.ValueStorage} if there is data fot specified {@code key} or {@code null}
     * @throws simpleindexer.exceptions.IndexException
     */
    @Nullable
    public ValueStorage<V> get(K key) throws IndexException;

    /**
     * Clear index.
     *
     * @throws IndexException
     */
    public void clear() throws IndexException;

    /**
     * Update index with given {@code data}.
     *
     * @param data for update index from
     * @throws IndexException
     */
    public void update(D data) throws IndexException;

    /**
     * Remove from index all values corresponding to given {@code data}.
     * In other words, from all keys contained in result of {@link simpleindexer.DataIndexer#index(D data)} corresponding
     * values should be removed.
     *
     * @param data to remove from index
     * @throws IndexException
     */
    public void remove(D data) throws IndexException;

}
