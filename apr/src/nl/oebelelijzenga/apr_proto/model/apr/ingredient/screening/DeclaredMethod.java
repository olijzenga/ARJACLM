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

package nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public final class DeclaredMethod extends DeclaredSymbol {
    private final MethodDeclaration methodDeclaration;
    private final List<DeclaredVariable> parameters;
    private final boolean isConstructor;
    private final List<QualifiedName> throwsExceptionTypes;
    private final IMethodBinding methodBinding;
    private final List<Statement> statements = new ArrayList<>();

    public DeclaredMethod(QualifiedName qualifiedName, SymbolVisibility visibility, boolean isStatic, MethodDeclaration methodDeclaration, List<DeclaredVariable> parameters, boolean isConstructor, List<QualifiedName> throwsExceptionTypes) {
        super(qualifiedName, visibility, isStatic);
        this.methodDeclaration = methodDeclaration;
        this.parameters = parameters;
        this.isConstructor = isConstructor;
        this.throwsExceptionTypes = throwsExceptionTypes;
        this.methodBinding = methodDeclaration.resolveBinding();
    }

    public MethodDeclaration methodDeclaration() {
        return methodDeclaration;
    }

    public IMethodBinding methodBinding() {
        return methodBinding;
    }

    public ITypeBinding returnTypeBinding() {
        return methodBinding().getReturnType();
    }

    public QualifiedName returnTypeName() {
        return QualifiedName.fromString(returnTypeBinding().getQualifiedName());
    }

    public List<DeclaredVariable> parameters() {
        return parameters;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public List<QualifiedName> getThrowsExceptionTypes() {
        return throwsExceptionTypes;
    }

    public List<Statement> statements() {
        return statements;
    }

    @Override
    public String toString() {
        return "DeclaredMethod[name=%s, visibility=%s, static=%s, constructor=%s, returnTypeBinding=%s, parameters=%s]"
                .formatted(qualifiedName, visibility, isStatic, isConstructor, returnTypeBinding(), parameters);
    }

    @Override
    public ITypeBinding getOutputTypeBinding() {
        return returnTypeBinding();
    }

    @Override
    public ITypeBinding getDeclaringClass() {
        return methodBinding().getDeclaringClass();
    }
}
