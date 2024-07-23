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

package nl.oebelelijzenga.apr_proto.parser.ingredient;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.DeclaredMethod;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.IngredientScreeningInfo;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArjaRulesIngredientScreener extends AbstractIngredientScreener {

    public ArjaRulesIngredientScreener(IngredientScreeningInfo ingredientScreeningInfo) {
        super(ingredientScreeningInfo);
    }

    /**
     * Implements ingredient filter rules from table 2 of the ARJA paper. Note that all rule methods return true if
     * the rule matches, and false if it is violated.
     */
    @Override
    public boolean screen(Statement statement, Statement ingredient) throws AprException {
        return rule1(statement, ingredient)
                && rule2(statement, ingredient)
                && rule3(statement, ingredient)
                && rule4(statement, ingredient)
                && rule5(statement, ingredient)
                && rule6(statement, ingredient);
    }

    public boolean rule1(Statement statement, Statement ingredient) {
        // The continue statement can be used as the ingredient only for a likely-buggy statement in the loop.
        if (!(ingredient instanceof ContinueStatement)) {
            return true;
        }

        return IngredientUtil.statementIsInLoop(statement);
    }

    public boolean rule2(Statement statement, Statement ingredient) {
        // The break statement can be used as the ingredient only for a likely-buggy statement in the loop or in the switch block.
        if (!(ingredient instanceof BreakStatement)) {
            return true;
        }

        return IngredientUtil.statementIsInLoop(statement) || IngredientUtil.statementIsInSwitchCase(statement);
    }

    public boolean rule3(Statement statement, Statement ingredient) throws AprException {
        // A case statement can be used as the ingredient only for a likely-buggy statement in a switch block having the same enumerated type.
        if (!(ingredient instanceof SwitchCase)) {
            return true;
        }


        Optional<SwitchStatement> statementSwitchStatement = ASTUtil.getParentOfType(statement, SwitchStatement.class);
        if (statementSwitchStatement.isEmpty()) {
            return false;
        }

        ITypeBinding ingredientSwitchType = IngredientUtil.getSwitchType(ASTUtil.getParentOfType(ingredient, SwitchStatement.class).orElseThrow());
        ITypeBinding statementSwitchType = IngredientUtil.getSwitchType(statementSwitchStatement.get());

        return IngredientUtil.typesStronglyMatch(ingredientSwitchType, statementSwitchType);
    }

    public boolean rule4(Statement statement, Statement ingredient) throws AprException {
        // A return/ throw statement can be used as the ingredient only for a likely-buggy statement in a method declaring the compatible return/throw type.
        DeclaredMethod method = ingredientScreeningInfo.getStatementMethodDeclaration(statement);

        if (ingredient instanceof ThrowStatement throwIngredient) {
            List<ITypeBinding> throwTypes = IngredientUtil.getMethodThrowsTypes(method.methodDeclaration());
            ITypeBinding thrownType = IngredientUtil.getThrowExceptionType(throwIngredient);

            // subclasses of RuntimeException and Error do not require explicit 'throws' declaration
            if (ASTUtil.instanceOf(thrownType, QualifiedName.fromString(RuntimeException.class.getName()))) {
                return true;
            }
            if (ASTUtil.instanceOf(thrownType, QualifiedName.fromString(Error.class.getName()))) {
                return true;
            }

            return throwTypes.stream().anyMatch(t -> IngredientUtil.typesWeaklyMatch(thrownType, t));
        }

        if (ingredient instanceof ReturnStatement returnIngredient) {
            Optional<ITypeBinding> returnedType = IngredientUtil.getReturnStatementType(returnIngredient);
            String methodReturnType = method.returnTypeBinding().getQualifiedName();
            if (returnedType.isEmpty() || methodReturnType.equals("void")) {
                return returnedType.isEmpty() && methodReturnType.equals("void");
            }

            return IngredientUtil.typesWeaklyMatch(returnedType.get(), method.returnTypeBinding());
        }

        return true;
    }

    public boolean rule5(Statement statement, Statement ingredient) {
        // A return/ throw statement can be used as the ingredient only for a likely-buggy statement that is the last statement of a block.
        if (!(ingredient instanceof ThrowStatement || ingredient instanceof ReturnStatement)) {
            return true;
        }

        return IngredientUtil.statementIsLastInBlock(statement);
    }

    public boolean rule6(Statement statement, Statement ingredient) {
        // A VDS can be used as the ingredient only for another VDS having the compatible declared type and the same variable names.
        if (!(ingredient instanceof VariableDeclarationStatement declarationIngredient)) {
            return true;
        }

        if (!(statement instanceof VariableDeclarationStatement declarationStatement)) {
            return false;
        }

        Map<String, ITypeBinding> ingredientVariables = IngredientUtil.getDeclaredVariables(declarationIngredient);
        Map<String, ITypeBinding> statementVariables = IngredientUtil.getDeclaredVariables(declarationStatement);

        if (!ingredientVariables.keySet().equals(statementVariables.keySet())) {
            return false;
        }

        for (String statementVariable : statementVariables.keySet()) {
            if (!IngredientUtil.typesStronglyMatch(ingredientVariables.get(statementVariable), statementVariables.get(statementVariable))) {
                return false;
            }
        }

        return true;
    }
}
