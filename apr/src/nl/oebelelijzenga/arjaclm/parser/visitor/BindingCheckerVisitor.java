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

package nl.oebelelijzenga.arjaclm.parser.visitor;

import nl.oebelelijzenga.arjaclm.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

/*
 * Visits symbol references and checks if all bindings can be resolved
 */
public class BindingCheckerVisitor extends IngredientStatementVisitor {
    private final List<UnresolvedBinding> unresolvedBindings = new ArrayList<>();

    public static List<UnresolvedBinding> getUnresolvedBindings(ASTNode node) {
        BindingCheckerVisitor visitor = new BindingCheckerVisitor();
        node.accept(visitor);
        return visitor.unresolvedBindings;
    }

    public record UnresolvedBinding(
            ASTNode node,
            boolean typeBinding
    ) {
        public String toString() {
            CompilationUnit compilationUnit = ASTUtil.getSelfOrParentOfType(node, CompilationUnit.class).orElseThrow();
            String result = "UnresolvedBinding[location=%s:%s, ".formatted(
                    ((TypeDeclaration) compilationUnit.types().get(0)).resolveBinding().getQualifiedName(),
                    compilationUnit.getLineNumber(node.getStartPosition())
            );

            if (typeBinding) {
                result += "type";
            } else {
                result += "name";
            }
            result += "=%s]".formatted(node);

            return result;
        }
    }

    @Override
    protected boolean visitIngredientStatement(Statement statement) {
        return true;
    }

    //    @Override
//    public boolean visit(MethodInvocation node) {
//        if (node.resolveMethodBinding() == null) {
//            unresolvedBindings.add(new UnresolvedBinding(node, false));
//        }
//
//        return super.visit(node);
//    }
//
//    @Override
//    public boolean visit(SuperMethodInvocation node) {
//        if (node.resolveMethodBinding() == null) {
//            unresolvedBindings.add(new UnresolvedBinding(node, false));
//        }
//
//        return super.visit(node);
//    }
//
//    @Override
//    public boolean visit(ClassInstanceCreation node) {
//        if (node.resolveConstructorBinding() == null) {
//            unresolvedBindings.add(new UnresolvedBinding(node, false));
//        }
//
//        return super.visit(node);
//    }
//
//    @Override
//    public boolean visit(TypeLiteral node) {
//        if (node.resolveTypeBinding() == null) {
//            unresolvedBindings.add(new UnresolvedBinding(node, true));
//        }
//
//        return super.visit(node);
//    }

    @Override
    public boolean visit(SimpleName node) {
        if (node.resolveBinding() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, false));
        }
        if (node.resolveTypeBinding() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, true));
        }

        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        if (node.resolveBinding() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, false));
        }
        if (node.resolveTypeBinding() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, true));
        }

        return false;
    }

    @Override
    public boolean visit(FieldAccess node) {
        int beforeErrorCount = unresolvedBindings.size();
        node.getExpression().accept(this);
        if (beforeErrorCount != unresolvedBindings.size()) {
            // We need the expression to be resolvable to be able to check if its an array access later on,
            // so exit early if something is wrong with the expression
            return false;
        }

        if (ASTUtil.isArrayLengthAccess(node)) {
            return false;
        }

        if (node.resolveFieldBinding() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, false));
        }
        if (node.resolveFieldBinding().getDeclaringClass() == null) {
            unresolvedBindings.add(new UnresolvedBinding(node, true));
        }

        return false;
    }
}
