package simpleindexer;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import simpleindexer.exceptions.IndexException;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Testing functional correctness of {@link simpleindexer.WordToPathIndex}.
 */
public class FunctionalTest extends IndexTestBase {

    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void starting(FrameworkMethod method) {
            System.out.println("Starting test: " + getClass()+ "." + method.getName());
        }

        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            System.out.println("Failed test: " + getClass() + "." + method.getName());
            e.printStackTrace();
        }
    };

    @Before
    public void initTest() throws IOException {
        final Path testDir = Files.createTempDirectory(TEST_DIR_NAME);
        addShutdownHook(testDir);

        testDirPath = testDir.toString();
        Paths.get(testDirPath, "foo1", "foo2", "foo3", "foo4").toFile().mkdirs();
        Paths.get(testDirPath, "foo1", "bar2", "foo3", "foo4").toFile().mkdirs();
        Paths.get(testDirPath, "bar1", "bar2", "bar3", "bar4", "bar5").toFile().mkdirs();
        Paths.get(testDirPath, "bar1", "bar2", "foo3", "bar4").toFile().mkdirs();

        createAndWrite(TEXT_A, testDirPath, "foo1", "file1");
        createAndWrite(TEXT_B, testDirPath, "foo1", "file2");
        createAndWrite(TEXT_C, testDirPath, "foo1", "file3");
        createAndWrite(TEXT_WIKI, testDirPath, "foo1", "foo2", "file1");
        createAndWrite(TEXT_HW,   testDirPath, "foo1", "foo2", "foo3", "file1");
        createAndWrite(TEXT_A, testDirPath, "foo1", "bar2", "file1");
        createAndWrite(TEXT_C, testDirPath, "foo1", "bar2", "foo3", "foo4", "file1");

        createAndWrite(TEXT_A, testDirPath, "bar1", "file1");
        createAndWrite(TEXT_B, testDirPath, "bar1", "bar2", "file1");
        createAndWrite(TEXT_C, testDirPath, "bar1", "bar2", "file2");
        createAndWrite(TEXT_WIKI, testDirPath, "bar1", "bar2", "bar3", "bar4", "file1");
        createAndWrite(TEXT_HW, testDirPath,   "bar1", "bar2", "foo3", "file1");
        createAndWrite(TEXT_A, testDirPath,   "bar1", "bar2", "foo3", "bar4", "file1");

        // use default IndexProperties
        Properties testProp = new Properties();
        testProp.setProperty(WordToPathIndex.IndexProperties.SKIP_FILES_WITHOUT_EXT_PROPERTY, "false");
        index = new WordToPathIndex(FileSystems.getDefault(), new WordToPathIndex.IndexProperties(testProp), testDir);
        try {
            Thread.sleep(sleepTimeBeforeMatching);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void afterIndexInitializeQueryTest() throws InterruptedException, IndexException {
        matchAll("aaaa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/bar2/file1", "foo1/file1");
        matchAll("Hello", "foo1/foo2/foo3/file1", "bar1/bar2/foo3/file1", "foo1/foo2/file1", "bar1/bar2/bar3/bar4/file1");
        matchCount("abracadabra", 0);
        matchCount("a", 6);
        matchAll("aa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/bar2/file1", "foo1/file1");
        matchAll("bb", "bar1/bar2/file1", "foo1/file2");
        matchAll("bbb", "bar1/bar2/bar3/bar4/file1", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
        matchAll("cccc", "bar1/bar2/file2", "foo1/file3", "foo1/bar2/foo3/foo4/file1");
        matchCount("bbbb", 2);
        matchAll("designed", "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
        matchCount("Java", 2);
        matchCount("WORA", 2);
        matchCount("once", 2);
        matchCount("once,", 0);
    }

    @Test
    public void modifyFileTest() throws InterruptedException, IndexException {
        appendToFile("bar1/bar2/file2", TEXT_A);
        appendToFile("bar1/bar2/foo3/bar4/file1", TEXT_C);
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("cccc", "bar1/bar2/file2", "foo1/file3", "foo1/bar2/foo3/foo4/file1", "bar1/bar2/foo3/bar4/file1");
        matchAll("aaaa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/bar2/file1", "foo1/file1", "bar1/bar2/file2");
    }

    @Test
    public void moveFileTest() throws InterruptedException, IOException, IndexException {
        Files.move(Paths.get(testDirPath, "bar1/bar2/file2"), Paths.get(testDirPath, "bar1/bar2/mfile2middle"));
        Thread.sleep(sleepTimeBeforeMatching);
        Files.move(Paths.get(testDirPath, "bar1/bar2/mfile2middle"), Paths.get(testDirPath, "bar1/bar2/mfile2"));
        Files.move(Paths.get(testDirPath, "bar1/bar2/bar3/bar4/file1"), Paths.get(testDirPath, "mfile1"));
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("designed", "mfile1", "foo1/foo2/file1");
        matchAll("bbb", "mfile1", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
        matchAll("cccc", "bar1/bar2/mfile2", "foo1/file3", "foo1/bar2/foo3/foo4/file1");
    }

    /*
     * This test also tests directory modification.
     */
    @Test
    public void deleteFileTest() throws InterruptedException, IOException, IndexException {
        Files.delete(Paths.get(testDirPath, "bar1/bar2/foo3/bar4/file1"));
        Files.delete(Paths.get(testDirPath, "foo1/bar2/file1"));
        Files.delete(Paths.get(testDirPath, "foo1/file3"));
        Files.delete(Paths.get(testDirPath, "bar1/bar2/bar3/bar4/file1"));
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("cccc", "bar1/bar2/file2", "foo1/bar2/foo3/foo4/file1");
        matchAll("aaaa", "bar1/file1", "foo1/file1");
        matchAll("bbb", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
        matchAll("Hello", "foo1/foo2/foo3/file1", "bar1/bar2/foo3/file1", "foo1/foo2/file1");
        matchAll("designed", "foo1/foo2/file1");
    }

    /*
     * This test also tests directory modification.
     */
    @Test
    public void createFileTest() throws InterruptedException, IOException, IndexException {
        createAndWrite("foo bar", testDirPath, "f1");
        createAndWrite("bar aa", testDirPath, "f2");
        createAndWrite("bar tar ccc", testDirPath, "bar1", "bar2", "f3");
        createAndWrite("test text Hello hello world Java", testDirPath, "bar1", "bar2", "bar3", "f4");
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("Java", "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1", "bar1/bar2/bar3/f4");
        matchAll("designed", "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
        matchAll("ccc", "foo1/file3", "bar1/bar2/file2", "foo1/bar2/foo3/foo4/file1", "bar1/bar2/f3");
        matchAll("bar", "f2", "bar1/bar2/f3", "f1");
        matchAll("foo", "f1");
        matchAll("tar", "bar1/bar2/f3");
        matchAll("aa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/bar2/file1", "foo1/file1", "f2");
    }

    @Test
    public void createDeleteFileTest() throws InterruptedException, IOException, IndexException {
        createAndWrite("foo bar", testDirPath, "f1");
        Files.delete(Paths.get(testDirPath, "f1"));
        createAndWrite("foo bar", testDirPath, "f1");
        Files.delete(Paths.get(testDirPath, "f1"));
        createAndWrite("foo bar", testDirPath, "f1");
        Files.delete(Paths.get(testDirPath, "f1"));
        Thread.sleep(sleepTimeBeforeMatching);
        afterIndexInitializeQueryTest();
        createAndWrite("foo bar", testDirPath, "f1");
        Files.delete(Paths.get(testDirPath, "f1"));
        createAndWrite("foo bar", testDirPath, "f1");
        Files.delete(Paths.get(testDirPath, "f1"));
        createAndWrite("foo bar", testDirPath, "f1");
        Thread.sleep(sleepTimeBeforeMatching);
        afterIndexInitializeQueryTest();
        matchAll("foo", "f1");
    }

    @Test
    public void createDeleteDirectoryTest() throws InterruptedException, IOException, IndexException {
        Paths.get(testDirPath, "bar3").toFile().mkdirs();
        createAndWrite("foo", testDirPath, "bar3", "f1");
        removeDirectory("bar3");
        Paths.get(testDirPath, "bar3").toFile().mkdirs();
        createAndWrite("foo", testDirPath, "bar3", "f1");
        removeDirectory("bar3");
        Paths.get(testDirPath, "bar3").toFile().mkdirs();
        createAndWrite("bar", testDirPath, "bar3", "f1");
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("bar", "bar3/f1");
        matchAll("foo");
    }

    @Test
    public void createDirectoryTest() throws InterruptedException, IOException, IndexException {
        Paths.get(testDirPath, "bar3").toFile().mkdirs();
        Paths.get(testDirPath, "foo1", "bar2", "foo5").toFile().mkdirs();
        createAndWrite("foo", testDirPath, "bar3", "f1");
        createAndWrite("bar", testDirPath, "foo1", "bar2", "foo5", "f1");
        createAndWrite("foo bar", testDirPath, "foo1", "bar2", "foo5", "f2");
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("bar", "foo1/bar2/foo5/f2", "foo1/bar2/foo5/f1");
        matchAll("foo", "foo1/bar2/foo5/f2", "bar3/f1");
    }

    @Test
    public void moveDirectoryTest() throws InterruptedException, IOException, IndexException {
        Files.move(Paths.get(testDirPath, "bar1/bar2/bar3/bar4"), Paths.get(testDirPath, "mbar4"));
        Thread.sleep(500);
        Files.move(Paths.get(testDirPath, "foo1/bar2"), Paths.get(testDirPath, "foo1/mbar2"));
        Thread.sleep(2 * sleepTimeBeforeMatching);
        matchAll("Hello", "foo1/foo2/foo3/file1", "bar1/bar2/foo3/file1", "foo1/foo2/file1", "mbar4/file1");
        matchAll("aa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/mbar2/file1", "foo1/file1");
        matchAll("bbb", "mbar4/file1", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
        matchAll("cccc", "bar1/bar2/file2", "foo1/file3", "foo1/mbar2/foo3/foo4/file1");
        matchAll("aaaa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/mbar2/file1", "foo1/file1");
        matchAll("designed", "mbar4/file1", "foo1/foo2/file1");
    }

    @Test
    public void deleteDirectoryTest() throws InterruptedException, IOException, IndexException {
        removeDirectory("bar1/bar2/bar3/bar4");
        removeDirectory("foo1/bar2");
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("Hello", "foo1/foo2/foo3/file1", "bar1/bar2/foo3/file1", "foo1/foo2/file1");
        matchAll("aa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/file1");
        matchAll("bbb", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
        matchAll("cccc", "bar1/bar2/file2", "foo1/file3");
        matchAll("aaaa", "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/file1");
        matchAll("designed", "foo1/foo2/file1");
    }

    @Test
    public void startWatchNewRootTest() throws InterruptedException, IOException {
        final String swTestDirPath = Paths.get(testDirPath).toString() + "startWatch";
        try {
            FileUtils.copyDirectory(new File(testDirPath), new File(swTestDirPath));
            index.startWatch(swTestDirPath);
            Thread.sleep(sleepTimeBeforeMatching);
            matchAnyTestDir("Hello", swTestDirPath, "foo1/foo2/foo3/file1", "bar1/bar2/foo3/file1", "foo1/foo2/file1", "bar1/bar2/bar3/bar4/file1");
            matchAllTestDir("abracadabra", swTestDirPath);
            matchAnyTestDir("aa", swTestDirPath, "bar1/bar2/foo3/bar4/file1", "bar1/file1", "foo1/bar2/file1", "foo1/file1");
            matchAnyTestDir("Java", swTestDirPath, "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
            matchAnyTestDir("bb", swTestDirPath, "bar1/bar2/file1", "foo1/file2");
            matchAnyTestDir("Java", testDirPath, "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
            matchAnyTestDir("bb", testDirPath, "bar1/bar2/file1", "foo1/file2");
            matchAnyTestDir("bbb", swTestDirPath, "bar1/bar2/bar3/bar4/file1", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
            matchAnyTestDir("cccc", swTestDirPath, "bar1/bar2/file2", "foo1/file3", "foo1/bar2/foo3/foo4/file1");
            matchAnyTestDir("bbb", testDirPath, "bar1/bar2/bar3/bar4/file1", "bar1/bar2/file1", "foo1/file2", "/foo1/foo2/file1");
            matchAnyTestDir("cccc", testDirPath, "bar1/bar2/file2", "foo1/file3", "foo1/bar2/foo3/foo4/file1");
            matchAnyTestDir("designed", swTestDirPath, "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
            matchAnyTestDir("designed", testDirPath, "bar1/bar2/bar3/bar4/file1", "foo1/foo2/file1");
        } catch (IndexException e) {
            e.printStackTrace();
        } finally {
            FileUtils.deleteDirectory(new File(String.valueOf(Paths.get(swTestDirPath))));
        }
    }

    @Test
    public void startWatchExistingRootsTest() throws InterruptedException, IOException, IndexException {
        index.startWatch(testDirPath);
        index.startWatch(Paths.get(testDirPath, "bar1"));
        index.startWatch(Paths.get(testDirPath, "bar1", "bar2"));
        afterIndexInitializeQueryTest();
    }

    @Test(expected = NoSuchFileException.class)
    public void startWatchFileTest() throws InterruptedException, IOException, IndexException {
        index.startWatch(Paths.get(testDirPath, "foo1", "file1"));
    }

    @Test(expected = NoSuchFileException.class)
    public void startWatchNotExistingTest() throws InterruptedException, IOException, IndexException {
        index.startWatch(Paths.get(testDirPath, "foo1", "notexisting"));
    }

    @Test
    public void stopWatchExistingRootTest() throws InterruptedException, IOException, IndexException {
        index.stopWatch(testDirPath);
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("Hello");
        matchAll("aa");
        matchAll("bb");
        matchAll("bbb");
        matchAll("cccc");
        matchAll("aaaa");
        matchAll("designed");
    }

    @Test
    public void stopWatchExistingDirsTest() throws InterruptedException, IOException, IndexException {
        index.stopWatch(Paths.get(testDirPath, "foo1/foo2/foo3/"));
        index.stopWatch(Paths.get(testDirPath, "bar1/"));
        Thread.sleep(sleepTimeBeforeMatching);
        matchAll("Hello", "foo1/foo2/file1");
        matchCount("a", 3);
        matchAll("aa", "foo1/bar2/file1", "foo1/file1");
        matchAll("bb", "foo1/file2");
        matchAll("bbb", "foo1/file2", "/foo1/foo2/file1");
        matchAll("cccc", "foo1/file3", "foo1/bar2/foo3/foo4/file1");
        matchAll("aaaa", "foo1/bar2/file1", "foo1/file1");
        matchAll("designed", "foo1/foo2/file1");
    }

    @Test(expected = NoSuchFileException.class)
    public void stopWatchFileTest() throws InterruptedException, IOException, IndexException {
        index.stopWatch(Paths.get(testDirPath, "foo1", "file1"));
    }

    @Test(expected = NoSuchFileException.class)
    public void stopWatchNotExistingRootTest1() throws InterruptedException, IOException, IndexException {
        index.stopWatch("/not/existing/root/");
        Thread.sleep(sleepTimeBeforeMatching);
        afterIndexInitializeQueryTest();
    }

    @Test(expected = NoSuchFileException.class)
    public void stopWatchNotExistingRootTest2() throws InterruptedException, IOException, IndexException {
        index.stopWatch(Paths.get(testDirPath, "foo1/foo2/notexists/"));
        Thread.sleep(sleepTimeBeforeMatching);
        afterIndexInitializeQueryTest();
    }

    @Test(expected = IllegalStateException.class)
    public void shutdownTest() throws IndexException, IOException, InterruptedException {
        index.shutdown();
        index.getPathsByWord("throw exception, please");
    }

    @Test(expected = IllegalStateException.class)
    public void shutdownOnoBigFilesMultiThreadTest() throws IndexException, IOException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        final Random random = new Random();
        for (int i = 0; i < 10; ++i) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    String rFileName = String.valueOf(random.nextInt(1000000000));
                    try {
                        generateBigFile(1 * 1024 * 1024, testDirPath, rFileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        index.shutdown();
        index.getPathsByWord("BBB");
        executor.shutdownNow();
    }

    @Test
    public void multiThreadQueryTest() throws InterruptedException, IndexException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(7);
        final Random random = new Random();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String[] words = new String[]{"aa", "bb", "cccc", "designed", "aaaa"};
                        int[] ans = new int[]{4, 2, 3, 2, 4};
                        List<Integer> vars = Arrays.asList(0, 1, 2, 3, 4);
                        Collections.shuffle(vars, random);
                        for (int i : vars) {
                            List<String> res = index.getPathsByWord(words[i]);
                            if (res.size() != ans[i]) {
                                throw new RuntimeException("Word " + words[i] + " contains not in " + ans[i] + " files, but in " + res.size());
                            }
                        }
                    } catch (InterruptedException | IndexException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();
    }

    // this test is allowed to fail, since it is strongly depends on OS/hardware.
    @Test
    public void bigFilesTest() throws IOException, InterruptedException, IndexException {
        // it supposed that MAX_FILE_SIZE_IN_BYTES = 1024 * 1024 * 30 in FileWrapper
        generateBigFile(3 * 1024 * 1024, testDirPath, "big1"); // too big
        generateBigFile(2 * 1024 * 1024, testDirPath, "big2"); // too big
        generateBigFile(1 * 1024 * 1024, testDirPath, "big3"); // ok
        generateBigFile(512 * 1024, testDirPath, "big4");      // ok
        Thread.sleep(500);
        fakeAppendToFile(testDirPath, "big1");
        Thread.sleep(500);
        fakeAppendToFile(testDirPath, "big2");
        Thread.sleep(2 * sleepTimeBeforeMatching);
        matchAll("AAA", "big3", "big4");
        matchAll("BBB", "big3", "big4");
        matchAll("CCC", "big3", "big4");
    }

}
