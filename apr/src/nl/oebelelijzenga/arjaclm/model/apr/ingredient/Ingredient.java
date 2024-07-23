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

package nl.oebelelijzenga.arjaclm.model.apr.ingredient;

import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.eclipse.jdt.core.dom.Statement;

import java.util.List;

public class Ingredient {
    private final List<Statement> statements;
    private final boolean isRedundancyIngredient;

    public Ingredient(List<Statement> statements, boolean isRedundancyIngredient) {
        this.statements = statements;
        this.isRedundancyIngredient = isRedundancyIngredient;
    }

    public Ingredient(Statement statement, boolean isRedundancyIngredient) {
        this(List.of(statement), isRedundancyIngredient);
    }

    public List<Statement> statements() {
        return statements;
    }

    public boolean isRedundancyIngredient() {
        return isRedundancyIngredient;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ingredient ingredient)) {
            return false;
        }
        return statements.stream().map(Statement::toString).toList().equals(ingredient.statements.stream().map(Statement::toString).toList());
    }

    @Override
    public int hashCode() {
        return statements.hashCode();
    }

    @Override
    public String toString() {
        String result = getClass().getSimpleName().replace("Ingredient", "") + "[\"";
        for (Statement statement : statements) {
            result += ASTUtil.statementToSingleLine(statement) + " ";
        }
        result += "\"]";
        return result;
    }

    public String toFileString() {
        String result = "%s[\n".formatted(getClass().getSimpleName());
        for (Statement statement : statements) {
            result += "\t" + ASTUtil.statementToSingleLine(statement) + "\n";
        }
        result += "]\n";
        return result;
    }
}
