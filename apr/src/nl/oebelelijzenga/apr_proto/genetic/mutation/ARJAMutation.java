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

package nl.oebelelijzenga.apr_proto.genetic.mutation;

import nl.oebelelijzenga.apr_proto.genetic.PseudoRandom;
import nl.oebelelijzenga.apr_proto.model.apr.ModificationPoint;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Edit;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;

import java.util.ArrayList;
import java.util.List;

public class ARJAMutation implements IMutation {

    private final float mutationProbability;

    public ARJAMutation(float mutationProbability) {
        this.mutationProbability = mutationProbability;
    }

    @Override
    public Variant apply(Variant variant) {
        long nrEnabledEdits = variant.edits().stream().filter(Edit::enabled).count();
        List<Edit> newEdits = new ArrayList<>();
        for (Edit edit : variant.edits()) {
            ModificationPoint modificationPoint = edit.modificationPoint();
            Edit newEdit = edit.copy();

            if (PseudoRandom.bool(mutationProbability * (edit.enabled() ? (nrEnabledEdits + 1) : 1.0f))) {
                newEdit = newEdit.withEnabled(!edit.enabled());
            }

            if (PseudoRandom.bool(mutationProbability)) {
                newEdit = newEdit.withManipulation(ManipulationName.pickRandomWeighted(modificationPoint.allowedManipulations()));
            }

            if (PseudoRandom.bool(mutationProbability)) {
                newEdit = newEdit.withIngredient(PseudoRandom.pick(modificationPoint.redundancyIngredients()));
            }

            newEdits.add(newEdit);
        }
        return variant.withEdits(newEdits, ARJAMutation.class);
    }
}
