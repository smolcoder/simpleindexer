package simpleindexer;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import simpleindexer.exceptions.IndexException;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class IndexTestBase {

    protected final static String TEST_DIR_NAME = "simpleindexer";

    protected final static String TEXT_A = "a aa aaa aaaa aaaaa, aaaaaa! aaaaaaa\n aaaaaaaa";
    protected final static String TEXT_B = "b bb bbb bbbb bbbbb, bbbbbb! bbbbbbb\n bbbbbbbb";
    protected final static String TEXT_C = "c cc ccc cccc ccccc, cccccc! ccccccc\n cccccccc";
    protected final static String TEXT_WIKI = "Java is a computer programming language that is concurrent, class-based, " +
            "object-oriented, and specifically Hello designed to have as few implementation dependencies as cc possible. " +
            "It is intended to let application developers write once, run anywhere (WORA), meaning that code " +
            "that runs on one platform does not need to be bbb recompiled to run on another.";
    protected final static String TEXT_HW = "Hello world!"; // yep, without comma

    protected String testDirPath;

    protected WordToPathIndex index;

    /**
     * We use Thread.sleep(sleepTimeBeforeMatching) between data generation/alteration and index calls
     * in order to get OS events be received by {@link java.nio.file.WatchService}.
     *
     * Also this time can be differ depending on OS (f.e. on Mac OS this time should be >1500 ms to get events,
     * but on Ubuntu 500 ms is enough.
     */
    protected long sleepTimeBeforeMatching = 1500; // milliseconds

    protected void matchAll(String query, String... suffixes) throws InterruptedException, IndexException {
        matchAllTestDir(query, testDirPath, suffixes);
    }

    protected void matchAllTestDir(String query, String testDirPath, String... suffixes) throws InterruptedException, IndexException {
        matchTestDirHelper(true, query, testDirPath, suffixes);
    }

    protected void matchAnyTestDir(String query, String testDirPath, String... suffixes) throws InterruptedException, IndexException {
        matchTestDirHelper(false, query, testDirPath, suffixes);
    }

    private void matchTestDirHelper(boolean strict, String query, String testDirPath, String... suffixes) throws InterruptedException, IndexException {
        List<String> list = index.getPathsByWord(query);
        if (strict)
            matchCount(query, list, suffixes.length);
        for (String s : suffixes) {
            Assert.assertTrue("List " + list + " doesn't contain path " + Paths.get(testDirPath, s),
                    list.contains(Paths.get(testDirPath, s).toString()));
        }
    }

    protected void matchCount(String query, int count) throws InterruptedException, IndexException {
        matchCount(query, index.getPathsByWord(query), count);
    }

    protected void matchCount(String query, List<String> paths, int count) throws InterruptedException, IndexException {
        Assert.assertEquals("Diff sizes on query '" + query + "': " + paths + ": ", count, paths.size());
    }

    protected void appendToFile(String filePath, String text) {
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Paths.get(testDirPath, filePath).toString(), true)))) {
            out.println(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void removeDirectory(String suffix) throws IOException {
        FileUtils.deleteDirectory(new File(String.valueOf(Paths.get(testDirPath, suffix))));
    }

    protected void createAndWrite(String text, String path, String ... other) throws IOException {
        Files.createFile(Paths.get(path, other));
        Files.write(Paths.get(path, other), text.getBytes());
    }

    protected void generateBigFile(int lines, String path, String ... other) throws IOException {
        Files.createFile(Paths.get(path, other));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; ++i) {
            sb.append("AAA BBB CCC DDD EEE\n");
        }
        try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(Paths.get(path, other).toFile()))) {
            out.write(sb.toString().getBytes());
        }
    }

    protected void fakeAppendToFile(String path, String ... other) throws IOException {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Paths.get(path, other).toString())))) {
            out.print("");
        }
    }

    protected static void addShutdownHook(final Path testDir) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(testDir.toFile());
                } catch (IOException e) {
                    System.err.println("Error while deleting temp test dir " + testDir + ": " + e);
                }
            }
        });
    }
    
}
