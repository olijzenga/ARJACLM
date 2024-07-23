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

package nl.oebelelijzenga.apr_proto.parser.visitor;

import org.eclipse.jdt.core.dom.*;

public abstract class StatementVisitor extends ASTVisitor {
    abstract boolean visitStatement(Statement statement);

    void endVisitStatement(Statement statement) {
    }

    public boolean visit(AssertStatement node) {
        return visitStatement(node);
    }

    public void endVisit(AssertStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(BreakStatement node) {
        return visitStatement(node);
    }

    public void endVisit(BreakStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(ContinueStatement node) {
        return visitStatement(node);
    }

    public void endVisit(ContinueStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(DoStatement node) {
        return visitStatement(node);
    }

    public void endVisit(DoStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(EmptyStatement node) {
        return visitStatement(node);
    }

    public void endVisit(EmptyStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(EnhancedForStatement node) {
        return visitStatement(node);
    }

    public void endVisit(EnhancedForStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(ExpressionStatement node) {
        return visitStatement(node);
    }

    public void endVisit(ExpressionStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(ForStatement node) {
        return visitStatement(node);
    }

    public void endVisit(ForStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(IfStatement node) {
        return visitStatement(node);
    }

    public void endVisit(IfStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(LabeledStatement node) {
        return visitStatement(node);
    }

    public void endVisit(LabeledStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(ReturnStatement node) {
        return visitStatement(node);
    }

    public void endVisit(ReturnStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(SwitchStatement node) {
        return visitStatement(node);
    }

    public void endVisit(SwitchStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(SynchronizedStatement node) {
        return visitStatement(node);
    }

    public void endVisit(SynchronizedStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(ThrowStatement node) {
        return visitStatement(node);
    }

    public void endVisit(ThrowStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(TryStatement node) {
        return visitStatement(node);
    }

    public void endVisit(TryStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(TypeDeclarationStatement node) {
        return visitStatement(node);
    }

    public void endVisit(TypeDeclarationStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(VariableDeclarationStatement node) {
        return visitStatement(node);
    }

    public void endVisit(VariableDeclarationStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(WhileStatement node) {
        return visitStatement(node);
    }

    public void endVisit(WhileStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(YieldStatement node) {
        return visitStatement(node);
    }

    public void endVisit(YieldStatement node) {
        endVisitStatement(node);
    }

    public boolean visit(SwitchCase node) {
        return visitStatement(node);
    }

    public void endVisit(SwitchCase node) {
        endVisitStatement(node);
    }

    public boolean visit(EnhancedForWithRecordPattern node) {
        return visitStatement(node);
    }

    public void endVisit(EnhancedForWithRecordPattern node) {
        endVisitStatement(node);
    }

    public boolean visit(ConstructorInvocation node) {
        return visitStatement(node);
    }

    public void endVisit(ConstructorInvocation node) {
        endVisitStatement(node);
    }

    public boolean visit(SuperConstructorInvocation node) {
        return visitStatement(node);
    }

    public void endVisit(SuperConstructorInvocation node) {
        endVisitStatement(node);
    }
}
