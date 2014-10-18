package simpleindexer.exceptions;

/**
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public class IndexException extends Exception {

    public IndexException(String msg) {
        super(msg);
    }

    public IndexException(Throwable e) {
        super(e);
    }
}
