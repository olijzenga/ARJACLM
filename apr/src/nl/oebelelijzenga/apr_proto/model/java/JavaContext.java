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

package nl.oebelelijzenga.apr_proto.model.java;

import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.model.apr.ClassPath;

import java.nio.file.Path;

public record JavaContext(
        Path rootDir,
        Path srcDir,
        Path testDir,
        Path aprDir,
        Path sourceBuildDir,
        Path testBuildDir,
        ClassPath compileClassPath,
        ClassPath testClassPath
) {
    public ClassPath getFullClassPath() {
        return compileClassPath.with(testClassPath).with(srcDir, testDir);
    }

    public JavaContext withRoot(Path newRootDir) {
        return new JavaContext(
                newRootDir,
                FileUtil.replaceRoot(srcDir, rootDir, newRootDir),
                FileUtil.replaceRoot(testDir, rootDir, newRootDir),
                FileUtil.replaceRoot(aprDir, rootDir, newRootDir),
                FileUtil.replaceRoot(sourceBuildDir, rootDir, newRootDir),
                FileUtil.replaceRoot(testBuildDir, rootDir, newRootDir),
                compileClassPath.replaceRoot(rootDir, newRootDir),
                testClassPath.replaceRoot(rootDir, newRootDir)
        );
    }

    public Path relativeSourceDir() {
        return rootDir.relativize(srcDir);
    }
}
