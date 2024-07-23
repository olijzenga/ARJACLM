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

package nl.oebelelijzenga.apr_proto.model.apr;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.execution.CommandResult;
import nl.oebelelijzenga.apr_proto.execution.CommandUtils;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import nl.oebelelijzenga.apr_proto.model.java.RawJavaFile;
import org.apache.commons.exec.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record Patch(
        int id,
        Variant variant,
        List<RawJavaFile> editedFiles
) {
    private static final Logger logger = LogManager.getLogger(Patch.class);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Patch other)) {
            return false;
        }

        return variant.equals(other.variant);
    }

    public List<Path> editedFilesPaths() {
        return editedFiles.stream().map(RawJavaFile::relativeFilePath).toList();
    }

    public boolean isEmptyPatch() {
        return variant.isEmptyVariant();
    }

    public static Patch empty() {
        return new Patch(0, Variant.create(new ArrayList<>()), new ArrayList<>());
    }

    public String generateDiff(JavaContext patchContext, JavaContext originalContext) throws AprException {
        StringBuilder diffContent = new StringBuilder();

        for (RawJavaFile editedFile : editedFiles()) {
            CommandLine command = CommandLine.parse("diff")
                    .addArgument("-u")  // Diff in git format
                    .addArgument(originalContext.rootDir().resolve(editedFile.relativeFilePath()).toString())
                    .addArgument(patchContext.rootDir().resolve(editedFile.relativeFilePath()).toString());
            CommandResult result = CommandUtils.runCommand(command, 5);

            // Only exit code 2 means something went wrong. Generating the diff is not functionally important so we
            // allow failure
            if (result.exitCode() == 2) {
                logger.warn("Failed to obtain patch diff: " + result);
                return "(diff unavailable)";
            }

            diffContent.append(result.stdout());
        }

        return diffContent.toString();
    }
}
