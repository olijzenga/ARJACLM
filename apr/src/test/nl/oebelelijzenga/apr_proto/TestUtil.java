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

package test.nl.oebelelijzenga.apr_proto;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.apr.ClassPath;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.GeneticConfig;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.io.PlmConfig;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.JavaParser;

import java.nio.file.Path;
import java.util.*;

public class TestUtil {

    public static ParsedJavaFile getParsedJavaFile(String sourceCode, String fileName) throws AprException {

        return getDummyJavaParser().parseSourceCode(sourceCode, Path.of("./" + fileName));
    }

    public static JavaProject getParsedJavaFileAsProject(String sourceCode, String fileName) throws AprException {
        return getParsedJavaFilesAsProject(Map.of(fileName, sourceCode));
    }

    public static JavaProject getParsedJavaFilesAsProject(Map<String, String> sourceFiles) throws AprException {
        List<ParsedJavaFile> parsedFiles = new ArrayList<>();
        for (String fileName : sourceFiles.keySet().stream().sorted().toList()) {
            parsedFiles.add(getParsedJavaFile(sourceFiles.get(fileName), fileName));
        }
        return new JavaProject(parsedFiles, List.of(), getDummyJavaContext());
    }

    public static JavaParser getDummyJavaParser() {
        return new JavaParser(getDummyJavaContext());
    }

    private static JavaContext getDummyJavaContext() {
        return new JavaContext(
                Path.of("."),
                Path.of("src"),
                Path.of("tests"),
                Path.of("apr"),
                Path.of("apr/build"),
                Path.of("apr/build-tests"),
                new ClassPath(new HashSet<>()),
                new ClassPath(new HashSet<>())
        );
    }

    public static AprConfig getDummyAprConfig() {
        return new AprConfig(
                new PlmConfig(true, "codet5", "large", "localhost", 5000, 0, 0, 0.1f),
                new GeneticConfig(1, 1, 1, 1, true, 0.1f, 1),
                Path.of(""),
                Path.of(""),
                Path.of(""),
                Path.of(""),
                Path.of(""),
                true,
                1,
                0.1f,
                0.9f,
                0.1f,
                40,
                1.0f,
                0.06f,
                0,
                false
        );
    }
}
