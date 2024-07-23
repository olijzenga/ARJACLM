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

package nl.oebelelijzenga.arjaclm.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.oebelelijzenga.arjaclm.api.dto.MaskPredictRequestDTO;
import nl.oebelelijzenga.arjaclm.api.dto.MaskPredictResponseDTO;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.io.JSONUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlmApiClient {

    public static final String MASK_TOKEN = "<mask>";

    private final String host;
    private final int port;
    private final Gson gson;

    public PlmApiClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    private String getBaseUrl() {
        return String.format("http://%s:%d", host, port);
    }

    private HttpResponse<String> doHttpRequest(String method, String url, Object content) throws PlmApiClientException {
        HttpRequest.BodyPublisher bodyPublisher;
        String requestContent = "";
        if (content == null) {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            requestContent = gson.toJson(content);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(requestContent);
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, bodyPublisher);

        if (content != null) {
            requestBuilder.header("Content-Type", "application/json");
        }

        try {
            return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new PlmApiClientException(
                    String.format(
                            "%s request to %s with body %s failed: %s",
                            method,
                            url,
                            requestContent,
                            e.getMessage()
                    ),
                    e
            );
        }
    }

    public MaskPredictResponseDTO maskPredict(MaskPredictRequestDTO request) throws AprException {
        HttpResponse<String> response = doHttpRequest("POST", getBaseUrl() + "/mask_predict", request);
        return JSONUtil.fromJson(response.body(), MaskPredictResponseDTO.class);
    }
}
