package simpleindexer.valuestorages;

import java.util.List;

/**
 * {@link simpleindexer.valuestorages.ValueStorage} is used in {@link simpleindexer.IndexStorage} to store values by specified key.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public interface ValueStorage<E> {

    /**
     * Add value {@code e} to storage.
     *
     * @param e element to add
     */
    public void add(E e);

    /**
     * Remove value {@code e} from storage.
     *
     * @param e element to remove
     * @return {@code true} if value {@code e} was exist and successfully removed,
     *          {@code false} otherwise.
     */
    public boolean remove(E e);

    /**
     * Represent storage as {@link java.util.List} of values.
     *
     * @return {@link java.util.List} of values contained in the storage.
     */
    public List<E> asList();

    /**
     * Make shallow copy of the storage.
     *
     * @return new {@link simpleindexer.valuestorages.ValueStorage} contains all values from current storage
     */
    public ValueStorage<E> copy();

    /**
     * Checks whether value storage is empty.
     */
    public boolean isEmpty();

}
