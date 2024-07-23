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

package nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.java.JavaProject;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.visitor.DeclaredClassSymbolsVisitor;
import nl.oebelelijzenga.apr_proto.parser.visitor.DeclaredLocalSymbolsVisitor;
import nl.oebelelijzenga.apr_proto.parser.visitor.ReferencedSymbolsVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import java.util.*;

public class IngredientScreeningInfo {

    private final ReferencedSymbols referencedSymbols;
    private final Map<QualifiedName, DeclaredClass> classes = new HashMap<>();
    private final Map<Statement, DeclaredClass> statementClasses = new HashMap<>();
    private final Map<Statement, DeclaredMethod> statementMethods = new HashMap<>();
    private final Map<Statement, List<DeclaredSymbol>> visibleSymbols = new HashMap<>();
    private final QualifiedName topLevelPackage;

    private IngredientScreeningInfo(DeclaredClassSymbols classSymbols, DeclaredLocalSymbols localSymbols, ReferencedSymbols referencedSymbols) {
        this.referencedSymbols = referencedSymbols;

        for (DeclaredClass cls : classSymbols.classes()) {
            this.classes.put(cls.getQualifiedName(), cls);

            for (DeclaredMethod declaredMethod : classSymbols.methods().get(cls)) {
                for (Statement statement : declaredMethod.statements()) {
                    statementClasses.put(statement, cls);
                    statementMethods.put(statement, declaredMethod);

                    if (localSymbols.statementSymbols().containsKey(statement)) {
                        // Only index visible symbols for statements for which this information is provided
                        visibleSymbols.put(statement, getVisibleSymbolsForStatement(statement, declaredMethod, cls, classSymbols, localSymbols));
                    }
                }
            }
        }

        topLevelPackage = classes.values().stream().map(c -> c.getQualifiedName().getParent()).min(Comparator.comparing(q -> q.elements().size())).orElseThrow();
    }

    private static List<DeclaredSymbol> getVisibleSymbolsForStatement(Statement statement, DeclaredMethod method, DeclaredClass currentCls,
                                                                      DeclaredClassSymbols classSymbols, DeclaredLocalSymbols localSymbols) {
        List<DeclaredSymbol> declaredSymbols = new ArrayList<>(localSymbols.get(statement));
        declaredSymbols.addAll(method.parameters());
        declaredSymbols.addAll(classSymbols.getSymbols(currentCls));

        for (DeclaredClass otherCls : classSymbols.classes()) {
            if (otherCls == currentCls) {
                continue;
            }

            for (DeclaredSymbol symbol : classSymbols.getSymbols(otherCls)) {
                if (symbol.isVisibleFromClass(currentCls.getTypeBinding())) {
                    declaredSymbols.add(symbol);
                }
            }
        }

        return declaredSymbols;
    }

    public static IngredientScreeningInfo create(JavaProject project, List<Statement> modificationCandidates, List<Statement> ingredientCandidates) {
        List<CompilationUnit> allCompilationUnits = project.sourceFiles().stream().map(ParsedJavaFile::compilationUnit).toList();
        DeclaredClassSymbols classSymbols = DeclaredClassSymbolsVisitor.getClassSymbols(allCompilationUnits);

        DeclaredLocalSymbols localSymbols = DeclaredLocalSymbolsVisitor.getLocalSymbols(modificationCandidates);

        ReferencedSymbols referencedSymbols = ReferencedSymbolsVisitor.getReferencedSymbols(ingredientCandidates);

        return new IngredientScreeningInfo(classSymbols, localSymbols, referencedSymbols);
    }

    public QualifiedName getTopLeveLPackage() {
        return topLevelPackage;
    }

    private List<ReferencedSymbol> getStatementReferencedSymbols(Statement statement) throws AprException {
        if (referencedSymbols.get(statement) == null) {
            throw new AprException("No referenced symbols info available for statement " + statement.toString(), null);
        }
        return referencedSymbols.get(statement);
    }

    public List<ReferencedVariable> getReferencedVariables(Statement statement) throws AprException {
        return getStatementReferencedSymbols(statement).stream().filter(x -> x instanceof ReferencedVariable).map(x -> (ReferencedVariable) x).toList();
    }

    public List<ReferencedMethod> getReferencedMethods(Statement statement) throws AprException {
        return getStatementReferencedSymbols(statement).stream().filter(x -> x instanceof ReferencedMethod).map(x -> (ReferencedMethod) x).toList();
    }

    public List<DeclaredSymbol> getDeclaredSymbols(Statement statement) throws AprException {
        if (!visibleSymbols.containsKey(statement)) {
            throw new AprException("No statement visible symbol info available for " + statement.toString(), null);
        }

        return visibleSymbols.get(statement);
    }

    public DeclaredClass getStatementClassDeclaration(Statement statement) throws AprException {
        if (!statementClasses.containsKey(statement)) {
            throw new AprException("No class info available for statement " + statement.toString(), null);
        }
        return statementClasses.get(statement);
    }

    public Optional<DeclaredClass> getDeclaredClassByName(QualifiedName qualifiedName) {
        return Optional.ofNullable(classes.get(qualifiedName));
    }

    public Optional<DeclaredClass> getDeclaredClassByName(String qualifiedName) {
        return getDeclaredClassByName(QualifiedName.fromString(qualifiedName));
    }

    public DeclaredMethod getStatementMethodDeclaration(Statement statement) throws AprException {
        if (!statementMethods.containsKey(statement)) {
            throw new AprException("No method declaration available for statement " + statement.toString(), null);
        }
        return statementMethods.get(statement);
    }
}
