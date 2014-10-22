package simpleindexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleindexer.fs.FileWrapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 *
 * Created by Arbuzov Ivan on 20/10/14.
 */
public class PathFilter {
    private static final Logger log = LoggerFactory.getLogger(PathFilter.class);

    private List<Pattern> patterns;

    private WordToPathIndex.IndexProperties properties;

    public PathFilter(File file, WordToPathIndex.IndexProperties properties) throws IOException {
        this(properties);
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = in.readLine();
        while (line != null) {
            if (!line.isEmpty()) {
                patterns.add(Pattern.compile(line));
            }
            line = in.readLine();
        }
    }

    public PathFilter(WordToPathIndex.IndexProperties properties) {
        patterns = new ArrayList<>();
        this.properties = properties;
    }

    public boolean accept(Path path) {
        return accept(path.toString());
    }

    public boolean accept(String path) {
        for (Pattern p : patterns) {
            if (p.matcher(path).find()) {
                return false;
            }
        }
        if (properties.isSkipFilesWithoutExt() && Files.isRegularFile(Paths.get(path)) && !path.contains("."))
            return false;
        return defaultAccept(path);
    }

    public boolean defaultAccept(String path) {
        return !(new FileWrapper(Paths.get(path)).isBinary());
    }

    public String toString() {
        if (patterns.isEmpty())
            return "Empty path filter.";
        return "Path filter's regexps: " + patterns.toString();
    }
}
