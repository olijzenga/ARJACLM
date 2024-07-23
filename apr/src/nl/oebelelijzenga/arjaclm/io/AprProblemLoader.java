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

package nl.oebelelijzenga.arjaclm.io;

import nl.oebelelijzenga.arjaclm.apr.AprProblem;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.execution.ExternalJavaExecutor;
import nl.oebelelijzenga.arjaclm.genetic.PatchManager;
import nl.oebelelijzenga.arjaclm.genetic.PseudoRandom;
import nl.oebelelijzenga.arjaclm.model.apr.Bug;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestCase;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestSuite;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.arjaclm.model.io.AprConfig;
import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AprProblemLoader {

    private static final Logger logger = LogManager.getLogger(AprProblemLoader.class);


    private final AprPreferences preferences;

    public AprProblemLoader(AprPreferences preferences) {
        this.preferences = preferences;
    }

    public AprProblem load() throws AprException {
        InputLoader inputLoader = new InputLoader(preferences);
        inputLoader.load();

        AprConfig config = inputLoader.getConfig();
        JavaContext context = inputLoader.getContext();
        Bug bug = inputLoader.getBug();

        PatchManager patchManager = new PatchManager(config, context);

        // Source files must be compiled at least once, as JavaProjectLoader cannot resolve AST symbols otherwise
        ExternalJavaExecutor executor = new ExternalJavaExecutor(config, bug, context);
        executor.compileAllSourceFiles().requireSuccess();
        executor.compileAllTestFiles().requireSuccess();

        JavaProject project = new JavaProjectLoader(context).loadJavaProject();
        TestSuite fullTestSuite = createFullTestSuite(config, project, bug);
        TestSuite sampledTestSuite = createSampledTestSuite(config, fullTestSuite);

        return new AprProblem(config, patchManager, project, bug, fullTestSuite, sampledTestSuite);
    }

    private TestSuite createFullTestSuite(AprConfig config, JavaProject project, Bug bug) throws AprException {
        Set<TestCase> positiveTests = testMethodNamesToMethods(expandTestSuiteToMethods(config, bug, project, bug.positiveTests()));
        Set<TestCase> negativeTests = testMethodNamesToMethods(expandTestSuiteToMethods(config, bug, project, bug.negativeTests()));

        // Remove all negative tests from positive tests. This way we can define an entire class to be positive but a single
        // method of that class to be negative
        positiveTests.removeAll(negativeTests);

        Set<TestCase> flakyTests = testMethodNamesToMethods(expandTestSuiteToMethods(config, bug, project, bug.flakyTests()));
        for (TestCase flakyTest : flakyTests) {
            if (positiveTests.contains(flakyTest)) {
                positiveTests.remove(flakyTest);
            } else {
                logger.warn("Could not find flaky test %s in list of positive tests".formatted(flakyTest));
            }
        }

        return new TestSuite(positiveTests, negativeTests);
    }

    private TestSuite createSampledTestSuite(AprConfig config, TestSuite fullTestSuite) {
        Set<TestCase> sampledPositiveTests = samplePositiveTests(fullTestSuite.positiveTests(), fullTestSuite.negativeTests(), config.positiveTestRatio());

        logger.info("Test suite contains %s positive tests (sampled %s)".formatted(fullTestSuite.positiveTests().size(), sampledPositiveTests.size()));
        for (TestCase testCase : sampledPositiveTests) {
            logger.debug(testCase);
        }

        logger.info("Test suite contains %s negative tests".formatted(fullTestSuite.negativeTests().size()));
        for (TestCase testCase : fullTestSuite.negativeTests()) {
            logger.debug(testCase);
        }

        return new TestSuite(sampledPositiveTests, fullTestSuite.negativeTests());
    }

    private static Set<TestCase> testMethodNamesToMethods(Set<String> methodNames) {
        return methodNames.stream().map(TestCase::fromString).collect(Collectors.toSet());
    }

    private Set<String> expandTestSuiteToMethods(AprConfig input, Bug bug, JavaProject project, Set<String> testClassesAndMethods) throws AprException {
        Set<String> result = new HashSet<>();
        List<String> testClasses = new ArrayList<>();
        for (String testClassOrMethod : testClassesAndMethods) {
            if (testClassOrMethod.contains("::")) {
                result.add(testClassOrMethod);
            } else {
                testClasses.add(testClassOrMethod);
            }
        }

        if (!testClasses.isEmpty()) {
            result.addAll((new ExternalJavaExecutor(input, bug, project.context())).getTestMethodsFromClass(testClasses));
        }

        return result;
    }

    /*
     * Samples a number of positive tests from the test suite in order to reduce fitness evaluation time.
     */
    private static Set<TestCase> samplePositiveTests(Set<TestCase> positiveTests, Set<TestCase> negativeTests, float positiveTestRatio)
    {
        int nrTests = (int) (positiveTests.size() * positiveTestRatio);
        if (positiveTests.size() <= nrTests) {
            return positiveTests;
        }

        logger.info("Sampling positive tests to reduce the number of test cases");

        Set<TestCase> testsInNegativeTestClasses = new HashSet<>();
        for (TestCase positiveTest : positiveTests) {
            if (negativeTests.stream().anyMatch(n -> QualifiedName.fromString(positiveTest.cls()).getParent().equals(QualifiedName.fromString(n.cls()).getParent()))) {
                testsInNegativeTestClasses.add(positiveTest);
            }
        }

        Set<TestCase> sampledPositiveTests = new HashSet<>(testsInNegativeTestClasses);

        if (sampledPositiveTests.size() >= nrTests) {
            return sampledPositiveTests;
        }

        List<TestCase> remainingPositiveTests = positiveTests.stream().filter(t -> !sampledPositiveTests.contains(t)).toList();
        sampledPositiveTests.addAll(PseudoRandom.pickList(remainingPositiveTests, nrTests - sampledPositiveTests.size()));

        return sampledPositiveTests;
    }
}
