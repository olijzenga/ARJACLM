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

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Edit;
import nl.oebelelijzenga.apr_proto.model.java.ParsedJavaFile;
import nl.oebelelijzenga.apr_proto.model.java.RawJavaFile;
import nl.oebelelijzenga.apr_proto.parser.manipulation.AbstractManipulation;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationFactory;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaEditor {

    public static List<RawJavaFile> getEditedSourceFiles(List<Edit> edits) throws AprException {
        Map<ParsedJavaFile, ASTRewrite> astRewrites = new HashMap<>();

        for (Edit edit : edits) {
            ASTRewrite rewrite = astRewrites.computeIfAbsent(
                    edit.modificationPoint().sourceFile(),
                    k -> ASTUtil.createRewriteForClass(edit.modificationPoint().cls())
            );

            AbstractManipulation manipulation = ManipulationFactory.getManipulation(
                    edit.manipulation(),
                    edit.modificationPoint().statement(),
                    edit.ingredient(),
                    rewrite
            );
            manipulation.manipulate();
        }

        List<RawJavaFile> editedFiles = new ArrayList<>();
        for (Map.Entry<ParsedJavaFile, ASTRewrite> entry : astRewrites.entrySet()) {
            IDocument sourceCode = new Document(entry.getKey().sourceCode());
            TextEdit textEdit = entry.getValue().rewriteAST(sourceCode, null);

            try {
                textEdit.apply(sourceCode);
            } catch (BadLocationException e) {
                throw new AprException("Failed to apply AST edits", e);
            }

            editedFiles.add(
                    new RawJavaFile(
                            entry.getKey().relativeFilePath(),
                            sourceCode.get()
                    )
            );
        }

        return editedFiles;
    }
}
