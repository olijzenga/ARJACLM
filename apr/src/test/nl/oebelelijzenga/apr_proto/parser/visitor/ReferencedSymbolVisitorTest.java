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
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedSymbols;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedMethod;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedSymbol;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.ReferencedVariable;
import nl.oebelelijzenga.apr_proto.parser.visitor.ReferencedSymbolsVisitor;
import org.eclipse.jdt.core.dom.*;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

import java.util.List;
import java.util.Optional;

public class ReferencedSymbolVisitorTest extends AprTestCase {

    public void testVisitReferencedSymbols() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    public static int add(int a, int b) {
                        return a+b;
                    }
                            
                    public static int add(int a, int b, int c) {
                        return add(add(a, b),c);
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");

        ReturnStatement returnStatement = (ReturnStatement) javaFile.classes().get(0).statements().get(1);
        assertEquals("return add(add(a,b),c);\n", returnStatement.toString());

        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(returnStatement);
        assertEquals(5, referencedSymbols.size());
        assertEquals(
                List.of(
                        // add(add(a, b),c)
                        new ReferencedMethod(
                                "add",
                                "mypkg.MyClass.add",
                                List.of(
                                        Optional.of(
                                                new ReferencedMethod(
                                                        "add",
                                                        "mypkg.MyClass.add",
                                                        List.of(
                                                                Optional.of(new ReferencedVariable("a", "a")),
                                                                Optional.of(new ReferencedVariable("b", "b"))
                                                        )
                                                )
                                        ),
                                        Optional.of(new ReferencedVariable("c", "c"))
                                )
                        ),
                        // add(a, b)
                        new ReferencedMethod(
                                "add",
                                "mypkg.MyClass.add",
                                List.of(
                                        Optional.of(new ReferencedVariable("a","a")),
                                        Optional.of(new ReferencedVariable("a", "b"))
                                )
                        ),
                        new ReferencedVariable("a", "a"),
                        new ReferencedVariable("b", "b"),
                        new ReferencedVariable("c", "c")
                ),
                referencedSymbols
        );
    }

    public void testMultipleStatements() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    public static int add(int a, int b) {
                        return a+b;
                    }
                            
                    public static int add(int a, int b, int c) {
                        a = b + c;
                        return c;
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass cls = javaFile.classes().get(0);

        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(cls.statements());

        Statement statement1 = cls.statements().get(0);
        assertStatementEquals("return a+b;", statement1);
        assertEquals(2, referencedSymbols.get(statement1).size());
        assertEquals("a", referencedSymbols.get(statement1).get(0).getOriginalReference());
        assertEquals("b", referencedSymbols.get(statement1).get(1).getOriginalReference());

        Statement statement2 = cls.statements().get(1);
        assertStatementEquals("a = b + c;", statement2);
        assertEquals(3, referencedSymbols.get(statement2).size());
        assertEquals("a", referencedSymbols.get(statement2).get(0).getOriginalReference());
        assertEquals("b", referencedSymbols.get(statement2).get(1).getOriginalReference());
        assertEquals("c", referencedSymbols.get(statement2).get(2).getOriginalReference());

        Statement statement3 = cls.statements().get(2);
        assertStatementEquals("return c;", statement3);
        assertEquals(1, referencedSymbols.get(statement3).size());
        assertEquals("c", referencedSymbols.get(statement3).get(0).getOriginalReference());
    }

    public void testReferenceClassProperties() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    public static final int x = 3;
                    private int y;
                    
                    public MyClass(int y) {
                        this.y = y;
                    }
                                
                    public static int getSum(int z) {
                        return x + y + z;
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        ReturnStatement returnStatement = (ReturnStatement) javaFile.classes().get(0).statements().get(1);
        assertEquals("return x + y + z;\n", returnStatement.toString());

        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(returnStatement);

        assertEquals(
                List.of(
                        new ReferencedVariable("x", "mypkg.MyClass.x"),
                        new ReferencedVariable("y", "mypkg.MyClass.y"),
                        new ReferencedVariable("z", "z")
                ),
                referencedSymbols
        );

        assertFalse(((ReferencedVariable) referencedSymbols.get(0)).isLocal());
        assertFalse(((ReferencedVariable) referencedSymbols.get(1)).isLocal());
        assertTrue(((ReferencedVariable) referencedSymbols.get(2)).isLocal());
    }

    public void testGetReferencedMethod() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    public static int add(int a, int b) {
                        return a+b;
                    }
                            
                    public static int add(int a, int b, int c) {
                        return add(MyClass.add(a, b),c);
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(javaFile.classes().get(0).statements().get(1));

        assertEquals(
                List.of(
                    new ReferencedMethod(
                            "MyClass.add",
                            "mypkg.MyClass.add",
                            List.of(
                                    Optional.of(
                                            new ReferencedMethod(
                                                    "MyClass.add",
                                                    "mypkg.MyClass.add",
                                                    List.of(
                                                            Optional.of(new ReferencedVariable("a", "a")),
                                                            Optional.of(new ReferencedVariable("b", "b"))
                                                    )
                                            )
                                    ),
                                    Optional.of(new ReferencedVariable("c", "c"))
                            )
                    ),
                    new ReferencedMethod(
                            "add",
                            "mypkg.MyClass.add",
                            List.of(
                                    Optional.of(new ReferencedVariable("a", "a")),
                                    Optional.of(new ReferencedVariable("b", "b"))
                            )
                    ),
                    new ReferencedVariable("a", "a"),
                    new ReferencedVariable("b", "b"),
                    new ReferencedVariable("c", "c")
                ),
                referencedSymbols
        );
    }

    public void testUnnamedVariableTypes() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    public static int add(int a, int b, int c) {
                        return a + b + c;
                    }
                            
                    public static int foo(int a) {
                        return add(a, a+1, 1);
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(javaFile.classes().get(0).statements().get(1));

        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "add",
                                "mypkg.MyClass.add",
                                List.of(
                                        Optional.of(new ReferencedVariable("a", "a")),
                                        Optional.empty(),
                                        Optional.empty()
                                )
                        ),
                        new ReferencedVariable("a", "a"),
                        new ReferencedVariable("a", "a")
                ),
                referencedSymbols
        );
    }

    public void testGetReferencedClassPropertyParameter() throws AprException {
        String source = """
                package mypkg;
                        
                public class MyClass {
                    private int x;
                    
                    public MyClass(int x) {
                        this.x = x;
                    }
                
                    public static int add(int a, int b) {
                        return a+b;
                    }
                    
                    public int addX(int a) {
                        return this.add(a, this.x);
                    }
                }
                """;

        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(javaFile.classes().get(0).statements().get(2));

        assertEquals(
                List.of(
                        new ReferencedMethod(
                                "add",
                                "mypkg.MyClass.add",
                                List.of(
                                        Optional.of(new ReferencedVariable("a", "a")),
                                        Optional.of(new ReferencedVariable("x", "mypkg.MyClass.x"))
                                )
                        ),
                        new ReferencedVariable("a", "a"),
                        new ReferencedVariable("x", "mypkg.MyClass.x")
                ),
                referencedSymbols
        );
    }

    public void testFieldAccess() throws AprException {
        String source = """
                package mypkg;
                
                public class MyClass1 {
                    public int x;
                }
                
                public class MyClass2 {
                    public MyClass1 c1;
                    public int[] arr;
                    
                    public void foo() {
                        this.c1.x++;
                        return this.arr.length;
                    }
                }
                """;

        JavaProject project = TestUtil.getParsedJavaFileAsProject(source, "MyClass.java");
        Statement statement1 = project.sourceFiles().get(0).classes().get(1).statements().get(0);
        Statement statement2 = project.sourceFiles().get(0).classes().get(1).statements().get(1);

        List<ReferencedSymbol> referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(statement1);
        assertEquals(1, referencedSymbols.size());
        assertEquals(new ReferencedVariable("c1.x", "mypkg.MyClass1.x"), referencedSymbols.get(0));

        referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(statement2);
        assertEquals(1, referencedSymbols.size());
        assertEquals(new ReferencedVariable("arr.length", "array.length"), referencedSymbols.get(0));
    }
}
