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
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.*;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import nl.oebelelijzenga.apr_proto.parser.visitor.DeclaredClassSymbolsVisitor;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

public class DeclaredClassSymbolsVisitorTest extends AprTestCase {

    public void testGetClassAndMethodInfo() {
        String source = """
        package mypkg;
        
        public class MyClass {
            private String myField;
            
            public MyClass(String myField) {
                int x = 0;
                this.myField = myField;
            }
            
            protected boolean eq(String other) {
                return myField.equals(other);
            }
            
            private static int len(String str) {
                return str.length;
            }
        }
        """;


        ASTParser parser = ASTUtil.createSingleFileParser("MyClass.java");
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        DeclaredClassSymbols classSymbols = DeclaredClassSymbolsVisitor.getClassSymbols(cu);
        assertEquals(1, classSymbols.classes().size());
        DeclaredClass declaredClass = classSymbols.classes().get(0);

        assertEquals("mypkg.MyClass", declaredClass.getQualifiedName().toString());

        assertEquals(1, classSymbols.fields().get(declaredClass).size());
        DeclaredVariable field = classSymbols.fields().get(declaredClass).get(0);
        assertEquals("mypkg.MyClass.myField", field.getQualifiedName().toString());
        assertEquals("java.lang.String", field.getQualifiedTypeName().toString());
        assertEquals(SymbolVisibility.PRIVATE, field.getVisibility());

        // Note that the constructor is excluded
        assertEquals(3, classSymbols.methods().get(declaredClass).size());

        DeclaredMethod method0 = classSymbols.methods().get(declaredClass).get(0);
        assertEquals("mypkg.MyClass.<init>", method0.getQualifiedName().toString());
        assertEquals("void", method0.returnTypeName().toString());
        assertEquals(SymbolVisibility.PUBLIC, method0.getVisibility());
        assertFalse(method0.isStatic());
        assertTrue(method0.isConstructor());
        assertEquals(1, method0.parameters().size());
        assertEquals("java.lang.String", method0.parameters().get(0).getQualifiedTypeName().toString());
        assertEquals("myField", method0.parameters().get(0).getQualifiedName().toString());
        assertEquals(SymbolVisibility.LOCAL, method0.parameters().get(0).getVisibility());
        assertEquals(2, method0.statements().size());

        DeclaredMethod method1 = classSymbols.methods().get(declaredClass).get(1);
        assertEquals("mypkg.MyClass.eq", method1.getQualifiedName().toString());
        assertEquals("boolean", method1.returnTypeName().toString());
        assertEquals(SymbolVisibility.PROTECTED, method1.getVisibility());
        assertFalse(method1.isStatic());
        assertFalse(method1.isConstructor());
        assertEquals(1, method1.parameters().size());
        assertEquals("java.lang.String", method1.parameters().get(0).getQualifiedTypeName().toString());
        assertEquals("other", method1.parameters().get(0).getQualifiedName().toString());
        assertEquals(SymbolVisibility.LOCAL, method1.parameters().get(0).getVisibility());
        assertEquals(1, method1.statements().size());

        DeclaredMethod method2 = classSymbols.methods().get(declaredClass).get(2);
        assertEquals("mypkg.MyClass.len", method2.getQualifiedName().toString());
        assertEquals("int", method2.returnTypeName().toString());
        assertEquals(SymbolVisibility.PRIVATE, method2.getVisibility());
        assertTrue(method2.isStatic());
        assertFalse(method2.isConstructor());
        assertEquals(1, method2.parameters().size());
        assertEquals("java.lang.String", method2.parameters().get(0).getQualifiedTypeName().toString());
        assertEquals("str", method2.parameters().get(0).getQualifiedName().toString());
        assertEquals(SymbolVisibility.LOCAL, method2.parameters().get(0).getVisibility());
    }

    public void testMethodThrowsExceptions() throws AprException {
        String source = """
                package mypkg;
                
                public class MyException extends Exception {}
                
                public class MyClass {
                    public void foo() throws MyException, RuntimeException {}
                }
                        """;


        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        DeclaredClassSymbols classSymbols = DeclaredClassSymbolsVisitor.getClassSymbols(javaFile.compilationUnit());
        DeclaredClass declaredClass = classSymbols.classes().get(1);

        DeclaredMethod method = classSymbols.methods().get(declaredClass).get(0);
        assertEquals(2, method.getThrowsExceptionTypes().size());
        assertEquals(new QualifiedName("mypkg", "MyException"), method.getThrowsExceptionTypes().get(0));
        assertEquals(new QualifiedName("java", "lang", "RuntimeException"), method.getThrowsExceptionTypes().get(1));
    }
}
