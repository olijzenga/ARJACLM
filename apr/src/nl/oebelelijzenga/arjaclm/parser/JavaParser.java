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

package nl.oebelelijzenga.arjaclm.parser;

import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.exception.AprIOException;
import nl.oebelelijzenga.arjaclm.io.FileUtil;
import nl.oebelelijzenga.arjaclm.model.java.JavaContext;
import nl.oebelelijzenga.arjaclm.model.java.ParsedJavaFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaParser {

    public static final int LANGUAGE_STANDARD = AST.JLS8;

    private final JavaContext context;

    public JavaParser(JavaContext context) {
        this.context = context;
    }

    public static CompilationUnit parseStandaloneSourceFile(String sourceCode, String fileName) {
        ASTParser parser = ASTUtil.createSingleFileParser(fileName);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        return (CompilationUnit) parser.createAST(null);
    }

    public List<ParsedJavaFile> parseSourceFiles(List<Path> sourceFilePaths) throws AprException {
        ASTParser parser = getASTParser();
        JavaFileASTRequestor requestor = new JavaFileASTRequestor(this);
        parser.createASTs(
                FileUtil.pathListToStringArray(sourceFilePaths),
                null,
                new String[]{},
                requestor,
                null
        );

        return requestor.getFiles();
    }

    public ParsedJavaFile parseCompilationUnit(CompilationUnit compilationUnit, Path sourceFilePath, String sourceCode) throws AprException {
        // Use relative paths for source files so that the project can be used with different contexts
        Path filePath = context.rootDir().relativize(sourceFilePath);
        JavaASTVisitor visitor = new JavaASTVisitor(compilationUnit, context, filePath, sourceCode);
        compilationUnit.accept(visitor);

        try {
            return visitor.getJavaFile();
        } catch (AprIOException e) {
            throw new AprException("Failed to visit CompilationUnit", e);
        }
    }

    public ParsedJavaFile parseSourceCode(String sourceCode, Path sourceFilePath) throws AprException {
        ASTParser parser = ASTUtil.createSingleFileParser(sourceFilePath.getParent().toString());
        parser.setSource(sourceCode.toCharArray());
        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
        return parseCompilationUnit(compilationUnit, sourceFilePath, sourceCode);
    }

    public static Block parseStatementsToBlock(String sourceCode) {
        ASTParser parser = ASTParser.newParser(JavaParser.LANGUAGE_STANDARD);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_STATEMENTS);
        return (Block) parser.createAST(null);
    }

    public static List<Statement> parseStatements(String sourceCode) {
        return new ArrayList<Statement>(parseStatementsToBlock(sourceCode).statements());
    }

    private ASTParser getASTParser() {
        String srcDir = context.srcDir().toString();

        ASTParser parser = ASTParser.newParser(LANGUAGE_STANDARD);
        parser.setEnvironment(
                context.compileClassPath().asStringArray(),
                new String[]{srcDir},
                null,
                true)
        ;
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        @SuppressWarnings("rawtypes") Map options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);

        return parser;
    }
}
