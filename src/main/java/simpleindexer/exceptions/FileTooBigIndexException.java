package simpleindexer.exceptions;

/**
 * Created by Ivan Arbuzov.
 * 10/18/14.
 */
public class FileTooBigIndexException extends IndexException{
    public FileTooBigIndexException(String path, long real, long expected) {
        super("File " + path + " too large for indexing: " + real + " > " + expected);
    }
}
