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

package nl.oebelelijzenga.apr_proto.cli;

import nl.oebelelijzenga.apr_proto.api.PlmApiClient;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictRequestDTO;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictResponseDTO;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictResponseMaskReplacementDTO;
import nl.oebelelijzenga.apr_proto.api.dto.MaskPredictResponseResultDTO;
import nl.oebelelijzenga.apr_proto.exception.AprException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "mask_predict")
public class PlmApiClientCommand implements Callable<Integer> {

    @Parameters(description = "Name of the model to use")
    String modelName;

    @Parameters(description = "Mask predict input", defaultValue = "def abs(val):\\n    if val < 0:\\n<mask>\\n    else:\\n        return val\\n")
    String input;

    @Option(names = {"-r", "--results"}, description = "Number of mask predict results", defaultValue = "10")
    Integer nrResults;

    @Option(names = {"--model-variant"}, description = "Variant of the model to use")
    String modelVariant;

    @Override
    public Integer call() throws AprException {
        PlmApiClient client = new PlmApiClient("localhost", 5000);

        MaskPredictRequestDTO request = new MaskPredictRequestDTO(input, modelName, modelVariant, nrResults);
        MaskPredictResponseDTO response = client.maskPredict(request);

        System.out.println("API Request Succeeded");
        System.out.println("Model load time: " + response.getModelLoadTime());
        System.out.println("Predict time: " + response.getPredictTime());
        System.out.println("Results:" + response.getResults().size());

        for (MaskPredictResponseResultDTO result : response.getResults()) {
            System.out.println("Result " + (response.getResults().indexOf(result) + 1));
            String header = "=".repeat(10) + " confidence: " + result.getConfidence() + " " + "=".repeat(10);
            System.out.println(header);
            System.out.println(result.getFullResult());
            System.out.println("=".repeat(header.length()));

            for (MaskPredictResponseMaskReplacementDTO replacement : result.getMaskReplacements()) {
                System.out.println(replacement.getMask() + " => " + replacement.getReplacement());
            }

            System.out.println("=".repeat(header.length()) + "\n");
        }

        return 0;
    }
}
