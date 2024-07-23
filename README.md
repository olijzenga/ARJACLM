# apr-beun

## Installation

This project uses Poetry for package management
```shell
poetry install  # Install dependencies
poetry shell   # Activate virtual environment

# Alternatively
pip install --extra-index-url https://download.pytorch.org/whl/cu118 -e .
```

## Usage

This project currently has two methods of usage, namely a CLI and an API for mask prediction.

### Mask Prediction CLI

Usage information for the mask prediction CLI can be found using the command below. Each command 
corresponds to one language model. For most language models more than one variant is available.

```shell
python mask_predict.py --help
```

### Mask Prediction API

The mask prediction API provides similar functionality compared to the CLI but in web API form.
This is used for executing mask prediction from Java code. The API is started as follows:

```shell
flask --app plm.api.api run  # Standard mode
flask --app plm.api.api --debug run  # Debug/development mode
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
  "text": "def some_fn(): <mask>",  // Input with <mask> tokens
  "model_name": "unixcoder",  // Name of the PLM
  "model_variant": null,  // PLM variant, available options differ per PLM
  "nr_results": 10  // Number of mask predictions to generate, defaults to 10
}
```

### Preparing Defects4J bugs

First, `mvn`, `svn` and `sloccount1` must be installed, and the Defects4J and fault-localization-data submodules
must be initialized. This is done as follows:

```shell
# Initialize Defects4J
cd apr/tools/defects4j
cpanm --installdeps .
./init.sh

# Initialize fault-localization-data
cd apr/tools/fault-localization-data
./setup.sh
```

Then, a bug can be prepared as follows:

```shell
poetry shell

# Load Chart 7
cd apr/tools
python prepare_d4j_bugs.py load Chart 7
```

The bug will be loaded to `apr/tests/local/Chart_7_buggy`.

### Running APR

To run the APR tool itself, some preparations must be performed. First, `compile_java8_tools.sh` must be run once to compile the necessary Java 8 classes. Furthermore, the `diff` executable must be a available on the path.

The APR tool itself can be run on a bug directory as follows:
```shell
./apr.sh repair [bug_dir]
```

Output files for each APR run can be found in `var/out`.
