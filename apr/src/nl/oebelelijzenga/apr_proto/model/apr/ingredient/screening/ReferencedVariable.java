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

public class ReferencedVariable extends ReferencedSymbol {
    public ReferencedVariable(String originalReference, QualifiedName qualifiedName) {
        super(originalReference, qualifiedName);
    }

    public ReferencedVariable(String originalReference, String qualifiedName) {
        this(originalReference, QualifiedName.fromString(qualifiedName));
    }

    public boolean isLocal() {
        return qualifiedName.elements().size() == 1;
    }

    @Override
    public String toString() {
        return "ReferencedVariable{ref=%s, qn='%s'}".formatted(originalReference, qualifiedName);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReferencedVariable referencedVariable)) {
            return false;
        }
        return referencedVariable.qualifiedName.equals(qualifiedName);
    }
}
