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

package nl.oebelelijzenga.apr_proto.apr;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.exception.SanityCheckFailedException;
import nl.oebelelijzenga.apr_proto.genetic.PopulationResult;
import nl.oebelelijzenga.apr_proto.io.AprProblemLoader;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.io.JSONUtil;
import nl.oebelelijzenga.apr_proto.model.io.AprPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class AprRun {
    private static final Logger logger = LogManager.getLogger(AprRun.class);

    private final AprPreferences preferences;

    public AprRun(AprPreferences preferences) {
        this.preferences = preferences;
    }

    public AprResult execute() throws AprException {
        clearRunLog();
        logPreferences(preferences);

        long startTime = System.currentTimeMillis();

        AprProblem aprProblem;
        try {
            aprProblem = new AprProblemLoader(preferences).load();
            logger.info("Output directory is " + aprProblem.config().runOutDir());
        } catch (AprException e) {
            logger.error("Failed to load APR problem", e);
            return new AprResult(
                    true,
                    false,
                    System.currentTimeMillis() - startTime,
                    new PopulationResult()
            );
        }

        AprResult result = repair(startTime, aprProblem);

        AprRunDto runDto = new AprRunDto(preferences, aprProblem.config(), aprProblem.bug().name(), new AprResultDto(result));
        FileUtil.writeFile(aprProblem.config().runOutDir().resolve("run.json"), JSONUtil.toJSON(runDto));
        clearRunLog(aprProblem.config().runOutDir());

        return result;
    }

    private AprResult repair(long startTime, AprProblem aprProblem) {
        PopulationResult result;
        try {
            result = aprProblem.repair();
        } catch (AprException e) {
            logger.error("APR job encountered an exception", e);
            return new AprResult(
                    !(e instanceof SanityCheckFailedException),
                    true,
                    System.currentTimeMillis() - startTime,
                    new PopulationResult()
            );
        }
        return new AprResult(
                true,
                false,
                System.currentTimeMillis() - startTime,
                result
        );
    }

    private void clearRunLog(Optional<Path> exportDir) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        Appender infoAppender = config.getAppender("RUN_INFO_FILE");
        config.getAppenders().remove(infoAppender.getName());
        Appender debugAppender = config.getAppender("RUN_DEBUG_FILE");
        config.getAppenders().remove(debugAppender.getName());
        ctx.updateLoggers();

        Path runInfoLogPath = Path.of("var/log/run_info.log");
        Path runDebugLogPath = Path.of("var/log/run_debug.log");

        try {
            if (exportDir.isPresent()) {
                Files.copy(runInfoLogPath, exportDir.get().resolve("info.log"));
                Files.copy(runDebugLogPath, exportDir.get().resolve("debug.log"));
            }

            Files.write(runInfoLogPath, new byte[]{});
            Files.write(runDebugLogPath, new byte[]{});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        config.addAppender(infoAppender);
        config.addAppender(debugAppender);
        ctx.updateLoggers();
    }

    private void clearRunLog() {
        clearRunLog(Optional.empty());
    }

    private void clearRunLog(Path exportDir) {
        clearRunLog(Optional.of(exportDir));
    }

    public void logPreferences(AprPreferences preferences) {
        logger.info("APR Preferences:");
        for (Map.Entry<String, String> entry : preferences.toMap().entrySet()) {
            logger.info("%-25s = %s".formatted(entry.getKey(), entry.getValue()));
        }
    }
}
