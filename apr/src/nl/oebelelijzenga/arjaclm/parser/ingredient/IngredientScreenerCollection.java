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

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.IngredientScreeningInfo;
import nl.oebelelijzenga.arjaclm.model.io.AprConfig;
import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.Statement;

import java.util.List;

public class IngredientScreenerCollection {

    private static final Logger logger = LogManager.getLogger(IngredientScreenerCollection.class);

    public static boolean screen(Statement statement, Statement ingredient, AprConfig aprConfig, IngredientScreeningInfo ingredientScreeningInfo) throws AprException {
        for (AbstractIngredientScreener screener : createIngredientScreeners(aprConfig, ingredientScreeningInfo)) {
            if (!screener.screen(statement, ingredient)) {
                logger.debug(
                        "%s rejected ingredient \"%s\" for statement \"%s\"".formatted(
                                screener.getClass().getSimpleName(),
                                ASTUtil.statementToSingleLine(ingredient),
                                ASTUtil.statementToSingleLine(statement)
                        )
                );
                return false;
            }
        }
        return true;
    }

    private static List<AbstractIngredientScreener> createIngredientScreeners(AprConfig input, IngredientScreeningInfo ingredientScreeningInfo) {
        return List.of(
                new ArjaRulesIngredientScreener(ingredientScreeningInfo),
                new ReferencedVariableIngredientScreener(ingredientScreeningInfo),
                new ReferencedMethodIngredientScreener(ingredientScreeningInfo)
        );
    }
}
