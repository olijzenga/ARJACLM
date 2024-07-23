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

import org.apache.commons.exec.CommandLine;

import java.util.HashMap;
import java.util.Map;

public record CommandResult(
        String command,
        Map<String, String> environment,
        boolean success,
        boolean timedOut,
        int exitCode,
        String stdout,
        String stderr
) {
    public CommandResult(CommandLine command, Map<String, String> environment, boolean success, boolean timedOut, int exitCode, String stdout, String stderr) {
        this(String.join(" ", command.toStrings()), environment, success, timedOut, exitCode, stdout, stderr);
    }

    public String toFileString() {
        String envString = String.join("\n", environment.entrySet().stream().map(e -> "%s=%s".formatted(e.getKey(), e.getValue())).toList());
        return "command:\n%s\nenv:\n%s\nexitcode: %d\nstdout:\n%s\nstderr:\n%s\n".formatted(
                command,
                envString,
                exitCode,
                stdout,
                stderr
        );
    }

    public static CommandResult empty() {
        return new CommandResult("", new HashMap<>(), true, false, 0, "", "");
    }
}
