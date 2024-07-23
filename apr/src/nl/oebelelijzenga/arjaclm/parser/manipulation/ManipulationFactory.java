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

import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class ManipulationFactory {
    public static AbstractManipulation getManipulation(ManipulationName manipulation, Statement target, Ingredient ingredient,
                                                       ASTRewrite rewriter) {
        switch (manipulation) {
            case DELETE -> {
                return new DeleteManipulation(target, ingredient, rewriter);
            }
            case REPLACE -> {
                return new ReplaceManipulation(target, ingredient, rewriter);
            }
            case INSERT_BEFORE -> {
                return new InsertBeforeManipulation(target, ingredient, rewriter);
            }
            case INSERT_AFTER -> {
                return new InsertAfterManipulation(target, ingredient, rewriter);
            }
            default -> throw new IllegalArgumentException(manipulation.toString());
        }
    }
}
