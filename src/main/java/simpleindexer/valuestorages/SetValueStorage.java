package simpleindexer.valuestorages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Implementation of {@link simpleindexer.valuestorages.ValueStorage} based on {@link ConcurrentSkipListSet}.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public class SetValueStorage<E> implements ValueStorage<E> {

    private final ConcurrentSkipListSet<E> set = new ConcurrentSkipListSet<>();

    @Override
    public void add(E e) {
        set.add(e);
    }

    @Override
    public boolean remove(E e) {
        return set.remove(e);
    }

    @Override
    public List<E> asList() {
        List<E> list = new ArrayList<>();
        for (E e : set.clone())
            list.add(e);
        return list;
    }

    public SetValueStorage<E> copy() {
        SetValueStorage<E> copy = new SetValueStorage<>();
        ConcurrentSkipListSet<E> copiedSet = set.clone();
        for (E e : copiedSet)
            copy.add(e);
        return copy;
    }


    public String toString() {
        return set.toString();
    }
}
