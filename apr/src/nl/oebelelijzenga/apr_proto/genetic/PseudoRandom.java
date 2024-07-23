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

package nl.oebelelijzenga.apr_proto.genetic;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class PseudoRandom {

    private static Random random = new Random(0);

    public static void setSeed(int seed) {
        random = new Random(seed);
    }

    public static boolean coinflip() {
        return random.nextBoolean();
    }

    public static boolean bool(float probability) {
        return random.nextFloat(0.0f, 1.0f) < probability;
    }

    public static int intRange(int min, int max) {
        return intRangeExclusive(min, max + 1);
    }

    public static int intRangeExclusive(int min, int maxExclusive) {
        return random.nextInt(min, maxExclusive);
    }

    public static int intRangeExclusive(int maxExclusive) {
        return random.nextInt(0, maxExclusive);
    }

    public static <T> T pick(T[] values) {
        return values[intRangeExclusive(values.length)];
    }

    public static <T> T pick(List<T> values) {
        return values.get(intRangeExclusive(values.size()));
    }

    public static <T> T pickWeighted(List<Pair<Float, T>> values) {
        float weightsSum = values.stream().map(Pair::getLeft).reduce(0.0f, Float::sum);
        float val = random.nextFloat(0, weightsSum);
        float total = 0.0f;
        for (Pair<Float, T> entry : values) {
            if (val <= total + entry.getLeft()) {
                return entry.getRight();
            }
            total += entry.getLeft();
        }
        throw new IllegalStateException();
    }

    public static <T> List<T> pickList(List<T> values, int n) {
        if (n == 0) {
            return List.of();
        }

        if (values.size() <= n) {
            return values;
        }

        return shuffle(values).subList(0, n);
    }

    public static <T> Set<T> pickSet(Set<T> values, int n) {
        return new HashSet<>(pickList(new ArrayList<>(values), n));
    }

    public static <T> List<T> shuffle(List<T> list) {
        List<T> listCopy = new ArrayList<>(list);
        Collections.shuffle(listCopy, random);
        return listCopy;
    }
}
