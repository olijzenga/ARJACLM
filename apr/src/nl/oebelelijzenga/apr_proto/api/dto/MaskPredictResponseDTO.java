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

public class MaskPredictResponseDTO {
    private final float model_load_time;
    private final float predict_time;
    private final int max_vram_mib;
    private final ArrayList<MaskPredictResponseResultDTO> results;

    public MaskPredictResponseDTO(float model_load_time, float predict_time, int max_vram_mib, ArrayList<MaskPredictResponseResultDTO> results) {
        this.model_load_time = model_load_time;
        this.predict_time = predict_time;
        this.max_vram_mib = max_vram_mib;
        this.results = results;
    }

    public float getModelLoadTime() {
        return model_load_time;
    }

    public float getPredictTime() {
        return predict_time;
    }

    public int getMaxVramMib() {
        return max_vram_mib;
    }

    public ArrayList<MaskPredictResponseResultDTO> getResults() {
        return results;
    }
}
