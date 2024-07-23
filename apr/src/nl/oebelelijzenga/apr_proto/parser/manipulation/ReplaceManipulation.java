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

package nl.oebelelijzenga.apr_proto.parser.manipulation;

import nl.oebelelijzenga.apr_proto.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.Collections;
import java.util.List;

public class ReplaceManipulation extends AbstractManipulation {

    public ReplaceManipulation(Statement target, Ingredient ingredient, ASTRewrite rewriter) {
        super(target, ingredient, rewriter);
    }

    @Override
    public boolean manipulate() {
        List<Statement> ingredientCopies = ASTUtil.copyStatements(target.getAST(), ingredient.statements());

        if (target instanceof IfStatement statementIf && ingredient.statements().size() == 1 && ingredient.statements().get(0) instanceof IfStatement ingredientIf) {
            // Only replace the if-condition when replacing an if-statement with another
            Expression conditionCopy = (Expression) ASTNode.copySubtree(rewriter.getAST(), ingredientIf.getExpression());
            rewriter.replace(statementIf.getExpression(), conditionCopy, null);
            return true;
        }

        if (target.getParent() instanceof Block block) {
            ListRewrite lrw = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);

            // Reverse ingredients so that they are added in the right order into the AST
            Collections.reverse(ingredientCopies);
            for (Statement ingredientCopy : ingredientCopies) {
                lrw.insertAfter(ingredientCopy, target, null);
            }

            lrw.remove(target, null);
        } else {
            Block newBlock = target.getAST().newBlock();

            for (Statement ingredientCopy : ingredientCopies) {
                newBlock.statements().add(ingredientCopy);
            }

            rewriter.replace(target, newBlock, null);
        }

        return true;
    }
}
