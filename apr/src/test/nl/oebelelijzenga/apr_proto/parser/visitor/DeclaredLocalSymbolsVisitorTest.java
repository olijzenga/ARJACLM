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
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.DeclaredLocalSymbols;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.DeclaredSymbol;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.visitor.DeclaredLocalSymbolsVisitor;
import org.eclipse.jdt.core.dom.Statement;
import test.nl.oebelelijzenga.apr_proto.parser.AprTestCase;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

import java.util.List;

public class DeclaredLocalSymbolsVisitorTest extends AprTestCase {

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
                        if (x == 3) {
                            int y = 1;
                            y++;
                        } else {
                            return true;
                        }
                        return myField.equals(other);
                    }
                }
                """;


        ParsedJavaFile javaFile = TestUtil.getParsedJavaFile(source, "MyClass.java");
        JavaClass myClass = javaFile.classes().get(0);

        DeclaredLocalSymbols declaredLocalSymbols = DeclaredLocalSymbolsVisitor.getLocalSymbols(myClass.statements());

        // Declaration statements themselves should not have their own declaration listed
        Statement statement = myClass.statements().get(3);
        assertStatementEquals("int y = 1;", statement);
        List<DeclaredSymbol> localSymbols = declaredLocalSymbols.get(statement);
        assertNotNull(localSymbols);

        assertEquals(1, localSymbols.size());
        assertEquals("x", localSymbols.get(0).getQualifiedName().toString());
        assertEquals("int", localSymbols.get(0).getOutputTypeBinding().getQualifiedName());

        // Statements in the then-block should inherit declared variables from the upper scope (method) as well
        statement = myClass.statements().get(4);
        assertStatementEquals("y++;", statement);
        localSymbols = declaredLocalSymbols.get(statement);
        assertNotNull(localSymbols);

        assertEquals(2, localSymbols.size());
        assertEquals("x", localSymbols.get(0).getQualifiedName().toString());
        assertEquals("int", localSymbols.get(0).getOutputTypeBinding().getQualifiedName());
        assertEquals("y", localSymbols.get(1).getQualifiedName().toString());
        assertEquals("int", localSymbols.get(1).getOutputTypeBinding().getQualifiedName());

        // The else-block should also inherit from upper scope but not from the then-block
        statement = myClass.statements().get(5);
        assertStatementEquals("return true;", statement);
        localSymbols = declaredLocalSymbols.get(statement);
        assertNotNull(localSymbols);

        assertEquals(1, localSymbols.size());
        assertEquals("x", localSymbols.get(0).getQualifiedName().toString());

        // The return statement should also not inherit any symbols from the if-statement
        statement = myClass.statements().get(6);
        assertStatementEquals("return myField.equals(other);", statement);
        localSymbols = declaredLocalSymbols.get(statement);
        assertNotNull(localSymbols);

        assertEquals(1, localSymbols.size());
        assertEquals("x", localSymbols.get(0).getQualifiedName().toString());
    }
}
