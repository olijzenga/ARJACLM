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

package nl.oebelelijzenga.apr_proto.parser.visitor;

import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.*;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class DeclaredClassSymbolsVisitor extends IngredientStatementVisitor {

    private static final Logger logger = LogManager.getLogger(DeclaredClassSymbolsVisitor.class);
    private final Stack<DeclaredClass> classStack = new Stack<>();
    private final List<DeclaredClass> classes = new ArrayList<>();
    private final Map<DeclaredClass, List<DeclaredVariable>> fields = new HashMap<>();
    private final Map<DeclaredClass, List<DeclaredMethod>> methods = new HashMap<>();

    public static DeclaredClassSymbols getClassSymbols(List<CompilationUnit> compilationUnits) {
        DeclaredClassSymbolsVisitor visitor = new DeclaredClassSymbolsVisitor();
        for (CompilationUnit compilationUnit : compilationUnits) {
            compilationUnit.accept(visitor);
        }
        return new DeclaredClassSymbols(visitor.classes, visitor.fields, visitor.methods);
    }

    public static DeclaredClassSymbols getClassSymbols(CompilationUnit compilationUnit) {
        return getClassSymbols(List.of(compilationUnit));
    }

    private DeclaredClass currentClass() {
        try {
            return classStack.peek();
        } catch (EmptyStackException e) {
            throw e;
        }
    }

    private List<DeclaredVariable> currentClassFields() {
        return fields.get(currentClass());
    }

    private List<DeclaredMethod> currentClassMethods() {
        return methods.get(currentClass());
    }

    @Override
    protected boolean visitIngredientStatement(Statement statement) {
        List<DeclaredMethod> methods = currentClassMethods();
        methods.get(methods.size() - 1).statements().add(statement);

        return ASTUtil.canContainNestedStatements(statement);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        DeclaredClass cls = new DeclaredClass(ASTUtil.getDeclarationVisibility(node), node.resolveBinding());
        classStack.push(cls);
        classes.add(cls);
        fields.put(cls, new ArrayList<>());
        methods.put(cls, new ArrayList<>());
        return true;
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        classStack.pop();
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        DeclaredClass cls = new DeclaredClass(ASTUtil.getDeclarationVisibility(node), node.resolveBinding());
        classStack.push(cls);
        classes.add(cls);
        fields.put(cls, new ArrayList<>());
        methods.put(cls, new ArrayList<>());
        return true;
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        classStack.pop();
    }

    @Override
    public boolean visit(Initializer node) {
        // Always ignore static blocks
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        IVariableBinding binding = node.resolveBinding();
        if (binding == null) {
            logger.error("Variable binding for variable declaration fragment %s cannot be resolved".formatted(node));
            return super.visit(node);
        }

        if (node.getParent() instanceof FieldDeclaration fieldDeclaration) {
            // Instance variable
            currentClassFields().add(
                    new DeclaredVariable(
                            QualifiedName.fromString(binding.getDeclaringClass().getQualifiedName() + "." + binding.getName()),
                            ASTUtil.getDeclarationVisibility(fieldDeclaration),
                            ASTUtil.isStatic(fieldDeclaration),
                            binding
                    )
            );
        }

        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        IMethodBinding binding = node.resolveBinding();
        if (binding == null) {
            logger.error("Method binding for method declaration %s cannot be resolved".formatted(node));
            return super.visit(node);
        }

        List<DeclaredVariable> parameters = getMethodParameterDeclarations(node);
        List<QualifiedName> throwsExceptions = getMethodThrowsExceptionTypes(node);

        boolean isConstructor = binding.isConstructor();
        DeclaredMethod declaredMethod = new DeclaredMethod(
                QualifiedName.fromString(
                        binding.getDeclaringClass().getQualifiedName() +
                                "." +
                                (isConstructor ? "<init>" : binding.getName())
                ),
                ASTUtil.getDeclarationVisibility(node),
                ASTUtil.isStatic(node),
                node,
                parameters,
                isConstructor,
                throwsExceptions
        );

        currentClassMethods().add(declaredMethod);

        return true;
    }

    private List<DeclaredVariable> getMethodParameterDeclarations(MethodDeclaration methodDeclaration) {
        List<DeclaredVariable> parameters = new ArrayList<>();
        for (Object parameterObj : methodDeclaration.parameters()) {
            if (parameterObj instanceof SingleVariableDeclaration parameter) {
                IVariableBinding parameterBinding = parameter.resolveBinding();
                if (parameterBinding == null) {
                    logger.warn("IVariableBinding of method parameter is null");
                } else {
                    parameters.add(
                            new DeclaredVariable(
                                    QualifiedName.fromString(parameterBinding.getName()),
                                    SymbolVisibility.LOCAL,
                                    false,
                                    parameterBinding
                            )
                    );
                }
            } else {
                logger.warn("Method parameter %s is not a SingleVariableDeclaration".formatted(parameterObj));
            }
        }
        return parameters;
    }

    private List<QualifiedName> getMethodThrowsExceptionTypes(MethodDeclaration methodDeclaration) {
        List<QualifiedName> exceptionTypes = new ArrayList<>();
        for (Object obj : methodDeclaration.thrownExceptionTypes()) {
            Type type = (Type) obj;
            exceptionTypes.add(QualifiedName.fromString(type.resolveBinding().getQualifiedName()));
        }
        return exceptionTypes;
    }
}
