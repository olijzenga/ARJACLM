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

package nl.oebelelijzenga.apr_proto.execution.java8;

import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class TestMethodResolver {

    public static class MyException extends java.lang.Exception {
        public MyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Resolves all test methods for a test class. The first argument is the classpath used to load the test class. The
     * second argument is the name of the class.
     */
    public static void main(String[] args) throws MyException {
        for (int i = 1; i < args.length; i++) {
            List<String> testMethods = getTestMethodsFromClass(args[0], args[i]);
            for (String method : testMethods) {
                System.out.println(method);
            }
        }
    }

    public static List<String> getTestMethodsFromClass(String classpath, String className) throws MyException {
        URLClassLoader testLoader;
        try {
            testLoader = createTestLoader(classpath);
        } catch (MalformedURLException e) {
            throw new MyException("Failed to create test loader", e);
        }

        Class<?> testClass;
        try {
            testClass = Class.forName(className, true, testLoader);
        } catch (ClassNotFoundException e) {
            throw new MyException("Failed to load test class " + className, e);
        }

        Runner runner = Request.aClass(testClass).getRunner();
        if (runner instanceof ErrorReportingRunner) {
            throw new MyException("Failed to request runner info for test class " + className, null);
        }

        ArrayList<String> result = new ArrayList<>();
        for (Description test : runner.getDescription().getChildren()) {
            // a parameterized atomic test case does not have a method name
            if (test.getMethodName() == null) {
                for (Method m : testClass.getMethods()) {
                    // JUnit 3: an atomic test case is "public", does not return anything ("void"), has 0
                    // parameters and starts with the word "test"
                    // JUnit 4: an atomic test case is annotated with @Test
                    if (looksLikeATest(m)) {
                        result.add(className + "::" + m.getName()); // test.getDisplayName()
                    }
                }
            } else {
                // non-parameterized atomic test case
                result.add(className + "::" + test.getMethodName());
            }
        }

        return result;
    }

    private static URLClassLoader createTestLoader(String classpath) throws MalformedURLException {
        String[] split = classpath.split(":");
        URL[] urls = new URL[split.length];
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            File f = new File(s);
            URL url = f.toURI().toURL();
            urls[i] = url;
        }
        return new URLClassLoader(urls);
    }

    private static boolean looksLikeATest(Method m) {
        if (m.isAnnotationPresent(org.junit.Test.class)) {
            return true;
        }
        return (
                m.getParameterTypes().length == 0 &&
                        m.getReturnType().equals(Void.TYPE) &&
                        Modifier.isPublic(m.getModifiers()) &&
                        m.getName().startsWith("test")
        );
    }
}
