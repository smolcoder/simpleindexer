package simpleindexer.fs;

import simpleindexer.exceptions.FileHasZeroLengthException;
import simpleindexer.exceptions.FileTooBigIndexException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wrapper for {@link File} file with lazy reading and file size bounders.
 *
 * @author Ivan Arbuzov
 * 10/8/14.
 */
public class FileWrapper {

    private final File file;

    private byte[] asBytes;

    private String asString;

    private final long maxFileSizeInBytes;

    public FileWrapper(Path path, long maxFileSizeInBytes) {
        this.file = path.toFile();
        this.maxFileSizeInBytes = maxFileSizeInBytes;
    }

    public Path getPath() {
        return Paths.get(file.getPath());
    }

    public String getContent() throws IOException, FileTooBigIndexException, FileHasZeroLengthException {
        return getContent(Charset.defaultCharset());
    }

    public String getContent(Charset charset) throws IOException, FileTooBigIndexException, FileHasZeroLengthException {
        if (asString == null)
            asString = new String(getBytes(), charset);
        return asString;
    }

    public byte[] getBytes() throws IOException, FileTooBigIndexException, FileHasZeroLengthException {
        if (asBytes == null)
            read();
        return asBytes;
    }

    private void read() throws IOException, FileTooBigIndexException, FileHasZeroLengthException {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            final long length = file.length();
            if (length == 0) {
                throw new FileHasZeroLengthException(file.toString());
            }
            if (length > maxFileSizeInBytes) {
                throw new FileTooBigIndexException(file.toString(), length, maxFileSizeInBytes);
            }
            asBytes = org.apache.commons.io.IOUtils.toByteArray(stream, length);
        }
    }
    @Override
    public String toString() {
        return getPath().toString();
    }

}
