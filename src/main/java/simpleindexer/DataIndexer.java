package simpleindexer;


import org.jetbrains.annotations.NotNull;
import simpleindexer.exceptions.IndexException;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for indexers used to extract data represented as map from {@code K key} to {@code V value},
 * which will be used in {@link simpleindexer.Index} as key and value respectively.
 *
 * @param <K> the type of elements used as keys in returned {@link java.util.Map}
 * @param <V> the type of elements used as values in in returned {@link java.util.Map}
 * @param <D> the type of elements used to extract data from.
 *
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public interface DataIndexer<K, V, D> {

    /**
     *
     * @param data to index key-values from
     * @return {@link java.util.Map} with key-values contained in {@code data}
     * @throws IOException
     */
    @NotNull
    Map<K, V> index(@NotNull D data) throws IndexException;

}
