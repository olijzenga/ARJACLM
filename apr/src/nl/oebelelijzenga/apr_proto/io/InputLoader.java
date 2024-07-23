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

package nl.oebelelijzenga.apr_proto.io;

import nl.oebelelijzenga.apr_proto.exception.AprIOException;
import nl.oebelelijzenga.apr_proto.genetic.PseudoRandom;
import nl.oebelelijzenga.apr_proto.model.apr.*;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.GeneticConfig;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.io.AprPreferences;
import nl.oebelelijzenga.apr_proto.model.io.PlmConfig;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InputLoader {

    public static final String BUG_FILE = "bug.json";

    private final AprPreferences preferences;
    private AprConfig input;
    private JavaContext context;
    private Bug bug;

    public InputLoader(AprPreferences preferences) {
        this.preferences = preferences;
    }

    public AprConfig getConfig() {
        return input;
    }

    public JavaContext getContext() {
        return context;
    }

    public Bug getBug() {
        return bug;
    }

    public void load() throws AprIOException {
        BugDTO bugDto = loadJSONBug();
        Path bugDir = FileUtil.pathToCanonical(preferences.bugDir());

        input = loadAPRInput(bugDto, bugDir);
        context = loadJavaContext(bugDto, bugDir);
        bug = loadBug(bugDto, bugDir);
    }

    private AprConfig loadAPRInput(BugDTO bugDTO, Path bugDir) throws AprIOException {
        PseudoRandom.setSeed(preferences.seed());

        if (bugDTO.positiveTests().isEmpty() || bugDTO.negativeTests().isEmpty()) {
            throw new AprIOException("At least one positive and negative test is required", null);
        }

        if (preferences.positiveTestWeight() < 0 || preferences.positiveTestWeight() > 1.0) {
            throw new AprIOException("Positive path weight must be between 0 and 1", null);
        }

        Path runDir = getRunDir(FileUtil.pathToCanonical(preferences.outDir()), bugDir);
        FileUtil.mkdir(runDir);
        FileUtil.mkdir(preferences.fitnessCacheDir());

        return new AprConfig(
                new PlmConfig(
                        preferences.plmEnabled(),
                        preferences.plmName(),
                        preferences.plmVariant(),
                        preferences.plmApiHost(),
                        preferences.plmApiPort(),
                        preferences.plmNrPromptContextLines(),
                        preferences.plmNrInfills(),
                        preferences.plmMutationProbability()
                ),
                new GeneticConfig(
                        preferences.populationSize(),
                        preferences.nrGenerations(),
                        preferences.nrAdditionalGenerations(),
                        preferences.eliteCount(),
                        preferences.earlyExit(),
                        preferences.mutationProbabilityMultiplier(),
                        preferences.geneticSearchTimeLimitSeconds()
                ),
                FileUtil.pathToCanonical(preferences.java8Home()),
                FileUtil.pathToCanonical(preferences.java8ToolsDir()),
                runDir,
                runDir.getParent(),
                preferences.fitnessCacheDir(),
                preferences.usePersistentFitnessCache(),
                preferences.nrJobs(),
                preferences.positiveTestWeight(),
                1 - preferences.positiveTestWeight(),
                preferences.modificationPointSuspiciousnessThreshold(),
                preferences.maxNrModificationPoints(),
                preferences.positiveTestRatio(),
                preferences.mu(),
                preferences.seed(),
                preferences.deleteIntermediatePatchDirs()
        );
    }

    /**
     * Example output for SimpleExample: [path to apr]/var/out/SimpleExample_20240214_121700
     */
    private Path getRunDir(Path rootOutDir, Path bugDir) throws AprIOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String formattedDateTime = formatter.format(LocalDateTime.now());

        return FileUtil.pathToCanonical(rootOutDir).resolve(bugDir.getFileName()).resolve(bugDir.getFileName() + "_" + formattedDateTime);
    }

    private JavaContext loadJavaContext(BugDTO bugDto, Path bugDir) throws AprIOException {
        Path aprDir = FileUtil.cleanPath(bugDir, bugDto.aprDir() == null ? "apr" : bugDto.aprDir());
        FileUtil.mkdir(aprDir);

        Path buildDir = FileUtil.cleanPath(bugDir, bugDto.sourceBuildDir() == null ? "apr/build" : bugDto.sourceBuildDir());
        Path testBuildDir = FileUtil.cleanPath(bugDir, bugDto.testBuildDir() == null ? "apr/build-tests" : bugDto.testBuildDir());

        return new JavaContext(
                bugDir,
                FileUtil.cleanPath(bugDir, bugDto.srcDir()),
                FileUtil.cleanPath(bugDir, bugDto.testDir()),
                aprDir,
                buildDir,
                testBuildDir,
                createClassPath(bugDir, bugDto.compileClassPath()),
                createClassPath(bugDir, bugDto.testClassPath()).with(buildDir, testBuildDir)
        );
    }

    private Bug loadBug(BugDTO bugDto, Path bugDir) throws AprIOException {
        return new Bug(
                bugDir.getFileName().toString().replace("_", " ").replace("buggy", ""),
                bugDto.positiveTests(),
                bugDto.negativeTests(),
                bugDto.flakyTests(),
                bugDto.env(),
                getBugLocations(bugDto.buggyLines(), bugDir)
        );
    }

    private BugDTO loadJSONBug() throws AprIOException {
        String fileContent = FileUtil.readFile(preferences.bugDir().resolve(BUG_FILE));
        return JSONUtil.fromJson(fileContent, BugDTO.class);
    }

    private List<BugLocation> getBugLocations(List<BuggyLineDTO> buggyLines, Path rootDir) throws AprIOException {
        List<BugLocation> result = new ArrayList<>();
        for (BuggyLineDTO line : buggyLines) {
            if (line.susScore() < 0 || line.susScore() > 1.0f) {
                throw new AprIOException("The susScore of a buggy line must not be less than 0 or greater than 1", null);
            }
            result.add(
                    new BugLocation(FileUtil.cleanPath(rootDir, line.file()), line.lineNr(), line.susScore())
            );
        }
        return result;
    }

    private ClassPath createClassPath(Path rootDir, List<String> paths) throws AprIOException {
        Set<Path> pathSet = new HashSet<>();
        for (String path : paths) {
            Path cleanPath = FileUtil.cleanPath(rootDir, path);
            pathSet.add(cleanPath);
        }
        return new ClassPath(pathSet);
    }
}
