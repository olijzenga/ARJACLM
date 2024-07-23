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

import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.QualifiedName;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.screening.SymbolVisibility;
import nl.oebelelijzenga.apr_proto.model.java.JavaClass;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.util.*;

public class ASTUtil {
    public static ITypeBinding getTypeBinding(Statement s) {
        ASTNode node = s;
        while (true) {
            if (node instanceof AbstractTypeDeclaration abstractTypeDeclaration) {
                return abstractTypeDeclaration.resolveBinding();
            }
            if (node instanceof AnonymousClassDeclaration anonymousClassDeclaration) {
                return anonymousClassDeclaration.resolveBinding();
            }

            if (node.getParent() == null) {
                throw new IllegalStateException();
            }

            node = node.getParent();
        }
    }

    public static ASTRewrite createRewriteForClass(JavaClass javaClass) {
        return ASTRewrite.create(javaClass.compilationUnit().getAST());
    }

    public static AST createAST() {
        return AST.newAST(AST.JLS8);
    }

    public static ASTParser createSingleFileParser(String fileName) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setEnvironment(
                new String[]{},
                new String[]{},
                null,
                true)
        ;
        parser.setUnitName(fileName);
        parser.setResolveBindings(true);
        parser.setStatementsRecovery(true);

        @SuppressWarnings("rawtypes") Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);

        return parser;
    }

    public static SymbolVisibility getDeclarationVisibility(BodyDeclaration bodyDeclaration) {
        for (Object modifier : bodyDeclaration.modifiers()) {
            if (modifier instanceof Modifier) {
                Modifier.ModifierKeyword modifierKeyword = ((Modifier) modifier).getKeyword();
                if (modifierKeyword == Modifier.ModifierKeyword.PUBLIC_KEYWORD) {
                    return SymbolVisibility.PUBLIC;
                } else if (modifierKeyword == Modifier.ModifierKeyword.PROTECTED_KEYWORD) {
                    return SymbolVisibility.PROTECTED;
                } else if (modifierKeyword == Modifier.ModifierKeyword.PRIVATE_KEYWORD) {
                    return SymbolVisibility.PRIVATE;
                }
            }
        }
        return SymbolVisibility.PACKAGE_PRIVATE;
    }

    public static boolean isStatic(BodyDeclaration bodyDeclaration) {
        return Modifier.isStatic(bodyDeclaration.getModifiers());
    }

    public static <T extends ASTNode> Optional<T> getParentOfType(ASTNode node, Class<T> type) {
        node = node.getParent();
        while (node != null) {
            if (type.isInstance(node)) {
                return Optional.of(type.cast(node));
            }
            node = node.getParent();
        }
        ;

        return Optional.empty();
    }

    public static <T extends ASTNode> Optional<T> getSelfOrParentOfType(ASTNode node, Class<T> type) {
        if (type.isInstance(node)) {
            return Optional.of(type.cast(node));
        }
        return getParentOfType(node, type);
    }

    public static List<ITypeBinding> getSuperClasses(ITypeBinding typeBinding) {
        List<ITypeBinding> superClasses = new ArrayList<>();
        while (typeBinding.getSuperclass() != null) {
            superClasses.add(typeBinding.getSuperclass());
            typeBinding = typeBinding.getSuperclass();
        }
        return superClasses;
    }

    /**
     * Checks whether typeBinding or one of its superclasses is of type className
     */
    public static boolean instanceOf(ITypeBinding typeBinding, QualifiedName className) {
        List<ITypeBinding> types = getSuperClasses(typeBinding);
        types.add(typeBinding);
        return types.stream().anyMatch(t -> t.getQualifiedName().equals(className.toString()));
    }

    public static CompilationUnit getCompilationUnitForNode(ASTNode node) {
        while (!(node instanceof CompilationUnit)) {
            node = node.getParent();
        }
        return (CompilationUnit) node;
    }

    public static boolean subTreeMatch(ASTNode node1, ASTNode node2) {
        return node1.subtreeMatch(new ASTMatcher(true), node2);
    }

    public static boolean isArrayLengthAccess(FieldAccess node) {
        return node.getName().getIdentifier().equals("length")
                && node.getExpression().resolveTypeBinding().isArray();
    }

    public static String statementToSingleLine(Statement statement) {
        return statement.toString().replace('\n', ' ');
    }

    public static boolean canContainNestedStatements(Statement statement) {
        return statement instanceof Block ||
                statement instanceof IfStatement ||
                statement instanceof ForStatement ||
                statement instanceof WhileStatement ||
                statement instanceof DoStatement ||
                statement instanceof TryStatement ||
                statement instanceof SwitchStatement ||
                statement instanceof SynchronizedStatement ||
                statement instanceof EnhancedForStatement;
    }

    public static List<Statement> copyStatements(AST ast, List<Statement> statements) {
        List<Statement> copies = new ArrayList<>();
        for (Statement statement : statements) {
            copies.add((Statement) ASTNode.copySubtree(ast, statement));
        }
        return copies;
    }

    public static String formatStatements(String sourceCode) {
        CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);
        TextEdit edit = codeFormatter.format(CodeFormatter.K_STATEMENTS, sourceCode, 0, sourceCode.length(), 0, null);

        if (edit == null) {
            // Code is already properly formatted
            return sourceCode;
        }

        IDocument document = new Document(sourceCode);
        try {
            edit.apply(document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return document.get();
    }
}
