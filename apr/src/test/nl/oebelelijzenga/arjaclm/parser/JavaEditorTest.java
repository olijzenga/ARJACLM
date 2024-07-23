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

package test.nl.oebelelijzenga.arjaclm.parser;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.model.apr.ModificationPoint;
import nl.oebelelijzenga.arjaclm.model.apr.genetic.Edit;
import nl.oebelelijzenga.arjaclm.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import nl.oebelelijzenga.arjaclm.model.java.RawJavaFile;
import nl.oebelelijzenga.arjaclm.parser.JavaEditor;
import nl.oebelelijzenga.arjaclm.parser.manipulation.ManipulationName;
import test.nl.oebelelijzenga.arjaclm.TestUtil;

import java.util.ArrayList;
import java.util.List;

public class JavaEditorTest extends AprTestCase {

    public void testEditJava() throws AprException {
        String sourceCode = """
        package packageSimpleExample;
                
        public class SimpleExample {
            public int mid(int x, int y, int z){
                int ret;
                ret = z;
                if(y<z){
                   if(x<y){
                        ret = y;
                   } else if(x<z){
                        ret = z; // bug, it should be ret=x;
                   }
                }else{
                   if(x>y){
                        ret = y;
                   }else if(x>z){
                        ret = x;
                   }
                }
                return ret;
            }
        }
        """;

        ParsedJavaFile parsedJavaFile = TestUtil.getParsedJavaFile(sourceCode, "MyClass.java");

        Edit edit = new Edit(
            true,
                ManipulationName.REPLACE,
                new ModificationPoint(
                    0,
                    // Second occurrence of "ret = z"
                    parsedJavaFile.classes().get(0).statements().get(6),
                    parsedJavaFile,
                    parsedJavaFile.classes().get(0),
                    1.0f,
                    new ArrayList<>(),
                    ManipulationName.defaultEnabledManipulations()
                ),
                // Use "ret = y" as ingredient
                new Ingredient(parsedJavaFile.classes().get(0).statements().get(4), true)
        );

        List<RawJavaFile> editedFiles = JavaEditor.getEditedSourceFiles(List.of(edit));
        assertEquals(1, editedFiles.size());

        assertSourceCodeEquals(
                """
                package packageSimpleExample;
                                        
                public class SimpleExample {
                    public int mid(int x, int y, int z){
                        int ret;
                        ret = z;
                        if(y<z){
                           if(x<y){
                                ret = y;
                           } else if(x<z){
                                ret = y;
                           }
                        }else{
                           if(x>y){
                                ret = y;
                           }else if(x>z){
                                ret = x;
                           }
                        }
                        return ret;
                    }
                }
                """.strip(),
               editedFiles.get(0).sourceCode().strip()
        );
    }

    /*
     * Replacing an if-statement with another should only replace their conditions
     */
    public void testEditIfStatement() throws AprException {
        String sourceCode = """
        package mypkg;
                
        public class MyClass {
            public int foo() {
                int a = 0;
                if (x > 0) {
                    return a;
                }
                return x;
            }
            
            public int bar() {
                int b = 0;
                if (x >= 0) {
                    return b;
                }
                return x;
            }
        }
        """;

        ParsedJavaFile parsedJavaFile = TestUtil.getParsedJavaFile(sourceCode, "MyClass.java");

        // Replaces the first if statement with the second
        Edit edit = new Edit(
                true,
                ManipulationName.REPLACE,
                new ModificationPoint(
                        0,
                        parsedJavaFile.classes().get(0).statements().get(1),
                        parsedJavaFile,
                        parsedJavaFile.classes().get(0),
                        1.0f,
                        new ArrayList<>(),
                        ManipulationName.defaultEnabledManipulations()
                ),
                new Ingredient(parsedJavaFile.classes().get(0).statements().get(5), true)
        );

        List<RawJavaFile> editedFiles = JavaEditor.getEditedSourceFiles(List.of(edit));
        assertEquals(1, editedFiles.size());

        assertEquals(
                """
                package mypkg;
                        
                public class MyClass {
                    public int foo() {
                        int a = 0;
                        if (x >= 0) {
                            return a;
                        }
                        return x;
                    }
                    
                    public int bar() {
                        int b = 0;
                        if (x >= 0) {
                            return b;
                        }
                        return x;
                    }
                }
                """.strip(),
                editedFiles.get(0).sourceCode().strip()
        );
    }
}
