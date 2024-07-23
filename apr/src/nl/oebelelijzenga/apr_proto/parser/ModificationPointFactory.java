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

package nl.oebelelijzenga.apr_proto.parser;

import nl.oebelelijzenga.apr_proto.NumberUtil;
import nl.oebelelijzenga.apr_proto.ThreadUtil;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.apr.BugLocation;
import nl.oebelelijzenga.apr_proto.model.apr.ModificationPoint;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.IngredientScreeningInfo;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.ingredient.IngredientScreenerCollection;
import nl.oebelelijzenga.apr_proto.parser.ingredient.IngredientUtil;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;
import nl.oebelelijzenga.apr_proto.parser.visitor.BindingCheckerVisitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.stream.Stream;

public class ModificationPointFactory {

    private static final Logger logger = LogManager.getLogger(ModificationPointFactory.class);

    private final AprConfig config;
    private final JavaProject project;
    private final Bug bug;

    private ModificationPointFactory(AprConfig config, JavaProject project, Bug bug) {
        this.config = config;
        this.project = project;
        this.bug = bug;
    }

    public static List<ModificationPoint> create(AprConfig input, JavaProject project, Bug bug) throws AprException {
        return (new ModificationPointFactory(input, project, bug)).createModificationPoints();
    }

    private List<ModificationPoint> createModificationPoints() throws AprException {
        logger.info("Creating modification points");
        long startTime = System.currentTimeMillis();

        List<BugLocation> sortedBugLocations = bug.suspiciousLocations().stream()
                .filter(l -> l.susScore() >= this.config.modificationPointSuspiciousnessThreshold())
                .sorted(Comparator.comparing(BugLocation::susScore).reversed())
                .toList();

        List<BugLocationInProject> bugLocationsInProject = new ArrayList<>();
        for (BugLocation bugLocation : sortedBugLocations) {
            Optional<BugLocationInProject> bugLocationInProject = getBugLocationInProject(bugLocation);
            if (bugLocationInProject.isPresent()) {
                bugLocationsInProject.add(bugLocationInProject.get());

                if (bugLocationsInProject.size() == config.maxNrModificationPoints()) {
                    break;
                }
            }
        }

        // Obtain list of filtered but locations, but in their original order of appearance as it is slightly more intuitive for debugging
        List<BugLocationInProject> bugLocations = bugLocationsInProject.stream()
                .sorted(Comparator.comparing(l -> bug.suspiciousLocations().indexOf(l.location)))
                .toList();

        List<ModificationPoint> result = new ArrayList<>();
        for (BugLocationInProject l : bugLocations) {
            ModificationPoint modificationPoint = makeModificationPoint(l.file, l.cls, l.statement, l.location, bugLocations.indexOf(l));
            if (!modificationPoint.redundancyIngredients().isEmpty()) {
                // Silently drop modification points which have no ingredients since at least one ingredient is needed for genetic search
                result.add(modificationPoint);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Creating modification points took %.1f seconds".formatted((float) (endTime - startTime) / 1000));

        return result;
    }

    private record BugLocationInProject(
            ParsedJavaFile file,
            JavaClass cls,
            Statement statement,
            BugLocation location
    ) {}

    private Optional<BugLocationInProject> getBugLocationInProject(BugLocation location) throws AprException {
        for (ParsedJavaFile file : project.sourceFiles()) {
            if (!FileUtil.pathEquals(location.file(), project.context().rootDir().resolve(file.relativeFilePath()))) {
                continue;
            }

            for (JavaClass cls : file.classes()) {
                for (Statement statement : cls.statements()) {
                    if (cls.compilationUnit().getLineNumber(statement.getStartPosition()) == location.lineNr()) {
                        return Optional.of(new BugLocationInProject(file, cls, statement, location));
                    }
                }
            }

            logger.warn("Failed to match %s to a statement in the file".formatted(location));
            return Optional.empty();
        }

        throw new AprException("Failed to match buggy line info %s to a Java source file".formatted(location));
    }

    private ModificationPoint makeModificationPoint(ParsedJavaFile file, JavaClass cls, Statement statement, BugLocation location, int index) throws AprException {
        List<Statement> candidateIngredients = getCandidateIngredientStatementsForStatementInClass(cls);
        checkIngredientCandidateASTs(candidateIngredients);
        IngredientScreeningInfo ingredientScreeningInfo = IngredientScreeningInfo.create(project, List.of(statement), candidateIngredients);

        List<Statement> filteredIngredients = getFilteredIngredientStatements(statement, candidateIngredients, ingredientScreeningInfo);

        ModificationPoint modificationPoint = new ModificationPoint(
                index,
                statement,
                file,
                cls,
                NumberUtil.round(location.susScore(), 3),
                filteredIngredients.stream().map(i -> new Ingredient(i, true)).toList(),
                getAllowedManipulationsForStatement(statement, ingredientScreeningInfo)
        );

        if (filteredIngredients.isEmpty()) {
            logger.warn("Could not find ingredients for modification point " + modificationPoint);
        }
        return modificationPoint;
    }

    private void checkIngredientCandidateASTs(List<Statement> ingredientCandidates) throws AprException {
        Set<CompilationUnit> compilationUnits = new HashSet<>(ingredientCandidates.stream().map(ASTUtil::getCompilationUnitForNode).toList());
        List<BindingCheckerVisitor.UnresolvedBinding> allUnresolvedBindings = new ArrayList<>();
        for (CompilationUnit compilationUnit : compilationUnits) {
            List<BindingCheckerVisitor.UnresolvedBinding> unresolvedBindings = BindingCheckerVisitor.getUnresolvedBindings(compilationUnit);
            allUnresolvedBindings.addAll(unresolvedBindings);

            for (BindingCheckerVisitor.UnresolvedBinding unresolvedBinding : unresolvedBindings) {
                logger.warn("Found unresolved binding " + unresolvedBinding.toString());
            }
        }

        if (!allUnresolvedBindings.isEmpty()) {
            throw new AprException("Encountered %d unresolved bindings in compilation units for ingredient candidates".formatted(allUnresolvedBindings.size()));
        }
    }

    private List<Statement> getCandidateIngredientStatementsForStatementInClass(JavaClass javaClass) {
        List<Statement> ingredients = new ArrayList<>();
        for (ParsedJavaFile javaFile : project.sourceFiles()) {
            for (JavaClass cls : javaFile.classes()) {
                if (cls.packageName().equals(javaClass.packageName())) {
                    ingredients.addAll(cls.statements());
                }
            }
        }
        return ingredients;
    }

    private List<Statement> getFilteredIngredientStatements(Statement statement, List<Statement> ingredients, IngredientScreeningInfo ingredientScreeningInfo) throws AprException {
        List<Statement> allowedIngredients = new ArrayList<>();
        for (Statement ingredient : ingredients) {
            if (IngredientScreenerCollection.screen(statement, ingredient, config, ingredientScreeningInfo)) {
                allowedIngredients.add(ingredient);
            }
        }
        return allowedIngredients;
    }

    /**
     * Implements operation filter rules from table 1 of the ARJA paper
     */
    private List<ManipulationName> getAllowedManipulationsForStatement(Statement statement, IngredientScreeningInfo ingredientScreeningInfo) throws AprException {
        List<ManipulationName> manipulations = new ArrayList<>(ManipulationName.defaultEnabledManipulations());

        // Rule 1
        if (statement instanceof VariableDeclarationStatement) {
            manipulations.remove(ManipulationName.DELETE);
            return manipulations;
        }

        // Rule 2
        if (
                (statement instanceof ReturnStatement || statement instanceof ThrowStatement) &&
                        IngredientUtil.statementIsLastInMethod(statement) &&
                        !ingredientScreeningInfo.getStatementMethodDeclaration(statement).returnTypeName().equals(new QualifiedName("void"))
        ) {
            manipulations.remove(ManipulationName.DELETE);
            return manipulations;
        }

        return manipulations;
    }
}
