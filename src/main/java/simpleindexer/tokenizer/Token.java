package simpleindexer.tokenizer;

/**
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public class Token {
    private final String data;

    public Token(String data) {
        this.data = data;
    }

    public String get() {
        return this.data;
    }
}
