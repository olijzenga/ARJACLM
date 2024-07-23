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

package nl.oebelelijzenga.apr_proto.execution;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.java.CompilationResult;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import org.apache.commons.exec.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptJavaCompiler implements IJavaCompiler {

    public static final String COMPILATION_SCRIPT = "compile.sh";
    public static final long COMPILATION_TIMEOUT = 300;  // Seconds

    private final AprConfig input;
    private final Bug bug;
    private final JavaContext context;

    public ScriptJavaCompiler(AprConfig input, Bug bug, JavaContext context) {
        this.input = input;
        this.bug = bug;
        this.context = context;
    }

    public CompilationResult compile(List<Path> filePaths, Path outDir) throws AprException {
        // NOTE: this method compiles all files, not only the ones provided
        FileUtil.mkdir(outDir);

        Map<String, String> env = new HashMap<>(bug.env());
        if (!env.containsKey("JAVA_HOME")) {
            env.put("JAVA_HOME", input.java8Home().toString());
        }

        CommandResult commandResult = CommandUtils.runCommand(
                CommandLine.parse(getCompilationScriptPath(context).toString()),
                COMPILATION_TIMEOUT,
                context.rootDir(),
                env
        );
        FileUtil.writeFile(context.aprDir().resolve("compile.log"), commandResult.toFileString());

        return new CompilationResult(commandResult.success(), commandResult);
    }

    private static Path getCompilationScriptPath(JavaContext context) {
        return context.rootDir().resolve(COMPILATION_SCRIPT);
    }
}
