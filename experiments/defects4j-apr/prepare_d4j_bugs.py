# Copyright (c) 2024 Oebele Lijzenga
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import csv
import dataclasses
import json
import re
import fire
import os
import subprocess
import shutil
import logging
import xml.etree.ElementTree
from dataclasses import dataclass

TOOLS_DIR = os.path.dirname(os.path.realpath(__file__))
DEFAULT_BUGS_DIR = os.path.normpath(os.path.join(TOOLS_DIR, "..", "tests", "local"))
HAMCREST_PATH = os.path.normpath(os.path.join(TOOLS_DIR, "..", "lib", "hamcrest-core-1.3.jar"))
GZOLTAR_VERSION = "1.7.4-SNAPSHOT"
FAULT_LOCALIZATION_DATA_DIR = os.path.join(TOOLS_DIR, "fault-localization-data")

D4J_ENV = {
    "TZ": "America/Los_Angeles",
    "LANG": "en_US.UTF-8",
    "LANGUAGE": "en_US",
    "LC_CTYPE": "en_US.UTF-8",
    "LC_NUMERIC": "en_US.UTF-8",
    "LC_TIME": "en_US.UTF-8",
    "LC_COLLATE": "en_US.UTF-8",
    "LC_MONETARY": "en_US.UTF-8",
    "LC_MESSAGES": "en_US.UTF-8",
    "LC_PAPER": "en_US.UTF-8",
    "LC_NAME": "en_US.UTF-8",
    "LC_ADDRESS": "en_US.UTF-8",
    "LC_TELEPHONE": "en_US.UTF-8",
    "LC_MEASUREMENT": "en_US.UTF-8",
    "LC_IDENTIFICATION": "en_US.UTF-8"
}

# Project names mapped to bug counts
D4J_PROJECTS = [
    CHART := 'Chart',
    CLI := 'Cli',
    CLOSURE := 'Closure',
    CODEC := 'Codec',
    COLLECTIONS := 'Collections',
    COMPRESS := 'Compress',
    CSV := 'Csv',
    GSON := 'Gson',
    JACKSONCORE := 'JacksonCore',
    JACKSONDATABIND := 'JacksonDatabind',
    JACKSONXML := 'JacksonXml',
    JSOUP := 'Jsoup',
    JXPATH := 'JxPath',
    LANG := 'Lang',
    MATH := 'Math',
    MOCKITO := 'Mockito',
    TIME := 'Time'
]


@dataclass
class ProjectInfo:
    nr_bugs: int
    bug_nrs_to_skip: list[int] = dataclasses.field(default_factory=list)
    flaky_tests: list[str] = dataclasses.field(default_factory=list)
    disabled: bool = False


D4J_PROJECTS_INFO = {
    CHART: ProjectInfo(
        26,
        flaky_tests=[
            # Fails under specific locale configurations
            "org.jfree.data.time.junit.DayTests::testParseDay",
            # Fails under specific locale configurations for Chart 008
            "org.jfree.chart.axis.junit.SegmentedTimelineTests::testMondayThroughFridaySegmentedTimeline",
            "org.jfree.chart.axis.junit.SegmentedTimelineTests::testFifteenMinIncludedAndExcludedSegments",
            "org.jfree.data.time.junit.TimeSeriesCollectionTests::testGetSurroundingItems",
            "org.jfree.chart.axis.junit.SegmentedTimelineTests::testFifteenMinSegmentedTimeline"
        ]
    ),
    CLI: ProjectInfo(
        39,
        flaky_tests=[
            # All fail when ran individually
            "org.apache.commons.cli.BugsTest::test13666",
            "org.apache.commons.cli.HelpFormatterTest::testOptionWithoutShortFormat2"
        ]
    ),
    CLOSURE: ProjectInfo(
        174,
        bug_nrs_to_skip=[
            71,  # GZoltar cannot find failing test
            72,  # Same
        ],
        disabled=True
    ),
    CODEC: ProjectInfo(
        18,
        flaky_tests=[
            # Issues with special characters on the cluster
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Renault-french-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Mickiewicz-polish-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Thompson-english-one of]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Nu\u00f1ez-spanish-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Carvalho-portuguese-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u010capek-czech-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Sjneijder-dutch-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Klausewitz-german-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[K\u00fc\u00e7\u00fck-turkish-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Giacometti-italian-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Nagy-hungarian-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Ceau\u015fescu-romanian-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Angelopoulos-greeklatin-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u0391\u03b3\u03b3\u03b5\u03bb\u03cc\u03c0\u03bf\u03c5\u03bb\u03bf\u03c2-greek-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u041f\u0443\u0448\u043a\u0438\u043d-cyrillic-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u05db\u05d4\u05df-hebrew-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u00e1cz-any-exact]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u00e1tz-any-exact]",

            # Same, but test parameter name is different for codec 17 and 18
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Renault-french-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Mickiewicz-polish-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Thompson-english-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Nu\u00f1ez-spanish-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Carvalho-portuguese-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u010capek-czech-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Sjneijder-dutch-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Klausewitz-german-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[K\u00fc\u00e7\u00fck-turkish-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Giacometti-italian-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Nagy-hungarian-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Ceau\u015fescu-romanian-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[Angelopoulos-greeklatin-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u0391\u03b3\u03b3\u03b5\u03bb\u03cc\u03c0\u03bf\u03c5\u03bb\u03bf\u03c2-greek-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u041f\u0443\u0448\u043a\u0438\u043d-cyrillic-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u05db\u05d4\u05df-hebrew-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u00e1cz-any-{2}]",
            "org.apache.commons.codec.language.bm.LanguageGuessingTest::testLanguageGuessing[\u00e1tz-any-{2}]",
        ]
    ),
    COLLECTIONS: ProjectInfo(4),
    COMPRESS: ProjectInfo(
        47,
        flaky_tests=[
            # Fails due to too long file name
            "org.apache.commons.compress.archivers.tar.TarArchiveOutputStreamTest::testCount",
            "org.apache.commons.compress.archivers.tar.TarArchiveOutputStreamTest::testPadsOutputToFullBlockLength",
            # Require separate execution environments
            "org.apache.commons.compress.ArchiveReadTest::testArchive[0]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[1]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[2]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[3]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[4]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[5]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[6]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[7]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[8]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[9]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[10]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[11]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[12]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[13]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[14]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[15]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[16]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_ustar.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_cAEf.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_odc.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_crc.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS.zip]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS.ar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_bin.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_cf.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_-c.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD.zip]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_pax.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=SunOS_cEf.tar]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_hpbin.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD_crc.cpio]",
            "org.apache.commons.compress.ArchiveReadTest::testArchive[file=FreeBSD.ar]"
        ],
        bug_nrs_to_skip=[
            # Negative test is also flaky
            9,
            # Tests unexpectedly failing under GZoltar
            36
        ]
    ),
    CSV: ProjectInfo(
        16,
        flaky_tests=[
            'org.apache.commons.csv.CSVLexerTest::testNextToken4',
            'org.apache.commons.csv.CSVLexerTest::testNextToken5',
            'org.apache.commons.csv.CSVLexerTest::testNextToken6',
            'org.apache.commons.csv.CSVLexerTest::testSurroundingSpacesAreDeleted',
            'org.apache.commons.csv.CSVLexerTest::testCommentsAndEmptyLines',
            'org.apache.commons.csv.CSVLexerTest::testComments',
            'org.apache.commons.csv.CSVLexerTest::testSurroundingTabsAreDeleted',
            'org.apache.commons.csv.CSVLexerTest::testBackslashWithEscaping',
            'org.apache.commons.csv.CSVLexerTest::testBackslashWithoutEscaping',
            'org.apache.commons.csv.CSVLexerTest::testDelimiterIsWhitespace',
            'org.apache.commons.csv.CSVLexerTest::testIgnoreEmptyLines',
            'org.apache.commons.csv.TokenMatchersTest::testMatches',
            'org.apache.commons.csv.CSVLexerTest::testEscapedMySqlNullValue',
            'org.apache.commons.csv.CSVLexerTest::testEscapedCharacter',
            'org.apache.commons.csv.CSVParserTest::testBackslashEscaping'
        ],
        bug_nrs_to_skip=[
            # Relevant tests are flaky
            3
        ],
        disabled=True  # Too much of a hassle
    ),
    GSON: ProjectInfo(18),
    JACKSONCORE: ProjectInfo(26, disabled=True),
    JACKSONDATABIND: ProjectInfo(
        112,
        bug_nrs_to_skip=[
            37,  # GZoltar cannot find failing test
            38,  # Same here
            40,
            41,
            50,  # A ton of failing tests due to import issue
            51,  # GZoltar cannot find failing test
            58,  # CircularityError
            59,  # same
            60,  # same
            61,  # NoClassDefFoundError
            62,  # GZoltar cannot find failing test
            63,  # CircularityError
            64,  # Ton of failing tests due to import issue
            65,  # ClassCircularityError
            66,  # ClassCircularityError
            67,  # ClassNotFoundError
            68,  # ClassCircularityError
            *[x for x in range(69, 113)]  # ClassCircularityError
        ],
        flaky_tests=[
            # Fails with GZoltar. Seems to be library import issue
            "com.fasterxml.jackson.databind.interop.TestCglibUsage::testSimpleProxied",
            "com.fasterxml.jackson.databind.interop.TestGroovyBeans::testSimpleSerialization",
            "com.fasterxml.jackson.databind.interop.TestGroovyBeans::testSimpleDeserialization",
            "com.fasterxml.jackson.databind.type.TestTypeFactoryWithClassLoader::testUsesCorrectClassLoaderWhenThreadClassLoaderIsNull",
            "com.fasterxml.jackson.databind.type.TestTypeFactoryWithClassLoader::testUsesCorrectClassLoaderWhenThreadClassLoaderIsNotNull",
            "com.fasterxml.jackson.databind.type.TestTypeFactoryWithClassLoader::testThreadContextClassLoaderIsUsedIfNotUsingWithClassLoader",
            "com.fasterxml.jackson.databind.type.TestTypeFactoryWithClassLoader::testUsesFallBackClassLoaderIfNoThreadClassLoaderAndNoWithClassLoader"
        ],
        disabled=True
    ),
    JACKSONXML: ProjectInfo(6, disabled=True),
    JSOUP: ProjectInfo(
        93,
        bug_nrs_to_skip=[
            78,  # Both related to ConnectTest, which does not work
            91
        ],
        flaky_tests=[
            # Fails when run individually
            'org.jsoup.nodes.EntitiesTest::getByName',
            # Use local web server, fails to connect for some reason
            "org.jsoup.integration.ConnectTest::fetchURl",
            "org.jsoup.integration.ConnectTest::sendsRequestBodyWithUrlParams",
            "org.jsoup.integration.ConnectTest::doesGet",
            "org.jsoup.integration.ConnectTest::postFiles",
            "org.jsoup.integration.ConnectTest::doesPost",
            "org.jsoup.integration.ConnectTest::sendsRequestBody",
            "org.jsoup.integration.ConnectTest::doesPut",
            "org.jsoup.integration.ConnectTest::sendsRequestBodyJsonWithoutData",
            "org.jsoup.integration.ConnectTest::fetchURIWithWihtespace",
            "org.jsoup.integration.ConnectTest::sendsRequestBodyJsonWithData",
            "org.jsoup.integration.ConnectTest::bodyAndBytesAvailableBeforeParse",
            "org.jsoup.integration.ConnectTest::bodyAfterParseThrowsValidationError",
            "org.jsoup.integration.ConnectTest::multipleParsesOkAfterBufferUp",
            "org.jsoup.integration.ConnectTest::parseParseThrowsValidates",
            "org.jsoup.integration.ConnectTest::handlesEmtpyStreamDuringBufferdRead",
            "org.jsoup.integration.ConnectTest::handlesEmtpyStreamDuringBufferedRead",
            "org.jsoup.integration.ConnectTest::doesPostFor307",
            "org.jsoup.integration.ConnectTest::handlesEmptyRedirect",
            "org.jsoup.integration.ConnectTest::handlesRedirect",
            "org.jsoup.integration.ConnectTest::doesNotPostFor302",
            "org.jsoup.integration.ConnectTest::doesPostMultipartWithoutInputstream",
            "org.jsoup.integration.ConnectTest::ignoresExceptionIfSoConfigured",
            "org.jsoup.integration.ConnectTest::getUtf8Bom",
            "org.jsoup.integration.ConnectTest::testBinaryThrowsExceptionWhenTypeIgnored",
            "org.jsoup.integration.ConnectTest::testBinaryResultThrows",
            "org.jsoup.integration.ConnectTest::fetchURIWithWhitespace",
            "org.jsoup.integration.ConnectTest::throwsExceptionOn404",
            "org.jsoup.integration.ConnectTest::testBinaryContentTypeThrowsException"
        ]
    ),
    JXPATH: ProjectInfo(22, disabled=True),
    LANG: ProjectInfo(
        64,
        bug_nrs_to_skip=list(range(40, 65)),  # Issues with toString representation of objects
        flaky_tests=[
            # https://github.com/rjust/defects4j/issues/340
            # All fail when ran individually
            "org.apache.commons.lang.EntitiesPerformanceTest::testUnescapeArray",
            "org.apache.commons.lang.EntitiesPerformanceTest::testEscapeArray",
            "org.apache.commons.lang.EntitiesPerformanceTest::testLookupHash",
            "org.apache.commons.lang.EntitiesPerformanceTest::testLookupTree",
            "org.apache.commons.lang.EntitiesPerformanceTest::testLookupArray",

            # IDK why these fail under GZoltar
            "org.apache.commons.lang.enums.ValuedEnumTest::testCompareTo_classloader_equal",
            "org.apache.commons.lang.enums.ValuedEnumTest::testCompareTo_classloader_different",

            # Random according to Defects4J
            "org.apache.commons.lang3.RandomStringUtilsTest::testRandomNumeric",
            "org.apache.commons.lang3.RandomStringUtilsTest::testRandomAlphabetic",
            "org.apache.commons.lang3.RandomStringUtilsTest::testRandomAscii",
            "org.apache.commons.lang3.RandomStringUtilsTest::testRandomStringUtilsHomog",
            "org.apache.commons.lang.RandomStringUtilsTest::testRandomAlphaNumeric",
            "org.apache.commons.lang.RandomStringUtilsTest::testRandomNumeric",
            "org.apache.commons.lang.RandomStringUtilsTest::testRandomAlphabetic",
            "org.apache.commons.lang.RandomStringUtilsTest::testRandomAscii",
            "org.apache.commons.lang.RandomStringUtilsTest::testRandomStringUtilsHomog"
        ]
    ),
    MATH: ProjectInfo(
        106,
        bug_nrs_to_skip=list(range(100, 107)),  # Compile never finishes on the cluster for some reason
        flaky_tests=[
            # Random according to Defects4J
            "org.apache.commons.math.optimization.direct.CMAESOptimizerTest::testCigTab",
            "org.apache.commons.math3.optimization.direct.CMAESOptimizerTest::testCigTab",
            "org.apache.commons.math.optimization.direct.CMAESOptimizerTest::testDiagonalRosen",
            "org.apache.commons.math3.optimization.direct.CMAESOptimizerTest::testDiagonalRosen",
            "org.apache.commons.math.optimization.direct.CMAESOptimizerTest::testMaximize",
            "org.apache.commons.math3.optimization.direct.CMAESOptimizerTest::testMaximize",
            "org.apache.commons.math.stat.descriptive.summary.SumTest::testWeightedConsistency",
            "org.apache.commons.math3.stat.descriptive.summary.SumTest::testWeightedConsistency",

            # Broken according to Defects4J
            "org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegratorTest::testIntegratorControls",
            "org.apache.commons.math.ode.GraggBulirschStoerIntegratorTest::testIntegratorControls",

            # More random tests
            "org.apache.commons.math.distribution.BinomialDistributionTest::testSampling",
            "org.apache.commons.math.distribution.CauchyDistributionTest::testSampling",
            "org.apache.commons.math.distribution.ChiSquareDistributionTest::testSampling",
            "org.apache.commons.math.distribution.ExponentialDistributionTest::testSampling",
            "org.apache.commons.math.distribution.FDistributionTest::testSampling",
            "org.apache.commons.math.distribution.GammaDistributionTest::testSampling",
            "org.apache.commons.math.distribution.HypergeometricDistributionTest::testSampling",
            "org.apache.commons.math.distribution.NormalDistributionTest::testSampling",
            "org.apache.commons.math.distribution.PascalDistributionTest::testSampling",
            "org.apache.commons.math.distribution.PoissonDistributionTest::testSampling",
            "org.apache.commons.math.distribution.TDistributionTest::testSampling",
            "org.apache.commons.math.distribution.WeibullDistributionTest::testSampling",
            "org.apache.commons.math.distribution.ZipfDistributionTest::testSampling",

            "org.apache.commons.math3.distribution.BinomialDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.CauchyDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.ChiSquareDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.ExponentialDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.FDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.GammaDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.HypergeometricDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.NormalDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.PascalDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.PoissonDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.TDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.WeibullDistributionTest::testSampling",
            "org.apache.commons.math3.distribution.ZipfDistributionTest::testSampling",

            "org.apache.commons.math.analysis.function.LogitTest::testValueWithInverseFunction",
            "org.apache.commons.math.analysis.function.LogitTest::testDerivativeWithInverseFunction"

            "org.apache.commons.math3.optim.nonlinear.scalar.nonderiv.CMAESOptimizerTest::testMaximize",
            "org.apache.commons.math3.optim.nonlinear.scalar.nonderiv.CMAESOptimizerTest::testRosen",

            # Very slow, would require higher timeout which slows down the entire program
            "org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizerTest::testConstrainedRosenWithMoreInterpolationPoints",
            "org.apache.commons.math3.optimization.direct.BOBYQAOptimizerTest::testConstrainedRosenWithMoreInterpolationPoints",
            "org.apache.commons.math.optimization.direct.BOBYQAOptimizerTest::testConstrainedRosenWithMoreInterpolationPoints"
        ]
    ),
    MOCKITO: ProjectInfo(
        38,
        flaky_tests=[
            # This one just fails with GZoltar
            "org.mockitousage.basicapi.MockingMultipleInterfacesTest::should_mock_class_with_interfaces_of_different_class_loader_AND_different_classpaths",

            # These three pass or fails randomly with GZoltar
            "org.mockitousage.verification.VerificationInOrderWithCallsTest::shouldFailToCreateCallsWithZeroArgument",
            "org.mockitousage.verification.VerificationInOrderWithCallsTest::shouldFailToCreateCallsWithNegativeArgument",
            "org.mockitousage.verification.VerificationInOrderWithCallsTest::shouldFailToCreateCallsForNonInOrderVerification",

            # Fails when run individually
            "org.mockitousage.annotation.MockInjectionUsingConstructorTest::constructor_is_called_for_each_test"
        ],
        disabled=True  # Issues with compilation on the cluster
    ),
    TIME: ProjectInfo(
        26,
        flaky_tests=[
            # Fails when run individually (https://github.com/rjust/defects4j/issues/342)
            "org.joda.time.TestPeriodType::testForFields4",
            # Fails on assertSame check which seems like it should fail
            "org.joda.time.TestDateTime_Basics::testToDateTime_DateTimeZone",
            "org.joda.time.TestDateTime_Basics::testWithZoneRetainFields_DateTimeZone",
            # Random according to D4J
            "org.joda.time.TestDateTimeUtils::testOffsetMillisToZero"
        ]
    )
}

log = logging.getLogger(__name__)


def configure_logging(verbose: bool):
    global log

    log_file = os.path.join(TOOLS_DIR, 'temp', 'prepare_d4j_bugs.log')
    if os.path.exists(log_file):
        # Clear log file on every run
        os.remove(log_file)

    formatter = logging.Formatter('%(asctime)s - %(name)s:%(lineno)d - %(levelname)s - %(message)s')

    file_handler = logging.FileHandler(log_file)
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.DEBUG if verbose else logging.INFO)
    console_handler.setFormatter(formatter)

    logging.basicConfig(level=logging.DEBUG, handlers=[file_handler, console_handler])

    log = logging.getLogger(__name__)


def fix_project_name(project_name: str) -> str:
    # Fixes capitalization of Defects4J projects as the tool requires an exact match
    for d4j_project in D4J_PROJECTS:
        if d4j_project.lower() == project_name.lower():
            return d4j_project

    log.error(f'Project "{project_name}" does not exist')
    exit(1)


def command_to_msg(command: str | list, result: subprocess.CompletedProcess, cwd: str | None = None, env: dict | None = None) -> str:
    return f'Command: "{command}"\nExit code:{result.returncode}\nCwd:{cwd}\nEnv:{env}\nStdout:\n{result.stdout}\nStderr:\n{result.stderr}\n'


def exec(command: str | list, cwd: str | None = None, require_success: bool = True, env: dict | None = None) -> subprocess.CompletedProcess:
    if env is None:
        full_env = None
    if env is not None:
        # Inherit existing environment variables
        full_env = os.environ.copy()
        full_env.update(env)

    if isinstance(command, list):
        command = " ".join(command)

    log.debug("Executing command " + command)

    result = subprocess.run(
        command,
        shell=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=cwd,
        env=full_env,
        timeout=3600,
    )

    if require_success and result.returncode != 0:
        log.error(command_to_msg(command, result, cwd, env))
        log.error("Command failed")
        exit(1)
    else:
        log.debug(command_to_msg(command, result, cwd, env))

    return result


def get_java8_home_override(java8_home: str | None) -> str | None:
    def _is_java8(javac_executable: str) -> bool:
        result = exec(f"{javac_executable} -version", require_success=False)
        # Idk why but javac writes version data to stderr
        return result.returncode == 0 and result.stderr.strip().startswith("javac 1.8")

    if _is_java8("javac"):
        # Do need to override java home since the default java version is okay
        return None

    if java8_home is None:
        java8_home = "/usr/lib/jvm/java-8-openjdk"
        log.info("Using java home " + java8_home)

    if _is_java8(os.path.join(java8_home, "bin", "javac")):
        return java8_home

    log.error("Could not find valid java8 installation, try explicitly providing the java8 home")
    exit(1)


def remove_prefix(path: str, prefix: str):
    if not path.startswith(prefix):
        return path
    return path[len(prefix):]


def which(executable: str) -> str:
    result = shutil.which(executable)
    if result is None:
        log.error(f"Could not find executable {executable}")
        exit(1)
    return result


def dirname(path: str, n: int) -> str:
    for _ in range(n):
        path = os.path.dirname(path)
    return path


class Defects4JProjectLoader:

    def __init__(self, project_name: str, bug_nr: int, bugs_dir: str, java8_home: str | None, perfect_localization: bool) -> None:
        self._project_name: str = project_name
        self._bug_nr: int = bug_nr
        self._bugs_dir: str = bugs_dir
        self._java8_home: str | None = java8_home
        self._perfect_localization: bool = perfect_localization

        self._bug_dir: str = os.path.abspath(os.path.normpath(os.path.join(self._bugs_dir, f"{self._project_name}_{str(self._bug_nr).rjust(3, '0')}_buggy")))
        if self._perfect_localization:
            self._bug_dir += "_perfect"

        self._project_info = D4J_PROJECTS_INFO[self._project_name]

    def bug_file_exists(self) -> bool:
        return os.path.exists(os.path.join(self._bug_dir, "bug.json"))

    def load(self):
        log.debug("")
        log.debug("")
        log.info(f"Loading {self._project_name} {self._bug_nr}")
        log.debug("Sanity checking Defects4J installation...")
        info_command = f"defects4j info -p {self._project_name} -b {self._bug_nr}"
        result = self._exec(info_command, require_success=False)

        if result.returncode != 0:
            if result.stderr.startswith(f"Error: {self._project_name}-{self._bug_nr} is a deprecated bug"):
                log.info(f"Bug {self._project_name} {self._bug_nr} is deprecated, skipping")
                return
            else:
                log.error(command_to_msg(info_command, result))
                exit(1)

        log.debug(result.stdout)
        log.debug("Sanity check OK")

        log.debug("Checking out project...")
        if os.path.exists(self._bug_dir):
            shutil.rmtree(self._bug_dir)
        self._exec(f"defects4j checkout -p {self._project_name} -v {self._bug_nr}b -w {self._bug_dir}")
        log.debug("Checkout OK")

        log.debug("Exporting project properties...")
        src_dir = self._export_d4j_prop("dir.src.classes")
        test_dir = self._export_d4j_prop("dir.src.tests")
        src_build_dir = self._export_d4j_prop("dir.bin.classes")
        test_build_dir = self._export_d4j_prop("dir.bin.tests")
        compile_class_path_str = self._export_d4j_prop("cp.compile")
        test_class_path_str = self._export_d4j_prop("cp.test")
        negative_tests_str = self._export_d4j_prop("tests.trigger")
        relevant_classes_str = self._export_d4j_prop("classes.relevant")
        log.debug("Export OK")

        compile_class_path = self._simplify_classpath([remove_prefix(p, self._bug_dir + "/") for p in compile_class_path_str.split(":")])
        test_class_path = self._simplify_classpath([remove_prefix(p, self._bug_dir + "/") for p in test_class_path_str.split(":")])
        negative_tests = [t.strip() for t in negative_tests_str.split("\n")]
        relevant_classes = [c.strip() for c in relevant_classes_str.split("\n")]

        if set(negative_tests).intersection(set(self._project_info.flaky_tests)):
            log.error("The following tests are marked both as negative tests and flaky tests:")
            for t in set(negative_tests).intersection(set(self._project_info.flaky_tests)):
                log.error(t)
            exit(1)

        log.debug("Compiling and testing project...")
        self._exec("defects4j test", cwd=self._bug_dir)
        log.debug("Compile and test OK")

        with open(os.path.join(self._bug_dir, 'all_tests'), 'r') as f:
            all_tests = []
            for line in f.readlines():
                line = line.strip()
                if line == '':
                    continue

                test_method = line.split('(')[0]
                test_class = line.split('(')[1][:-1]
                all_tests.append(f"{test_class}::{test_method}")
        log.debug(f"Project version has {len(all_tests)} tests in total")

        if self._perfect_localization:
            log.debug("Obtaining buggy lines using perfect localization...")
            buggy_lines = self._get_perfect_bug_localization(src_dir)
        else:
            log.debug("Obtaining buggy lines using GZoltar...")
            buggy_lines = self._get_gzoltar_bug_localization(test_class_path, all_tests, negative_tests, src_dir, src_build_dir, relevant_classes)
        if len(buggy_lines) == 0:
            log.error("Localization returned zero suspicious lines")
            exit(1)

        log.debug("Buggy lines OK")
        log.debug(f"Found {len(buggy_lines)} suspicious lines")

        log.debug("Generating bug.json...")
        bug_data = {
            "srcDir": src_dir,
            "testDir": test_dir,
            "sourceBuildDir": src_build_dir,
            "testBuildDir": test_build_dir,
            "compileClassPath": compile_class_path,
            "testClassPath": test_class_path,
            "env": D4J_ENV,
            "flakyTests": self._project_info.flaky_tests,
            "buggyLines": [{"file": f, "lineNr": l, "susScore": s} for f, l, s in sorted(buggy_lines, key=lambda x: x[2], reverse=True)],
            "negativeTests": negative_tests,
            "positiveTests": [t for t in all_tests if t not in negative_tests]
        }

        with open(os.path.join(self._bug_dir, "bug.json"), "w") as f:
            f.write(json.dumps(bug_data, indent=2))

        log.debug("bug.json OK")

        log.debug("Generating compile.sh")
        compile_script = "set -e\n"
        if self._java8_home is not None:
            compile_script += f"export PATH=$JAVA_HOME/bin:$PATH\n"
        compile_script += "defects4j compile\n"

        compile_script_path = os.path.join(self._bug_dir, "compile.sh")
        with open(compile_script_path, "w") as f:
            f.write(compile_script)
        os.chmod(compile_script_path, 0o744)

        log.debug("compile.sh OK")

        log.info("Done")

    def _exec(self, command: str | list, cwd: str | None = None, require_success: bool = True, env: dict | None = None):
        if self._java8_home is not None:
            prefix = [
                f"PATH={os.path.join(self._java8_home, 'bin')}:$PATH",
                f"JAVA_HOME={self._java8_home}"
            ]
            if isinstance(command, str):
                command = " ".join(prefix + [command])
            else:
                command = prefix + command

        return exec(command, cwd, require_success, env)

    def _export_d4j_prop(self, property: str) -> str:
        result = self._exec(f"defects4j export -p {property}", cwd=self._bug_dir)

        # Remove ant output
        lines = result.stdout.split("\n")
        lines = [l for l in lines if not l.startswith("Running ant")]

        return "\n".join(lines).strip()

    def _simplify_classpath(self, classpath: list[str]) -> list[str]:
        log.debug("Original classpath " + str(classpath))
        result = list(set(classpath))

        preferred_jar_versions: set[tuple] = set()

        # Only accept JUnit JARs which are provided by Defects4J, not the one provided by the project itself
        for entry in result:
            if not re.fullmatch(r"junit-\d+\.\d+.jar", os.path.basename(entry)):
                continue

            if not os.path.dirname(entry).endswith(os.path.join("defects4j", "framework", "projects", "lib")):
                continue

            junit_version = os.path.basename(entry).split('-', 1)[1][:-4]
            preferred_jar_versions.add(('junit', junit_version))

        # Check if any entry is also referenced in pom.xml. If so remove other versions of this dependency
        # from the classpath. Needed as cp.test contains a lot of different versions for the JacksonDatabind
        # project
        if os.path.exists(os.path.join(self._bug_dir, 'pom.xml')):
            tree = xml.etree.ElementTree.parse(os.path.join(self._bug_dir, 'pom.xml'))
            for dependency in tree.getroot().findall("./{*}dependencies/{*}dependency"):
                if dependency.find('./{*}version') is None:
                    # Cannot do version selection without an explicitly provided version number
                    continue

                group_id = dependency.find('./{*}groupId').text.strip()
                artifact_id = dependency.find('./{*}artifactId').text.strip()
                version = dependency.find('./{*}version').text.strip()

                if artifact_id == "junit":
                    # Avoid conflict with the previous rule
                    continue

                while version.startswith("${"):
                    property_name = version[2:-1]
                    version_elem = tree.getroot().find("./{*}properties/{*}" + property_name)
                    if version_elem is not None:
                        version = version_elem.text.strip()

                dependency_path_pattern = r".*" + re.escape(f"/{artifact_id}/{version}/{artifact_id}") + r".*\.jar"
                matched_entry = None
                for entry in result:
                    if re.fullmatch(dependency_path_pattern, entry) is not None:
                        matched_entry = entry
                        break

                if matched_entry is None:
                    # No matching JAR for dependency so we let it be
                    continue

                preferred_jar_versions.add((artifact_id, os.path.basename(matched_entry)[len(artifact_id) + 1:-4]))

        if os.path.exists(os.path.join(self._bug_dir, 'maven-build.xml')):
            tree = xml.etree.ElementTree.parse(os.path.join(self._bug_dir, 'maven-build.xml'))
            for path_element in tree.getroot().findall("./{*}path/{*}pathelement"):
                location = path_element.attrib['location']
                if location.startswith("${maven.repo.local}"):
                    location, _ = os.path.split(location)
                    tail, version = os.path.split(location)
                    _, artifact_id = os.path.split(tail)

                    if artifact_id == "junit":
                        # Avoid conflict with the first rule
                        continue

                    preferred_jar_versions.add((artifact_id, version))

        for artifact_id, version in preferred_jar_versions:
            pattern = re.escape(artifact_id) + r"\-\d+\.\d+.*" + re.escape(".jar")
            for entry in list(result):
                file_name = os.path.basename(entry)
                if re.fullmatch(pattern, file_name) is None:
                    continue

                if file_name == f"{artifact_id}-{version}.jar":
                    continue

                result.remove(entry)
                log.debug("Removed superfluous JAR " + entry)

        # Make absolute entries which point to the bug dir relative
        for entry in list(result):
            if entry.startswith(self._bug_dir):
                # Path is absolute but can be made relative
                result.remove(entry)
                result.append(entry[len(self._bug_dir):])

        # Move jars which have an absolute path to the 'lib' folder of the repo
        for entry in list(result):
            if not os.path.isabs(entry) or not entry.endswith(".jar"):
                continue

            # Inline the jar into the project using the lib folder to make the bug folder more portable
            lib_dir = os.path.join(self._bug_dir, "lib")
            if not os.path.exists(lib_dir):
                os.mkdir(lib_dir)

            # Add prefix to avoid possible JAR file name overlap
            jar_name = "apr-inlined-" + os.path.basename(entry)
            if not os.path.exists(os.path.join(lib_dir, jar_name)):
                shutil.copy(entry, os.path.join(lib_dir, jar_name))
                log.debug("Inlined " + entry)

            result.remove(entry)
            result.append(os.path.join("lib", jar_name))

        result = list(set(result))
        log.debug("Simplified classpath " + str(result))
        return result

    def _get_perfect_bug_localization(self, src_dir: str) -> set[tuple[str, int, float]]:
        buggy_lines_file_path = os.path.join(TOOLS_DIR, "temp", "buggy_lines", f"{self._project_name}-{self._bug_nr}.buggy.lines")
        os.makedirs(os.path.basename(buggy_lines_file_path), exist_ok=True)

        buggy_lines_script_result = None
        buggy_lines_script_command = None
        if not os.path.exists(buggy_lines_file_path):
            script = os.path.join(TOOLS_DIR, "fault-localization-data", "d4j_integration", "get_buggy_lines.sh")
            env = {
                "D4J_HOME": dirname(which("defects4j"), 3),
                "SLOC_HOME": os.path.dirname(which("sloccount"))
            }

            # get_buggy_lines.sh [project name] [bug nr] [out dir]
            buggy_lines_script_command = f"{script} {self._project_name} {self._bug_nr} {os.path.dirname(buggy_lines_file_path)}"
            buggy_lines_script_result = self._exec(buggy_lines_script_command, env=env)
        else:
            log.debug("Re-using existing buggy lines file")

        with open(buggy_lines_file_path, "r") as f:
            buggy_lines_content = f.read()

        if buggy_lines_content.strip() == "":
            os.remove(buggy_lines_file_path)
            if buggy_lines_script_result is not None and buggy_lines_script_command is not None:
                log.error("command output:")
                log.error(command_to_msg(buggy_lines_script_command, buggy_lines_script_result, env=env))
            log.error("Buggy lines file was empty, something probably went wrong with obtaining buggy lines.")
            exit(1)

        # Parse buggy file content
        buggy_lines = set()
        for line in buggy_lines_content.strip().split("\n"):
            file, line_nr_str, _ = line.split('#', 2)
            line_nr = int(line_nr_str)
            buggy_lines.add((os.path.join(src_dir, file), line_nr, 1.0))

        return buggy_lines

    def _get_gzoltar_bug_localization(self, test_class_path: list[str], all_tests: list[str], negative_tests: list[str], src_dir: str, src_build_dir: str,
                                      relevant_classes: list[str]) -> set[tuple[str, int, float]]:
        gzoltar_dir = os.path.join(TOOLS_DIR, "gzoltar")
        gzoltar_cli_jar = os.path.join(gzoltar_dir, f"com.gzoltar.cli/target/com.gzoltar.cli-{GZOLTAR_VERSION}-jar-with-dependencies.jar")
        gzoltar_runtime_jar = os.path.join(gzoltar_dir, f"com.gzoltar.agent.rt/target/com.gzoltar.agent.rt-{GZOLTAR_VERSION}-all.jar")

        gzoltar_cli_classpath = f"{':'.join(test_class_path)}:{HAMCREST_PATH}:{gzoltar_cli_jar}"
        gzoltar_temp_dir = os.path.join(TOOLS_DIR, "temp", "gzoltar")
        if os.path.exists(gzoltar_temp_dir):
            shutil.rmtree(gzoltar_temp_dir)
        os.mkdir(gzoltar_temp_dir)

        if not os.path.exists(gzoltar_cli_jar) or not os.path.exists(gzoltar_runtime_jar):
            log.info("Compiling GZoltar...")
            self._exec("mvn clean install", cwd=gzoltar_dir)
            log.info("Compile OK")

        # Write GZoltar tests file
        tests_file = os.path.join(gzoltar_temp_dir, 'tests.txt')
        with open(tests_file, 'w') as f:
            for test in all_tests:
                if test in self._project_info.flaky_tests:
                    continue
                f.write(f"JUNIT,{test.replace('::', '#')}\n")

        # Run test suite with instrumentation
        ser_file = os.path.join(gzoltar_temp_dir, "gzoltar.ser")
        if os.path.exists(ser_file):
            os.remove(ser_file)

        relevant_classes_elements = []
        for relevant_class in relevant_classes:
            sanitized_name = relevant_class.replace("$", "\\$")  # Deal with class names like com.google.gson.internal.$Gson$Types
            relevant_classes_elements.append(sanitized_name)
            relevant_classes_elements.append(sanitized_name + "\\$*")  # This entry deals with subclasses
        relevant_classes_str = ":".join(relevant_classes_elements)

        result = self._exec(
            f'java -javaagent:{gzoltar_runtime_jar}=destfile={ser_file},buildlocation={src_build_dir},includes="{relevant_classes_str}",excludes="",inclnolocationclasses=false,output="FILE" ' \
            f"-cp {gzoltar_cli_classpath} com.gzoltar.cli.Main runTestMethods --testMethods {tests_file} --collectCoverage",
            cwd=self._bug_dir,
            env=D4J_ENV
        )
        if result.stderr != "":
            log.warning("Gzoltar wrote " + str(len(result.stderr.split("\n"))) + " lines to stderr while executing tests")

        # Generate fault localization report
        result = self._exec(
            [
                "java",
                "-cp", gzoltar_cli_classpath,
                "com.gzoltar.cli.Main", "faultLocalizationReport",
                "--buildLocation", src_build_dir,
                "--granularity", "line",
                "--inclPublicMethods",
                "--inclStaticConstructors",
                "--inclDeprecatedMethods",
                "--dataFile", ser_file,
                "--outputDirectory", gzoltar_temp_dir,
                "--family", "sfl",
                "--formula", "ochiai",
                "--metric", "entropy",
                "--formatter", "txt"
            ],
            cwd=self._bug_dir,
            env=D4J_ENV
        )

        if result.stderr != "":
            log.error("GZoltar runTestMethod wrote one or more errors to stderr while generating the fault localization report")
            exit(1)

        ranking_file = os.path.join(gzoltar_temp_dir, "sfl", "txt", "ochiai.ranking.csv")
        test_results_file = os.path.join(gzoltar_temp_dir, "sfl", "txt", "tests.csv")
        if not os.path.exists(ranking_file) or not os.path.exists(test_results_file):
            log.error("One or more GZoltar fault localization report files are missing!")
            exit(1)

        # Check whether the test execution was as expected
        test_sanity_check_ok = True
        executed_tests = set()
        with open(test_results_file, "r") as f:
            lines = f.readlines()[1:]  # skip header line
            for line in lines:
                line = line.strip()

                # Sometimes the test failure reason flows over into the next line. For this reason we first
                # check if a line is a valid variants line
                if not re.fullmatch(r"[a-z0-9]+(\.[a-z0-9]+)*\.[\w\$]+\#[\w\[\]=\._\- ]+,((PASS)|(FAIL)),.*", line):
                    log.warning(f"Skipping probably corrupted test variants line '{line}'")
                    continue

                elements = line.split(",")
                name = elements[0].replace("#", "::")
                executed_tests.add(name)

                class_name = name if "::" not in name else name.split("::")[0]
                outcome = elements[1]
                assert outcome in ("PASS", "FAIL")

                if name not in all_tests and class_name not in all_tests:
                    if name.startswith("org.junit"):
                        # IDK why but sometimes it tries to run tests in JUnit classes, we ignore this
                        log.warning("Ignoring variants for testcase " + name)
                        continue

                    log.error(f"Encountered variants for unexpected test {name}")
                    test_sanity_check_ok = False
                    continue

                if (outcome == "FAIL") == (name in negative_tests or class_name in negative_tests):
                    continue

                log.error(f"Unexpected variants {outcome} for test {name}")
                test_sanity_check_ok = False

        if not test_sanity_check_ok:
            log.error("Expected negative tests: " + ", ".join(negative_tests))
            exit(1)

        all_negative_tests_check_ok = True
        for test in negative_tests:
            if test not in executed_tests:
                log.error(f"Test {test} is missing in GZoltar report")
                all_negative_tests_check_ok = False

        if not all_negative_tests_check_ok:
            exit(1)

        # Parse and return localization variants
        result = set()
        with open(ranking_file, "r") as f:
            reader = csv.DictReader(f, ["name", "suspiciousness_value"], delimiter=";")
            next(reader)  # Skip header
            for entry in reader:
                sus_score = float(entry['suspiciousness_value'])
                if sus_score == 0:
                    continue

                # Entry looks something like this:
                # org.jsoup.helper$HttpConnection$Response#processResponseHeaders(java.util.Map):123
                method_reference, line_nr = entry["name"].split(":", 1)
                package = method_reference.split("#")[0].split("$")[0]
                if "$Gson$Preconditions" in method_reference:
                    class_name = "$Gson$Preconditions"
                elif "$Gson$Types" in method_reference:
                    class_name = "$Gson$Types"
                else:
                    class_name = method_reference.split("#")[0].split("$")[1]

                relative_file_path = os.path.join(
                    src_dir,
                    # Get e.g mypkg/subpkg/MyClass.java from mypkg.subpkg.MyClass#someMethod(...)
                    f"{package}.{class_name}".replace(".", "/") + ".java"
                )

                if not os.path.exists(os.path.join(self._bug_dir, relative_file_path)):
                    log.error(f"Could not find java file {relative_file_path} for name {entry['name']}")
                    exit(1)

                result.add((relative_file_path, line_nr, sus_score))
        return result


class PrepareD4JBugsCLI:

    @staticmethod
    def load(project: str, bug_nr: int, bugs_dir: str = DEFAULT_BUGS_DIR, java8_home: str | None = None, perfect_localization: bool = False, verbose: bool = False):
        configure_logging(verbose)

        os.makedirs(bugs_dir, exist_ok=True)

        java8_home = get_java8_home_override(java8_home)
        project = fix_project_name(project)

        if bug_nr in D4J_PROJECTS_INFO[project].bug_nrs_to_skip:
            log.warning("This bug is on the D4J bug blacklist")

        Defects4JProjectLoader(project, bug_nr, bugs_dir, java8_home, perfect_localization).load()

    @staticmethod
    def load_many(n: int = 999, project: str | None = None, bugs_dir: str = DEFAULT_BUGS_DIR, java8_home: str | None = None, force: bool = False,
                  perfect_localization: bool = False, verbose: bool = False):
        configure_logging(verbose)

        java8_home = get_java8_home_override(java8_home)
        projects = [fix_project_name(project)] if project is not None else D4J_PROJECTS

        log.info(f"Loading up to {n} bugs for project(s) {', '.join(projects)}")

        for project in projects:
            project_info = D4J_PROJECTS_INFO[project]

            if project_info.disabled:
                log.info(f"Skipping {project} as it is disabled")
                continue

            nr_bugs = project_info.nr_bugs
            for bug_nr in range(1, min(n + 1, nr_bugs + 1)):
                if bug_nr in project_info.bug_nrs_to_skip:
                    log.info(f"Skipping blacklisted bug {project} {bug_nr}")
                    continue

                loader = Defects4JProjectLoader(project, bug_nr, bugs_dir, java8_home, perfect_localization)

                if not force and loader.bug_file_exists():
                    log.info(f"Skipping {project} {bug_nr} as it already exists")
                    continue

                loader.load()

    @staticmethod
    def update(bugs_dir: str, verbose: bool = False):
        """
        Updated prepared bugs with new flaky test info, and removes newly blacklisted bugs
        """
        configure_logging(verbose)

        for bug_dir in sorted(os.listdir(bugs_dir)):
            project_name = bug_dir.split("_")[0]
            bug_nr = int(bug_dir.split("_")[1])
            project_info = D4J_PROJECTS_INFO[project_name]

            if project_info.disabled or bug_nr in project_info.bug_nrs_to_skip:
                log.info(f"Deleting bug dir {bug_dir}")
                shutil.rmtree(os.path.join(bugs_dir, bug_dir))
                continue

            bug_json_path = os.path.join(bugs_dir, bug_dir, 'bug.json')
            with open(bug_json_path, 'r') as f:
                bug_data = json.load(f)

            if bug_data.get('flakyTests') != project_info.flaky_tests:
                log.info(f"Updating bug.json for bug {bug_dir}")

                bug_data["flakyTests"] = project_info.flaky_tests
                with open(bug_json_path, 'w') as f:
                    f.write(json.dumps(bug_data, indent=4))


if __name__ == "__main__":
    fire.Fire(PrepareD4JBugsCLI)
