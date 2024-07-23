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

package nl.oebelelijzenga.arjaclm.model.apr.fitness;

import nl.oebelelijzenga.arjaclm.execution.CommandResult;
import nl.oebelelijzenga.arjaclm.model.java.CompilationResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record FitnessResult(
        float testSuiteFitness,
        float patchSizeFitness,
        CompilationResult compilationResult,
        TestSummary testSummary
) {
    public record TestSummary(
            boolean success,
            Set<TestCase> failedPositiveTests,
            float positiveTestFailureRatio,
            // Negative test cases which failed
            Set<TestCase> failedNegativeTests,
            float negativeTestFailureRatio,
            CommandResult commandResult
    ) {
        public int getNrFailedPositiveTests() {
            return failedPositiveTests.size();
        }

        public int getNrFailedNegativeTests() {
            return failedNegativeTests.size();
        }

        public Set<TestCase> failedTests() {
            HashSet<TestCase> tests = new HashSet<>(failedPositiveTests);
            tests.addAll(failedNegativeTests);
            return tests;
        }
    }

    ;

    public List<Float> asScoresList() {
        return List.of(testSuiteFitness, patchSizeFitness);
    }

    public String scoresString() {
        return "{" + String.join(", ", asScoresList().stream().map("%.2f"::formatted).toList()) + "}";
    }

    public boolean isTestAdequate() {
        return compilationResult.success() && testSummary.success() && testSummary.failedNegativeTests.isEmpty() && testSummary.failedPositiveTests().isEmpty();
    }

    public boolean isSanityCheckResult() {
        return compilationResult.success() && testSummary.positiveTestFailureRatio() == 0.0f && testSummary.negativeTestFailureRatio() == 1.0f;
    }

    public String getSummary() {
        return String.format(
                "FitnessResult[testSuiteFitness=%s, patchSizeFitness=%s, compile=%s, failedPos=%s, failedNeg=%s]",
                testSuiteFitness,
                patchSizeFitness,
                compilationResult.success(),
                testSummary == null ? 0 : testSummary.getNrFailedPositiveTests(),
                testSummary == null ? 0 : testSummary.getNrFailedNegativeTests()
        );
    }

    public String toFileString() {
        StringBuilder result = new StringBuilder("test suite fitness: %s\n".formatted(testSuiteFitness));
        result.append("patch size fitness: %s\n".formatted(patchSizeFitness));
        result.append("compile success: %s\n".formatted(compilationResult.success()));
        result.append("test adequate: %s\n".formatted(isTestAdequate()));

        if (compilationResult.success()) {
            result.append("positive test failures: %s (%.1f%%)\n".formatted(
                    testSummary.getNrFailedPositiveTests(),
                    testSummary.positiveTestFailureRatio * 100
            ));
            for (TestCase testCase : testSummary.failedPositiveTests) {
                result.append("    ").append(testCase.toString()).append("\n");
            }

            result.append("negative test failures: %s (%.1f%%)\n".formatted(
                    testSummary.getNrFailedNegativeTests(),
                    testSummary.negativeTestFailureRatio * 100
            ));
            for (TestCase testCase : testSummary.failedNegativeTests) {
                result.append("    ").append(testCase.toString()).append("\n");
            }
        }

        return result.toString();
    }
};
