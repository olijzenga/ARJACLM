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
import nl.oebelelijzenga.apr_proto.execution.java8.JUnitTestRunner;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.io.JSONUtil;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.TestCase;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.TestCaseResult;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.TestSuite;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.TestSuiteResult;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import org.apache.commons.exec.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ExternalJavaTestExecutor {

    private static final Logger logger = LogManager.getLogger(ExternalJavaTestExecutor.class);

    public static final int TEST_SUITE_TIMEOUT = 120;
    public static final int EXIT_CODE_TEST_CASE_TIMEOUT = 10;
    public static final int EXIT_CODE_CLASS_NOT_FOUND = 11;
    public static final int EXIT_CODE_TEST_WAIT_INTERRUPTED = 12;
    public static final String TEST_RESULT_JSON_SEPARATOR = "============================== APR Test Results ==============================\n";

    private final Bug bug;
    private final AprConfig input;
    private final JavaContext context;
    private final List<TestCase> allTests;

    public ExternalJavaTestExecutor(Bug bug, AprConfig input, JavaContext context, TestSuite testSuite) {
        this.bug = bug;
        this.input = input;
        this.context = context;
        this.allTests = new ArrayList<>(testSuite.all());
    }

    public TestSuiteResult runTests() throws AprException {
        CommandResult commandResult = CommandUtils.runCommand(getTestCommand(allTests), TEST_SUITE_TIMEOUT, context.rootDir(), bug.env());
        FileUtil.writeFile(context.aprDir().resolve("test.log"), commandResult.toFileString() +"\n\n");

        if (commandResult.timedOut()) {
            return new TestSuiteResult(false, new HashMap<>(), commandResult, "Test suite exceeded timeout of %s seconds".formatted(TEST_SUITE_TIMEOUT));
        }

        if (commandResult.exitCode() == EXIT_CODE_TEST_CASE_TIMEOUT) {
            return new TestSuiteResult(false, new HashMap<>(), commandResult, "A test case timed out");
        }

        if (commandResult.exitCode() != 0) {
            throw new AprException("Test suite exited with status code " + commandResult.exitCode());
        }

        if (!commandResult.stdout().contains(TEST_RESULT_JSON_SEPARATOR)) {
            return new TestSuiteResult(false, new HashMap<>(), commandResult, "Java 8 test runner returned unexpected stdout, probably used System.exit");
        }

        String resultJsonString = commandResult.stdout().split(TEST_RESULT_JSON_SEPARATOR, 2)[1];
        JUnitTestRunner.TestResults testResults = JSONUtil.fromJson(resultJsonString, JUnitTestRunner.TestResults.class);

        if (testResults.results.size() != allTests.size()) {
            throw new AprException("Test suite result does not contain results for %s test cases, but expected %s".formatted(testResults.results.size(), allTests.size()));
        }

        logger.debug("Test script result contains results for %s tests".formatted(testResults.results.size()));

        Map<TestCase, TestCaseResult> results = new HashMap<>();
        for (JUnitTestRunner.TestResult testResult : testResults.results) {
            TestCase method = TestCase.fromString(testResult.testMethod);
            results.put(
                    method,
                    new TestCaseResult(testResult.passed, testResult.timedout, testResult.runtime, testResult.failures)
            );

            if (!allTests.contains(method))
            {
                throw new AprException("Unexpectedly received a result for test case %s".formatted(method));
            }
        }

        return new TestSuiteResult(true, results, commandResult, "Success");
    }

    private CommandLine getTestCommand(List<TestCase> testCases) {
        CommandLine command = CommandLine.parse(input.java8Home().resolve("bin/java").toString());
        command.addArgument("-cp");
        command.addArgument(context.testClassPath().with(input.java8ToolsDir()).toString());
        command.addArgument("-Xms128m");
        command.addArgument("-Xmx2G");
        command.addArgument(JUnitTestRunner.class.getName());

        logger.debug("Executing tests with command %s <%s test cases>".formatted(
                String.join(" ", command.toStrings()),
                testCases.size()
        ));

        testCases.sort(Comparator.comparing(TestCase::toString));
        for (TestCase testCase : testCases) {
            command.addArgument(testCase.toString());
        }

        return command;
    }
}
