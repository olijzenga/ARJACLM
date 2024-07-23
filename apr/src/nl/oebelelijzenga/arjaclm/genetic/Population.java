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

package nl.oebelelijzenga.arjaclm.genetic;

import nl.oebelelijzenga.arjaclm.NumberUtil;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.fitness.Fitness;
import nl.oebelelijzenga.arjaclm.genetic.crossover.CrossoverFactory;
import nl.oebelelijzenga.arjaclm.genetic.mutation.MutationFactory;
import nl.oebelelijzenga.arjaclm.genetic.selection.TournamentSelection;
import nl.oebelelijzenga.arjaclm.model.apr.ModificationPoint;
import nl.oebelelijzenga.arjaclm.model.apr.Patch;
import nl.oebelelijzenga.arjaclm.model.apr.fitness.FitnessResult;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Edit;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.GeneticConfig;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.arjaclm.model.io.AprConfig;
import nl.oebelelijzenga.arjaclm.parser.ingredient.ModificationScreener;
import nl.oebelelijzenga.arjaclm.parser.manipulation.ManipulationName;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Population {

    private static final Logger logger = LogManager.getLogger(Population.class);

    private final GeneticConfig geneticConfig;
    private final Fitness fitness;
    private final PatchManager patchManager;
    private List<Variant> variants;
    private final int size;
    private final boolean earlyExit;
    private final MutationFactory mutationFactory;
    private final CrossoverFactory crossoverFactory;
    private int generation = 0;
    private Map<Variant, FitnessResult> evaluation = new HashMap<>();
    private boolean foundSolution = false;
    private final Set<Integer> allPatchIds = new HashSet<>();

    private Population(AprConfig aprConfig, Fitness fitness, PatchManager patchManager, List<Variant> initialPopulation, float mutationProbability) {
        this.geneticConfig = aprConfig.geneticConfig();
        this.fitness = fitness;
        this.patchManager = patchManager;
        this.variants = initialPopulation;
        this.size = initialPopulation.size();
        this.earlyExit = aprConfig.geneticConfig().earlyExit();
        this.mutationFactory = new MutationFactory(aprConfig, mutationProbability);
        this.crossoverFactory = new CrossoverFactory();
    }

    public static Population create(AprConfig aprConfig, Fitness fitness, PatchManager patchManager, List<ModificationPoint> modificationPoints) throws AprException {
        List<Variant> variants = new ArrayList<>();
        float mutationProbability = NumberUtil.round(aprConfig.geneticConfig().mutationProbabilityMultiplier() / modificationPoints.size(), 3);
        logger.info("Mutation probability is %s".formatted(mutationProbability));

        // Shuffle modification points such that single-point crossover does not only favor a specific part of the code
        List<ModificationPoint> shuffledModificationPoints = PseudoRandom.shuffle(modificationPoints);
        for (int i = 0; i < aprConfig.geneticConfig().populationSize(); i++) {
            List<Edit> edits = new ArrayList<>();
            for (ModificationPoint modificationPoint : shuffledModificationPoints) {
                edits.add(
                        new Edit(
                                PseudoRandom.bool(modificationPoint.weight() * aprConfig.mu()),
                                ManipulationName.pickRandomWeighted(modificationPoint.allowedManipulations()),
                                modificationPoint,
                                PseudoRandom.pick(modificationPoint.redundancyIngredients())
                        )
                );
            }
            variants.add(Variant.create(edits));
        }
        return new Population(aprConfig, fitness, patchManager, variants, mutationProbability);
    }

    public PopulationResult evolve() throws AprException {
        long startTime = System.currentTimeMillis();

        logger.info("Evaluating initial population...");
        evaluateVariants();

        List<GenerationStatistics> generationStatistics = new ArrayList<>();
        generationStatistics.add(createGenerationStatistics(variants, System.currentTimeMillis() - startTime));

        int nrGenerations = geneticConfig.nrGenerations();
        while (generation < nrGenerations) {
            generation++;

            if (System.currentTimeMillis() - startTime >= ((long) geneticConfig.timeLimitSeconds() * 1000)) {
                logger.info("Genetic search execution time limit of %s seconds exceeded, exiting".formatted(geneticConfig.timeLimitSeconds()));
                break;
            }

            GenerationStatistics statistics = runGeneration();
            generationStatistics.add(statistics);

            // Exit if a test-adequate variants is found
            Variant fittestVariant = getFittestVariant();
            FitnessResult fittestVariantResult = evaluation.get(fittestVariant);
            if (!foundSolution && fittestVariantResult.isTestAdequate()) {
                foundSolution = true;
                logger.info(">>> Found test-adequate variants with scores %s after %d generations.".formatted(fittestVariantResult.scoresString(), generation));

                if (earlyExit) {
                    if (geneticConfig.nrAdditionalGenerations() != 0) {
                        logger.info("Doing %s additional generation(s) to potentially improve the variants, then exiting...".formatted(geneticConfig.nrAdditionalGenerations()));
                    }
                    nrGenerations = Math.min(geneticConfig.nrGenerations(), generation + geneticConfig.nrAdditionalGenerations());
                }
            }

            logger.info("Best fitness at end of generation %d is %s".formatted(generation, fittestVariantResult.scoresString()));
            String logMessage = "The population contains %s (%s unique) variants".formatted(variants.size(), Variant.unique(variants).size());
            List<Variant> testAdequateVariants = getTestAdequateVariants();
            if (!testAdequateVariants.isEmpty()) {
                logMessage += ", of which %s (%s unique) are test adequate".formatted(testAdequateVariants.size(), Variant.unique(testAdequateVariants).size());
            }
            logger.info(logMessage);
        }

        if (!foundSolution) {
            return new PopulationResult(List.of(), allPatchIds.size(), generationStatistics);
        }

        return new PopulationResult(Variant.unique(getTestAdequateVariants()), allPatchIds.size(), generationStatistics);
    }

    private GenerationStatistics runGeneration() throws AprException {
        long startTime = System.currentTimeMillis();

        for (Variant variant : variants) {
            allPatchIds.add(variant.patchId());
        }

        logger.info("");
        logger.info(">>> Running generation " + generation);

        logger.info("Creating new variants");
        createNewVariants();
        fixInvalidVariantEdits();
        List<Variant> allVariants = new ArrayList<>(variants);

        logger.info("Evaluating population");
        evaluateVariants();

        logger.info("Pruning population");
        prune();

        long endTime = System.currentTimeMillis();
        return createGenerationStatistics(allVariants, endTime - startTime);
    }

    private GenerationStatistics createGenerationStatistics(List<Variant> allVariants, long duration) {
        int nrEvaluatedVariants = allVariants.size();
        int nrUniqueEvaluatedVariants = Variant.unique(allVariants).size();
        int nrNewVariants = 0;
        for (Variant variant : allVariants) {
            if (!allPatchIds.contains(variant.patchId())) {
                nrNewVariants++;
                allPatchIds.add(variant.patchId());
            }
        }

        List<Variant> uniqueVariants = Variant.unique(variants);
        return new GenerationStatistics(
                generation,
                nrEvaluatedVariants,
                nrUniqueEvaluatedVariants,
                nrNewVariants,
                variants.size(),
                uniqueVariants.size(),
                (int) variants.stream().filter(v -> evaluation.get(v).isTestAdequate()).count(),
                (int) uniqueVariants.stream().filter(v -> evaluation.get(v).isTestAdequate()).count(),
                evaluation.get(getFittestVariant()).asScoresList(),
                duration
        );
    }

    private void createNewVariants() throws AprException {
        List<Variant> result = new ArrayList<>(variants);
        TournamentSelection selection = createSelection();

        for (int i = 0; i < size / 2; i++) {
            Variant parent1 = selection.selectOne();
            Variant parent2 = selection.selectOne();
            Pair<Variant, Variant> offspring = crossoverFactory.create().doCrossover(parent1, parent2);

            Variant offspring1 = mutationFactory.createRandomMutation().apply(offspring.getLeft());
            Variant offspring2 = mutationFactory.createRandomMutation().apply(offspring.getRight());

            result.add(offspring1);
            result.add(offspring2);
        }

        variants = result;
    }

    private void evaluateVariants() throws AprException {
        Map<Variant, Patch> variantPatches = new HashMap<>();
        for (Variant variant : variants) {
            variantPatches.put(variant, patchManager.createPatch(variant));
        }

        Map<Patch, FitnessResult> results = fitness.evaluate(new ArrayList<>(new HashSet<>(variantPatches.values())));

        evaluation = new HashMap<>();
        for (Variant variant : variants) {
            evaluation.put(variant, results.get(variantPatches.get(variant)));
        }
    }

    private void prune() throws AprException {
        TournamentSelection selection = createSelection();
        List<Variant> sortedVariants = PseudoRandom.shuffle(variants).stream().sorted(selection::compareVariants).toList();
        List<Variant> elites = sortedVariants.subList(sortedVariants.size() - geneticConfig.eliteCount(), sortedVariants.size());
        List<Variant> remainingVariants = new ArrayList<>(variants);
        remainingVariants.removeAll(elites);
        variants = selection.select(remainingVariants, size - elites.size());
        variants.addAll(elites);
    }

    private void fixInvalidVariantEdits() {
        for (Variant variant : variants) {
            for (int i = 0; i < variant.edits().size(); i++) {
                Edit edit = variant.edits().get(i);
                if (!edit.enabled()) {
                    continue;
                }

                ModificationPoint modificationPoint = edit.modificationPoint();
                Ingredient ingredient = edit.ingredient();
                ManipulationName manipulation = edit.manipulation();

                if (!ingredient.isRedundancyIngredient()) {
                    // Only check and fix redundancy ingredients, not plm ingredients
                    continue;
                }

                if (ModificationScreener.screen(modificationPoint.statement(), ingredient, manipulation)) {
                    continue;
                }

                // Edit is not valid. See if any other ingredient suffices and replace it if so.
                List<Ingredient> validIngredients = modificationPoint.redundancyIngredients().stream().filter(
                        x -> ModificationScreener.screen(
                                modificationPoint.statement(),
                                x,
                                manipulation
                        )
                ).toList();

                if (validIngredients.isEmpty()) {
                    // No valid alternative so we disable this edit
                    edit = edit.withEnabled(false);
                } else {
                    // Pick a random valid ingredient to replace the invalid one with
                    edit = edit.withIngredient(PseudoRandom.pick(validIngredients));
                }

                variant.edits().set(i, edit);
            }
        }
    }

    private TournamentSelection createSelection() {
        return new TournamentSelection(variants, evaluation);
    }

    private Variant getFittestVariant() {
        float bestTestSuiteFitness = variants.stream().map(v -> evaluation.get(v).testSuiteFitness()).sorted().findFirst().orElseThrow();
        return variants.stream()
                .filter(v -> evaluation.get(v).testSuiteFitness() == bestTestSuiteFitness)
                .min(Comparator.comparing(v -> evaluation.get(v).patchSizeFitness()))
                .orElseThrow();
    }

    private List<Variant> getTestAdequateVariants() {
        return variants.stream().filter(v -> evaluation.get(v).isTestAdequate()).toList();
    }
}
