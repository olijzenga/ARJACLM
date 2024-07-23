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

package nl.oebelelijzenga.arjaclm.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "apr_cli", subcommands = {
        BugLocalizationCommand.class,
        ClmApiClientCommand.class,
        ParseJavaCommand.class,
        SanityCheckCommand.class,
        RepairCommand.class,
        BenchmarkCommand.class
})
public class AprCli {

    static class ExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception e, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
            e.printStackTrace(commandLine.getErr());
            commandLine.getErr().println("Command failed with an exception:");
            commandLine.getErr().println(e.getClass().getName() + ": " + e.getMessage());
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "log4j2.properties");

        int exitCode = new CommandLine(new AprCli())
                .setExecutionExceptionHandler(new AprCli.ExecutionExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }
}
