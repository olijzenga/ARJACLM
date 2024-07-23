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

import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.ReferencedMethod;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.ReferencedSymbol;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.ReferencedSymbols;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.ReferencedVariable;
import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class ReferencedSymbolsVisitor extends StatementVisitor {
    private final Stack<ReferencedMethod> currentMethodInvocations = new Stack<>();
    private final Stack<Statement> currentStatements = new Stack<>();
    private final Map<Statement, List<ReferencedSymbol>> referencedSymbols = new HashMap<>();

    public static ReferencedSymbols getReferencedSymbols(List<Statement> statements) {
        ReferencedSymbolsVisitor visitor = new ReferencedSymbolsVisitor();
        for (Statement statement : statements) {
            statement.accept(visitor);
        }
        return new ReferencedSymbols(visitor.referencedSymbols);
    }

    public static List<ReferencedSymbol> getReferencedSymbols(Statement statement) {
        return getReferencedSymbols(List.of(statement)).get(statement);
    }

    private void addReferencedSymbol(ASTNode node, ReferencedSymbol referencedSymbol) {
        if (!currentMethodInvocations.isEmpty() && (node.getParent() instanceof MethodInvocation || node.getParent() instanceof ClassInstanceCreation)) {
            currentMethodInvocations.peek().getArguments().add(Optional.of(referencedSymbol));
        }

        for (Statement statement : currentStatements) {
            referencedSymbols.get(statement).add(referencedSymbol);
        }
    }

    @Override
    boolean visitStatement(Statement statement) {
        currentStatements.push(statement);
        referencedSymbols.put(statement, new ArrayList<>());
        return true;
    }

    @Override
    void endVisitStatement(Statement statement) {
        currentStatements.pop();
    }

    @Override
    public boolean visit(FieldAccess node) {
        IVariableBinding fieldBinding = node.resolveFieldBinding();

        String originalName = node.toString();
        if (originalName.startsWith("this.")) {
            originalName = originalName.substring(5);
        }

        if (fieldBinding.getDeclaringClass() == null && ASTUtil.isArrayLengthAccess(node)) {
            addReferencedSymbol(
                    node,
                    new ReferencedVariable(
                            originalName,
                            "array.length"
                    )
            );
            return false;
        }

        addReferencedSymbol(
                node,
                new ReferencedVariable(
                        originalName,
                        fieldBinding.getDeclaringClass().getQualifiedName() + "." + node.getName()
                )
        );

        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding binding = node.getName().resolveBinding();

        // If we have some_obj.x, then by doing this we visit the some_obj part, but not x since process that in this method
        node.getQualifier().accept(this);

        if (binding instanceof IVariableBinding variableBinding) {
            if (variableBinding.getDeclaringClass() == null) {
                return false;
            }

            addReferencedSymbol(
                    node,
                    new ReferencedVariable(
                            node.getFullyQualifiedName(),
                            variableBinding.getDeclaringClass().getQualifiedName() + "." + node.getName().toString()
                    )
            );
        }

        // Make it so that the SimpleName visitor does not need to deal with this
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();

        if (binding == null || node.isDeclaration() || binding instanceof IMethodBinding) {
            return super.visit(node);
        }

        if (binding instanceof IVariableBinding variableBinding) {
            String qualifiedName = variableBinding.getName();
            if (variableBinding.getDeclaringClass() != null) {
                qualifiedName = variableBinding.getDeclaringClass().getQualifiedName() + "." + qualifiedName;
            }

            addReferencedSymbol(
                    node,
                    new ReferencedVariable(node.toString(), qualifiedName)
            );
            return super.visit(node);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding methodBinding = node.resolveMethodBinding();

        String originalReference = "";
        if (node.getExpression() != null) {
            originalReference += node.getExpression().toString() + ".";
        }
        originalReference += node.getName().toString();

        ReferencedMethod referencedMethod = new ReferencedMethod(
                originalReference,
                methodBinding.getDeclaringClass().getQualifiedName() + "." + methodBinding.getName(),
                new ArrayList<>()
        );

        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        }

        addReferencedSymbol(node, referencedMethod);
        currentMethodInvocations.push(referencedMethod);

        // Manually visit children so that we can detect if we can infer argument types
        for (Object arg : node.arguments()) {
            int prevNrArguments = referencedMethod.getArguments().size();
            ((ASTNode) arg).accept(this);
            if (referencedMethod.getArguments().size() == prevNrArguments) {
                // Failed to extract symbol reference from object. Insert empty instead
                referencedMethod.getArguments().add(Optional.empty());
            }
        }

        currentMethodInvocations.pop();

        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        IMethodBinding methodBinding = node.resolveConstructorBinding();

        ReferencedMethod referencedMethod = new ReferencedMethod(
                // We don't really know if the constructor is using the qualified name or not but we just assume not.
                node.getType().resolveBinding().getName(),
                methodBinding.getDeclaringClass().getQualifiedName() + ".<init>",
                new ArrayList<>()
        );

        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        }

        addReferencedSymbol(node, referencedMethod);
        currentMethodInvocations.push(referencedMethod);

        // Manually visit children so that we can detect if we can infer argument types
        for (Object arg : node.arguments()) {
            int prevNrArguments = referencedMethod.getArguments().size();
            ((ASTNode) arg).accept(this);
            if (referencedMethod.getArguments().size() == prevNrArguments) {
                // Failed to extract symbol reference from object. Insert empty instead
                referencedMethod.getArguments().add(Optional.empty());
            }
        }

        currentMethodInvocations.pop();

        return false;
    }
}
