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

package nl.oebelelijzenga.apr_proto.model.apr;

import nl.oebelelijzenga.apr_proto.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;
import org.eclipse.jdt.core.dom.Statement;

import java.util.List;

public record ModificationPoint(
        int index,
        Statement statement,
        ParsedJavaFile sourceFile,
        JavaClass cls,
        // Suspicion value for this statement, 0..1, higher is more sus
        float weight,
        // Patch ingredients obtained from the redundancy assumption
        List<Ingredient> redundancyIngredients,
        List<ManipulationName> allowedManipulations
) {
    @Override
    public String toString() {
        return String.format(
                "ModificationPoint[i=%s, statement='%s', location=%s, weight=%s, num_ingredients=%s, manipulations=%s]",
                index,
                ASTUtil.statementToSingleLine(statement),
                "%s:%s".formatted(cls.getFullName(), cls.compilationUnit().getLineNumber(statement.getStartPosition())),
                weight,
                redundancyIngredients.size(),
                allowedManipulations
        );
    }
}
