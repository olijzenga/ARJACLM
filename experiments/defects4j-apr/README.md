# Defects4J

This directory contains the tools for preparing Defects4J bugs for benchmarking of AJRACLM, and for processing benchmark results. 
Defects4j, fault-localization-data and GZoltar are provided through git submodules which can be retrieved using 
`git submodule init && git submodule update`.

Note that benchmarking itself is performed directly using a command of ARJACLM. Check the main README for instructions
on how to run the benchmark.

## prepare_d4j_bugs.py

Additionally requires `svn` and `sloccount` to be available on the PATH.

Prepares one or more Defects4J bugs for benchmarking by performing fault localization, and adding the `bug.json` file used by ARJACLM.
This script has grown quite complex over time due the problems I encountered with localization and APR for Defects4J bugs. For this reason
the script is not guaranteed to work on your system. It might provide a reference for dealing with Defects4J nevertheless. The script
contains a list of skipped bugs and ignored tests cases and their respective reasons. 

```shell
# Prepare all bugs
python prepare_d4j_bugs.py load_many --bugs-dir=[bugs dir]

# Use perfect localization from fault-localization-data instead of GZoltar
python prepare_d4j_bugs.py load_many --perfect-localization

# Prepare a single bug
python prepare_d4j_bugs.py load Math 1

# Update the flaky test list for an already prepared bug
python prepare_d4j_bugs.py update [bugs dir]
```

## print_results.py

Prints a summary of the results of a single ARJACLM benchmark run. 

```shell
# Print results for a benchmark run
python print_results.py [result dir]

# Only provide a list of results for each bug where an execution error occurred
python print_results.py [result dir] --failures-only

# Format the bug result table using Markdown
python print_results.py [result dir] --markdown

# Also export infill generation time, peak VRAM and analysis of compilation rate of patch ingredients
python print_results.py [result dir] --full
```
