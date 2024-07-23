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

import junit.framework.TestCase;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.parser.ingredient.ReferencedVariableIngredientScreener;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.screening.IngredientScreeningInfo;
import org.eclipse.jdt.core.dom.Statement;
import test.nl.oebelelijzenga.arjaclm.TestUtil;

import java.util.List;

public class ReferencedVariableIngredientScreenerTest extends TestCase {
    public void testScreenSimpleLocalVariables() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    public static void foo(int x) {
                        int y = 1;  // statement 0
                        y = x;
                        int z = 2;
                        y = z;  // statement 3
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        ReferencedVariableIngredientScreener screener = new ReferencedVariableIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertTrue(screener.screen(statements.get(0), statements.get(0)));
        assertFalse(screener.screen(statements.get(0), statements.get(1)));
        assertTrue(screener.screen(statements.get(0), statements.get(2)));
        assertFalse(screener.screen(statements.get(0), statements.get(3)));

        assertTrue(screener.screen(statements.get(1), statements.get(0)));
        assertTrue(screener.screen(statements.get(1), statements.get(1)));
        assertTrue(screener.screen(statements.get(1), statements.get(2)));
        assertFalse(screener.screen(statements.get(1), statements.get(3)));  // z is not yet declared

        assertTrue(screener.screen(statements.get(2), statements.get(0)));
        assertTrue(screener.screen(statements.get(2), statements.get(1)));
        assertTrue(screener.screen(statements.get(2), statements.get(2)));
        assertFalse(screener.screen(statements.get(2), statements.get(3)));  // z is not yet declared

        assertTrue(screener.screen(statements.get(3), statements.get(0)));
        assertTrue(screener.screen(statements.get(3), statements.get(1)));
        assertTrue(screener.screen(statements.get(3), statements.get(2)));
        assertTrue(screener.screen(statements.get(3), statements.get(3)));
    }

    public void testScreenClassVariables() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    int x = 3;
                    int y = 2;
                
                    public static void foo(int x) {
                        x++;   // statement 0
                        MyClass c = new MyClass();
                        c.y = x;
                        x = c.x;   // statement 3
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> statements = project.sourceFiles().get(0).classes().get(0).statements();

        ReferencedVariableIngredientScreener screener = new ReferencedVariableIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertTrue(screener.screen(statements.get(0), statements.get(0)));
        assertTrue(screener.screen(statements.get(0), statements.get(1)));
        assertFalse(screener.screen(statements.get(0), statements.get(2)));
        assertFalse(screener.screen(statements.get(0), statements.get(3)));

        assertTrue(screener.screen(statements.get(1), statements.get(0)));
        assertTrue(screener.screen(statements.get(1), statements.get(1)));
        assertFalse(screener.screen(statements.get(1), statements.get(2)));
        assertFalse(screener.screen(statements.get(1), statements.get(3)));

        assertTrue(screener.screen(statements.get(2), statements.get(0)));
        assertTrue(screener.screen(statements.get(2), statements.get(1)));
        assertTrue(screener.screen(statements.get(2), statements.get(2)));
        assertTrue(screener.screen(statements.get(2), statements.get(3)));

        assertTrue(screener.screen(statements.get(3), statements.get(0)));
        assertTrue(screener.screen(statements.get(3), statements.get(1)));
        assertTrue(screener.screen(statements.get(3), statements.get(2)));
        assertTrue(screener.screen(statements.get(3), statements.get(3)));
    }

    public void testScreenClassVariablesFromOtherClass() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass1 {
                    int x = 1;
                    int y = 2;
                    int z = 3;
                    
                    public static void foo() {
                        x += y;  // statement 1
                        z = x;  // statement 2
                    }
                }
                
                class MyClass2 {
                    int y = 2;
                
                    public void foo(int x) {
                        y += x;  // statement 3
                    }
                }
                
                class MyClass3 {
                    int y = 2;
                    public static void foo(int x) {
                        x++;  // statement 4
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(0).statements().get(0);
        Statement statement2 = project.sourceFiles().get(0).classes().get(0).statements().get(1);
        Statement statement3 = project.sourceFiles().get(0).classes().get(1).statements().get(0);
        Statement statement4 = project.sourceFiles().get(0).classes().get(2).statements().get(0);

        ReferencedVariableIngredientScreener screener = new ReferencedVariableIngredientScreener(
                IngredientScreeningInfo.create(project, List.of(statement3, statement4), List.of(statement1, statement2, statement4))
        );

        assertTrue(screener.screen(statement3, statement1));
        assertFalse(screener.screen(statement3, statement2));
        assertTrue(screener.screen(statement3, statement4));

        // Cannot use statement 1 at 4 since it would be a static reference
        assertFalse(screener.screen(statement4, statement1));
        assertTrue(screener.screen(statement4, statement4));
    }

    public void testStaticVariableReferences() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass1 {
                    public static int x = 3;
                    
                    public static void foo() {
                        mypkg.MyClass1.x = 1;  // statement 1
                        MyClass1.x = 1;  // statement 2
                        x = 1;  // statement 3
                    }
                }
                
                class MyClass2 {
                    public void foo() {
                        int z = 0;  // statement 4
                    }
                    public static void bar() {
                        int z = 0;  // statement 5
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(0).statements().get(0);
        Statement statement2 = project.sourceFiles().get(0).classes().get(0).statements().get(1);
        Statement statement3 = project.sourceFiles().get(0).classes().get(0).statements().get(2);
        Statement statement4 = project.sourceFiles().get(0).classes().get(1).statements().get(0);
        Statement statement5 = project.sourceFiles().get(0).classes().get(1).statements().get(1);

        ReferencedVariableIngredientScreener screener = new ReferencedVariableIngredientScreener(
                IngredientScreeningInfo.create(project, List.of(statement4, statement5), List.of(statement1, statement2, statement3))
        );

        assertTrue(screener.screen(statement4, statement1));
        assertTrue(screener.screen(statement4, statement2));
        assertFalse(screener.screen(statement4, statement3));

        assertTrue(screener.screen(statement5, statement1));
        assertTrue(screener.screen(statement5, statement2));
        assertFalse(screener.screen(statement5, statement3));
    }

    public void testNestedReferences() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass1 {
                    static int x = 3;
                    int y = 1;
                }
                
                class MyClass2 {
                    static MyClass1 c1 = new MyClass1();
                    MyClass1 c2 = new MyClass1();
                    
                    public void foo() {
                        int x = mypkg.MyClass2.c1.x;
                        x = MyClass2.c1.x;
                        x = c1.x;
                        
                        x = mypkg.MyClass2.c1.y;
                        x = MyClass2.c1.y;
                        x = c1.y;
                        
                        x = c2.x;
                    }
                }
                
                class MyClass3 {
                    static int x = 0;
                
                    public static void bar() {
                        x++;
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        List<Statement> fooStatements = project.sourceFiles().get(0).classes().get(1).statements();
        Statement barStatement = project.sourceFiles().get(0).classes().get(2).statements().get(0);

        ReferencedVariableIngredientScreener screener = new ReferencedVariableIngredientScreener(
                IngredientScreeningInfo.create(project, List.of(barStatement), fooStatements)
        );

        assertTrue(screener.screen(barStatement, fooStatements.get(0)));
        assertTrue(screener.screen(barStatement, fooStatements.get(1)));
        assertFalse(screener.screen(barStatement, fooStatements.get(2)));

        assertTrue(screener.screen(barStatement, fooStatements.get(3)));
        assertTrue(screener.screen(barStatement, fooStatements.get(4)));
        assertFalse(screener.screen(barStatement, fooStatements.get(5)));

        assertFalse(screener.screen(barStatement, fooStatements.get(6)));
    }
}
