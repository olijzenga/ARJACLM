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

package test.nl.oebelelijzenga.arjaclm.model.apr.genetic;

import junit.framework.TestCase;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.model.apr.ModificationPoint;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Edit;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Variant;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.arjaclm.model.java.JavaClass;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import nl.oebelelijzenga.arjaclm.parser.manipulation.ManipulationName;
import test.nl.oebelelijzenga.arjaclm.TestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VariantTest extends TestCase {

    public void testCacheHashCode() throws AprException {
        String source = """
        class MyClass {
            public void foo() {
                int x = 0;
                int x = 0;
            }
        }
        """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        ParsedJavaFile file = project.sourceFiles().get(0);
        JavaClass cls = file.classes().get(0);

        Variant variant1 = new Variant(
                List.of(
                        new Edit(
                                true,
                                ManipulationName.REPLACE,
                                new ModificationPoint(123, cls.statements().get(0), file, cls, 1.0f, new ArrayList<>(), new ArrayList<>()),
                                new Ingredient(cls.statements().get(1), true)
                        ),
                        // Disabled edits should not matter for the hashcode
                        new Edit(
                                false,
                                ManipulationName.INSERT_BEFORE,
                                new ModificationPoint(123, cls.statements().get(0), file, cls, 1.0f, new ArrayList<>(), new ArrayList<>()),
                                new Ingredient(cls.statements().get(1), true)
                        )
                ),
                Optional.empty(),
                Optional.empty()
        );

        Variant variant2 = new Variant(
                List.of(
                        new Edit(
                                true,
                                ManipulationName.REPLACE,
                                new ModificationPoint(123, cls.statements().get(1), file, cls, 0.5f, new ArrayList<>(), new ArrayList<>()),
                                new Ingredient(cls.statements().get(0), false)
                        )
                ),
                Optional.empty(),
                Optional.empty()
        );

        // Enabled edits hashcode is only based on enabled, manipulation name, modification point index and ingredient statements
        assertEquals(variant1.enabledEditsHashCode(), variant2.enabledEditsHashCode());
    }
}
