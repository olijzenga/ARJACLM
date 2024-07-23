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

package test.nl.oebelelijzenga.apr_proto.parser.ingredient;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.parser.ingredient.ModificationScreener;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;
import org.eclipse.jdt.core.dom.*;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

import java.util.List;

public class ModificationScreenerTest extends AprTestCase {

    public void testRule1() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        String x = "";
                        if (x == null) {    // statement 1
                            x = "";
                        } else {
                            x += "test";
                        }
                    }
                    
                    void bar() {
                        String x = "";
                        x += "hello world"; // statement 5
                        if (x == null) {    // statement 6
                            x = "";
                        } else {
                            x += "test";
                        }
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertStatementEquals("String x = \"\";", statements.get(4));
        assertTrue(ModificationScreener.screen(statements.get(5), statements.get(1), ManipulationName.REPLACE));

        assertTrue(statements.get(6) instanceof IfStatement);
        assertTrue(ModificationScreener.screen(statements.get(6), statements.get(1), ManipulationName.DELETE));
        assertTrue(ModificationScreener.screen(statements.get(6), statements.get(1), ManipulationName.INSERT_BEFORE));
        assertTrue(ModificationScreener.screen(statements.get(6), statements.get(1), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(6), statements.get(1), ManipulationName.REPLACE));
    }

    public void testRule2() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        String x = "";  // statement 0
                        int y = 0;
                        float z = 0.1f;
                        x += "test";
                        if (y > z) {    // statement 4
                            return;     // statement 5
                        }
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(2), ManipulationName.REPLACE));

        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(3), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(3), ManipulationName.DELETE));

        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(4), ManipulationName.REPLACE));
        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(5), ManipulationName.REPLACE));
    }

    public void testRule3() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        String x = "";  // statement 0
                        int y = 0;
                        x += "test";
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.INSERT_BEFORE));
        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.INSERT_AFTER));
        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.DELETE));
        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.REPLACE));

        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(2), ManipulationName.INSERT_BEFORE));
        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(2), ManipulationName.INSERT_AFTER));
    }

    public void testRule4() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        throw new RuntimeException();  // statement 0
                        return;         // statement 1
                        if (true) {     // statement 2
                            throw new RuntimeException();
                        } else {
                            return;
                        }
                        try {           // statement 5
                            int x = 0;
                        } finally {
                            return;
                        }
                        try {           // statement 8
                            int y = 0;
                        } catch (InteruppedException e) {
                            throw new RuntimeException();
                        } catch (RuntimeException e) {
                            return;
                        }
                        int x = 0;  // statement 12
                    }
                    
                    void bar() {
                        int x = 0;
                        x++;        // statement 14
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertTrue(statements.get(0) instanceof ThrowStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(0), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(0), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(14), statements.get(0), ManipulationName.INSERT_BEFORE));

        assertTrue(statements.get(1) instanceof ReturnStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(1), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(1), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(14), statements.get(1), ManipulationName.INSERT_BEFORE));

        assertTrue(statements.get(2) instanceof IfStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(2), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(2), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(14), statements.get(2), ManipulationName.INSERT_BEFORE));

        assertTrue(statements.get(5) instanceof TryStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(5), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(5), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(14), statements.get(5), ManipulationName.INSERT_BEFORE));

        assertTrue(statements.get(8) instanceof TryStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(8), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(8), ManipulationName.INSERT_AFTER));
        assertFalse(ModificationScreener.screen(statements.get(14), statements.get(8), ManipulationName.INSERT_BEFORE));

        assertTrue(statements.get(12) instanceof VariableDeclarationStatement);
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(12), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(12), ManipulationName.INSERT_AFTER));
        assertTrue(ModificationScreener.screen(statements.get(14), statements.get(12), ManipulationName.INSERT_BEFORE));
    }

    public void testRule5() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    int foo() {
                        return x;   // statement 0
                    }
                    
                    int bar() {
                        if (true) { // statement 1
                            throw new RuntimeException();
                        } else {
                            return 1;
                        }
                    }
                    
                    void baz() {
                        int x = 0;
                        x++;  // statement 5
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertTrue(ModificationScreener.screen(statements.get(0), statements.get(1), ManipulationName.REPLACE));
        assertFalse(ModificationScreener.screen(statements.get(0), statements.get(5), ManipulationName.REPLACE));

        assertTrue(ModificationScreener.screen(statements.get(1), statements.get(0), ManipulationName.REPLACE));
        assertFalse(ModificationScreener.screen(statements.get(1), statements.get(5), ManipulationName.REPLACE));

        assertTrue(ModificationScreener.screen(statements.get(5), statements.get(0), ManipulationName.REPLACE));
        assertTrue(ModificationScreener.screen(statements.get(5), statements.get(1), ManipulationName.REPLACE));
    }

    public void testRule6() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    int foo() {
                        int x = 0;  // statement 0
                        int y = 0;
                        x = 1;
                        x = 2;
                        y = 1;      // statement 4
                        x = y = 3;
                        x = y = 4;
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        assertFalse(ModificationScreener.screen(statements.get(3), statements.get(2), ManipulationName.INSERT_BEFORE));
        assertTrue(ModificationScreener.screen(statements.get(3), statements.get(4), ManipulationName.INSERT_BEFORE));

        assertTrue(ModificationScreener.screen(statements.get(3), statements.get(4), ManipulationName.INSERT_BEFORE));
        assertFalse(ModificationScreener.screen(statements.get(5), statements.get(6), ManipulationName.INSERT_BEFORE));
    }
}
