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
import nl.oebelelijzenga.apr_proto.exception.AprIOException;
import org.apache.commons.exec.*;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CommandUtils {

    public static CommandResult runCommand(CommandLine command, long timeout, Path workdir, Map<String, String> environment) throws AprException {
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(workdir.toString()));

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout * 1000);
        executor.setWatchdog(watchdog);

        // Use limited size output streams to avoid OutOfMemory errors caused by untrusted programs
        LimitedSizeOutputStream stdoutStream = new LimitedSizeOutputStream();
        LimitedSizeOutputStream stderrStream = new LimitedSizeOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(stdoutStream, stderrStream));

        // The spawned process inherts the environment of the parent Java process here. This is mostly done so that
        // the APR tool can find the defects4j executable using the $PATH variable.
        Map<String, String> executionEnv = new HashMap<>(System.getenv());
        executionEnv.putAll(environment);

        int exitCode;
        try {
            exitCode = executor.execute(command, executionEnv);
            assert exitCode == 0;
        } catch (ExecuteException e) {
            exitCode = e.getExitValue();
        } catch (IOException e) {
            throw new AprIOException("Failed to execute command " + command, e);
        }

        return new CommandResult(
                command,
                executionEnv,
                exitCode == 0 && !watchdog.killedProcess(),
                watchdog.killedProcess(),
                exitCode,
                stdoutStream.toString(),
                stderrStream.toString()
        );
    }

    public static CommandResult runCommand(CommandLine command, long timeout, Path workDir) throws AprException {
        return runCommand(command, timeout, workDir, new HashMap<>());
    }

    public static CommandResult runCommand(CommandLine command, long timeout) throws AprException {
        return runCommand(command, timeout, getProcessWorkdir());
    }

    private static Path getProcessWorkdir() {
        return Path.of(System.getProperty("user.dir"));
    }

    private static class LimitedSizeOutputStream extends OutputStream {

        private static final int MAX_SIZE = 10_000_000;

        private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        private long additionalChars = 0;

        @Override
        public void write(int i) throws IOException {
            if (byteArrayOutputStream.size() > MAX_SIZE) {
                additionalChars++;
                return;
            }

            byteArrayOutputStream.write(i);
        }

        @Override
        public String toString() {
            if (additionalChars != 0) {
                return "%s\n<%s more characters>".formatted(byteArrayOutputStream.toString(), additionalChars);
            }

            return byteArrayOutputStream.toString();
        }
    }
}
