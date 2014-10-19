package simpleindexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * Created by Arbuzov Ivan on 20/10/14.
 */
public class PathFilter {
    private static final Logger log = LoggerFactory.getLogger(PathFilter.class);

    private List<Pattern> patterns;

    public PathFilter(File file) throws IOException {
        this();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = in.readLine();
        while (line != null) {
            if (!line.isEmpty()) {
                patterns.add(Pattern.compile(line));
            }
            line = in.readLine();
        }
    }

    public PathFilter() {
        patterns = new ArrayList<>();
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
        return true;
    }

    public String toString() {
        if (patterns.isEmpty())
            return "Empty path filter.";
        return "Path filter's regexps: " + patterns.toString();
    }
}
