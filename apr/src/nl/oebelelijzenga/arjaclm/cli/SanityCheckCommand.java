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

package nl.oebelelijzenga.arjaclm.cli;

import nl.oebelelijzenga.arjaclm.apr.AprProblem;
import nl.oebelelijzenga.arjaclm.exception.AprException;
import nl.oebelelijzenga.arjaclm.io.AprProblemLoader;
import nl.oebelelijzenga.arjaclm.model.io.AprPreferences;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "sanity")
public class SanityCheckCommand extends AbstractSingleAPRRunCommand implements Callable<Integer> {

    @Override
    public Integer call() throws AprException {
        System.out.println("Compiling files for " + bugDir);

        AprPreferences preferences = createPreferences();
        AprProblem aprProblem = new AprProblemLoader(preferences).load();

        System.out.println("Running evaluation of default variant...");
        aprProblem.sanityCheck();

        System.out.println("Evaluation finished");
        return 0;
    }
}
