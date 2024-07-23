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

import junit.framework.TestCase;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.parser.ingredient.ReferencedMethodIngredientScreener;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.IngredientScreeningInfo;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import test.nl.oebelelijzenga.apr_proto.TestUtil;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;

import java.util.List;

public class ReferencedMethodIngredientScreenerTest extends AprTestCase {
    public void testSimpleMethodCall() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass1 {
                    static void foo() {}
                    void bar() {}
                    
                    void fn() {
                        foo();  // statement 1
                        MyClass1.foo();  // statement 2
                        bar();  // statement 3
                    }
                }
                
                class MyClass2 {
                    static void foo() {
                        int x = 1; // statement 4
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(0).statements().get(0);
        Statement statement2 = project.sourceFiles().get(0).classes().get(0).statements().get(1);
        Statement statement3 = project.sourceFiles().get(0).classes().get(0).statements().get(2);
        Statement statement4 = project.sourceFiles().get(0).classes().get(1).statements().get(0);

        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, List.of(statement4), List.of(statement1, statement2, statement3))
        );

        assertTrue(screener.screen(statement4, statement1));
        assertTrue(screener.screen(statement4, statement2));
        assertFalse(screener.screen(statement4, statement3));
    }

    public void testMethodCallWithArguments() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass1 {
                    static void foo(int x, String y) {}
                    static void foo(int x)
                
                    static void bar() {
                        int x, z = 3, 1;
                        String y = "Hello world";
                        MyClass1.foo(x, y);     // statement 1
                        MyClass1.foo(x);        // statement 2
                        MyClass1.foo(z);        // statement 3
                    }
                }
                
                class MyClass2 {
                    static void myfunction() {
                        int x = 1;
                        String y = "test";
                        x++;                    // statement 4
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(0).statements().get(2);
        Statement statement2 = project.sourceFiles().get(0).classes().get(0).statements().get(3);
        Statement statement3 = project.sourceFiles().get(0).classes().get(0).statements().get(4);
        Statement statement4 = project.sourceFiles().get(0).classes().get(1).statements().get(2);

        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, List.of(statement4), List.of(statement1, statement2, statement3))
        );

        assertTrue(screener.screen(statement4, statement1));
        assertTrue(screener.screen(statement4, statement2));
        assertFalse(screener.screen(statement4, statement3));
    }

    public void testChart7Ingredient() throws AprException {
        String source = """
            package org.jfree.data.time;
            
            import java.util.Date;
            import java.util.List;
            
            interface TimePeriod {
                public Date getStart();
                public Date getEnd();
            }
            
            class TimePeriodValue {
                private TimePeriod period;
                
                public TimePeriod getPeriod() {
                    return this.period;
                }
            }
           
            class TimePeriodValues {
                private List data;
                private int minMiddleIndex;
                
                public TimePeriodValue getDataItem(int index) {
                    return (TimePeriodValue) this.data.get(index);
                }
                
                public void foo() {
                    long s = getDataItem(this.minMiddleIndex).getPeriod().getStart().getTime();  // statement 1
                    long e = getDataItem(this.minMiddleIndex).getPeriod().getEnd().getTime();    // statement 2
                }
            }
        """;


        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "TimePeriod.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(2).statements().get(1);
        Statement statement2 = project.sourceFiles().get(0).classes().get(2).statements().get(2);

        List<Statement> statements = List.of(statement1, statement2);
        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertTrue(statement1 instanceof VariableDeclarationStatement);
        assertTrue(statement2 instanceof VariableDeclarationStatement);
        assertTrue(screener.screen(statement1, statement2));
        assertTrue(screener.screen(statement2, statement1));
    }

    public void testMath1Ingredient() throws AprException {
        String source = """
            package org.apache.commons.math3.exception.util;
            
            import java.io.Serializable;
            
            public interface Localizable extends Serializable {
                String getSourceString();
            }
            
            public class ExceptionContext {
                public void addMessage(Localizable pattern, Object... arguments) {
                }
            }
            
            public class MathIllegalStateException {
                private final ExceptionContext context;
                
                public ExceptionContext getContext() {
                    return context;
                }
            }
            
            public class ConvergenceException extends MathIllegalStateException {
                public ConvergenceException(Localizable pattern, Object... args) {
                    getContext().addMessage(pattern, args);  // statement 1
                }
            }
            
            public class OtherContext {
                public void addMessage(Localizable pattern, Object... arguments) {
                }
            }
            
            public class IngredientClass {
                public void foo(Localizable pattern, Object... args) {
                    getContext().addMessage(pattern, args);  // statement 2
                }
                
                public OtherContext getContext() {
                    return null;
                }
            }
        """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "ConvergenceException.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(3).statements().get(0);
        Statement statement2 = project.sourceFiles().get(0).classes().get(5).statements().get(0);

        List<Statement> statements = List.of(statement1, statement2);
        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertStatementEquals("getContext().addMessage(pattern, args);", statement1);
        assertStatementEquals("getContext().addMessage(pattern, args);", statement2);
        assertTrue(screener.screen(statement1, statement2));
    }

    public void testJsoup1Ingredient() throws AprException {
        String source = """
        package org.jsoup.nodes;
        
        public class TextNode {
            static boolean lastCharIsWhitespace(StringBuilder sb) {
                if (sb.length() == 0)
                    return false;
                String lastChar = sb.substring(sb.length()-1, sb.length());
                return lastChar.equals(" ");
            }
        }
        """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "TextNode.java");
        Statement statement = project.sourceFiles().get(0).classes().get(0).statements().get(2);
        assertStatementEquals("String lastChar = sb.substring(sb.length()-1, sb.length());", statement);

        List<Statement> statements = List.of(statement);
        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertTrue(screener.screen(statement, statement));
    }

    public void testJsoup30Ingredient() throws AprException {
        String source = """
        package org.jsoup;
        
        public class Cleaner {
            public Cleaner(String whitelist) {
            }
        }
        
        public class MyClass {
            public static void foo() {
                String whitelist = "abcd";
                Cleaner cleaner = new Cleaner(whitelist);
            }
        }
        """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "Cleaner.java");
        Statement statement = project.sourceFiles().get(0).classes().get(1).statements().get(1);
        assertStatementEquals("Cleaner cleaner = new Cleaner(whitelist);", statement);

        List<Statement> statements = List.of(statement);
        ReferencedMethodIngredientScreener screener = new ReferencedMethodIngredientScreener(
                IngredientScreeningInfo.create(project, statements, statements)
        );

        assertTrue(screener.screen(statement, statement));
    }
}
