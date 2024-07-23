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

package test.nl.oebelelijzenga.apr_proto.parser;

import junit.framework.TestCase;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.apr.BugLocation;
import nl.oebelelijzenga.apr_proto.model.apr.ModificationPoint;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.parser.ModificationPointFactory;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ModificationPointFactoryTest extends TestCase {

    private static List<ManipulationName> defaultManipulationsExceptDelete() {
        List<ManipulationName> defaultManipulations = new ArrayList<>(ManipulationName.defaultEnabledManipulations());
        defaultManipulations.remove(ManipulationName.DELETE);
        return defaultManipulations;
    }

    public void testArjaTable1Rule1() throws AprException {
        // Variable declaration statements should not be deleted
        String source = """
                package mypkg;
                
                class MyClass {
                    void fn() {
                        int x = 0;  // line 5
                        int y = 0;
                        x++;        // line 7
                        y++;
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<ModificationPoint> modificationPoints = ModificationPointFactory.create(
                TestUtil.getDummyAprConfig(),
                project,
                new Bug(
                        "MyBug",
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashMap<>(),
                        List.of(
                                new BugLocation(Path.of("MyClass.java"), 5, 1.0f),
                                new BugLocation(Path.of("MyClass.java"), 7, 1.0f)
                        )
                )
        );

        assertEquals(2, modificationPoints.size());
        assertEquals(defaultManipulationsExceptDelete(), modificationPoints.get(0).allowedManipulations());
        assertEquals(ManipulationName.defaultEnabledManipulations(), modificationPoints.get(1).allowedManipulations());
    }

    public void testArjaTable1Rule2() throws AprException {
        // Do not delete a return/throw statement which is the last statement of a method not declared void.
        String source = """
                package mypkg;
                
                class MyClass {
                    void fn1() {
                        int x = 0;
                        return;  // line 6
                    }
                    
                    int fn2() {
                        int x = 0;
                        return x;  // line 11
                    }
                    
                    void fn3() throws RuntimeException {
                        int x = 0;
                        throw new RuntimeException();  // line 16
                    }
                    
                    int fn4() throws RuntimeException {
                        int x = 0;
                        throw new RuntimeException();  // line 21
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<ModificationPoint> modificationPoints = ModificationPointFactory.create(
                TestUtil.getDummyAprConfig(),
                project,
                new Bug(
                        "MyBug",
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashMap<>(),
                        List.of(
                            new BugLocation(Path.of("MyClass.java"), 6, 1.0f),
                            new BugLocation(Path.of("MyClass.java"), 11, 1.0f),
                            new BugLocation(Path.of("MyClass.java"), 16, 1.0f),
                            new BugLocation(Path.of("MyClass.java"), 21, 1.0f)
                    )
                )
        );

        assertEquals(4, modificationPoints.size());
        assertEquals(ManipulationName.defaultEnabledManipulations(), modificationPoints.get(0).allowedManipulations());
        assertEquals(defaultManipulationsExceptDelete(), modificationPoints.get(1).allowedManipulations());
        assertEquals(ManipulationName.defaultEnabledManipulations(), modificationPoints.get(2).allowedManipulations());
        assertEquals(defaultManipulationsExceptDelete(), modificationPoints.get(3).allowedManipulations());
    }
}
