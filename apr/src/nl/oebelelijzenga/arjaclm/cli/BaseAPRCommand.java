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

import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import picocli.CommandLine;

import java.nio.file.Path;

public class BaseAPRCommand {

    @CommandLine.Option(names = {"--java8-home"})
    protected Path java8Home = Path.of("/usr/lib/jvm/java-8-openjdk");

    @CommandLine.Option(names = {"--java8-tools-path"})
    protected Path java8ToolsPath = Path.of(System.getProperty("user.dir")).resolve("target/java8");

    @CommandLine.Option(names = {"-o", "--out-dir"})
    protected Path outDir = Path.of(System.getProperty("user.dir")).resolve("var/out");

    @CommandLine.Option(names = {"--fitness-cache-dir"}, description = "Directory where fitness cache is stored")
    protected Path fitnessCacheDir = null;

    @CommandLine.Option(names = {"--persistent-fitness-cache"}, description = "Disable fitness cache")
    protected boolean usePersistentFitnessCache = true;

    @CommandLine.Option(names = {"-j", "--nr-jobs"}, description = "The number of threads used to evaluate variants in parallel")
    protected int nrJobs = 3;

    @CommandLine.Option(names = {"-p", "--population-size"})
    protected int populationSize = 40;

    @CommandLine.Option(names = {"-g", "--nr-generations"})
    protected int nrGenerations = 20;

    @CommandLine.Option(names = {"--nr-additional-generations"}, description = "The number of generations to run after a test-adequate solution is found. Only used when early exit is enabled.")
    protected int nrAdditionalGenerations = 5;

    @CommandLine.Option(names = {"--elite-count"}, description = "The number of best variants that are guaranteed to be preserved every generation")
    protected int eliteCount = 1;

    @CommandLine.Option(names = {"--early-exit"}, description = "Whether to stop immediately after finding a test-adequate solution")
    protected boolean earlyExit = false;

    @CommandLine.Option(names = {"--positive-test-weight"}, description = "Weight assigned to the failure rate of positive tests for fitness")
    protected float positiveTestWeight = 0.33f;

    @CommandLine.Option(names = {"--mutation-probability-multiplier"}, description = "Multiplier for the 1/[nr modpoints] mutation probability")
    protected float mutationProbabilityMultiplier = 0.1f;

    @CommandLine.Option(names = {"--suspiciousness-threshold"}, description = "The minimum suspiciousness for modification points")
    protected float modificationPointSuspiciousnessThreshold = 0.1f;

    @CommandLine.Option(names = {"--max-modpoints"}, description = "Maximum number of modification points")
    protected int maxNrModificationPoints = 40;

    @CommandLine.Option(names = {"--positive-test-ratio"}, description = "Ratio of the number of positive tests sampled for evaluating patches")
    protected float positiveTestRatio = 0.2f;

    @CommandLine.Option(names = {"--mu"}, description = "Multiplier for the probability that a modification point is initially set as enabled in the population")
    protected float mu = 0.06f;

    @CommandLine.Option(names = {"--clm-enabled"}, description = "Whether to use CLMs to generate patch ingredients")
    protected boolean clmEnabled = true;

    @CommandLine.Option(names = {"--clm-name"}, description = "Name of the CLM to use")
    protected String clmName = "refact";

    @CommandLine.Option(names = {"--clm-variant"}, description = "Variant of the CLM to use")
    protected String clmVariant = "1_6B-fim";

    @CommandLine.Option(names = {"--clm-api-host"}, description = "Host of the CLM API")
    protected String clmApiHost = "localhost";

    @CommandLine.Option(names = {"--clm-api-port"}, description = "Port of the CLM API")
    protected int clmApiPort = 5000;

    @CommandLine.Option(names = {"--clm-context-lines"}, description = "Number of lines of code context provided in CLM prompts")
    protected int clmNrPromptContextLines = 100;

    @CommandLine.Option(names = {"--clm-mutation-probability"}, description = "Probability of using the CLM mutation instead of the ARJA mutation")
    protected float clmMutationProbability = 0.4f;

    @CommandLine.Option(names = {"--clm-nr-infills"}, description = "The number of infills generated per CLM mutation")
    protected int clmNrInfills = 1;

    @CommandLine.Option(names = {"--delete-patch-dirs"}, description = "Whether to delete the patch directories after execution")
    protected boolean deletePatchDirs = getDefaultDeletePatchDirs();

    @CommandLine.Option(names = {"--time-limit"}, description = "Execution time limit for genetic search in seconds")
    protected int geneticSearchTimeLimitSeconds = 99999;

    public AprPreferences createPreferences(Path bugDir, int seed) {
        return new AprPreferences(
                bugDir,
                java8Home,
                java8ToolsPath,
                outDir,
                fitnessCacheDir != null ? fitnessCacheDir : outDir.resolve("cache"),
                usePersistentFitnessCache,
                nrJobs,
                seed,
                populationSize,
                nrGenerations,
                nrAdditionalGenerations,
                eliteCount,
                earlyExit,
                positiveTestWeight,
                mutationProbabilityMultiplier,
                modificationPointSuspiciousnessThreshold,
                maxNrModificationPoints,
                positiveTestRatio,
                mu,
                clmEnabled,
                clmName,
                clmVariant,
                clmApiHost,
                clmApiPort,
                clmNrPromptContextLines,
                clmNrInfills,
                clmMutationProbability,
                deletePatchDirs,
                geneticSearchTimeLimitSeconds
        );
    }

    protected boolean getDefaultDeletePatchDirs() {
        return false;
    }
}
