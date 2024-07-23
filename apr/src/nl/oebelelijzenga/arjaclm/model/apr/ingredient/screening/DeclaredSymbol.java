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

package nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening;

import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

public abstract class DeclaredSymbol {

    protected final QualifiedName qualifiedName;
    protected final SymbolVisibility visibility;
    protected final boolean isStatic;

    public DeclaredSymbol(QualifiedName qualifiedName, SymbolVisibility visibility, boolean isStatic) {
        this.qualifiedName = qualifiedName;
        this.visibility = visibility;
        this.isStatic = isStatic;
    }

    public String getShortName() {
        return qualifiedName.last();
    }

    public QualifiedName getQualifiedName() {
        return qualifiedName;
    }

    public SymbolVisibility getVisibility() {
        return visibility;
    }

    public boolean isLocal() {
        return visibility == SymbolVisibility.LOCAL;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public abstract ITypeBinding getOutputTypeBinding();

    public abstract ITypeBinding getDeclaringClass();

    public boolean isVisibleFromClass(ITypeBinding cls) {
        switch (visibility) {
            case PUBLIC -> {
                return true;
            }
            case PROTECTED -> {
                List<ITypeBinding> classes = ASTUtil.getSuperClasses(cls);
                classes.add(cls);
                return classes.stream().map(x -> x.getQualifiedName().equals(qualifiedName.getParent().toString())).findAny().get();
            }
            case PACKAGE_PRIVATE -> {
                QualifiedName clsParent = QualifiedName.fromString(cls.getQualifiedName()).getParent();
                QualifiedName declaringClassParent = QualifiedName.fromString(getDeclaringClass().getQualifiedName()).getParent();
                return clsParent.equals(declaringClassParent);
            }
            case PRIVATE -> {
                return cls.getQualifiedName().equals(qualifiedName.getParent().toString());
            }
        }
        throw new IllegalArgumentException(visibility.toString());
    }

    public boolean isFieldOf(ITypeBinding cls) {
        List<ITypeBinding> classes = ASTUtil.getSuperClasses(cls);
        classes.add(cls);
        return classes.stream().anyMatch(x -> x.getQualifiedName().equals(qualifiedName.getParent().toString()));
    }
}
