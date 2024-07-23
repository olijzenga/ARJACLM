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

package nl.oebelelijzenga.arjaclm.parser;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.io.FileUtil;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaFileASTRequestor extends FileASTRequestor {

    private final List<ParsedJavaFile> files = new ArrayList<>();
    private final JavaParser parser;

    public JavaFileASTRequestor(JavaParser parser) {
        this.parser = parser;
    }

    public List<ParsedJavaFile> getFiles() {
        return files;
    }

    @Override
    public void acceptAST(String sourceFilePath, CompilationUnit ast) {
        ParsedJavaFile file;
        try {
            Path path = Path.of(sourceFilePath);
            file = parser.parseCompilationUnit(
                    ast,
                    path,
                    FileUtil.readFile(path)
            );
        } catch (AprException e) {
            // Cant pass on this error here :(
            throw new RuntimeException(e);
        }

        if (file != null) {
            files.add(file);
        }
    }
}
