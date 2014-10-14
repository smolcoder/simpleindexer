package simpleindexer.utils;

/**
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public class IndexerUtils {
    public static <T> T checkNotNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    public static <T> T checkNotNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

}
