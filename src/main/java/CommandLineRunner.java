import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
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

    public static final String[] COMMANDS = {"h", "q", "find", "add", "rm", "remove", "count"};

    private static Path getPath(String root, String mayBeRelative) {
        if (Paths.get(mayBeRelative).isAbsolute()) {
            return Paths.get(mayBeRelative);
        }
        return Paths.get(root, mayBeRelative).normalize();
    }

    public static void main(String[] args) throws IOException {
        WordToPathIndex index;
        String forRelativePaths = System.getProperty("user.dir");
        try {
            System.out.println("Initialize index...");
            index = new WordToPathIndex(FileSystems.getDefault(), System.getProperties());
            System.out.println("Used index properties: " + index.getProperties());
            System.out.println("Index initialized.");
            System.out.println(USAGE);
            System.out.println("You're in " + forRelativePaths);
        } catch (IOException e) {
            System.out.println("Can't start index: " + e);
            System.out.println("Exit.");
            return;
        }
        ConsoleReader console = new ConsoleReader();
        console.setPrompt(PROMPT);
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(
                new ArgumentCompleter.WhitespaceArgumentDelimiter(),
                new StringsCompleter(COMMANDS),
                new FileNameCompleter());
        console.addCompleter(argumentCompleter);
        String cmd;
        while((cmd = console.readLine()) != null) {
            try {
                if (cmd.isEmpty()) continue;
                if (cmd.startsWith("q"))
                {
                    console.println("Shutdown indexer. Wait, please...");
                    index.shutdown();
                    console.println("Done. Exit.");
                    console.flush();
                    System.exit(0);
                } else if (cmd.startsWith("h"))
                {
                    console.println(USAGE);
                } else if (cmd.startsWith("add"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        console.println(USAGE);
                    } else {
                        console.println("Adding " + arg[1] + " ...");
                        index.startWatch(getPath(forRelativePaths, arg[1]));
                    }
                } else if (cmd.startsWith("rm") || cmd.startsWith("remove"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        console.println(USAGE);
                    } else {
                        index.stopWatch(getPath(forRelativePaths, arg[1]));
                    }
                } else if (cmd.startsWith("count"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        console.println(USAGE);
                    } else {
                        List<String> paths = index.getPathsByWord(arg[1]);
                        console.println(Integer.toString(paths.size()));
                    }
                } else if (cmd.startsWith("find"))
                {
                    String[] arg = cmd.split(" ");
                    if (arg.length != 2) {
                        console.println(USAGE);
                    } else {
                        List<String> paths = index.getPathsByWord(arg[1]);
                        for (String p : paths) {
                            console.println(p);
                        }
                    }
                } else {
                    console.println("ERROR: Unknown command: " + cmd);
                    console.println(USAGE);
                }
            } catch (IndexException | NoSuchFileException e) {
                console.println("ERROR: " + e.getMessage());
            } catch (InterruptedException e) {
                console.println("Process was interrupted. Exit.");
                return;
            }
        }
    }

}
