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

package test.nl.oebelelijzenga.arjaclm.parser.ingredient;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.IngredientScreeningInfo;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.parser.ingredient.ArjaRulesIngredientScreener;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import test.nl.oebelelijzenga.arjaclm.parser.AprTestCase;
import test.nl.oebelelijzenga.arjaclm.TestUtil;

import java.util.List;

public class ArjaRulesIngredientScreenerTest extends AprTestCase {
    public void testRule1() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        while(true) {
                            continue;   // statement 1
                        }
                    }
                    
                    void bar() {
                        int x = 0;      // statement 2
                        while (true) {
                            x++;        // statement 4
                        }
                        for (int i = 0; i < 10; i++) {
                            x += 1;     // statement 6
                        }
                        do {
                            x += 2;     // statement 8
                        } while (true)
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements, List.of(statements.get(1)))
        );

        assertStatementEquals("int x = 0;", statements.get(2));
        assertFalse(screener.screen(statements.get(2), statements.get(1)));

        assertFalse(screener.screen(statements.get(3), statements.get(1)));
        assertStatementEquals("x++;", statements.get(4));
        assertTrue(screener.screen(statements.get(4), statements.get(1)));

        assertFalse(screener.screen(statements.get(5), statements.get(1)));
        assertStatementEquals("x += 1;", statements.get(6));
        assertTrue(screener.screen(statements.get(6), statements.get(1)));

        assertFalse(screener.screen(statements.get(7), statements.get(1)));
        assertStatementEquals("x += 2;", statements.get(8));
        assertTrue(screener.screen(statements.get(8), statements.get(1)));
    }

    public void testRule2Loop() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        while(true) {
                            break;      // statement 1
                        }
                    }
                    
                    void bar() {
                        int x = 0;      // statement 2
                        while (true) {
                            x++;        // statement 4
                        }
                        for (int i = 0; i < 10; i++) {
                            x += 1;     // statement 6
                        }
                        do {
                            x += 2;     // statement 8
                        } while (true)
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements, List.of(statements.get(1)))
        );

        assertStatementEquals("int x = 0;", statements.get(2));
        assertFalse(screener.screen(statements.get(2), statements.get(1)));

        assertFalse(screener.screen(statements.get(3), statements.get(1)));
        assertStatementEquals("x++;", statements.get(4));
        assertTrue(screener.screen(statements.get(4), statements.get(1)));

        assertFalse(screener.screen(statements.get(5), statements.get(1)));
        assertStatementEquals("x += 1;", statements.get(6));
        assertTrue(screener.screen(statements.get(6), statements.get(1)));

        assertFalse(screener.screen(statements.get(7), statements.get(1)));
        assertStatementEquals("x += 2;", statements.get(8));
        assertTrue(screener.screen(statements.get(8), statements.get(1)));
    }

    public void testRule2Switch() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        while(true) {
                            break;          // statement 1
                        }
                    }
                    
                    void bar() {
                        int x = 0;          // statement 2
                        switch(x) {         // statement 3
                            case 1:         // statement 4
                                x += 1;     // statement 5
                            default -> {    // statement 6
                                x += 2;     // statement 7
                            }
                        }
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements, List.of(statements.get(1)))
        );

        assertStatementEquals("int x = 0;", statements.get(2));
        assertFalse(screener.screen(statements.get(2), statements.get(1)));

        assertTrue(statements.get(3) instanceof SwitchStatement);
        assertFalse(screener.screen(statements.get(3), statements.get(1)));

        assertTrue(statements.get(4) instanceof SwitchCase);
        assertFalse(screener.screen(statements.get(4), statements.get(1)));

        assertStatementEquals("x += 1;", statements.get(5));
        assertTrue(screener.screen(statements.get(5), statements.get(1)));

        assertTrue(statements.get(6) instanceof SwitchCase);
        assertFalse(screener.screen(statements.get(6), statements.get(1)));

        assertStatementEquals("x += 2;", statements.get(7));
        assertTrue(screener.screen(statements.get(7), statements.get(1)));
    }

    public void testRule3() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void foo() {
                        int x = 0;
                        switch (x) {
                            case 1:         // statement 2
                                x += 1;
                            default:        // statement 4
                                x = 0;
                        }
                        
                        String y = "hello world";
                        switch (y) {
                            case "hello":   // statement 8
                                break;
                            default:        // statement 10
                                y = "";
                        }
                    }
                    
                    void bar() {
                        int x = 0;
                        switch(x) {
                            case 1:         // statement 14
                                x += 1;     // statement 15
                        }
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(
                        project,
                        statements.subList(14, 16),
                        List.of(statements.get(2), statements.get(4), statements.get(8), statements.get(10))
                )
        );

        for (int statementNr : List.of(14, 15)) {
            // Results should not differ for switch case or a switch body

            assertTrue(statements.get(2) instanceof SwitchCase);
            assertTrue(screener.screen(statements.get(statementNr), statements.get(2)));

            assertTrue(statements.get(4) instanceof SwitchCase);
            assertTrue(screener.screen(statements.get(statementNr), statements.get(4)));

            assertTrue(statements.get(8) instanceof SwitchCase);
            assertFalse(screener.screen(statements.get(statementNr), statements.get(8)));

            // Despite being a "default" block and thus not causing conflict, the parent type still does not match so
            // we reject this option.
            assertTrue(statements.get(10) instanceof SwitchCase);
            assertFalse(screener.screen(statements.get(statementNr), statements.get(10)));
        }
    }

    public void testRule4Return() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void a() {
                        return;  // statement 0
                    }
                    int b() {
                        return 1 + 2;  // statement 1
                    }
                    String c() {
                        return "";  // statement 2
                    }
                    
                    void foo() {
                        int x = 0;  // statement 3
                    }
                    int bar() {
                        return 1;  // statement 4
                    }
                    String baz() {
                        return "";  // statement 5
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements.subList(3, 6), statements.subList(0, 3))
        );

        assertTrue(screener.screen(statements.get(3), statements.get(0)));
        assertFalse(screener.screen(statements.get(4), statements.get(0)));
        assertFalse(screener.screen(statements.get(5), statements.get(0)));

        assertFalse(screener.screen(statements.get(3), statements.get(1)));
        assertTrue(screener.screen(statements.get(4), statements.get(1)));
        assertFalse(screener.screen(statements.get(5), statements.get(1)));

        assertFalse(screener.screen(statements.get(3), statements.get(2)));
        assertFalse(screener.screen(statements.get(4), statements.get(2)));
        assertTrue(screener.screen(statements.get(5), statements.get(2)));
    }


    public void testRule4Throw() throws AprException {
        String source = """
                package mypkg;
                
                class MyException1 extends RuntimeException {}
                class MyException2 extends Error {}
                class MyException3 extends Exception {}
                class MyException4 extends Exception {}
                                
                class MyClass {
                    void a() {
                        throw new RuntimeException();  // statement 0
                    }
                    void b() {
                        throw new MyException1();  // statement 1
                    }
                    void c() {
                        throw new MyException2();  // statement 2
                    }
                    void d() throws MyException3 {
                        throw new MyException3();  // statement 3
                    }
                    void e() throws MyException4 {
                        throw new MyException4();  // statement 4
                    }
                    
                    void foo() {
                        return;  // statement 5
                    }
                    void bar() throws MyException3 {
                        return;  // statement 6
                    }
                    void baz() throws MyException3, MyException4 {
                        return;  // statement 7
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(4).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements.subList(5, 8), statements.subList(0, 5))
        );

        // RuntimeException, Error and their subclasses do not require explicit declaration
        assertTrue(screener.screen(statements.get(5), statements.get(0)));
        assertTrue(screener.screen(statements.get(6), statements.get(0)));
        assertTrue(screener.screen(statements.get(7), statements.get(0)));

        assertTrue(screener.screen(statements.get(5), statements.get(1)));
        assertTrue(screener.screen(statements.get(6), statements.get(1)));
        assertTrue(screener.screen(statements.get(7), statements.get(1)));

        assertTrue(screener.screen(statements.get(5), statements.get(2)));
        assertTrue(screener.screen(statements.get(6), statements.get(2)));
        assertTrue(screener.screen(statements.get(7), statements.get(2)));

        assertFalse(screener.screen(statements.get(5), statements.get(3)));
        assertTrue(screener.screen(statements.get(6), statements.get(3)));
        assertTrue(screener.screen(statements.get(7), statements.get(3)));

        assertFalse(screener.screen(statements.get(5), statements.get(4)));
        assertFalse(screener.screen(statements.get(6), statements.get(4)));
        assertTrue(screener.screen(statements.get(7), statements.get(4)));
    }

    public void testRule5Return() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void a() {
                        return;         // statement 0
                    }
                    
                    void foo() {
                        int x = 0;      // statement 1
                        if (x == 0) {   // statement 2
                            int y = 1;  // statement 3
                            x += 1;
                        } else {
                            int z = 2;  // statement 5
                            x += 2;
                        }
                        x = 5;          // statement 7
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements.subList(1, 8), List.of(statements.get(0)))
        );

        assertFalse(screener.screen(statements.get(1), statements.get(0)));
        assertFalse(screener.screen(statements.get(2), statements.get(0)));
        assertFalse(screener.screen(statements.get(3), statements.get(0)));
        assertTrue(screener.screen(statements.get(4), statements.get(0)));
        assertFalse(screener.screen(statements.get(5), statements.get(0)));
        assertTrue(screener.screen(statements.get(6), statements.get(0)));
        assertTrue(screener.screen(statements.get(7), statements.get(0)));
    }

    public void testRule5Throw() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    void a() {
                        throw new RuntimeException();  // statement 0
                    }
                    
                    void foo() {
                        int x = 0;      // statement 1
                        if (x == 0) {   // statement 2
                            int y = 1;  // statement 3
                            x += 1;
                        } else {
                            int z = 2;  // statement 5
                            x += 2;
                        }
                        x = 5;          // statement 7
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements.subList(1, 8), List.of(statements.get(0)))
        );

        assertFalse(screener.screen(statements.get(1), statements.get(0)));
        assertFalse(screener.screen(statements.get(2), statements.get(0)));
        assertFalse(screener.screen(statements.get(3), statements.get(0)));
        assertTrue(screener.screen(statements.get(4), statements.get(0)));
        assertFalse(screener.screen(statements.get(5), statements.get(0)));
        assertTrue(screener.screen(statements.get(6), statements.get(0)));
        assertTrue(screener.screen(statements.get(7), statements.get(0)));
    }

    public void testRule6() throws AprException {
        String source = """
                package mypkg;
                
                import java.util.List;
                import java.util.ArrayList;
                
                class MyClass {
                    void a() {
                    int x = 0;                      // statement 0
                        int y = 1, z = 2;           // statement 1
                    }
                    void b() {
                        int x = 0, y = 1;           // statement 2
                    }
                    void c() {
                        int x = 0, y = 1, z = 2;    // statement 3
                    }
                    void d() {
                        int x;                      // statement 4
                        int y,z;                    // statement 5
                    }
                    void e() {
                        List<String> x = new ArrayList<>();  // statement 6
                        String y = "", z = "";      // statement 7
                    }
                    
                    void foo() {
                        int x = 10;                 // statement 8
                        int y = x+1, z = x+2;       // statement 9
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();
        ArjaRulesIngredientScreener screener = new ArjaRulesIngredientScreener(
                IngredientScreeningInfo.create(project, statements.subList(8, 10), statements.subList(0, 8))
        );

        assertTrue(screener.screen(statements.get(8), statements.get(0)));
        assertFalse(screener.screen(statements.get(9), statements.get(0)));

        assertFalse(screener.screen(statements.get(8), statements.get(1)));
        assertTrue(screener.screen(statements.get(9), statements.get(1)));

        assertFalse(screener.screen(statements.get(8), statements.get(2)));
        assertFalse(screener.screen(statements.get(9), statements.get(2)));

        assertFalse(screener.screen(statements.get(8), statements.get(3)));
        assertFalse(screener.screen(statements.get(9), statements.get(3)));

        assertTrue(screener.screen(statements.get(8), statements.get(4)));
        assertFalse(screener.screen(statements.get(9), statements.get(4)));

        assertFalse(screener.screen(statements.get(8), statements.get(5)));
        assertTrue(screener.screen(statements.get(9), statements.get(5)));

        assertFalse(screener.screen(statements.get(8), statements.get(6)));
        assertFalse(screener.screen(statements.get(9), statements.get(6)));

        assertFalse(screener.screen(statements.get(8), statements.get(7)));
        assertFalse(screener.screen(statements.get(9), statements.get(7)));
    }
}
