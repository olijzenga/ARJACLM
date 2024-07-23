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

package nl.oebelelijzenga.apr_proto.genetic.mutation;

import nl.oebelelijzenga.apr_proto.api.PlmApiClient;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictRequestDTO;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictResponseDTO;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictResponseMaskReplacementDTO;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import nl.oebelelijzenga.apr_proto.genetic.PseudoRandom;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Edit;
import nl.oebelelijzenga.apr_proto.model.apr.genetic.Variant;
import nl.oebelelijzenga.apr_proto.model.apr.ingredient.Ingredient;
import nl.oebelelijzenga.apr_proto.model.io.PlmConfig;
import nl.oebelelijzenga.apr_proto.parser.ASTUtil;
import nl.oebelelijzenga.apr_proto.parser.JavaEditor;
import nl.oebelelijzenga.apr_proto.parser.JavaParser;
import nl.oebelelijzenga.apr_proto.parser.manipulation.ManipulationName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.Statement;

import java.util.*;

public class PlmMutation implements IMutation {

    private static final Logger logger = LogManager.getLogger(PlmMutation.class);
    private static final String maskPlaceholderStatement = "System.out.println(\"this is my mask placeholder\");";

    private final PlmConfig plmConfig;
    private static final Map<String, List<List<Statement>>> maskReplacementCache = new HashMap<>();
    private final float mutationProbability;

    public PlmMutation(PlmConfig plmConfig, float mutationProbability) {
        this.plmConfig = plmConfig;
        this.mutationProbability = mutationProbability * 1.5f;
    }

    @Override
    public Variant apply(Variant variant) throws AprException {
        long nrEnabledEdits = variant.edits().stream().filter(Edit::enabled).count();
        List<Edit> newEdits = new ArrayList<>(variant.edits());
        for (Edit edit : variant.edits()) {
            Edit newEdit = edit.copy();

            if (PseudoRandom.bool(mutationProbability * (edit.enabled() ? (nrEnabledEdits + 1) : 1.0f))) {
                newEdit = newEdit.withEnabled(!edit.enabled());
            }

            if ((newEdit.enabled() && !edit.enabled()) || PseudoRandom.bool(mutationProbability)) {
                Optional<Edit> optionalEdit = withPlmIngredient(edit, newEdits);
                if (optionalEdit.isPresent()) {
                    newEdit = optionalEdit.get();
                }
            }

            newEdits = IMutation.replaceEdit(newEdits, edit, newEdit);
        }

        return variant.withEdits(newEdits, PlmMutation.class);
    }

    private Optional<Edit> withPlmIngredient(Edit targetEdit, List<Edit> edits) throws AprException {
        Edit maskEdit = new Edit(
                true,
                PseudoRandom.pick(List.of(ManipulationName.REPLACE, ManipulationName.INSERT_BEFORE)),
                targetEdit.modificationPoint(),
                new Ingredient(JavaParser.parseStatements(maskPlaceholderStatement).get(0), false)
        );

        // Create list of edits which edit the file that is edited by targetEdit, and replace targetEdit with maskEdit
        // to obtain the masked version of the code
        List<Edit> editsWithMask = new ArrayList<>();
        for (Edit edit : edits) {
            if (!(edit.enabled() || edit == targetEdit)) {
                continue;
            }
            if (edit.modificationPoint().sourceFile().relativeFilePath().equals(targetEdit.modificationPoint().sourceFile().relativeFilePath())) {
                editsWithMask.add(edit);
            }
        }
        editsWithMask = IMutation.replaceEdit(editsWithMask, targetEdit, maskEdit);

        String maskedCodeWithPlaceholder = JavaEditor.getEditedSourceFiles(editsWithMask).get(0).sourceCode();
        if (!maskedCodeWithPlaceholder.contains(maskPlaceholderStatement)) {
            logger.warn("Failed to generate code with mask token, skipping mask prediction");
            return Optional.empty();
        }

        String maskedCode = maskedCodeWithPlaceholder.replace(maskPlaceholderStatement, PlmApiClient.MASK_TOKEN);
        String prompt = getLimitedScope(maskedCode,  plmConfig.nrPromptContextLines() / 2);  // Divide by two since its both before and after

        List<List<Statement>> maskReplacements = getMaskReplacements(prompt, maskEdit);
        if (maskReplacements.isEmpty()) {
            logger.warn("Mask predict returned no usable ingredients, ignoring result");
            // Return the existing un-edited variant in this case instead of using the empty ingredient as it would
            // basically represent a delete operation. Traditional search-based APR already does enough of that by itself
            return Optional.empty();
        }

        logger.info("Successfully generated ingredient using PLM");
        List<Statement> maskReplacement = pickMaskReplacement(maskReplacements);

        Ingredient ingredient = new Ingredient(maskReplacement, false);
        return Optional.of(maskEdit.withIngredient(ingredient));
    }

    private List<List<Statement>> getMaskReplacements(String prompt, Edit maskEdit) throws AprException {
        String promptCacheKey = prompt.replaceAll("\\w", "");
        if (maskReplacementCache.containsKey(promptCacheKey)) {
            return maskReplacementCache.get(promptCacheKey);
        }

        // Generate mask replacement
        PlmApiClient plmApiClient = new PlmApiClient(plmConfig.apiHost(), plmConfig.apiPort());
        MaskPredictResponseDTO responseDto = plmApiClient.maskPredict(
                new MaskPredictRequestDTO(
                        prompt,
                        plmConfig.modelName(),
                        plmConfig.modelVariant(),
                        plmConfig.nrInfills()
                )
        );
        List<List<Statement>> maskReplacements = getNonEmptyUniqueFormattedMaskReplacements(responseDto, maskEdit);

        logger.debug(
                "Mask predict for modification point \"%s\" with manipulation %s completed in %.1f seconds with peak memory usage of %s MiB".formatted(
                        ASTUtil.statementToSingleLine(maskEdit.modificationPoint().statement()),
                        maskEdit.manipulation(),
                        responseDto.getPredictTime(),
                        responseDto.getMaxVramMib()
                )
        );
        logger.debug("Prompt:");
        logger.debug(getLimitedScope(prompt, 2));
        logger.debug("Got %s unique non-empty mask replacements".formatted(maskReplacements.size()));
        for (List<Statement> maskReplacement : maskReplacements) {
            logger.debug("Replacement: " + String.join("\n", maskReplacement.stream().map(Statement::toString).toList()));
        }

        maskReplacementCache.put(promptCacheKey, maskReplacements);
        return maskReplacements;
    }

    /*
     * Returns only the fragment of code surrounding the mask token
     */
    private static String getLimitedScope(String code, int nrContextLines) {
        String[] elements = code.split(PlmApiClient.MASK_TOKEN, 2);
        String left = elements[0];
        String right = elements[1];

        List<String> leftElements = Arrays.asList(left.split("\n"));
        List<String> rightElements = Arrays.asList(right.split("\n"));

        ArrayList<String> resultElements = new ArrayList<>();
        // Add elements before mask
        resultElements.addAll(leftElements.subList(Math.max(0, leftElements.size() - 1 - nrContextLines), leftElements.size()));

        // Fix mask line
        resultElements.set(resultElements.size() - 1, resultElements.get(resultElements.size() - 1) + PlmApiClient.MASK_TOKEN + rightElements.get(0));

        // Add elements after mask
        if (resultElements.size() >= 2) {
            resultElements.addAll(rightElements.subList(1, Math.min(nrContextLines + 1, rightElements.size())));
        }

        return String.join("\n", resultElements);
    }

    private List<List<Statement>> getNonEmptyUniqueFormattedMaskReplacements(MaskPredictResponseDTO responseDto, Edit maskEdit) {
        List<String> maskReplacementStrings = new ArrayList<>();
        List<List<Statement>> maskReplacements = new ArrayList<>();
        for (MaskPredictResponseMaskReplacementDTO maskReplacementDto : responseDto.getResults().get(0).getMaskReplacements())
        {
            String replacement = ASTUtil.formatStatements(maskReplacementDto.getReplacement());
            if (maskReplacementStrings.contains(replacement)) {
                continue;
            }

            List<Statement> parsedReplacement = JavaParser.parseStatements(replacement);
            if (parsedReplacement.isEmpty()) {
                if (replacement.isBlank()) {
                    logger.debug("Mask predict produced a blank statement");
                } else {
                    logger.debug("Mask predict produced an unparseable statement: " + replacement.replace('\n',' ').replace("\r", ""));
                }
                continue;
            }

            if (maskEdit.manipulation() == ManipulationName.REPLACE
                && parsedReplacement.size() == 1
                && parsedReplacement.get(0).equals(maskEdit.modificationPoint().statement())) {
                // Skip replacement operations where the code is replaced by an identical statement
                continue;
            }

            maskReplacementStrings.add(replacement);
            maskReplacements.add(parsedReplacement);
        }
        return maskReplacements;
    }

    private static List<Statement> pickMaskReplacement(List<List<Statement>> maskReplacements) {
        for (List<Statement> maskReplacement : maskReplacements) {
            if (PseudoRandom.bool(0.5f)) {
                return maskReplacement;
            }
        }
        return maskReplacements.get(0);
    }
}
