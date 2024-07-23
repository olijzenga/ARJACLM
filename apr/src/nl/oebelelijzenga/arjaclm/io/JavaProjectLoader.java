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

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.exception.AprIOException;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import nl.oebelelijzenga.arjaclm.model.java.RawJavaFile;
import nl.oebelelijzenga.arjaclm.parser.JavaParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaProjectLoader {
    private final JavaContext context;
    private final JavaParser parser;

    public JavaProjectLoader(JavaContext context) {
        this.context = context;
        this.parser = new JavaParser(context);
    }

    public JavaProject loadJavaProject() throws AprException {
        List<ParsedJavaFile> sourceFiles = parser.parseSourceFiles(getSourceFilePaths());

        List<RawJavaFile> testFiles = new ArrayList<>();
        for (Path testFilePath : getTestFilePaths()) {
            testFiles.add(
                    new RawJavaFile(
                            context.rootDir().relativize(testFilePath),
                            FileUtil.readFile(testFilePath)
                    )
            );
        }

        return new JavaProject(
                sourceFiles,
                testFiles,
                context
        );
    }

    private List<Path> getSourceFilePaths() throws AprIOException {
        List<Path> sourceFilePaths = FileUtil.getJavaSourceFilePaths(context.srcDir());
        // Filter out test files
        return sourceFilePaths.stream().filter(path -> !path.startsWith(context.testDir())).toList();
    }

    private List<Path> getTestFilePaths() throws AprIOException {
        return FileUtil.getJavaSourceFilePaths(context.testDir());
    }
}
