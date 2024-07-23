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

package nl.oebelelijzenga.arjaclm.model.apr;

import nl.oebelelijzenga.arjaclm.io.FileUtil;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public record ClassPath(Set<Path> paths) {

    public String[] asStringArray() {
        String[] result = new String[paths.size()];
        int i = 0;
        for (Path path : paths) {
            result[i] = path.toString();
            i++;
        }
        return result;
    }

    @Override
    public String toString() {
        return String.join(":", asStringArray());
    }

    public int size() {
        return paths.size();
    }

    public ClassPath with(ClassPath other) {
        Set<Path> paths = new HashSet<>();
        paths.addAll(this.paths);
        paths.addAll(other.paths);
        return new ClassPath(paths);
    }

    public ClassPath with(Path... paths) {
        Set<Path> pathSet = new HashSet<>();
        pathSet.addAll(this.paths);
        pathSet.addAll(Arrays.asList(paths));
        return new ClassPath(pathSet);
    }

    public ClassPath replaceRoot(Path rootDir, Path newRootDir) {
        Set<Path> pathSet = new HashSet<>();
        for (Path path : paths) {
            if (path.isAbsolute() && path.startsWith(rootDir)) {
                pathSet.add(FileUtil.replaceRoot(path, rootDir, newRootDir));
            } else {
                pathSet.add(path);
            }
        }
        return new ClassPath(pathSet);
    }
}
