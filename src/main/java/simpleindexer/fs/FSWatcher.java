package simpleindexer.fs;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleindexer.exceptions.IndexIllegalStateException;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static simpleindexer.utils.IndexerUtils.checkNotNull;

/**
 * Watcher if file system events represented as {@link java.nio.file.WatchEvent}.
 * <p>
 * Creates separate thread for polling events from {@link java.nio.file.WatchService}
 * Sends event wrapped to {@link simpleindexer.fs.FSEvent} to {@link simpleindexer.fs.FSEventDispatcher} instance.
 *
 * @author Ivan Arbuzov
 * 10/7/14.
 */
public class FSWatcher {

    private static final Logger log = LoggerFactory.getLogger(FSWatcher.class);

    @NotNull
    private final WatchService watchService;

    @NotNull
    private final FSEventDispatcher<? extends FSEventListener> eventDispatcher;

    @NotNull
    private final Thread watcherThread = new Thread(new Runnable() {

        private void processKey(WatchKey key) throws IOException {
            for (WatchEvent event : key.pollEvents()) {
                eventDispatcher.dispatch(new FSEvent((Path) key.watchable(), event));
            }
        }

        @Override
        public void run() {
            while (true) {
                if (watcherThread.getState() == Thread.State.TERMINATED) {
                    return;
                }
                WatchKey key;
                try {
                    key = watchService.poll(100, TimeUnit.MILLISECONDS);
                    if (key == null)
                        continue;
                } catch (InterruptedException e) {
                    log.warn("watcherThread was interrupted. Exit.");
                    return;
                }
                try {
                    processKey(key);
                    boolean valid = key.reset();
                    if (!valid) {
                        log.warn("key {} is not valid. Trying last poll.", key.watchable());
                        processKey(key);
                    }
                } catch (IOException e) {
                    log.error("Error while dispatching {}: {}", key.watchable(), e);
                } catch (IndexIllegalStateException e) {
                    log.warn(e.toString());
                    log.warn("Stop watcher due to IndexIllegalStateException");
                    return;
                }
            }
        }
    }, "watcher");

    /**
     * Create watcher with specified {@link java.nio.file.WatchService watchService} and
     * {@link simpleindexer.fs.FSEventDispatcher eventDispatcher}.
     *
     * @param watchService for polling events from
     * @param eventDispatcher for sending events to
     */
    public FSWatcher(@NotNull WatchService watchService, @NotNull FSEventDispatcher<? extends FSEventListener> eventDispatcher) {
        this.watchService = checkNotNull(watchService, "watchService");
        this.eventDispatcher = checkNotNull(eventDispatcher, "eventDispatcher");
    }

    /**
     * Starts watcher.
     */
    public void start() {
        log.info("starting FSWatcher...");
        if (watcherThread.getState() != Thread.State.NEW) {
            log.warn("watcherThread is already started.");
            return;
        }
        watcherThread.start();
    }

    /**
     * Stops watcher.
     * It will join watching thread after it interruption.
     */
    public void stop() {
        log.info("stopping FSWatcher...");
        if (watcherThread.getState() == Thread.State.TERMINATED || watcherThread.isInterrupted()) {
            log.warn("watcherThread is already stopped.");
            return;
        }
        watcherThread.interrupt();
        try {
            watcherThread.join();
        } catch (InterruptedException e) {
            log.error("stopping was interrupted: {}", e);
        }
    }
}
