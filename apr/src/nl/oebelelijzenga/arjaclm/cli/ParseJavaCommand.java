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

package nl.oebelelijzenga.arjaclm.cli;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.io.InputLoader;
import nl.oebelelijzenga.arjaclm.io.JavaProjectLoader;
import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import nl.oebelelijzenga.arjaclm.model.java.JavaClass;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import nl.oebelelijzenga.arjaclm.model.java.JavaProject;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import org.eclipse.jdt.core.dom.Statement;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "parse_java")
public class ParseJavaCommand extends AbstractSingleAPRRunCommand implements Callable<Integer> {

    @Override
    public Integer call() throws AprException {
        System.out.println("Localizing bug for " + bugDir);

        AprPreferences preferences = createPreferences();
        InputLoader inputLoader = new InputLoader(preferences);
        inputLoader.load();
        JavaContext context = inputLoader.getContext();

        System.out.println("Context:");
        System.out.println(context);

        JavaProjectLoader projectLoader = new JavaProjectLoader(context);
        JavaProject project = projectLoader.loadJavaProject();
        System.out.println("Parser variants:");
        for (ParsedJavaFile file : project.sourceFiles()) {
            System.out.println("===== File " + file.relativeFilePath());
            for (JavaClass cls : file.classes()) {
                System.out.println("Class " + cls.getFullName());

                System.out.println("> Statements:");
                for (Statement statement : cls.statements()) {
                    System.out.println(statement.getClass().getName());
                    System.out.println(statement);
                }
            }
        }

        return 0;
    }
}
