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

package nl.oebelelijzenga.arjaclm.model.apr.genetic;

import nl.oebelelijzenga.arjaclm.model.apr.ModificationPoint;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import nl.oebelelijzenga.arjaclm.parser.manipulation.ManipulationName;
import org.eclipse.jdt.core.dom.Statement;

public record Edit(
        boolean enabled,
        ManipulationName manipulation,
        ModificationPoint modificationPoint,
        Ingredient ingredient
) {
    public Edit copy() {
        return new Edit(
                enabled,
                manipulation,
                modificationPoint,
                ingredient
        );
    }

    public Edit withManipulation(ManipulationName manipulation) {
        return new Edit(enabled, manipulation, modificationPoint, ingredient);
    }

    public Edit withEnabled(boolean enabled) {
        return new Edit(enabled, manipulation, modificationPoint, ingredient);
    }

    public Edit withIngredient(Ingredient ingredient) {
        return new Edit(enabled, manipulation, modificationPoint, ingredient);
    }

    @Override
    public String toString() {
        return String.format(
                "Edit[e=%s, manip=\"%s\", modpoint=%s: \"%s\", plmIngr=%s, ingr=%s]",
                enabled ? "t" : "f",
                manipulation,
                modificationPoint.index(),
                ASTUtil.statementToSingleLine(modificationPoint.statement()),
                ingredient.isRedundancyIngredient() ? "f" : "t",
                ingredient
        );
    }

    public String toFileString() {
        return String.format(
                "Edit[\n\tenabled=%s,\n\tmanipulation=\"%s\",\n\tmodpoint=\"%s: %s\",\n\tplmIngr=%s,\n\tingredient=\n%s]",
                enabled ? "t" : "f",
                manipulation,
                modificationPoint.index(),
                ASTUtil.statementToSingleLine(modificationPoint.statement()),
                ingredient.isRedundancyIngredient() ? "f" : "t",
                ingredient.toFileString().indent(8)
        );
    }

    public int cacheHashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + manipulation.toString().hashCode();
        result = 31 * result + modificationPoint.index();

        String ingredientString;
        if (manipulation == ManipulationName.DELETE) {
            ingredientString = "";  // Ignore ingredients in hashcode for DELETE since it does not matter in this case
        } else {
            ingredientString = String.join("", ingredient.statements().stream().map(Statement::toString).toList());
        }
        result = 31 * result + ingredientString.hashCode();
        return result;
    }
}
