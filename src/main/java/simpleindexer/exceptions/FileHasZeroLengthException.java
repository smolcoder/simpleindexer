package simpleindexer.exceptions;

/**
 * Created by Ivan Arbuzov.
 * 10/18/14.
 */
public class FileHasZeroLengthException extends IndexException{
    public FileHasZeroLengthException(String path) {
        super("File " + path + " has zero length.");
    }
}
