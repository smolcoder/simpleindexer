import simpleindexer.exceptions.IndexException;
import simpleindexer.WordToPathIndex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;

/**
 * Created by Ivan Arbuzov.
 * 10/18/14.
 */
public class CommandLineRunner {

    public static final String USAGE =
            "Usage: q -- quit this program.\n" +
            "       h -- print help.\n" +
            "       add <path> -- start watch directory `path`. If path is not absolute, relative to current directory path will be used.\n" +
            "       rm <path> -- stop watch `path`.\n" +
            "       remove <path> -- see `rm` command.\n" +
            "       find <word> -- print all file-paths `word` is contained in. Note: `word` should be without whitespaces.\n";

    public static final String PROMPT = ">> ";

    private static Path getPath(String root, String mayBeRelative) {
        if (Paths.get(mayBeRelative).isAbsolute()) {
            return Paths.get(mayBeRelative);
        }
        return Paths.get(root, mayBeRelative).normalize();
    }

    public static void main(String[] args) {
        WordToPathIndex index = null;
        String forRelativePaths = System.getProperty("user.dir");
        try {
            System.out.println("Initialize index...");
            index = new WordToPathIndex(FileSystems.getDefault(), System.getProperties());
            System.out.println("Used index properties: " + index.getProperties());
            System.out.println("Index initialized.");
            System.out.println(USAGE);
            System.out.println("You're in " + forRelativePaths);
        } catch (IOException e) {
            System.err.println("Can't start index: " + e);
            System.err.println("Exit.");
            return;
        }
        String cmd = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            System.out.print(PROMPT);
            try {
                cmd = reader.readLine();
            } catch (IOException e) {
                System.err.println("Error while reading line: " + e);
                continue;
            }
            try {
                if (cmd == null) {
                    return;
                }
                if (cmd.isEmpty()) continue;
                if (cmd.startsWith("q"))
                {
                    index.shutdown();
                    System.out.println("Quit.");
                    return;
                } else if (cmd.startsWith("h"))
                {
                    System.out.println(USAGE);
                } else if (cmd.startsWith("add"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        System.out.println(USAGE);
                    } else {
                        index.startWatch(getPath(forRelativePaths, arg[1]));
                    }
                } else if (cmd.startsWith("rm") || cmd.startsWith("remove"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        System.out.println(USAGE);
                    } else {
                        index.stopWatch(getPath(forRelativePaths, arg[1]));
                    }
                } else if (cmd.startsWith("find")) {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        System.out.println(USAGE);
                    } else {
                        List<String> paths = index.getPathsByWord(arg[1]);
                        for (String p : paths) {
                            System.out.println(p);
                        }
                    }
                } else {
                    System.out.println("Unknown command: " + cmd);
                    System.out.println(USAGE);
                }
            } catch (IndexException | NoSuchFileException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Process was interrupted. Exit.");
                return;
            }
        }
    }

}
