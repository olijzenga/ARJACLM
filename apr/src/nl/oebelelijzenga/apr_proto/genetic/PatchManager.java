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

package nl.oebelelijzenga.apr_proto.genetic;

import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.exception.AprIOException;
import nl.oebelelijzenga.apr_proto.fitness.Fitness;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.model.apr.Patch;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.FitnessResult;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import nl.oebelelijzenga.apr_proto.model.java.JavaContext;
import nl.oebelelijzenga.apr_proto.model.java.RawJavaFile;
import nl.oebelelijzenga.apr_proto.parser.JavaEditor;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PatchManager {

    private final Path patchFolder;
    private final Path summariesFolder;
    private final Path resultsFolder;
    private final JavaContext sourceContext;
    private final JavaContext originalContext;

    private final Map<Integer, Patch> variantPatchCache = new HashMap<>();
    private final Map<Integer, JavaContext> existingContexts = new HashMap<>();

    public PatchManager(AprConfig input, JavaContext inputContext) throws AprException {
        this.patchFolder = input.runOutDir().resolve("patches");
        this.summariesFolder = input.runOutDir().resolve("summaries");
        this.resultsFolder = input.runOutDir().resolve("results");
        this.sourceContext = inputContext;

        originalContext = sourceContext.withRoot(patchFolder.resolve("original"));
        createPatchContextFolder(sourceContext, originalContext, true);
        existingContexts.put(0, originalContext);  // Original context has patch id 0
    }


    public synchronized Patch createPatch(Variant variant) throws AprException {
        Patch patch;

        int hashCode = variant.enabledEditsHashCode();
        if (variantPatchCache.containsKey(hashCode)) {
            patch = variantPatchCache.get(hashCode);
        } else {
            patch = new Patch(variantPatchCache.size(), variant, JavaEditor.getEditedSourceFiles(variant.enabledEdits()));
            variantPatchCache.put(hashCode, patch);
        }

        variant.setPatchId(patch.id());
        return patch;
    }

    public synchronized JavaContext createPatchContext(Patch patch) throws AprException {
        if (existingContexts.containsKey(patch.id())) {
            return existingContexts.get(patch.id());
        }

        return createPatchContext(patch, this.patchFolder, true);
    }

    public synchronized JavaContext createResultContext(Patch patch) throws AprException {
        return createPatchContext(patch, this.resultsFolder, false);
    }

    private JavaContext createPatchContext(Patch patch, Path patchFolder, boolean copySourceFiles) throws AprException {
        if (patch.id() == 0) {
            throw new AprException("Cannot create new original patch context as it is already created");
        }

        Path patchRoot = patchFolder.resolve("patch_" + patch.id());
        JavaContext context = originalContext.withRoot(patchRoot);
        createPatchContextFolder(originalContext, context, copySourceFiles);

        for (RawJavaFile editedFile : patch.editedFiles()) {
            Path editedFilePath = context.rootDir().resolve(editedFile.relativeFilePath());
            FileUtil.mkdir(editedFilePath.getParent());
            FileUtil.writeFile(editedFilePath, editedFile.sourceCode());
        }

        existingContexts.put(patch.id(), context);

        return context;
    }

    private void createPatchContextFolder(JavaContext from, JavaContext to, boolean copySourceFiles) throws AprIOException {
        FileUtil.mkdir(to.rootDir());

        if (copySourceFiles) {
            FileUtil.copySourceCodeFolder(from.rootDir(), to.rootDir());
        }

        if (Files.exists(to.aprDir())) {
            FileUtil.deleteDirectory(to.aprDir());
        }
        FileUtil.mkdir(to.aprDir());
    }

    public void writePatchInfoFile(FitnessResult result, Patch patch, JavaContext patchContext) throws AprException {
        String patchInfoContent = generatePatchInfoContent(patch, result);

        // Write patch.txt in apr directory of the variant
        FileUtil.writeFile(patchContext.aprDir().resolve("patch.txt"), patchInfoContent);

        // Write the same file but in the summaries folder (next to the patches folder), and with a file name providing
        // more info
        String summaryFileName;
        if (patch.isEmptyPatch()) {
            summaryFileName = "orig_";
        } else {
            summaryFileName = "p%04d_".formatted(patch.id());
        }
        summaryFileName += "testf_";
        summaryFileName += result.testSuiteFitness() == Fitness.MAX_LOSS ? "MAX__" : "%.2f_".formatted(result.testSuiteFitness());
        summaryFileName += "sizef_";
        summaryFileName += result.patchSizeFitness() == Fitness.MAX_LOSS ? "MAX__" : "%.2f_".formatted(result.patchSizeFitness());
        summaryFileName += "e_%02d_".formatted(patch.variant().enabledEdits().size());

        int nrFailedPositiveTests = result.testSummary() == null ? 0 : result.testSummary().getNrFailedPositiveTests();
        int nrFailedNegativeTests = result.testSummary() == null ? 0 : result.testSummary().getNrFailedNegativeTests();
        summaryFileName += "pf_%02d_".formatted(nrFailedPositiveTests);
        summaryFileName += "nf_%02d_".formatted(nrFailedNegativeTests);

        if (!result.compilationResult().success()) {
            summaryFileName += "cf";
        } else if (result.isTestAdequate()) {
            summaryFileName += "ta";
        } else {
            summaryFileName += "__";
        }

        summaryFileName += ".txt";

        FileUtil.mkdir(summariesFolder);
        FileUtil.writeFile(summariesFolder.resolve(summaryFileName), patchInfoContent);
    }

    private String generatePatchInfoContent(Patch patch, FitnessResult fitnessResult) throws AprException {
        String separator = "========";
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(separator).append(" fitness ").append(separator).append("\n");
        stringBuilder.append(fitnessResult.toFileString()).append("\n");

        stringBuilder.append(separator).append(" variant ").append(separator).append("\n");
        stringBuilder.append(patch.variant().toFileString()).append("\n");

        stringBuilder.append(separator).append(" diff ").append(separator).append("\n");
        stringBuilder.append(patch.generateDiff(createPatchContext(patch), sourceContext)).append("\n");

        stringBuilder.append(separator).append(" variant trace ").append(separator).append("\n");
        for (Pair<Variant, Class<?>> version : patch.variant().getHistory()) {
            stringBuilder.append(version.getRight().getSimpleName()).append("\n");
            stringBuilder.append(version.getLeft().toFileString()).append("\n");
        }
        stringBuilder.append("\n");

        stringBuilder.append(separator).append("compile log").append(separator).append("\n");
        stringBuilder.append(fitnessResult.compilationResult().commandResult().toFileString()).append("\n");

        stringBuilder.append(separator).append("test log").append(separator).append("\n");
        if (fitnessResult.testSummary() == null) {
            stringBuilder.append("(no test results)\n");
        } else {
            String testLogString = fitnessResult.testSummary().commandResult().toFileString();
            // Cap test log to 1MB
            int logSizeLimit = 1_000_000;
            if (testLogString.length() > logSizeLimit) {
                int originalLength = testLogString.length();
                testLogString = testLogString.substring(0, logSizeLimit);
                testLogString += "[%s more characters]".formatted(originalLength - logSizeLimit);
            }
            stringBuilder.append(testLogString);
        }
        stringBuilder.append("\n");

        return stringBuilder.toString();
    }
}
