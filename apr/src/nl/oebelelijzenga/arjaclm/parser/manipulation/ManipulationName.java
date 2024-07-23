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

package nl.oebelelijzenga.arjaclm.parser.manipulation;

import nl.oebelelijzenga.arjaclm.genetic.PseudoRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public enum ManipulationName {
    DELETE,
    REPLACE,
    INSERT_AFTER,
    INSERT_BEFORE;


    public static List<ManipulationName> defaultEnabledManipulations() {
        // Leave out INSERT_AFTER since ARJA does this as well
        return List.of(ManipulationName.DELETE, ManipulationName.REPLACE, ManipulationName.INSERT_BEFORE);
    }

    /*
     * Picks a random manipulation from the list of provided options. DELETE is less likely to be selected
     * as it does not use patch ingredients and therefore often results in duplicate patches.
     */
    public static ManipulationName pickRandomWeighted(List<ManipulationName> options) {
        List<Pair<Float, ManipulationName>> weightedOptions = new ArrayList<>();
        for (ManipulationName option : options) {
            float weight = option == DELETE ? 0.4f : 1.0f;
            weightedOptions.add(Pair.of(weight, option));
        }
        return PseudoRandom.pickWeighted(weightedOptions);
    }
}
