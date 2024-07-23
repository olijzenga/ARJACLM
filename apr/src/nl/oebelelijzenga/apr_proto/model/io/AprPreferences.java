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

package nl.oebelelijzenga.apr_proto.model.io;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record AprPreferences(
        Path bugDir,
        Path java8Home,
        Path java8ToolsDir,
        Path outDir,
        Path fitnessCacheDir,
        boolean usePersistentFitnessCache,
        int nrJobs,
        int seed,
        int populationSize,
        int nrGenerations,
        int nrAdditionalGenerations,
        int eliteCount,
        boolean earlyExit,
        float positiveTestWeight,
        float mutationProbabilityMultiplier,
        float modificationPointSuspiciousnessThreshold,
        int maxNrModificationPoints,
        float positiveTestRatio,
        float mu,
        boolean plmEnabled,
        String plmName,
        String plmVariant,
        String plmApiHost,
        int plmApiPort,
        int plmNrPromptContextLines,
        int plmNrInfills,
        float plmMutationProbability,
        boolean deleteIntermediatePatchDirs,
        int geneticSearchTimeLimitSeconds
) {
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("bugDir", bugDir.toString());
        map.put("java8Home", java8Home.toString());
        map.put("java8ToolsDir", java8ToolsDir.toString());
        map.put("outDir", outDir.toString());
        map.put("fitnessCacheDir", fitnessCacheDir.toString());
        map.put("usePersistentFitnessCache", Boolean.toString(usePersistentFitnessCache));
        map.put("nrJobs", Integer.toString(nrJobs));
        map.put("seed", Integer.toString(seed));
        map.put("populationSize", Integer.toString(populationSize));
        map.put("nrGenerations", Integer.toString(nrGenerations));
        map.put("nrAdditionalGenerations", Integer.toString(nrAdditionalGenerations));
        map.put("eliteCount", Integer.toString(eliteCount));
        map.put("earlyExit", Boolean.toString(earlyExit));
        map.put("positiveTestWeight", Float.toString(positiveTestWeight));
        map.put("mutationProbabilityMultiplier", Float.toString(mutationProbabilityMultiplier));
        map.put("modificationPointSuspiciousnessThreshold", Float.toString(modificationPointSuspiciousnessThreshold));
        map.put("maxNrModificationPoints", Integer.toString(maxNrModificationPoints));
        map.put("positiveTestRatio", Float.toString(positiveTestRatio));
        map.put("mu", Float.toString(mu));
        map.put("plmEnabled", Boolean.toString(plmEnabled));
        map.put("plmName", plmName);
        map.put("plmVariant", plmVariant);
        map.put("plmApiHost", plmApiHost);
        map.put("plmApiPort", Integer.toString(plmApiPort));
        map.put("plmNrPromptContextLines", Integer.toString(plmNrPromptContextLines));
        map.put("plmMutationProbability", Float.toString(plmMutationProbability));
        map.put("plmNrInfills", Integer.toString(plmNrInfills));
        map.put("deleteIntermediatePatchDirs", Boolean.toString(deleteIntermediatePatchDirs));
        map.put("geneticSearchTimeLimitSeconds", Integer.toString(geneticSearchTimeLimitSeconds));
        return map;
    }
}
