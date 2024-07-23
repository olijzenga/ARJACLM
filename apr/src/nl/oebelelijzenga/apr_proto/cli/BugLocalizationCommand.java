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

package nl.oebelelijzenga.apr_proto.cli;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.io.InputLoader;
import nl.oebelelijzenga.apr_proto.io.JavaProjectLoader;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.apr.BugLocation;
import nl.oebelelijzenga.apr_proto.model.apr.ModificationPoint;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.io.AprPreferences;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import nl.oebelelijzenga.apr_proto.parser.ModificationPointFactory;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "localize")
public class BugLocalizationCommand extends AbstractSingleAPRRunCommand implements Callable<Integer> {

    @Option(names = {"-m", "--modification-points"})
    boolean createModificationPoints = true;

    @Override
    public Integer call() throws AprException {
        System.out.println("Localizing bug for " + bugDir);

        AprPreferences preferences = createPreferences();
        InputLoader inputLoader = new InputLoader(preferences);
        inputLoader.load();
        AprConfig input = inputLoader.getConfig();
        JavaContext context = inputLoader.getContext();
        Bug bug = inputLoader.getBug();

        System.out.println("Input");
        System.out.println(input);

        System.out.println("Localization:");
        for (BugLocation location : bug.suspiciousLocations()) {
            System.out.println(location.file() + ":" + location.lineNr() + " susScore=" + location.susScore());
        }

        if (createModificationPoints) {
            JavaProjectLoader projectLoader = new JavaProjectLoader(context);
            JavaProject project = projectLoader.loadJavaProject();
            List<ModificationPoint> modificationPoints = ModificationPointFactory.create(input, project, bug);

            System.out.println("Modification Points:");
            for (ModificationPoint modificationPoint : modificationPoints) {
                System.out.println("\n");
                System.out.println(modificationPoint);
                System.out.println(">> Ingredients:");
                for (Ingredient redundancyIngredient : modificationPoint.redundancyIngredients()) {
                    Statement statement = redundancyIngredient.statements().get(0);
                    System.out.printf(
                            ">> %s:%s   %s\n",
                            ASTUtil.getParentOfType(statement, TypeDeclaration.class).orElseThrow().getName().toString(),
                            ASTUtil.getParentOfType(statement, CompilationUnit.class).orElseThrow().getLineNumber(statement.getStartPosition()),
                            ASTUtil.statementToSingleLine(statement)
                    );
                }
            }
        }

        return 0;
    }
}
