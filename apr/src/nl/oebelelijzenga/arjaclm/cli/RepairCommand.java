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
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "repair")
public class RepairCommand extends AbstractSingleAPRRunCommand implements Callable<Integer> {

    private static final Logger logger = LogManager.getLogger(RepairCommand.class);

    @Override
    public Integer call() throws Exception {
        AprPreferences preferences = createPreferences();
        AprRun aprRun = new AprRun(preferences);
        AprResult result = aprRun.execute();

        List<Variant> results = result.population().correctVariants();
        if (results.isEmpty()) {
            logger.info("Failed to find a fix :(");
            return 0;
        }

        List<Pair<Long, Variant>> variantCounts = new ArrayList<>();
        for (Variant variant : Variant.unique(results)) {
            variantCounts.add(ImmutablePair.of(results.stream().filter(r -> r.patchId() == variant.patchId()).count(), variant));
        }
        variantCounts.sort(Comparator.comparingLong(Pair::getLeft));
        Collections.reverse(variantCounts);

        // Show the results in order of how often they appear
        logger.info("");
        logger.info("Test-adequate patches:");
        for (Pair<Long, Variant> entry : variantCounts) {
            logger.info("  %2sx %s".formatted(entry.getLeft(), entry.getRight().toString()));
        }

        return 0;
    }
}
