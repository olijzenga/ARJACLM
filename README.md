# ARJACLM: Automatic Repair of JAva programs using Code Language Models

ARJACLM is a search-based technique for automated program repair (APR), based on the principles of [ARJA](https://github.com/yyxhdy/arja), which leverages
state-of-the-art code language models (CLM) to generate additional patch ingredients.

My research on search-based APR using CLMs was part of my thesis for my Computer Science master degree. It is published
under the MIT license, so feel free to use (parts of) it for your own purposes.

## Structure

This repository consists of three components. First, the `apr` directory contains ARJACLM which is implemented in Java.
ARJACLM uses CLMs to generate code infills. This is done by calling a separate API written in Python which provides mask prediction
functionality for many CLMs. The `clm` directory contains the Python application which exposes mask prediction functionality
for many CLMs under a unified input and output format, and includes the API which is used by ARJACLM. The `experiments` directory
contains the files that I used to evaluate the infill generation capabilities of 20 CLMs, and the tools for preparing bugs of
the [Defects4J](https://github.com/rjust/defects4j) dataset for benchmarking of ARJACLM, and processing the results.

## Requirements

ARJACLM has been developed and evaluated on Linux. Some minor adjustments might be required to run it on MacOS or Windows.

- JDK 17 (for compiling and running ARJACLM)
- JDK 8 (for compiling and testing Defects4J bugs)
- Maven
- Python 3.9 or greater
- CUDA (for running CLMs on Nvidia GPUs)
- Defects4J
- The `diff` command

Both Defects4J and `diff` must be available on the path. Defects4J can be installed via the `experiments/defects4j-apr/defects4j` submodule.
Initialize the git submodules (`git submodule init && git submodule update`) and check `experiments/defects4j-apr/defects4j` for instructions,
or install Defects4J at another location.

## Installation

Dependencies for the `clm` package are managed through [Poetry](https://github.com/python-poetry/poetry), and can be installed via
Poetry, or via Pip:

```shell
# Using Pip
pip install .

# Using Pip (development mode)
pip install -e .

# Using Poetry
poetry install  # Install dependencies
poetry shell   # Activate virtual environment
```

For ARJACLM, some Java 8 classes must be compiled beforehand. This can be done using the `apr/compile_java8_tools.sh` script,
or by manually executing the commands of this script with the appropriate path to the Java 8 compiler.

## Usage

### ARJACLM

The following ARJACLM commands are available. Additional arguments can be discovered using the `--help` option.

```shell
# Repair a bug
./apr.sh repair [bug_dir]
./apr.sh repair [bug_dir] --clm-enabled=false
./apr.sh repair tests/SimpleExample

# Repair a collection of bugs located in the same directory
./apr.sh benchmark [bugs_dir]

# Extract and print fault localization info from `bug.json`
./apr.sh localize [bug dir]

# Execute a mask predict API request on the API
./apr.sh mask_predict [model name]

# Load and parse the source code of a bug and print the parsed statements
./apr.sh parse_java [bug dir]

# Perform a sanity check, checking if all positive tests pass, and all negative tests fail
./apr.sh sanity [bug dir]
```

Output files for APR runs and benchmarking can be found in the `apr/var/out` directory. Note that by default ARJACLM uses Refact
to generate patch ingredients. The CLM API must be running locally (using `flask --app clm.api.api run`) to allow for the generation
of patch ingredients. Alternatively, CLM-based patch ingredients can be disabled using the `--clm-enabled=false` argument. Moreover,
ARJACLM can be configured to access the CLM API via a different host or port using the `--clm-api-host` and `--clm-api-port` arguments.

### CLM

The CLM package can be used separately to experiment with mask prediction using various CLMs. This can be done through the 
command line, or the API.

### Mask Prediction CLI

The example below shows a simple mask predict command. The input file should contain a `<mask>`, which is the universal mask token
which is translated to the appropriate mask prediction format for each CLM. For a list of available CLMs and their variants, either provide an incorrect
CLM name and it will provide the available values, or check the CLM names of `MaskPredictModel` subclasses in the `clm/clms` folder.

```shell
# Example usages
python mask_predict.py codet5 --model-variant=large ./inputs/abs_expr.py
python mask_predict.py codet5 --model-variant=large ./inputs/abs_expr.py --nr-beams=5
python mask_predict.py codet5 --model-variant=large ./inputs/abs_expr.py --top-p=0.5 temperature=1
python mask_predict.py codet5 --model-variant=large ./inputs/abs_expr.py --quantization-mode=8bit

# Usage info
python mask_predict.py --help
```

### Mask Prediction API

The mask prediction API provides similar functionality compared to the CLI but in web API form.
This is used for executing mask prediction from Java code. The API is started as follows:

```shell
flask --app clm.api.api run  # Standard mode
flask --app clm.api.api --debug run  # Debug/development mode
```

Flask development mode also enables automatic reloading of the API when code is changed.

#### API Endpoints

Currently, there is only one API endpoint.

##### POST /mask_predict
* Path parameters: none
* Query parameters: none
* Request body:

```json
{
  "text": "def hello_world(): <mask>",  // Input with <mask> token
  "model_name": "unixcoder",  // Name of the PLM
  "model_variant": null,  // PLM variant, available options differ per PLM
  "nr_results": 10  // Number of mask predictions to generate, defaults to 10
}
```
