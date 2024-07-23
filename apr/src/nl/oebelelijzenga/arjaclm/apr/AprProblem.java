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

package nl.oebelelijzenga.arjaclm.apr;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.exception.AprIOException;
import nl.oebelelijzenga.arjaclm.exception.SanityCheckFailedException;
import nl.oebelelijzenga.arjaclm.execution.JavaExecutorFactory;
import nl.oebelelijzenga.arjaclm.fitness.Fitness;
import nl.oebelelijzenga.arjaclm.fitness.FitnessCache;
import nl.oebelelijzenga.arjaclm.fitness.NoFitnessCache;
import nl.oebelelijzenga.arjaclm.genetic.PatchManager;
import nl.oebelelijzenga.arjaclm.genetic.Population;
import nl.oebelelijzenga.arjaclm.genetic.PopulationResult;
import nl.oebelelijzenga.arjaclm.model.apr.Bug;
import nl.oebelelijzenga.arjaclm.model.apr.ModificationPoint;
import nl.oebelelijzenga.arjaclm.model.apr.Patch;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.FitnessResult;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestCase;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.TestSuite;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import nl.oebelelijzenga.arjaclm.model.io.AprConfig;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.parser.ModificationPointFactory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AprProblem {

    private static final Logger logger = LogManager.getLogger(AprProblem.class);

    private final AprConfig config;
    private final PatchManager patchManager;
    private final JavaProject project;
    private final Bug bug;
    private final TestSuite fullTestSuite;
    private final TestSuite sampledTestSuite;
    private List<ModificationPoint> modificationPoints;

    public AprProblem(AprConfig config, PatchManager patchManager, JavaProject project, Bug bug, TestSuite fullTestSuite, TestSuite sampledTestSuite) {
        this.config = config;
        this.patchManager = patchManager;
        this.project = project;
        this.fullTestSuite = fullTestSuite;
        this.sampledTestSuite = sampledTestSuite;
        this.bug = bug;
    }

    public PopulationResult repair() throws AprException {
        logger.info("Fixing %s with seed %s".formatted(bug.name(), config.seed()));

        List<ModificationPoint> modificationPoints = modificationPoints();
        logger.info("The repair task uses %s modification points:".formatted(modificationPoints.size()));
        for (ModificationPoint modificationPoint : modificationPoints) {
            logger.info(modificationPoint);
        }

        logger.info("Doing sanity check");
        sanityCheck();
        logger.info("Sanity check passed");

        Fitness fitness = createFitness(sampledTestSuite, true);
        Population population = Population.create(config, fitness, patchManager, modificationPoints);
        PopulationResult result = population.evolve();

        logger.info("Genetic search found %s unique test-adequate patches for bug %s with seed %s".formatted(result.correctVariants().size(), bug.name(), config.seed()));

        logger.info("Validating test-adequate patches using the full test suite");
        List<Variant> testAdequateVariants = getPostProcessedVariants(result.correctVariants());

        List<Variant> faultyVariants = result.correctVariants().stream().filter(v -> !testAdequateVariants.contains(v)).toList();
        if (!faultyVariants.isEmpty()) {
            logger.info("%s variants were not test-adequate in post-processing and have been deleted".formatted(faultyVariants.size()));
        }

        if (config.deleteIntermediatePatchDirs()) {
            logger.info("Cleaning up patch directory");
            try {
                FileUtils.deleteDirectory(new File(config.runOutDir().resolve("patches").toString()));
            } catch (IOException e) {
                throw new AprIOException("Failed to delete patch directories", e);
            }
        }

        return new PopulationResult(testAdequateVariants, result.nrUniqueVariants(), result.generations());
    }

    public void sanityCheck() throws AprException {
        Fitness fitness = createFitness(fullTestSuite, false);
        FitnessResult fitnessResult = fitness.evaluate(patchManager.createPatch(Variant.create(new ArrayList<>())));

        if (!fitnessResult.isSanityCheckResult()) {
            logSanityCheckFailure(fitnessResult, bug);

            List<String> unexpectedFailures = fitnessResult.testSummary().failedTests().stream().filter(t -> !bug.negativeTests().contains(t.toString())).map(Object::toString).toList();
            List<String> unexpectedPasses = bug.negativeTests().stream().filter(t -> !fitnessResult.testSummary().failedNegativeTests().contains(TestCase.fromString(t))).toList();
            throw new SanityCheckFailedException("Unexpected failures: %s, unexpected passes: %s".formatted(unexpectedFailures, unexpectedPasses));
        }
    }

    public AprConfig config() {
        return config;
    }

    public JavaProject project() {
        return project;
    }

    public Bug bug() {
        return bug;
    }

    private List<ModificationPoint> modificationPoints() throws AprException {
        if (this.modificationPoints == null) {
            this.modificationPoints = ModificationPointFactory.create(config, project, bug);
        }
        return this.modificationPoints;
    }

    private void logSanityCheckFailure(FitnessResult fitnessResult, Bug bug) {
        logger.error("Sanity check failed");
        logger.error("Failing tests:");
        for (TestCase testCase : fitnessResult.testSummary().failedTests()) {
            logger.error("- %s".formatted(testCase));
        }
        logger.error("Tests that were expected to fail:");
        for (String testCase : bug.negativeTests()) {
            logger.error("- " + testCase);
        }
    }

    private Fitness createFitness(TestSuite testSuite, boolean cache) throws AprIOException {
        return new Fitness(
                new JavaExecutorFactory(config, bug),
                cache ? FitnessCache.create(config, bug) : new NoFitnessCache(),
                patchManager,
                testSuite,
                config.positiveTestWeight(),
                config.negativeTestWeight(),
                config.nrJobs()
        );
    }

    private List<Variant> getPostProcessedVariants(List<Variant> variants) throws AprException {
        Fitness fitness = createFitness(fullTestSuite, false);
        List<Variant> result = new ArrayList<>();
        for (Variant variant : variants) {
            Patch patch = patchManager.createPatch(variant);
            FitnessResult fitnessResult = fitness.evaluate(patch);
            if (fitnessResult.isTestAdequate()) {
                result.add(variant);
                JavaContext resultContext = patchManager.createResultContext(patch);
                patchManager.writePatchInfoFile(fitnessResult, patch, resultContext);
            } else {
                logger.info("Dropping patch %s as it does not pass the full test suite".formatted(variant.patchId()));
            }
        }
        return result;
    }
}
