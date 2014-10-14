package simpleindexer.tokenizer;

/**
 * Abstraction for implementation custom text tokenize: simple whitespace-based splitter, lexers, etc.
 * Typically is used with {@link simpleindexer.DataIndexer}.
 *
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public interface Tokenizer {

    public boolean hasMoreTokens();

    public Token nextToken();
}
