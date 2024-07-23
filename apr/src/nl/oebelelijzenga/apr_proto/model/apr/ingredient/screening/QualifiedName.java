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


import java.util.ArrayList;
import java.util.List;

public class QualifiedName {

    private final List<String> elements;

    public QualifiedName(List<String> elements) {
        this.elements = elements;
    }

    public QualifiedName(String... elements) {
        this(List.of(elements));
    }

    public static QualifiedName fromString(String qualifiedName) {
        return new QualifiedName(List.of(qualifiedName.split("\\.")));
    }

    public List<String> elements() {
        return elements;
    }

    public QualifiedName getParent() {
        return new QualifiedName(elements.subList(0, elements.size() - 1));
    }

    public String last() {
        return elements.get(elements.size() - 1);
    }

    public boolean startsWith(QualifiedName qualifiedName) {
        if (qualifiedName.elements.size() > elements.size()) {
            return false;
        }

        for (int i = 0; i < qualifiedName.elements.size(); i++) {
            if (!qualifiedName.elements.get(i).equals(elements.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(QualifiedName qualifiedName) {
        if (qualifiedName.elements.size() > elements.size()) {
            return false;
        }

        for (int i = 0; i < qualifiedName.elements.size(); i++) {
            if (!qualifiedName.elements.get(i).equals(elements.get(elements.size() - qualifiedName.elements.size() + i))) {
                return false;
            }
        }
        return true;
    }

    public QualifiedName add(String element) {
        List<String> elements = new ArrayList<>(elements());
        elements.add(element);
        return new QualifiedName(elements);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof QualifiedName qualifiedName)) {
            return false;
        }

        return elements.equals(qualifiedName.elements);
    }

    @Override
    public String toString() {
        return String.join(".", elements);
    }
}
