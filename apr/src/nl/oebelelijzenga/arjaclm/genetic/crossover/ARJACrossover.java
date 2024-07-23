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

package nl.oebelelijzenga.arjaclm.genetic.crossover;

import nl.oebelelijzenga.arjaclm.genetic.PseudoRandom;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Edit;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ARJACrossover implements ICrossover {

    @Override
    public Pair<Variant, Variant> doCrossover(Variant parent1, Variant parent2) {
        int size = parent1.edits().size();

        List<Edit> child1Edits = new ArrayList<>();
        List<Edit> child2Edits = new ArrayList<>();

        int manipulationCrossoverPoint;
        int ingredientCrossoverPoint;
        if (size == 1) {
            // This is needed since otherwise the crossover point is always zero and both the manipulation and
            // ingredient get flipped which is not necessarily useful.
            manipulationCrossoverPoint = PseudoRandom.intRange(0, 1);
            ingredientCrossoverPoint = manipulationCrossoverPoint == 1 ? 0 : 1;
        } else {
            manipulationCrossoverPoint = PseudoRandom.intRangeExclusive(0, size);
            ingredientCrossoverPoint = PseudoRandom.intRangeExclusive(0, size);
        }

        for (int i = 0; i < size; i++) {
            Edit parent1Edit = parent1.edits().get(i);
            Edit parent2Edit = parent2.edits().get(i);
            Edit child1Edit = parent1Edit.copy();
            Edit child2Edit = parent2Edit.copy();

            if (i >= manipulationCrossoverPoint) {
                child1Edit = child1Edit.withManipulation(parent2Edit.manipulation());
                child2Edit = child2Edit.withManipulation(parent1Edit.manipulation());
            }

            if (i >= ingredientCrossoverPoint) {
                child1Edit = child1Edit.withIngredient(parent2Edit.ingredient());
                child2Edit = child2Edit.withIngredient(parent1Edit.ingredient());
            }

            if (parent1Edit.enabled() != parent2Edit.enabled()) {
                if (PseudoRandom.coinflip()) {
                    child1Edit = child1Edit.withEnabled(parent2Edit.enabled());
                    child2Edit = child2Edit.withEnabled(parent1Edit.enabled());
                }
            }

            child1Edits.add(child1Edit);
            child2Edits.add(child2Edit);
        }

        return ImmutablePair.of(
                parent1.withEdits(child1Edits, ARJACrossover.class),
                parent2.withEdits(child2Edits, ARJACrossover.class)
        );
    }
}
