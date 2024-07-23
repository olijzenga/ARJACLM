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

package nl.oebelelijzenga.apr_proto.fitness;

import com.google.gson.reflect.TypeToken;
import nl.oebelelijzenga.apr_proto.exception.AprIOException;
import nl.oebelelijzenga.apr_proto.io.FileUtil;
import nl.oebelelijzenga.apr_proto.io.JSONUtil;
import nl.oebelelijzenga.apr_proto.model.apr.Bug;
import nl.oebelelijzenga.apr_proto.model.apr.Patch;
import nl.oebelelijzenga.apr_proto.model.apr.fitness.FitnessResult;
import nl.oebelelijzenga.apr_proto.model.io.AprConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FitnessCache implements IFitnessCache {

    private static final Logger logger = LogManager.getLogger(FitnessCache.class);
    private final AprConfig aprConfig;
    private final Bug bug;
    private final Map<Integer, FitnessResult> cache;

    private FitnessCache(AprConfig aprConfig, Bug bug, HashMap<Integer, FitnessResult> cache) {
        this.aprConfig = aprConfig;
        this.bug = bug;
        this.cache = cache;
    }

    public static FitnessCache create(AprConfig aprConfig, Bug bug) throws AprIOException {
        Path cacheFilePath = getCacheFilePath(aprConfig, bug);
        if (!Files.exists(cacheFilePath)) {
            return new FitnessCache(aprConfig, bug, new HashMap<>());
        }

        HashMap<Integer, FitnessResult> cache = new HashMap<>();
        if (aprConfig.usePersistentFitnessCache())
        {
            // Load cache file
            String cacheFileContent = FileUtil.readFile(cacheFilePath);
            Type jsonDataType = new TypeToken<HashMap<Integer, FitnessResult>>() {
            }.getType();
            try {
                cache = JSONUtil.fromJson(cacheFileContent, jsonDataType);
            } catch (AprIOException e) {
                logger.warn("Loading fitness cache file failed, ignoring it.");
            }

            logger.info("Loaded %s fitness cache entries".formatted(cache.size()));
        }

        return new FitnessCache(aprConfig, bug, cache);
    }

    private static Path getCacheFilePath(AprConfig aprConfig, Bug bug) {
        return aprConfig.fitnessCacheDir().resolve("fitness_cache_%s.json".formatted(bug.simpleName()));
    }

    @Override
    public Optional<FitnessResult> get(Patch patch) {
        return Optional.ofNullable(cache.get(patch.variant().enabledEditsHashCode()));
    }

    @Override
    public void put(Patch patch, FitnessResult result) {
        cache.put(patch.variant().enabledEditsHashCode(), result);
    }

    @Override
    public void save() throws AprIOException {
        if (!aprConfig.usePersistentFitnessCache())
        {
            return;
        }

        String cacheFileContent = JSONUtil.toJSON(cache);
        FileUtil.writeFile(getCacheFilePath(aprConfig, bug), cacheFileContent);
    }
}
