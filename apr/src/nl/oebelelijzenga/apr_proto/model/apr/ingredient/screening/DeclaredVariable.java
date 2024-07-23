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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

public class DeclaredVariable extends DeclaredSymbol {
    protected final IVariableBinding variableBinding;

    public DeclaredVariable(QualifiedName qualifiedName, SymbolVisibility visibility, boolean isStatic, IVariableBinding variableBinding) {
        super(qualifiedName, visibility, isStatic);
        this.variableBinding = variableBinding;
    }

    public QualifiedName getQualifiedTypeName() {
        return QualifiedName.fromString(variableBinding.getType().getQualifiedName());
    }

    public ITypeBinding getTypeBinding() {
        return variableBinding.getType();
    }

    public IVariableBinding getVariableBinding() {
        return variableBinding;
    }

    public int getModifiers() {
        return variableBinding.getModifiers();
    }

    @Override
    public String toString() {
        return "DeclaredVariable[name=%s, visibility=%s, static=%s, variableBinding=%s]"
                .formatted(qualifiedName, visibility, isStatic, variableBinding);
    }

    @Override
    public ITypeBinding getOutputTypeBinding() {
        return variableBinding.getType();
    }

    @Override
    public ITypeBinding getDeclaringClass() {
        return variableBinding.getDeclaringClass();
    }
}
