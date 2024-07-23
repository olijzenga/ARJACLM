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

package test.nl.oebelelijzenga.apr_proto.parser.visitor;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedMethod;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedSymbol;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedSymbols;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedVariable;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.visitor.ReferencedSymbolsVisitor;
import org.eclipse.jdt.core.dom.Statement;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReferencedSymbolsVisitorTest extends AprTestCase {

    public void testGetStatementInfo() throws AprException {
        String source = """
                package mypkg;

                public class MyClass {
                    private String myField;
                    
                    public MyClass(String myField) {
                        this.myField = myField;
                    }
                    
                    protected boolean eq(String other) {
                        int x = 3;
                        return myField.equals(other);
                    }
                }
                        """;


        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass = javaFile.classes().get(0);
        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass.statements());

        Statement statement = myClass.statements().get(2);
        assertEquals("return myField.equals(other);\n", statement.toString());
        List<ReferencedSymbol> symbols = referencedSymbols.get(statement);

        assertEquals(3, symbols.size());
        assertEquals(
                new ReferencedVariable("myField", "mypkg.MyClass.myField"),
                symbols.get(0)
        );
        assertEquals(
                new ReferencedMethod(
                        "myField",
                        "java.lang.String.equals",
                        List.of(
                                Optional.of(new ReferencedVariable("other", "other"))
                        )
                ),
                symbols.get(1)
        );
        assertEquals(
                new ReferencedVariable("myField", "other"),
                symbols.get(2)
        );
    }

    public void testOriginalReference() throws AprException {
        String source = """
                package mypkg;
                
                import java.util.List;

                public class MyClass {
                    private static final String staticField = "static";
                    private String myField = "";
              
                    protected void foo(String x) {
                        this.myField = x;
                        myField += " ";
                        staticField += " ";
                        MyClass.staticField += " ";
                        List.of("hello");
                        bar(MyClass.staticField);
                        MyClass.bar(staticField);
                        MyClass.bar(this.myField);
                    }
                    
                    public static void bar(String x) {}
                }
                        """;


        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass = javaFile.classes().get(0);
        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass.statements());

        assertEquals(8, myClass.statements().size());

        // this.myField = x;
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "myField",
                                "mypkg.MyClass.myField"
                        ),
                        new ReferencedVariable(
                                "x",
                                "x"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(0))
        );

        // myField += " ";
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "myField",
                                "mypkg.MyClass.myField"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(1))
        );

        // staticField += " ";
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "staticField",
                                "mypkg.MyClass.staticField"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(2))
        );

        // MyClass.staticField += " "
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "MyClass.staticField",
                                "mypkg.MyClass.staticField"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(3))
        );

        // List.of("hello");
        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "List.of",
                                "java.util.List.of",
                                List.of(Optional.empty())
                        )
                ),
                referencedSymbols.get(myClass.statements().get(4))
        );

        // bar(MyClass.staticField)
        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "bar",
                                "mypkg.MyClass.bar",
                                List.of(Optional.of(new ReferencedVariable("MyClass.staticField", "mypkg.MyClass.staticField")))
                        ),
                        new ReferencedVariable("MyClass.staticField", "mypkg.MyClass.staticField")
                ),
                referencedSymbols.get(myClass.statements().get(5))
        );

        // MyClass.bar(staticField);
        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "MyClass.bar",
                                "mypkg.MyClass.bar",
                                List.of(Optional.of(new ReferencedVariable("staticField", "mypkg.MyClass.staticField")))
                        ),
                        new ReferencedVariable("staticField", "mypkg.MyClass.staticField")
                ),
                referencedSymbols.get(myClass.statements().get(6))
        );

        // MyClass.bar(this.myField);
        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "MyClass.bar",
                                "mypkg.MyClass.bar",
                                List.of(Optional.of(new ReferencedVariable("myField", "mypkg.MyClass.myField")))
                        ),
                        new ReferencedVariable("myField", "mypkg.MyClass.myField")
                ),
                referencedSymbols.get(myClass.statements().get(7))
        );
    }

    public void testPublicSymbolAccess() throws AprException {
        String source = """
                package mypkg;
                
                public class MyClass1 {
                    public int x = 1;
                    
                    public int y() {
                        return 2;
                    }
                }

                public class MyClass2 {
                    public MyClass1 c1;
                
                    public MyClass2() {
                        c1 = new MyClass1();            // statement 0
                    }
                
                    protected void foo(String x) {
                        int x = c1.x;                   // statement 1
                        MyClass1 c2 = new MyClass1();
                        int y = c2.y();
                    }
                }
                
                public class MyClass3 {
                    public static void foo() {
                        MyClass2 c2 = new MyClass2();   // statement 4
                        int x = c2.c1.x;
                        int y = c2.c1.y();
                    }
                }
                        """;


        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass2 = javaFile.classes().get(1);
        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass2.statements());

        // c1 = new MyClass1();
        assertEquals(
                List.of(
                        new ReferencedVariable(
                            "c1",
                            "mypkg.MyClass2.c1"
                        ),
                        new ReferencedMethod(
                                "MyClass1",
                                "mypkg.MyClass1.<init>",
                                new ArrayList<>()
                        )
                ),
                referencedSymbols.get(myClass2.statements().get(0))
        );

        // int x = c1.x;
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "c1",
                                "mypkg.MyClass2.c1"
                        ),
                        new ReferencedVariable(
                                "c1.x",
                                "mypkg.MyClass1.x"
                        )
                ),
                referencedSymbols.get(myClass2.statements().get(1))
        );

        // MyClass1 c2 = new MyClass2();
        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "MyClass1",
                                "mypkg.MyClass1.<init>",
                                new ArrayList<>()
                        )
                ),
                referencedSymbols.get(myClass2.statements().get(2))
        );

        // int y = c2.y();
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "c2",
                                "c2"
                        ),
                        new ReferencedMethod(
                                "MyClass1",
                                "mypkg.MyClass1.y",
                                new ArrayList<>()
                        )
                ),
                referencedSymbols.get(myClass2.statements().get(3))
        );

        // Check MyClass3
        JavaClass myClass3 = javaFile.classes().get(2);
        referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass3.statements());

        // int x = c2.c1.x;
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "c2",
                                "c2"
                        ),
                        new ReferencedVariable(
                                "c2.c1",
                                "mypkg.MyClass2.c1"
                        ),
                        new ReferencedVariable(
                                "c2.c1.x",
                                "mypkg.MyClass1.x"
                        )
                ),
                referencedSymbols.get(myClass3.statements().get(1))
        );

        // int y = c2.c1.y();
        assertEquals(
                List.of(
                        new ReferencedVariable(
                                "c2",
                                "c2"
                        ),
                        new ReferencedVariable(
                                "c2.c1",
                                "mypkg.MyClass2.c1"
                        ),
                        new ReferencedMethod(
                                "c2.c1.y",
                                "mypkg.MyClass1.y",
                                new ArrayList<>()
                        )
                ),
                referencedSymbols.get(myClass3.statements().get(2))
        );
    }

    public void testComplexArgumentReference() throws AprException {
        String source = """
                package mypkg;
                
                class MyClass {
                    public static int incr(int x) {
                        return x+1;
                    }
                    
                    public static void foo() {
                        int y = 0;
                        incr(y*2);
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass = javaFile.classes().get(0);
        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass.statements());

        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "incr",
                                "mypkg.MyClass.incr",
                                List.of(Optional.empty())  // Cannot resolve complex reference with * operator
                        ),
                        // The variable referenced inside the complex argument should still found however
                        new ReferencedVariable(
                                "y",
                                "y"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(2))
        );
    }

    public void testConstructorArguments() throws AprException {
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

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass = javaFile.classes().get(1);
        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(myClass.statements());

        assertEquals(
                List.of(
                        new ReferencedMethod(
                             "Cleaner",
                             "org.jsoup.Cleaner.<init>",
                             List.of(
                                     Optional.of(new ReferencedVariable(
                                             "whitelist",
                                             "whitelist"
                                     ))
                             )
                        ),
                        new ReferencedVariable(
                                "whitelist",
                                "whitelist"
                        )
                ),
                referencedSymbols.get(myClass.statements().get(1))
        );
    }
}
