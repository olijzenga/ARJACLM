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
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.DeclaredSymbol;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.IngredientScreeningInfo;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedMethod;
import org.eclipse.jdt.core.dom.Statement;

import java.util.Optional;

/**
 * Filters out ingredients with conflicting method references
 */
public class ReferencedMethodIngredientScreener extends AbstractIngredientScreener {
    public ReferencedMethodIngredientScreener(IngredientScreeningInfo ingredientScreeningInfo) {
        super(ingredientScreeningInfo);
    }

    @Override
    public boolean screen(Statement statement, Statement ingredient) throws AprException {
        for (ReferencedMethod referencedMethod : ingredientScreeningInfo.getReferencedMethods(ingredient)) {
            if (!referencedMethod.getQualifiedName().startsWith(ingredientScreeningInfo.getTopLeveLPackage())) {
                // We can only screen references to symbols declared in this package. For all external symbols we assume
                // that they are correct.
                continue;
            }

            Optional<DeclaredSymbol> resolveResult = resolveSymbol(referencedMethod, statement);
            if (resolveResult.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
