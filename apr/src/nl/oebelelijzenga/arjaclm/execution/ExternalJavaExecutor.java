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

package nl.oebelelijzenga.arjaclm.execution;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.execution.java8.TestMethodResolver;
import nl.oebelelijzenga.arjaclm.io.FileUtil;
import nl.oebelelijzenga.arjaclm.model.apr.Bug;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestSuite;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestSuiteResult;
import nl.oebelelijzenga.arjaclm.model.io.AprConfig;
import nl.oebelelijzenga.arjaclm.model.java.CompilationResult;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import org.apache.commons.exec.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExternalJavaExecutor {

    private final AprConfig aprConfig;
    private final Bug bug;
    private final JavaContext context;
    private final IJavaCompiler compiler;

    public ExternalJavaExecutor(AprConfig aprConfig, Bug bug, JavaContext context) {
        this.aprConfig = aprConfig;
        this.bug = bug;
        this.context = context;

        this.compiler = new ScriptJavaCompiler(aprConfig, bug, context);
    }

    public CompilationResult compileSourceFiles(List<Path> filePaths) throws AprException {
        return compiler.compile(filePaths, context.sourceBuildDir());
    }

    public CompilationResult compileAllSourceFiles() throws AprException {
        List<Path> sourceFilePaths = FileUtil.getJavaSourceFilePaths(context.srcDir()).stream().filter(
                path -> !path.startsWith(context.testDir())
        ).toList();
        return compileSourceFiles(sourceFilePaths);
    }

    public CompilationResult compileTestFiles(List<Path> filePaths) throws AprException {
        return compiler.compile(filePaths, context.testBuildDir());
    }

    public CompilationResult compileAllTestFiles() throws AprException {
        return compileTestFiles(FileUtil.getJavaSourceFilePaths(context.testDir()));
    }

    public TestSuiteResult test(TestSuite testSuite) throws AprException {
        ExternalJavaTestExecutor executor = new ExternalJavaTestExecutor(bug, aprConfig, context, testSuite);
        return executor.runTests();
    }

    public List<String> getTestMethodsFromClass(List<String> classes) throws AprException {
        if (classes.isEmpty()) {
            return new ArrayList<>();
        }

        CommandLine command = CommandLine.parse(aprConfig.java8Home().resolve("bin/java").toString());
        command.addArgument("-cp");
        command.addArgument(context.testClassPath().with(aprConfig.java8ToolsDir()).toString());
        command.addArgument(TestMethodResolver.class.getName());
        command.addArgument(context.testClassPath().toString());
        for (String cls : classes) {
            command.addArgument(cls);
        }

        CommandResult commandResult = CommandUtils.runCommand(command, 10, context.rootDir(), bug.env());

        if (!commandResult.success()) {
            throw new AprException("Failed to extract test methods from classes: " + commandResult, null);
        }

        return List.of(commandResult.stdout().strip().split("\n"));
    }
}
