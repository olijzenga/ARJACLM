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
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.*;
import org.eclipse.jdt.core.dom.Statement;

import java.util.List;
import java.util.Optional;

/**
 * Implements an ingredient filtering rule which can by applied by only knowing the statement and its ingredient.
 */
public abstract class AbstractIngredientScreener {

    protected final IngredientScreeningInfo ingredientScreeningInfo;

    public AbstractIngredientScreener(IngredientScreeningInfo ingredientScreeningInfo) {
        this.ingredientScreeningInfo = ingredientScreeningInfo;
    }

    public abstract boolean screen(Statement statement, Statement ingredient) throws AprException;

    /**
     * Checks if a variable reference would be valid at the provided location, and returns the declared symbol if so
     *
     * @param referencedSymbol the variable (or method) reference
     * @param statement        location where the variable reference would take place
     * @return The declared symbol matching the reference
     */
    protected Optional<DeclaredSymbol> resolveSymbol(ReferencedSymbol referencedSymbol, Statement statement) throws AprException {
        List<DeclaredSymbol> declaredSymbols = ingredientScreeningInfo.getDeclaredSymbols(statement);
        for (DeclaredSymbol declaredSymbol : declaredSymbols) {
            if (symbolReferenceMatches(referencedSymbol, declaredSymbol, statement)) {
                return Optional.of(declaredSymbol);
            }
        }
        return Optional.empty();
    }

    private boolean localSymbolReferenceMatches(ReferencedSymbol referencedSymbol, DeclaredSymbol declaredSymbol, Statement statement) throws AprException {
        DeclaredClass cls = ingredientScreeningInfo.getStatementClassDeclaration(statement);
        DeclaredMethod containingMethod = ingredientScreeningInfo.getStatementMethodDeclaration(statement);

        // check name
        if (!referencedSymbol.getOriginalReference().equals(declaredSymbol.getShortName())) {
            return false;
        }
        // check static from/to non-static conflicts
        if (!(!containingMethod.isStatic() || declaredSymbol.isStatic() || declaredSymbol.isLocal())) {
            return false;
        }
        // check visibility
        if(!(declaredSymbol.getVisibility() == SymbolVisibility.LOCAL || (declaredSymbol.isVisibleFromClass(cls.getTypeBinding()) && declaredSymbol.isFieldOf(cls.getTypeBinding())))) {
            return false;
        }

        return true;
    }

    private boolean qualifiedSymbolReferenceMatches(ReferencedSymbol referencedSymbol, DeclaredSymbol declaredSymbol) {
        return (declaredSymbol.isStatic() || (declaredSymbol instanceof DeclaredMethod m && m.isConstructor()))
                && referencedSymbol.getQualifiedName().equals(declaredSymbol.getQualifiedName())
                && referencedSymbol.isStaticQualifiedReferenceTo(declaredSymbol);
    }


    private boolean symbolReferenceMatches(ReferencedSymbol referencedSymbol, DeclaredSymbol declaredSymbol, Statement statement) throws AprException {
        if (referencedSymbol instanceof ReferencedMethod && declaredSymbol instanceof DeclaredVariable) {
            return false;
        }
        if (referencedSymbol instanceof ReferencedVariable && declaredSymbol instanceof DeclaredMethod) {
            // Note: a side-effect of this is that we cannot match method references that do not call the method,
            // but this check makes ingredient screening 3-4x faster which is nice for development
            return false;
        }

        if (declaredSymbol instanceof DeclaredClass) {
            return referencedSymbol.getQualifiedName().equals(declaredSymbol.getQualifiedName());
        }

        if (localSymbolReferenceMatches(referencedSymbol, declaredSymbol, statement) || qualifiedSymbolReferenceMatches(referencedSymbol, declaredSymbol)) {
            // Straight forward matching succeeded so proceed checking compatibility of the resolved symbols.
            return matchedSymbolsAreCompatible(referencedSymbol, declaredSymbol, statement);
        }

        // The only remaining option is the reference being a nested property reference (e.g. obj.x). Try matching by resolving its parent.
        if (QualifiedName.fromString(referencedSymbol.getOriginalReference()).elements().size() < 2) {
            return false;
        }

        // For now assume we are still referencing the same type in the new context, this makes it so that we don't need to do a ton more work
        if (referencedSymbol.getQualifiedName().equals(declaredSymbol.getQualifiedName())) {
            return matchedSymbolsAreCompatible(referencedSymbol, declaredSymbol, statement);
        }

        return false;
    }

    private boolean matchedSymbolsAreCompatible(ReferencedSymbol referencedSymbol, DeclaredSymbol declaredSymbol, Statement statement) throws AprException {
        if (referencedSymbol instanceof ReferencedVariable) {
            return declaredSymbol instanceof DeclaredVariable;
        }

        ReferencedMethod referencedMethod = (ReferencedMethod) referencedSymbol;
        if (!(declaredSymbol instanceof DeclaredMethod declaredMethod)) {
            // Cannot resolve reference to variable
            return false;
        }

        if (referencedMethod.getArguments().size() != declaredMethod.parameters().size()) {
            return false;
        }

        for (int i = 0; i < referencedMethod.getArguments().size(); i++) {
            Optional<ReferencedSymbol> optionalReferencedArgument = referencedMethod.getArguments().get(i);
            DeclaredSymbol declaredParameter = declaredMethod.parameters().get(i);

            if (optionalReferencedArgument.isEmpty()) {
                // Cannot check this so skip it
                continue;
            }
            ReferencedSymbol referencedArgument = optionalReferencedArgument.get();

            Optional<DeclaredSymbol> declaredArgument = resolveSymbol(referencedArgument, statement);
            if (declaredArgument.isEmpty()) {
                return false;
            }

            // Check return type / variable type
            if (!IngredientUtil.typesWeaklyMatch(declaredArgument.get().getOutputTypeBinding(), declaredParameter.getOutputTypeBinding())) {
                return false;
            }
        }

        return true;
    }
}
