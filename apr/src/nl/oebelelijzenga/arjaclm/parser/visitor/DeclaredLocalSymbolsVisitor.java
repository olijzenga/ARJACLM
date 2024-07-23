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

package nl.oebelelijzenga.arjaclm.parser.visitor;

import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.*;
import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class DeclaredLocalSymbolsVisitor extends StatementVisitor {

    private final Set<Statement> statementsToIndex;
    private final Map<Statement, List<DeclaredSymbol>> statementSymbols = new HashMap<>();
    private final Stack<List<DeclaredSymbol>> symbolScopeStack = new Stack<>();

    public DeclaredLocalSymbolsVisitor(Set<Statement> statementsToIndex) {
        this.statementsToIndex = statementsToIndex;
    }

    public static DeclaredLocalSymbols getLocalSymbols(List<Statement> statements) {
        Set<Statement> statementSet = new HashSet<>(statements);
        DeclaredLocalSymbolsVisitor visitor = new DeclaredLocalSymbolsVisitor(statementSet);

        // Visit the method declaration of each statement to check for local variables.
        Set<MethodDeclaration> visitedMethodDeclarations = new HashSet<>();
        for (Statement statement : statements) {
            MethodDeclaration methodDeclaration = ASTUtil.getParentOfType(statement, MethodDeclaration.class).orElseThrow();

            if (visitedMethodDeclarations.contains(methodDeclaration)) {
                continue;
            }

            methodDeclaration.accept(visitor);
            visitedMethodDeclarations.add(methodDeclaration);
        }

        return new DeclaredLocalSymbols(visitor.statementSymbols);
    }

    private List<DeclaredSymbol> currentScope() {
        return symbolScopeStack.peek();
    }

    private void openScope() {
        symbolScopeStack.push(new ArrayList<>());
    }

    private void closeScope() {
        symbolScopeStack.pop();
    }

    @Override
    boolean visitStatement(Statement statement) {
        if (!statementsToIndex.contains(statement)) {
            return true;
        }

        List<DeclaredSymbol> declaredSymbols = new ArrayList<>();
        for (List<DeclaredSymbol> scope : symbolScopeStack) {
            declaredSymbols.addAll(scope);
        }
        statementSymbols.put(statement, declaredSymbols);

        return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        IVariableBinding binding = node.resolveBinding();
        if (binding == null) {
            return false;
        }

        if (!(node.getParent() instanceof FieldDeclaration)) {
            // Local variable
            currentScope().add(
                    new DeclaredVariable(
                            QualifiedName.fromString(binding.getName()),
                            SymbolVisibility.LOCAL,
                            false,
                            binding
                    )
            );
        }

        return false;
    }

    @Override
    public boolean visit(Block node) {
        openScope();
        return true;
    }

    @Override
    public void endVisit(Block node) {
        closeScope();
    }
}
