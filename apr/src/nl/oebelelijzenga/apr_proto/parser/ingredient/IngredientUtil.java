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

package nl.oebelelijzenga.apr_proto.parser.ingredient;

import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class IngredientUtil {
    public static boolean typesWeaklyMatch(ITypeBinding source, ITypeBinding target) {
        return source.isAssignmentCompatible(target);
    }

    public static boolean typesStronglyMatch(ITypeBinding source, ITypeBinding target) {
        if (!typesWeaklyMatch(source, target)) {
            return false;
        }

        return source.getQualifiedName().equals(target.getQualifiedName());
    }

    public static boolean statementIsLastInMethod(Statement statement) {
        return statementIsLastInBlock(statement) && statement.getParent().getParent() instanceof MethodDeclaration;
    }

    public static boolean statementIsLastInBlock(Statement statement) {
        if (statement.getParent() instanceof Block block) {
            return block.statements().get(block.statements().size() - 1) == statement;
        }
        return false;
    }

    public static boolean statementIsInLoop(Statement statement) {
        return ASTUtil.getParentOfType(statement, WhileStatement.class).isPresent()
                || ASTUtil.getParentOfType(statement, DoStatement.class).isPresent()
                || ASTUtil.getParentOfType(statement, ForStatement.class).isPresent();
    }

    public static boolean statementIsInSwitchCase(Statement statement) {
        // A switch is just a container for a sequence of SwitchCase statements and regular statements, so we cannot
        // check for a direct SwitchCase parent as it is a sibling instead.
        return !(statement instanceof SwitchCase) && ASTUtil.getParentOfType(statement, SwitchStatement.class).isPresent();
    }

    public static List<ITypeBinding> getMethodThrowsTypes(MethodDeclaration methodDeclaration) {
        List<ITypeBinding> result = new ArrayList<>();
        for (Object obj : methodDeclaration.thrownExceptionTypes()) {
            Type type = (Type) obj;
            result.add(type.resolveBinding());
        }
        return result;
    }

    public static ITypeBinding getThrowExceptionType(ThrowStatement statement) {
        return statement.getExpression().resolveTypeBinding();
    }

    public static Optional<ITypeBinding> getReturnStatementType(ReturnStatement statement) {
        if (statement.getExpression() == null) {
            return Optional.empty();
        }
        return Optional.of(statement.getExpression().resolveTypeBinding());
    }

    public static Map<String, ITypeBinding> getDeclaredVariables(VariableDeclarationStatement statement) {
        Map<String, ITypeBinding> declaredVariables = new HashMap<>();
        for (Object obj : statement.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            declaredVariables.put(fragment.getName().toString(), fragment.resolveBinding().getType());
        }
        return declaredVariables;
    }

    public static ITypeBinding getSwitchType(SwitchStatement statement) {
        return statement.getExpression().resolveTypeBinding();
    }

    private static Optional<Statement> getLastStatementInBlock(Block block) {
        if (block.statements().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((Statement) block.statements().get(block.statements().size() - 1));
    }

    /**
     * Returns whether the execution of this statement will variants in a return or throw statement.
     */
    public static boolean willReturnOrThrow(Statement statement) {
        if (statement instanceof ReturnStatement || statement instanceof ThrowStatement) {
            return true;
        }

        if (statement instanceof Block block) {
            Optional<Statement> lastStatement = getLastStatementInBlock(block);
            return lastStatement.map(IngredientUtil::willReturnOrThrow).orElse(false);
        }

        if (statement instanceof IfStatement ifStatement) {
            return ifStatementWillReturnOrThrow(ifStatement);
        }

        if (statement instanceof TryStatement tryStatement) {
            return tryStatementWillReturnOrThrow(tryStatement);
        }

        return false;
    }

    private static boolean ifStatementWillReturnOrThrow(IfStatement ifStatement) {
        if (ifStatement.getElseStatement() == null) {
            return false;
        }

        return willReturnOrThrow(ifStatement.getThenStatement()) && willReturnOrThrow(ifStatement.getElseStatement());
    }

    private static boolean tryStatementWillReturnOrThrow(TryStatement tryStatement) {
        if (tryStatement.getFinally() != null && willReturnOrThrow(tryStatement.getFinally())) {
            return true;
        }

        for (Object o : tryStatement.catchClauses()) {
            if (!willReturnOrThrow(((CatchClause) o).getBody())) {
                return false;
            }
        }

        // Note that we ignore the try body. This is how ARJA implements this function for its filtering rules. It might
        // be better to check the body as well but we will not spend time on figuring this out.

        return true;
    }
}
