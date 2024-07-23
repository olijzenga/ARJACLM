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

package nl.oebelelijzenga.arjaclm.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import nl.oebelelijzenga.arjaclm.exception.AprIOException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

public class JSONUtil {

    private static class PathTypeAdapter extends TypeAdapter<Path> {

        @Override
        public void write(JsonWriter out, Path value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Path read(JsonReader in) throws IOException {
            return Path.of(in.nextString());
        }
    }

    private static Gson gson() {
        return new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .registerTypeAdapter(Path.of("/").getClass(), new PathTypeAdapter())
                .create();
    }

    public static <T> T fromJson(String jsonString, Class<T> type) throws AprIOException {
        try {
            return gson().fromJson(jsonString, type);
        } catch (JsonSyntaxException e) {
            throw new AprIOException(
                    String.format(
                            "Failed to deserialize JSON string %s to object of type %s",
                            jsonString,
                            type.toString()
                    ),
                    e
            );
        }
    }

    public static <T> T fromJson(String jsonString, Type type) throws AprIOException {
        try {
            return gson().fromJson(jsonString, type);
        } catch (JsonSyntaxException e) {
            throw new AprIOException(
                    String.format(
                            "Failed to deserialize JSON string %s to object of type %s",
                            jsonString,
                            type.toString()
                    ),
                    e
            );
        }
    }

    public static String toJSON(Object data) {
        return gson().toJson(data);
    }
}
