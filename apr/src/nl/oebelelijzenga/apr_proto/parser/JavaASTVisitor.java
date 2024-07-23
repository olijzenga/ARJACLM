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

import nl.oebelelijzenga.apr_proto.exception.AprIOException;
import nl.oebelelijzenga.apr_proto.genetic.PseudoRandom;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.parser.visitor.IngredientStatementVisitor;
import org.eclipse.jdt.core.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor that gathers generic AST information
 */
public class JavaASTVisitor extends IngredientStatementVisitor {

    private final List<ITypeBinding> declaredTypes = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();
    private final JavaContext context;
    private final CompilationUnit compilationUnit;
    private final Path relativeFilePath;
    private final String sourceCode;

    public JavaASTVisitor(CompilationUnit compilationUnit, JavaContext context, Path relativeFilePath, String sourceCode) {
        this.context = context;
        this.compilationUnit = compilationUnit;
        this.relativeFilePath = relativeFilePath;
        this.sourceCode = sourceCode;
    }

    public ParsedJavaFile getJavaFile() throws AprIOException {
        List<JavaClass> classes = getJavaClasses();
        if (classes.isEmpty()) {
            // Usually the case when the file only contains an interface declaration
            return null;
        }

        return new ParsedJavaFile(
                relativeFilePath,
                sourceCode,
                classes,
                classes.get(0).packageName(),
                compilationUnit
        );
    }

    private List<JavaClass> getJavaClasses() {
        Map<ITypeBinding, JavaClass> classesByTypeBinding = new HashMap<>();

        // Create classes
        for (ITypeBinding typeBinding : declaredTypes) {
            String name = typeBinding.isAnonymous() ? "anon-%08d".formatted(PseudoRandom.intRange(0, 99999999)) : typeBinding.getName();

            classesByTypeBinding.put(
                    typeBinding,
                    new JavaClass(
                            name,
                            QualifiedName.fromString(typeBinding.getPackage().getName()),
                            compilationUnit,
                            relativeFilePath,
                            new ArrayList<>()
                    )
            );
        }


        // Link statements to classes
        for (Statement statement : statements) {
            ITypeBinding typeBinding = ASTUtil.getTypeBinding(statement);
            JavaClass cls = classesByTypeBinding.get(typeBinding);
            cls.statements().add(statement);
        }

        // Do this instead of getting the map values to preserve class ordering
        return declaredTypes.stream().map(classesByTypeBinding::get).toList();
    }

    @Override
    protected boolean visitIngredientStatement(Statement statement) {
        statements.add(statement);
        return true;
    }

    private void addTypeDeclaration(ITypeBinding typeBinding) {
        declaredTypes.add(typeBinding);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ITypeBinding tb = node.resolveBinding();
        if (tb != null) {
            addTypeDeclaration(tb);
        }
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        ITypeBinding tb = node.resolveBinding();
        if (tb != null) {
            addTypeDeclaration(tb);
        }

        return true;
    }
}
