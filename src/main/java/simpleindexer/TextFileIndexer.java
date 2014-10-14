package simpleindexer;

import gnu.trove.map.hash.THashMap;
import simpleindexer.fs.FileWrapper;
import simpleindexer.tokenizer.Token;
import simpleindexer.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of {@link simpleindexer.DataIndexer}, that splits file by " \t\n\r\f,.:;?![]'()".
 *
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public class TextFileIndexer implements DataIndexer<String, Void, FileWrapper>{

    @Override
    @NotNull
    public Map<String, Void> index(@NotNull final FileWrapper file) throws IndexException {
        Map<String, Void> result = new THashMap<>();
        try {
            Tokenizer tokenizer = new Tokenizer() {
                private StringTokenizer stringTokenizer = new StringTokenizer(file.getContent(), " \t\n\r\f,.:;?![]'()");

                @Override
                public boolean hasMoreTokens() {
                    return stringTokenizer.hasMoreTokens();
                }

                @Override
                public Token nextToken() {
                    return new Token(stringTokenizer.nextToken());
                }
            };

            while (tokenizer.hasMoreTokens()) {
                result.put(tokenizer.nextToken().get(), null);
            }
        } catch (IOException e) {
            throw new IndexException(e);
        }
        return result;
    }

}
