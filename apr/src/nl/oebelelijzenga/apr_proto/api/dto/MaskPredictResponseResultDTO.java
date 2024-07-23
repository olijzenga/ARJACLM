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

package nl.oebelelijzenga.apr_proto.api.dto;

import java.util.ArrayList;

public class MaskPredictResponseResultDTO {
    private final float confidence;
    private final String full_result;
    private final ArrayList<MaskPredictResponseMaskReplacementDTO> mask_replacements;

    public MaskPredictResponseResultDTO(float confidence, String full_result, ArrayList<MaskPredictResponseMaskReplacementDTO> mask_replacements) {
        this.confidence = confidence;
        this.full_result = full_result;
        this.mask_replacements = mask_replacements;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getFullResult() {
        return full_result;
    }

    public ArrayList<MaskPredictResponseMaskReplacementDTO> getMaskReplacements() {
        return mask_replacements;
    }
}
