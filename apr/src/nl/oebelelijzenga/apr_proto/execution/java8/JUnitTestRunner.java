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

package nl.oebelelijzenga.apr_proto.execution.java8;

import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestSuite;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JUnitTestRunner {

    // Due to Math optimizer tests
    public static final int TEST_CASE_TIMEOUT = 20;

    public static void main(String[] args) {
        List<String> testCases = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                // Remove quoting of testcase names using spaces (usually parametrized tests)
                testCases.add(arg.substring(1, arg.length() - 1));
            } else {
                testCases.add(arg);
            }
        }
        JUnitTestRunner runner = new JUnitTestRunner(testCases);
        List<TestResult> results;
        try {
            results = runner.run();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(11);
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(12);
            return;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(13);
            return;
        }

        System.out.println("============================== APR Test Results ==============================");
        System.out.println(resultsToJSON(results));

        if (!results.isEmpty() && results.get(results.size() - 1).timedout) {
            // Signal to calling process that a testcase timed out and the suite needs to be re-run for the remaining cases
            System.exit(10);
            return;
        }

        int activeCount = Thread.activeCount();
        if (activeCount != 1) {
            System.err.println("There are still " + activeCount + " active threads");
        }

        // Force exit the program, even if dangling threads produced by tests are still running
        System.exit(0);
    }

    public static class TestResults {
        public final List<TestResult> results;

        public TestResults(List<TestResult> results) {
            this.results = results;
        }
    }

    public static class TestResult {
        public final String testMethod;
        public final boolean passed;
        public final boolean timedout;
        public final float runtime;
        public final List<String> failures;

        public TestResult(String testMethod, boolean passed, boolean timedout, float runtime, List<String> failures) {
            this.testMethod = testMethod;
            this.passed = passed;
            this.timedout = timedout;
            this.runtime = runtime;
            this.failures = failures;
        }
    }

    private final List<String> testCases;
    private List<TestResult> results;

    public JUnitTestRunner(List<String> testCases) {
        this.testCases = testCases;
    }

    public List<TestResult> run() throws ClassNotFoundException, InterruptedException, IllegalArgumentException {
        runTests();
        return results;
    }

    private boolean useJUnit4() {
        try {
            Class<?> cls = Request.class;
        } catch (NoClassDefFoundError e) {
            return false;
        }
        return true;
    }

    private void runTests() throws InterruptedException, ClassNotFoundException {
        results = new ArrayList<>();
        for (String testCase : testCases) {
            if (!testCase.contains("::")) {
                throw new IllegalArgumentException("Test cases must reference a function, not a class");
            }

            String[] elements = testCase.split("::");
            assert elements.length == 2;
            String className = elements[0];
            String methodName = elements[1];

            TestResult result;
            try {
                result = useJUnit4() ? runTestJUnit4(className, methodName) : runTestJUnit3(className, methodName);
            } catch (Throwable t) {
                result = new TestResult(
                        testCase,
                        false,
                        false,
                        -1.0f,
                        Arrays.asList("Test case has thrown an exception: " + formatException(t))
                );
            }


            results.add(result);
            if (result.timedout) {
                // Just exit on timeout, main will return the appropriate signal
                return;
            }
        }
    }

    private TestResult runTestJUnit3(String className, String methodName) throws InterruptedException, ClassNotFoundException {
        Class<?> cls = Class.forName(className);

        junit.framework.TestResult junitTestResult = new junit.framework.TestResult();
        Test test = TestSuite.createTest(cls, methodName);

        Thread thread = new Thread(() -> test.run(junitTestResult));

        long startTime = System.currentTimeMillis();
        thread.start();
        thread.join(TEST_CASE_TIMEOUT * 1000);
        float runTime = (float) (System.currentTimeMillis() - startTime) / 1000;
        if (runTime >= TEST_CASE_TIMEOUT - 1) {
            System.out.println("Hella done waiting, stopping the thread");
        }

        if (thread.isAlive()) {
            thread.stop();
            return new TestResult(
                    className + "::" + methodName,
                    false,
                    true,
                    runTime,
                    Arrays.asList("Test case timeout of " + TEST_CASE_TIMEOUT + " seconds exceeded")
            );
        }

        List<String> failures = new ArrayList<>();
        for (TestFailure failure : Collections.list(junitTestResult.failures())) {
            failures.add(failure.toString());
        }

        return new TestResult(
                className + "::" + methodName,
                junitTestResult.wasSuccessful(),
                false,
                runTime,
                failures
        );
    }

    private Request getTestCaseRequest(String className, String methodName) throws ClassNotFoundException, IllegalArgumentException {
        return Request.method(Class.forName(className), methodName);
    }

    private TestResult runTestJUnit4(String className, String methodName) throws InterruptedException, ClassNotFoundException {
        Request testCase = getTestCaseRequest(className, methodName);

        JUnitCore runner = new JUnitCore();
        final Result[] results = new Result[1];
        Thread thread = new Thread(() -> {
            results[0] = runner.run(testCase);
        });
        thread.start();

        thread.join(TEST_CASE_TIMEOUT * 1000);
        if (thread.isAlive()) {
            thread.stop();
            return new TestResult(
                    className + "::" + methodName,
                    false,
                    true,
                    TEST_CASE_TIMEOUT,
                    Arrays.asList("Test case timeout of " + TEST_CASE_TIMEOUT + " seconds exceeded")
            );
        }

        Result result = results[0];
        return new TestResult(
                className + "::" + methodName,
                result.wasSuccessful(),
                false,
                (float) result.getRunTime() / 1000,
                result.getFailures().stream().map(JUnitTestRunner::formatTestFailure).collect(Collectors.toCollection(ArrayList::new))
        );
    }

    private static String formatTestFailure(Failure failure) {
        if (failure.getException() == null) {
            return failure.toString();
        }

        return failure.getTestHeader() + ":\n" + formatException(failure.getException());
    }

    private static String formatException(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        stringWriter.append(e.getMessage()).append("\n");

        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.close();

        return stringWriter.toString();
    }

    private static String resultsToJSON(List<TestResult> results) {
        StringBuilder jsonBuilder = new StringBuilder("{\"results\": [");

        for (TestResult result : results) {
            StringBuilder failuresString = new StringBuilder();
            for (String failure : result.failures) {
                failuresString.append("\n\t\t\"").append(sanitizeJSONString(failure)).append("\",");
            }
            if (!result.failures.isEmpty()) {
                // Remove trailing comma
                failuresString.deleteCharAt(failuresString.length() - 1);
            }

            jsonBuilder.append(
                    String.format(
                            "{\n\t\"testMethod\": \"%s\",\n\t\"passed\": %s,\n\t\"timedout\": %s,\n\t\"failures\": [%s\n\t] },",
                            sanitizeJSONString(result.testMethod),
                            result.passed,
                            result.timedout,
                            failuresString.toString()
                    )
            );
        }

        if (!results.isEmpty()) {
            // Remove trailing comma
            jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
        }
        jsonBuilder.append("\t]\n}");

        return jsonBuilder.toString();
    }

    private static String sanitizeJSONString(String string) {
        return string.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
