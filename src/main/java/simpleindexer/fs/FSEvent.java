package simpleindexer.fs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;



/**
 * Simple wrapping of {@link java.nio.file.WatchEvent.Kind}.
 * Used for simple pragmatically creating events.
 *
 * @see java.nio.file.WatchEvent.Kind
 * @author Ivan Arbuzov
 * 10/10/14.
 */
public class FSEvent {
    public static enum Kind {
        ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE, OVERFLOW;

        public static Kind toKind(final WatchEvent.Kind kind) {
            if (kind == StandardWatchEventKinds.ENTRY_CREATE)
                return ENTRY_CREATE;
            if (kind == StandardWatchEventKinds.ENTRY_DELETE)
                return ENTRY_DELETE;
            if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
                return ENTRY_MODIFY;
            if (kind == StandardWatchEventKinds.OVERFLOW)
                return OVERFLOW;
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    private final Kind kind;
    private final Path root;
    private final Path path;

    public FSEvent(final Path root, final WatchEvent e) {
        this.root = root;
        this.path = Paths.get(e.context().toString());
        this.kind = Kind.toKind(e.kind());
    }

    public boolean isDelete() {
        return kind == Kind.ENTRY_DELETE;
    }

    public boolean isCreate() {
        return kind == Kind.ENTRY_CREATE;
    }

    public boolean isModify() {
        return kind == Kind.ENTRY_MODIFY;
    }

    public boolean isOverflow() {
        return kind == Kind.OVERFLOW;
    }

    public Path getFullPath() {
        return Paths.get(root.toString(), path.toString());
    }

    public Kind getKind() {
        return kind;
    }

    public Path getPath() {
        return path;
    }

    public Path getRoot() {
        return root;
    }

    public String toString() {
        return kind + ": " + getFullPath();
    }

}
