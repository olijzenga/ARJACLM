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

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import test.nl.oebelelijzenga.apr_proto.TestUtil;

public class JavaParserTest extends AprTestCase {
    public void testSkipAnonymousClassDeclarationContent() throws AprException {
        String source = """
        package mypkg;
        
        class MyClass {
            public static void foo() {
                Runnable r = new Runnable() {
                     @Override
                     public void run() {
                         int x = 0;
                     }
                 }
            }
        }
        """;

        ParsedJavaFile file = TestUtil.getParsedJavaFile(source, "MyClass.java");
        assertEquals(1, file.classes().get(0).statements().size());
        assertTrue(file.classes().get(0).statements().get(0).toString().startsWith("Runnable r="));
    }
}
