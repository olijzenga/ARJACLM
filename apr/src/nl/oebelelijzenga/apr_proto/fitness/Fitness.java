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

package nl.oebelelijzenga.apr_proto.fitness;

import nl.oebelelijzenga.apr_proto.NumberUtil;
import nl.oebelelijzenga.apr_proto.ThreadUtil;
import nl.oebelelijzenga.apr_proto.exception.AprCriticalException;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.execution.ExternalJavaExecutor;
import nl.oebelelijzenga.apr_proto.execution.JavaExecutorFactory;
import nl.oebelelijzenga.apr_proto.genetic.PatchManager;
import nl.oebelelijzenga.apr_proto.model.apr.Patch;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.*;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import nl.oebelelijzenga.apr_proto.model.java.CompilationResult;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Fitness {

    private static final Logger logger = LogManager.getLogger(Fitness.class);
    public static final float MAX_LOSS = 999.0f;
    private final JavaExecutorFactory executorFactory;
    private final IFitnessCache fitnessCache;
    private final PatchManager patchManager;
    private final TestSuite testSuite;
    private final float positiveTestWeight;
    private final float negativeTestWeight;
    private final int nrJobs;

    private final Set<Integer> knownPatchIds = new HashSet<>();


    public Fitness(
            JavaExecutorFactory executorFactory,
            IFitnessCache fitnessCache,
            PatchManager patchManager,
            TestSuite testSuite,
            float positiveTestWeight,
            float negativeTestWeight,
            int nrJobs
    ) {
        this.executorFactory = executorFactory;
        this.fitnessCache = fitnessCache;
        this.patchManager = patchManager;
        this.testSuite = testSuite;
        this.positiveTestWeight = positiveTestWeight;
        this.negativeTestWeight = negativeTestWeight;
        this.nrJobs = nrJobs;
    }

    public FitnessResult evaluate(Patch patch) throws AprException {
        return evaluate(List.of(patch)).get(patch);
    }

    public Map<Patch, FitnessResult> evaluate(List<Patch> patches) throws AprException {
        List<Patch> patchesToEvaluate = new ArrayList<>(patches);
        Map<Patch, FitnessResult> results = new HashMap<>();

        // Check if any results are already in cache and remove the patch from the evaluation queue otherwise
        for (Patch patch : new ArrayList<>(patchesToEvaluate)) {
            Optional<FitnessResult> optionalCachedResult = getCachedFitnessResult(patch);
            if (optionalCachedResult.isPresent()) {
                logger.debug("Using cached variants " + optionalCachedResult.get().getSummary() + " for patch " + patch.id());
                results.put(patch, optionalCachedResult.get());
                patchesToEvaluate.remove(patch);
            }
        }

        long startTime = System.currentTimeMillis();

        Map<Patch, FitnessResult> newEvaluationResults = evaluateFitnessInParallel(patchesToEvaluate);

        float runTime = (float) (System.currentTimeMillis() - startTime) / 1000;
        logger.info(
                "Evaluated %s patches in %.1f seconds, of which %s were new in this run, and %s results were cached".formatted(
                        patches.size(),
                        runTime,
                        patches.stream().filter(p -> !knownPatchIds.contains(p.id())).count(),
                        results.size()
                )
        );

        results.putAll(newEvaluationResults);

        // Write patch info files and update and write fitness cache
        for (Patch patch : patches) {
            FitnessResult result = results.get(patch);
            fitnessCache.put(patch, result);
            if (!knownPatchIds.contains(patch.id())) {
                patchManager.writePatchInfoFile(result, patch, patchManager.createPatchContext(patch));
            }
            knownPatchIds.add(patch.id());
        }
        fitnessCache.save();

        return results;
    }

    private Map<Patch, FitnessResult> evaluateFitnessInParallel(List<Patch> patches) throws AprException {
        List<FitnessTask> tasks = patches.stream().map(p -> new FitnessTask(this, p, testSuite)).toList();
        ThreadUtil.runTasksInParallel(tasks, nrJobs);

        Map<Patch, FitnessResult> result = new HashMap<>();
        for (FitnessTask task : tasks) {
            if (task.getException() != null) {
                throw new AprException("Fitness evaluation task failed for patch %s".formatted(task.patch.id()), task.getException());
            }
            if (task.getResult() == null) {
                throw new AprCriticalException();
            }
            result.put(task.getPatch(), task.getResult());
        }

        return result;
    }

    private FitnessResult evaluateFitness(Patch patch, TestSuite testSuite) throws AprException {
        Variant variant = patch.variant();

        JavaContext context = patchManager.createPatchContext(patch);
        ExternalJavaExecutor executor = executorFactory.create(context);

        // Only compile edited files. Don't do anything if files are already compiled
        CompilationResult compilationResult = executor.compileSourceFiles(patch.editedFilesPaths());
        if (!compilationResult.success()) {
            return new FitnessResult(MAX_LOSS, MAX_LOSS, compilationResult, null);
        }

        logger.debug("Executing %s tests for patch %s".formatted(testSuite.all().size(), patch.id()));

        TestSuiteResult testSuiteResult = executor.test(testSuite);

        if (!testSuiteResult.success()) {
            logger.warn("Test execution failed with message \"%s\", probably just a bad patch".formatted(testSuiteResult.message()));
        }

        FitnessResult result = createFitnessResult(patch, compilationResult, testSuiteResult);

        logger.debug("Score for variant %s (%s) is %s".formatted(variant, context.rootDir().getFileName(), result.getSummary()));

        return result;
    }

    private Optional<FitnessResult> getCachedFitnessResult(Patch patch) {
        Optional<FitnessResult> optionalNaiveCachedResult = fitnessCache.get(patch);
        if (optionalNaiveCachedResult.isEmpty()) {
            return Optional.empty();
        }

        FitnessResult naiveCachedResult = optionalNaiveCachedResult.get();

        if (!naiveCachedResult.compilationResult().success()) {
            // Can re-use existing variants since weights are not applied when compilation fails
            return optionalNaiveCachedResult;
        }

        // Re-calculate fitness values as fitness weights can be different in this run compared to when the
        // cached variants was created.
        return Optional.of(createFitnessResult(
                naiveCachedResult.testSummary().positiveTestFailureRatio(),
                naiveCachedResult.testSummary().negativeTestFailureRatio(),
                patch.variant().enabledEdits().size(),
                naiveCachedResult.compilationResult(),
                naiveCachedResult.testSummary()
        ));
    }

    static class FitnessTask implements Runnable {
        private final Fitness fitness;
        private final Patch patch;
        private final TestSuite testSuite;
        private FitnessResult result;
        private Exception exception = null;

        public FitnessTask(Fitness fitness, Patch patch, TestSuite testSuite) {
            this.fitness = fitness;
            this.patch = patch;
            this.testSuite = testSuite;
        }

        @Override
        public void run() {
            try {
                result = this.fitness.evaluateFitness(patch, testSuite);
            } catch (AprException e) {
                exception = e;
            }
        }

        public Patch getPatch() {
            return patch;
        }

        public FitnessResult getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }

    private FitnessResult createFitnessResult(Patch patch, CompilationResult compilationResult, TestSuiteResult testSuiteResult) {
        Set<TestCase> failedPositiveTests = new HashSet<>();
        Set<TestCase> failedNegativeTests = new HashSet<>();

        for (Map.Entry<TestCase, TestCaseResult> entry : testSuiteResult.results().entrySet()) {
            TestCase testCase = entry.getKey();
            TestCaseResult result = entry.getValue();

            if (!result.passed()) {
                if (testSuite.positiveTests().contains(testCase)) {
                    failedPositiveTests.add(testCase);
                } else {
                    failedNegativeTests.add(testCase);
                }
            }
        }

        float failedPositiveTestRatio = Math.min(1.0f, (float) failedPositiveTests.size() / 5.0f);
        float failedNegativeTestRatio = (float) failedNegativeTests.size() / testSuite.negativeTests().size();

        return createFitnessResult(
                failedPositiveTestRatio,
                failedNegativeTestRatio,
                patch.variant().enabledEdits().size(),
                compilationResult,
                new FitnessResult.TestSummary(
                        testSuiteResult.success(),
                        failedPositiveTests,
                        failedPositiveTestRatio,
                        failedNegativeTests,
                        failedNegativeTestRatio,
                        testSuiteResult.commandResult()
                )
        );
    }

    private FitnessResult createFitnessResult(float failedPositiveTestRatio, float failedNegativeTestRatio, int nrEnabledEdits, CompilationResult compilationResult, FitnessResult.TestSummary testSummary) {
        if (nrEnabledEdits == 0 || !testSummary.success()) {
            // Punish the empty patch, and punish patches that cause test execution to fail altogether
            return new FitnessResult(MAX_LOSS, MAX_LOSS, compilationResult, testSummary);
        }

        float testSuiteFitness = NumberUtil.round((failedPositiveTestRatio * positiveTestWeight) + (failedNegativeTestRatio * negativeTestWeight), 2);
        float patchSizeFitness = (float) nrEnabledEdits;

        return new FitnessResult(
                testSuiteFitness,
                patchSizeFitness,
                compilationResult,
                testSummary
        );
    }
}
