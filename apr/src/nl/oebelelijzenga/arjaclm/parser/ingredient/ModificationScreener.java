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

package nl.oebelelijzenga.arjaclm.parser.ingredient;

import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import nl.oebelelijzenga.arjaclm.parser.manipulation.ManipulationName;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Checks whether specific combinations of statement, ingredient and modification are valid according to some filtering rules.
 */
public class ModificationScreener {

    public static boolean screen(Statement statement, Ingredient ingredient, ManipulationName manipulation) {
        return screen(statement, ingredient.statements().get(0), manipulation);
    }

    public static boolean screen(Statement statement, Statement ingredient, ManipulationName manipulation) {
        boolean r1 = rule1(statement, ingredient, manipulation);
        boolean r2 = rule2(statement, ingredient, manipulation);
        boolean r3 = rule3(statement, ingredient, manipulation);
        boolean r4 = rule4(statement, ingredient, manipulation);
        boolean r5 = rule5(statement, ingredient, manipulation);
        boolean r6 = rule6(statement, ingredient, manipulation);
        return r1 && r2 && r3 && r4 && r5 && r6;
    }

    /*
     * Do not replace a statement with the one having the same AST.
     */
    private static boolean rule1(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!manipulation.equals(ManipulationName.REPLACE)) {
            return true;
        }

        return !ASTUtil.subTreeMatch(ingredient, statement);
    }

    /*
     * Do not replace a VDS with the other kinds of statements.
     */
    private static boolean rule2(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!(manipulation.equals(ManipulationName.REPLACE) && statement instanceof VariableDeclarationStatement)) {
            return true;
        }

        return ingredient instanceof VariableDeclarationStatement;
    }

    /*
     * Do not insert a VDS before a VDS.
     *
     * Note that we actually also do not allow inserting a VDS after another
     */
    private static boolean rule3(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!(manipulation.equals(ManipulationName.INSERT_BEFORE) || manipulation.equals(ManipulationName.INSERT_AFTER))) {
            return true;
        }

        if (!(statement instanceof VariableDeclarationStatement)) {
            return true;
        }

        return !(ingredient instanceof VariableDeclarationStatement);
    }

    /*
     * Do not insert a return/throw statement before any statement.
     */
    private static boolean rule4(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!(manipulation.equals(ManipulationName.INSERT_BEFORE))) {
            return true;
        }

        return !IngredientUtil.willReturnOrThrow(ingredient);
    }

    /*
     * Do not replace a return statement (with return value) that is the last statement of a method with the other kinds
     * of statements.
     *
     * Note that there is actually no check for whether it is an empty return. Furthermore, it also checks for throw statements
     * besides returns. This is also how ARJA implements it.
     */
    private static boolean rule5(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!(
                manipulation.equals(ManipulationName.REPLACE)
                        && IngredientUtil.willReturnOrThrow(statement)
                        && IngredientUtil.statementIsLastInMethod(statement)
        )) {
            return true;
        }

        return IngredientUtil.willReturnOrThrow(ingredient);
    }

    /*
     * Do not insert an assignment statement before an assignment statement with the same left-hand side.
     */
    private static boolean rule6(Statement statement, Statement ingredient, ManipulationName manipulation) {
        if (!manipulation.equals(ManipulationName.INSERT_BEFORE)) {
            return true;
        }

        if (!(statement instanceof ExpressionStatement statementExpression)) {
            return true;
        }

        if (!(statementExpression.getExpression() instanceof Assignment statementAssignment)) {
            return true;
        }

        if (!(ingredient instanceof ExpressionStatement ingredientExpression)) {
            return true;
        }

        if (!(ingredientExpression.getExpression() instanceof Assignment ingredientAssignment)) {
            return true;
        }

        return !ASTUtil.subTreeMatch(statementAssignment.getLeftHandSide(), ingredientAssignment.getLeftHandSide());
    }
}
