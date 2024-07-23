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

import nl.oebelelijzenga.arjaclm.apr.AprResult;
import nl.oebelelijzenga.arjaclm.apr.AprRun;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.exception.AprIOException;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(name = "benchmark")
public class BenchmarkCommand extends BaseAPRCommand implements Callable<Integer> {

    private static final Logger logger = LogManager.getLogger(BenchmarkCommand.class);

    @CommandLine.Parameters(description = "Directory containing all bug directories")
    protected Path bugsDir;

    @CommandLine.Option(names = {"--nr-seeds"}, description = "The number of seeds each bug will be repaired with")
    protected int nrSeeds = 1;

    @CommandLine.Option(names = {"--start-seed"}, description = "The first seed to be evaluated")
    protected int startSeed = 0;

    private int nrErrors = 0;

    @Override
    public Integer call() throws AprException, IOException {
        if (nrSeeds == 0) {
            logger.error("nrSeeds must be at least 1");
            return 1;
        }

        if (fitnessCacheDir == null) {
            fitnessCacheDir = outDir.resolve("cache");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String formattedDateTime = formatter.format(LocalDateTime.now());
        outDir = outDir.resolve("benchmark_%s".formatted(formattedDateTime));

        List<Path> bugDirs = getAllBugDirs();
        logger.info("Benchmarking %s bugs with %s seeds".formatted(bugDirs.size(), nrSeeds));


        for (Path bugDir : bugDirs) {
            Map<Integer, Variant> allResults = new HashMap<>();
            String bugName = "";
            for (int seed = startSeed; seed < startSeed + nrSeeds; seed++) {
                AprPreferences preferences = createPreferences(bugDir, seed);

                AprRun run = new AprRun(preferences);
                AprResult result = run.execute();

                for (Variant variant : result.population().correctVariants()) {
                    allResults.put(variant.enabledEditsHashCode(), variant);
                }

                if (result.executionError()) {
                    nrErrors++;
                }

            }
            logger.info("In total, %s unique test-adequate results were found for bug %s over all seeds".formatted(allResults.size(), bugName));
        }

        if (nrErrors == 0) {
            logger.info("Benchmarking completed with no errors");
        } else {
            logger.error("Benchmarking completed with %s errors".formatted(nrErrors));
        }

        return 0;
    }

    public List<Path> getAllBugDirs() throws AprIOException {
        try (Stream<Path> paths = Files.list(bugsDir))
        {
            return paths.filter(p -> Files.isDirectory(p) && p.toString().endsWith("_buggy")).sorted().toList();
        } catch (IOException e) {
            throw new AprIOException("Failed to list bug directories", e);
        }
    }

    @Override
    protected boolean getDefaultDeletePatchDirs() {
        return true;
    }
}
