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

package nl.oebelelijzenga.apr_proto.model.apr.genetic;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public final class Variant {
    private final List<Edit> edits;
    private final Optional<Variant> previousVariant;
    private final Optional<Class<?>> creator;
    private int patchId = -1;  // Only used for debugging

    public Variant(List<Edit> edits, Optional<Variant> previousVariant, Optional<Class<?>> creator) {
        this.edits = edits;
        this.previousVariant = previousVariant;
        this.creator = creator;
    }

    public static Variant create(List<Edit> edits) {
        return new Variant(edits, Optional.empty(), Optional.empty());
    }

    private static Variant create(List<Edit> edits, Variant previousVariant, Class<?> createdBy) {
        return new Variant(edits, Optional.of(previousVariant), Optional.of(createdBy));
    }

    public Variant withEdits(List<Edit> newEdits, Class<?> creator) {
        return Variant.create(newEdits, this, creator);
    }

    public List<Edit> enabledEdits() {
        return edits.stream().filter(Edit::enabled).toList();
    }

    public int enabledEditsHashCode() {
        return enabledEdits().stream().map(Edit::cacheHashCode).toList().hashCode();
    }

    public boolean effectivelyEquals(Variant variant) {
        return enabledEditsHashCode() == variant.enabledEditsHashCode();
    }

    public boolean isEmptyVariant() {
        return enabledEdits().isEmpty();
    }

    public List<Pair<Variant, Class<?>>> getHistory() {
        if (previousVariant.isEmpty()) {
            return new ArrayList<>();
        }

        List<Pair<Variant, Class<?>>> history = previousVariant.get().getHistory();
        history.add(ImmutablePair.of(this, creator.orElseThrow()));

        return history;
    }

    public int patchId() {
        return patchId;
    }

    public void setPatchId(int patchId) {
        this.patchId = patchId;
    }

    @Override
    public String toString() {
        return String.format("Variant[id=%s, edits=%s]", patchId, enabledEdits());
    }

    public String toFileString() {
        StringBuilder result = new StringBuilder("Variant[\n\tid=").append(patchId).append(",\n\tedits=[\n");
        for (Edit edit : enabledEdits()) {
            result.append(edit.toFileString().indent(8));
        }
        result.append("\n\t]\n]\n");
        return result.toString();
    }

    public List<Edit> edits() {
        return edits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variant variant = (Variant) o;
        return Objects.equals(edits, variant.edits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edits);
    }

    public static List<Variant> unique(List<Variant> variants) {
        Map<Integer, Variant> uniqueVariants = new HashMap<>();
        for (Variant variant : variants) {
            uniqueVariants.put(variant.patchId, variant);
        }
        return new ArrayList<>(uniqueVariants.values());
    }
}
