/*
 * Copyright (c) 2024 Oebele Lijzenga
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.oebelelijzenga.arjaclm.io;

import nl.oebelelijzenga.arjaclm.exception.AprIOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtil {
    public static String readFile(Path path) throws AprIOException {
        try {
            // ISO_8859_1 is needed since Defects4J Codec 3 Base64Test.java fails to open otherwise
            return Files.readString(path, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new AprIOException("Failed to read file at " + path, e);
        }
    }

    public static void writeFile(Path path, String content) throws AprIOException {
        try {
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            throw new AprIOException("Failed to write to file " + path, e);
        }
    }

    public static List<Path> getJavaSourceFilePaths(Path parentDir) throws AprIOException {
        File srcFile = new File(parentDir.toString());
        Collection<File> javaFiles = FileUtils.listFiles(srcFile, new SuffixFileFilter(".java"), TrueFileFilter.INSTANCE);

        List<Path> result = new ArrayList<>();
        for (File file : javaFiles) {
            try {
                result.add(Path.of(file.getCanonicalPath()));
            } catch (IOException e) {
                throw new AprIOException(e.getMessage(), e);
            }
        }

        return result;
    }

    public static Path pathToCanonical(Path path) throws AprIOException {
        path = path.normalize();
        if (path.startsWith("~/")) {
            String homeDirectory = System.getProperty("user.home");
            path = Paths.get(homeDirectory, path.subpath(1, path.getNameCount()).toString());
        }

        try {
            return Path.of(new File(path.toString()).getCanonicalPath());
        } catch (IOException e) {
            throw new AprIOException(e.getMessage(), e);
        }
    }

    public static boolean pathEquals(Path a, Path b) throws AprIOException {
        a = pathToCanonical(a);
        b = pathToCanonical(b);
        return a.toString().equals(b.toString());
    }

    public static String[] pathListToStringArray(List<Path> paths) throws AprIOException {
        String[] result = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            result[i] = paths.get(i).toString();
        }
        return result;
    }

    public static Path cleanPath(Path rootDir, Path path) throws AprIOException {
        if (!path.isAbsolute()) {
            path = rootDir.resolve(path);
        }
        return FileUtil.pathToCanonical(path);
    }

    public static Path cleanPath(Path rootDir, String path) throws AprIOException {
        return cleanPath(rootDir, Path.of(path));
    }

    public static Path replaceRoot(Path path, Path oldRoot, Path newRoot) {
        assert path.startsWith(oldRoot);
        return newRoot.resolve(oldRoot.relativize(path));
    }

    public static void copySourceCodeFolder(Path source, Path target) throws AprIOException {
        if (target.startsWith(source)) {
            throw new AprIOException("Target path cannot start with source path as it leads to recursion issues", null);
        }

        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String dirName = dir.getFileName().toString();
                    if (dirName.equals(".git") || dirName.equals(".svn")) {
                        // Do not copy VCS directories to save disk space
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path newFile = target.resolve(source.relativize(file).toString());
                    Files.copy(file, newFile, StandardCopyOption.REPLACE_EXISTING);

                    // Copy timestamps to new file
                    BasicFileAttributeView sourceAttributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
                    BasicFileAttributes attributes = sourceAttributes.readAttributes();

                    BasicFileAttributeView targetAttributes = Files.getFileAttributeView(newFile, BasicFileAttributeView.class);
                    targetAttributes.setTimes(attributes.lastModifiedTime(), attributes.lastAccessTime(), attributes.creationTime());

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AprIOException("Failed to copy " + source + " to " + target, e);
        }
    }

    public static void mkdir(Path path) throws AprIOException {
        File dir = new File(path.toString());
        if (dir.exists()) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new AprIOException("Failed to create directory " + path, e);
        }
    }

    public static void deleteFile(Path path, boolean mustExist) throws AprIOException {
        if (!Files.exists(path)) {
            if (mustExist) {
                throw new AprIOException("File %s does not exist".formatted(path), null);
            }
            return;
        }

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new AprIOException("Failed to delete " + path, e);
        }
    }

    public static void deleteDirectory(Path path) throws AprIOException {
        try {
            FileUtils.deleteDirectory(new File(path.toString()));
        } catch (IOException e) {
            throw new AprIOException("Failed to delete directory", e);
        }
    }
}
