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

package nl.oebelelijzenga.apr_proto.genetic.selection;

import nl.oebelelijzenga.apr_proto.genetic.PseudoRandom;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.FitnessResult;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class TournamentSelection {

    private static final Logger logger = LogManager.getLogger(TournamentSelection.class);

    private final List<Variant> variants;
    private final Map<Variant, FitnessResult> evaluations;
    private final Map<Variant, Float> variantCrowdingDistances;
    // Variant mapped to the variants by which it is dominated
    private final Map<Variant, Set<Variant>> dominationMatrix;

    public TournamentSelection(List<Variant> variants, Map<Variant, FitnessResult> evaluations) {
        this.variants = variants;
        this.evaluations = evaluations;

        Map<Variant, List<Float>> variantScoreLists = new HashMap<>();
        for (Variant variant : variants) {
            variantScoreLists.put(variant, this.evaluations.get(variant).asScoresList());
        }

        List<Variant> uniqueVariants = Variant.unique(variants);
        Map<Variant, Set<Variant>> uniqueDominationMatrix = dominationMatrix(uniqueVariants, variantScoreLists);
        List<List<Variant>> paretoFronts = calculateParetoFronts(uniqueVariants, uniqueDominationMatrix).stream().map(v -> reduplicateVariantList(v, variants)).toList();
        this.dominationMatrix = reduplicateDominationMatrix(uniqueDominationMatrix, variants);

        this.variantCrowdingDistances = new HashMap<>();
        for (List<Variant> front : paretoFronts) {
            // Pre-compute each front sorted by each individual objective
            Map<Integer, List<Variant>> sortedFrontByObjective = new HashMap<>();
            for (int i = 0; i < variantScoreLists.get(front.get(0)).size(); i++) {
                int finalI = i;
                sortedFrontByObjective.put(i, front.stream().sorted(Comparator.comparing(a -> variantScoreLists.get(a).get(finalI))).toList());
            }

            for (Variant variant : front) {
                this.variantCrowdingDistances.put(variant, getCrowdingDistance(variant, sortedFrontByObjective, variantScoreLists));
            }
        }
    }

    private static Map<Variant, Set<Variant>> reduplicateDominationMatrix(Map<Variant, Set<Variant>> uniqueDominationMatrix, List<Variant> duplicateVariants) {
        Map<Variant, List<Variant>> duplicateVariantMap = new HashMap<>();
        for (Variant variant : uniqueDominationMatrix.keySet()) {
            duplicateVariantMap.put(variant, reduplicateVariantList(List.of(variant), duplicateVariants));
        }

        Map<Variant, Set<Variant>> result = new HashMap<>();
        for (Variant uniqueVariant : uniqueDominationMatrix.keySet()) {
            Set<Variant> duplicateDominatedBy = new HashSet<>();
            for (Variant uniqueDominatedVariant : uniqueDominationMatrix.get(uniqueVariant)) {
                duplicateDominatedBy.addAll(duplicateVariantMap.get(uniqueDominatedVariant));
            }

            for (Variant duplicateVariant : duplicateVariantMap.get(uniqueVariant)) {
                result.put(duplicateVariant, duplicateDominatedBy);
            }
        }

        return result;
    }

    private static List<Variant> reduplicateVariantList(List<Variant> uniqueVariants, List<Variant> duplicateVariants) {
        List<Variant> result = new ArrayList<>();
        for (Variant variant : uniqueVariants) {
            result.addAll(duplicateVariants.stream().filter(v -> v.patchId() == variant.patchId()).toList());
        }
        return result;
    }

    private static Map<Variant, Set<Variant>> dominationMatrix(List<Variant> variants, Map<Variant, List<Float>> variantScoreLists) {
        Map<Variant, Set<Variant>> dominationMatrix = new HashMap<>();

        for (int i = 0; i < variants.size(); i++) {
            Variant variant1 = variants.get(i);
            dominationMatrix.put(variant1, new HashSet<>());
            for (int j = 0; j < i; j++) {
                Variant variant2 = variants.get(j);

                if (dominates(variant2, variant1, variantScoreLists)) {
                    dominationMatrix.get(variant1).add(variant2);
                    continue;
                }

                if (dominates(variant1, variant2, variantScoreLists)) {
                    dominationMatrix.get(variant2).add(variant1);
                }
            }
        }

        return dominationMatrix;
    }

    private static boolean dominates(Variant variant1, Variant variant2, Map<Variant, List<Float>> variantScoreLists) {
        List<Float> variant1Scores = variantScoreLists.get(variant1);
        List<Float> variant2Scores = variantScoreLists.get(variant2);

        for (int i = 0; i < variant1Scores.size(); i++) {
            if (variant1Scores.get(i) > variant2Scores.get(i)) {
                return false;
            }
        }

        for (int i = 0; i < variant1Scores.size(); i++) {
            if (variant1Scores.get(i) < variant2Scores.get(i)) {
                return true;
            }
        }

        return false;
    }

    private static List<List<Variant>> calculateParetoFronts(List<Variant> variants, Map<Variant, Set<Variant>> dominationMatrix) {
        List<List<Variant>> fronts = new ArrayList<>();
        List<Variant> remainingVariants = new ArrayList<>(variants);

        while (!remainingVariants.isEmpty()) {
            List<Variant> currentFront = new ArrayList<>();
            for (Variant variant : remainingVariants) {
                Set<Variant> dominatedBy = dominationMatrix.get(variant);
                if (remainingVariants.stream().noneMatch(dominatedBy::contains)) {
                    currentFront.add(variant);
                }
            }
            remainingVariants.removeAll(currentFront);
            fronts.add(currentFront);
        }

        return fronts;
    }

    private static Float getCrowdingDistance(Variant variant, Map<Integer, List<Variant>> sortedFrontByObjective, Map<Variant, List<Float>> variantScoreLists) {
        float crowdingDistance = 0.0f;

        for (int i = 0; i < variantScoreLists.get(variant).size(); i++) {
            List<Variant> sortedFront = sortedFrontByObjective.get(i);

            int variantIndex = sortedFront.indexOf(variant);
            if (variantIndex == 0 || variantIndex == sortedFront.size() - 1) {
                return Float.POSITIVE_INFINITY;
            }

            Variant previousVariant = sortedFront.get(variantIndex - 1);
            Variant nextVariant = sortedFront.get(variantIndex + 1);
            crowdingDistance += variantScoreLists.get(nextVariant).get(i) - variantScoreLists.get(previousVariant).get(i);
        }
        return crowdingDistance;
    }

    public List<Variant> select(List<Variant> variants, int amount) throws SelectionException {
        if (variants.isEmpty()) {
            throw new SelectionException("List of variants is empty", null);
        }

        if (variants.size() <= amount) {
            logger.warn("Too few variants to perform tournament selection. Returning all variants.");
            return variants;
        }

        List<Variant> result = new ArrayList<>();
        List<Variant> remainingVariants = new ArrayList<>(variants);

        while (result.size() < amount) {
            Variant contestant1 = PseudoRandom.pick(remainingVariants);
            Variant contestant2 = PseudoRandom.pick(remainingVariants);

            Variant winner = compareVariants(contestant1, contestant2) >= 0 ? contestant1 : contestant2;
            remainingVariants.remove(winner);
            result.add(winner);
        }

        return result;
    }

    public Variant selectOne() throws SelectionException {
        return select(this.variants, 1).get(0);
    }

    public int compareVariants(Variant variant1, Variant variant2) {
        FitnessResult variant1Fitness = evaluations.get(variant1);
        FitnessResult variant2Fitness = evaluations.get(variant2);

        if (!variant1Fitness.compilationResult().success() && !variant2Fitness.compilationResult().success()) {
            return 0;
        }
        if (!variant1Fitness.compilationResult().success()) {
            return -1;
        }
        if (!variant2Fitness.compilationResult().success()) {
            return 1;
        }

        if (variant1Fitness.isTestAdequate() != variant2Fitness.isTestAdequate()) {
            return Boolean.compare(variant1Fitness.isTestAdequate(), variant2Fitness.isTestAdequate());
        }

        // Inverted comparison since a lower domination count is better
        int dominationCountResult = Long.compare(dominationMatrix.get(variant2).size(), dominationMatrix.get(variant1).size());
        if (dominationCountResult != 0) {
            return dominationCountResult;
        }

        // Normal comparison since a greater crowding distance is better
        return Float.compare(variantCrowdingDistances.get(variant1), variantCrowdingDistances.get(variant2));
    }
}
