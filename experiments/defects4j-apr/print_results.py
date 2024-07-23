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

"""
Prints a CSV of the results of an APR benchmark
"""

import dataclasses
import json
import os
import re
import fire
import logging

D4J_224_PROJECTS = [
    'Lang',
    'Chart',
    'Math',
    'Time'
]


def get_224_project(bug_id: str) -> str | None:
    for project in D4J_224_PROJECTS:
        if bug_id.startswith(project):
            return project
    return None


logging.basicConfig()
log = logging.getLogger()


@dataclasses.dataclass
class Generation:
    generation_nr: int
    nr_evaluated_variants: int
    nr_unique_evaluated_variants: int
    nr_unique_new_variants: int
    nr_final_variants: int
    nr_unique_final_variants: int
    nr_test_adequate_variants: int
    nr_unique_test_adequate_variants: int
    best_fitness: list[float]
    duration_millis: int

    @staticmethod
    def from_json(data: dict) -> "Generation":
        return Generation(
            data['generationNr'],
            data['nrEvaluatedVariants'],
            data['nrUniqueEvaluatedVariants'],
            data['nrUniqueNewVariants'],
            data['nrFinalVariants'],
            data['nrUniqueFinalVariants'],
            data['nrTestAdequateVariants'],
            data['nrUniqueTestAdequateVariants'],
            data['bestFitness'],
            data['durationMillis']
        )


@dataclasses.dataclass
class Population:
    correct_variants: list[int]
    nr_unique_variants: int
    generations: list[Generation]

    @staticmethod
    def from_json(data: dict) -> "Population":
        return Population(
            data['correctVariants'],
            data['nrUniqueVariants'],
            [Generation.from_json(g) for g in data['generations']]
        )


@dataclasses.dataclass
class Result:
    sanity_check_ok: bool
    execution_error: bool
    execution_time_millis: int
    population: Population

    @staticmethod
    def from_json(data: dict) -> "Result":
        return Result(
            data['sanityCheckOk'],
            data['executionError'],
            data['executionTimeMillis'],
            Population.from_json(data['population']),
        )


@dataclasses.dataclass
class Run:
    name: str
    dir: str
    bug_name: str
    preferences: dict
    config: dict
    result: Result

    @staticmethod
    def from_json(name: str, dir: str, data: dict) -> "Run":
        return Run(
            name,
            dir,
            data['bugName'].strip(),
            data['preferences'],
            data['config'],
            Result.from_json(data['result'])
        )

    def converged_at(self) -> int:
        """
        Returns the number of the first generation where less than popsize/10 new variants were evaluated.
        """
        threshold = self.config['geneticConfig']['populationSize'] / 10

        for generation in self.result.population.generations:
            if generation.nr_unique_new_variants <= threshold:
                return generation.generation_nr

        return self.config['geneticConfig']['nrGenerations']

    def max_new_variants(self) -> int:
        """
        Returns the highest number of new unique variants introduced in a single generation
        """
        if len(self.result.population.generations) < 2:
            return 0

        return max(
            gen.nr_unique_new_variants
            for gen in self.result.population.generations if gen.generation_nr != 0
        )


# Run retrieval
def get_run(run_dir: str) -> Run | None:
    name = os.path.basename(run_dir)

    result_file_path = os.path.join(run_dir, 'run.json')
    if not os.path.exists(result_file_path):
        log.warning(f"Missing result file for directory {run_dir}")
        return None

    with open(result_file_path, 'r') as f:
        data = json.load(f)

    return Run.from_json(name, run_dir, data)


def get_bug_runs(bug_run_dir: str) -> list[Run]:
    runs = []
    for run_dir in os.listdir(bug_run_dir):
        d = os.path.join(bug_run_dir, run_dir)
        if not os.path.isdir(d):
            continue

        run = get_run(d)
        if run is not None:
            runs.append(run)
    return runs


def get_benchmark_runs(benchmark_dir: str) -> list[Run]:
    runs = []
    for bug_dir in os.listdir(benchmark_dir):
        d = os.path.join(benchmark_dir, bug_dir)
        if not os.path.isdir(d):
            continue

        runs.extend(get_bug_runs(d))
    return runs


def get_runs(results_dir: str) -> list[Run]:
    basename = os.path.basename(os.path.abspath(results_dir))
    if re.fullmatch(r"benchmark_\d{8}_\d{6}", basename):
        return get_benchmark_runs(results_dir)
    if re.fullmatch(r"\w+_\d+_buggy", basename):
        return get_bug_runs(results_dir)
    if re.fullmatch(r"\w+_\d+_buggy_\d{8}_\d{6}", basename):
        result = get_run(results_dir)
        return [] if result is None else [result]
    if re.fullmatch(r"\d{6}.*", basename):
        for subdir in os.listdir(results_dir):
            if re.fullmatch(r"benchmark_\d{8}_\d{6}", subdir):
                return get_runs(os.path.join(results_dir, subdir))

    log.error(f"Failed to detect result directory type for dir '{basename}'")
    exit(1)


# Timing extraction
@dataclasses.dataclass
class TimingStats:
    min: float
    max: float
    median: float
    avg: float

    @staticmethod
    def from_times(times: list[float]) -> "TimingStats":
        times = sorted(times)
        if len(times) == 0:
            return TimingStats(0.0, 0.0, 0.0, 0.0)

        return TimingStats(
            round(times[0], 1),
            round(times[-1], 1),
            round(times[int(len(times) / 2)], 1),
            round((sum(times) / len(times)), 1)
        )

    @classmethod
    def from_runs(cls, runs: list[Run]) -> "TimingStats":
        runtimes = [run.result.execution_time_millis / 1000 for run in runs if not run.result.execution_error]
        return cls.from_times(runtimes)

    @staticmethod
    def empty() -> "TimingStats":
        return TimingStats(-1.0, -1.0, -1.0, -1.0)


def get_debug_log(run: Run) -> str:
    debug_log_file_path = os.path.join(run.dir, 'debug.log')
    if not os.path.exists(debug_log_file_path):
        log.warning(f"Missing debug log for directory {run.dir}")
        exit(1)

    with open(debug_log_file_path, 'r', errors='ignore') as f:
        debug_log = f.read()
    return debug_log


def get_infill_times_and_peak_vram(runs: list[Run]) -> tuple["TimingStats", int]:
    times = []
    peak_vram = -1
    for run in runs:
        if run.result.execution_error:
            continue

        infill_time = 0
        lines = get_debug_log(run).split("\n")
        for line in lines:
            if re.fullmatch(r"\d{2}:\d{2}:\d{2}\.\d{3} DEBUG PlmMutation:\d+ - Mask predict for modification point .*", line):
                infill_time += float(line.split('completed in ')[1].split(' ')[0])

                if 'with peak memory usage of' in line:
                    vram = int(line.split(' ')[-2])
                    if vram > peak_vram:
                        peak_vram = vram

        times.append(infill_time)

        if infill_time == 0:
            print('WARNING: Infill time for', run.name, 'is 0')

    return TimingStats.from_times(times), peak_vram


# Ingredient stats

@dataclasses.dataclass
class IngredientStats:
    clm_parse_rate: float
    n_clm_parse: int
    clm_compilation_rate: float
    n_clm_compile: int
    redundancy_compilation_rate: float
    r_redundancy_compile: int

    @staticmethod
    def empty():
        return IngredientStats(-1.0, -1, -1.0, -1, -1.0, -1)


def get_ingredient_stats(runs: list[Run]) -> IngredientStats:
    clm_compile_outcomes: list[bool] = []
    redundancy_compile_outcomes: list[bool] = []
    nr_clm_ingredients = 0
    nr_clm_parse_fails = 0

    for run in runs:
        if run.result.execution_error:
            continue

        summaries_dir = os.path.join(run.dir, 'summaries')
        summary_files = os.listdir(summaries_dir)

        for summary_file in summary_files:
            if summary_file.startswith('orig'):
                # Skip empty patch
                continue

            with open(os.path.join(summaries_dir, summary_file), 'r') as f:
                summary = f.read()

            fitness_info = summary.split('======== fitness ========')[1].split('======== variant ========')[0]
            variant_info = summary.split('======== variant ========')[1].split('======== diff ========')[0]

            compile_result = 'compile success: true' in fitness_info
            clm_ingr = 'plmIngr=t' in variant_info

            if not 'plmIngr=' in variant_info:
                print(summary_file)
                print('WARNING: CLM ingredient stats not available, skipping analysis')
                return IngredientStats.empty()

            if clm_ingr:
                clm_compile_outcomes.append(compile_result)
            else:
                redundancy_compile_outcomes.append(compile_result)

        with open(os.path.join(run.dir, 'debug.log'), 'r', errors='ignore') as f:
            lines = f.readlines()

        for line in lines:
            if re.fullmatch(r"\d{2}:\d{2}:\d{2}\.\d{3} DEBUG PlmMutation:\d+ - Got \d+ unique non-empty mask replacements\n", line):
                nr_replacements = int(line.split('Got ')[1].split(' ')[0])
                nr_clm_ingredients += nr_replacements
            elif re.fullmatch(r"\d{2}:\d{2}:\d{2}\.\d{3} DEBUG PlmMutation:\d+ - Mask predict produced an unparseable statement\n", line):
                nr_clm_ingredients += 1
                nr_clm_parse_fails += 1

    return IngredientStats(
        round(1 - (nr_clm_parse_fails / nr_clm_ingredients), 3) if nr_clm_ingredients else -1.0,
        nr_clm_ingredients,
        round(len([c for c in clm_compile_outcomes if c]) / len(clm_compile_outcomes), 3) if clm_compile_outcomes else -1.0,
        len(clm_compile_outcomes),
        round(len([c for c in redundancy_compile_outcomes if c]) / len(redundancy_compile_outcomes), 3) if redundancy_compile_outcomes else -1.0,
        len(redundancy_compile_outcomes)
    )


# Utils and main fn
def fmt_rows(data: list[tuple]) -> list[tuple[str]]:
    data_strings = [
        tuple(str(x) for x in row)
        for row in data
    ]
    max_col_lengths = []
    for i in range(len(data[0])):
        max_col_lengths.append(max(len(row[i]) for row in data_strings) + 1)

    return [
        tuple(value.ljust(max_col_lengths[i]) for i, value in enumerate(row))
        for row in data_strings
    ]


def tuples_to_csv(data: list[tuple]) -> str:
    formatted_rows = fmt_rows(data)
    result = ""
    for row in formatted_rows:
        result += ", ".join(elem for elem in row) + "\n"
    return result


def tuples_to_markdown(data: list[tuple]) -> str:
    formatted_rows = fmt_rows(data)
    formatted_rows.insert(1, tuple('-' * len(col_header) for col_header in formatted_rows[0]))
    result = ""
    for row in formatted_rows:
        result += "|" + ("|".join(row)) + "|\n"
    return result


def avg(data: list[int | float], ndigits: int = 1) -> float:
    return round(sum(data) / len(data), ndigits)


def main(results_dir: str, failures_only: bool = False, markdown: bool = False, full: bool = False):
    if results_dir.endswith(os.sep):
        results_dir = results_dir[:-len(os.sep)]

    if not os.path.exists(results_dir):
        print(f"Directory {results_dir} does not exist")

    run_data = get_runs(results_dir)
    run_data.sort(key=lambda d: d.name)
    print(f"Found a total of {len(run_data)} results")

    if failures_only:
        run_data = [run for run in run_data if run.result.execution_error]

    rows: list[tuple] = [("Run Timestamp", "Bug Name", "Seed", "Error", "Sanity OK", "Time (s)", "Correct Patches", "Total Patches", "Converged At", "Max New Variants")]
    for data in run_data:
        rows.append(
            (
                data.name.split("_buggy_")[1],
                data.bug_name,
                data.preferences['seed'],
                data.result.execution_error,
                data.result.sanity_check_ok,
                int(data.result.execution_time_millis / 1000),
                len(data.result.population.correct_variants),
                data.result.population.nr_unique_variants,
                data.converged_at(),
                data.max_new_variants()
            )
        )

    if markdown:
        print(tuples_to_markdown(rows))
    else:
        print(tuples_to_csv(rows))

    total_seconds = int(sum(data.result.execution_time_millis for data in run_data) / 1000)
    nr_successful_runs = len([data for data in run_data if data.result.population.correct_variants])
    nr_execution_errors = len([data for data in run_data if data.result.execution_error])
    runtime_stats = TimingStats.from_runs(run_data)
    infill_stats, peak_vram = get_infill_times_and_peak_vram(run_data) if full else (TimingStats.empty(), -1)
    ingredient_stats = get_ingredient_stats(run_data) if full else IngredientStats.empty()

    print("")
    print("General stats:")
    print(f"total execution time = {total_seconds}s ({round(total_seconds / 3600, 1)}h)")
    print(f"successful runs (bug fixed) = {nr_successful_runs} / {len(run_data)} ({round((nr_successful_runs / len(run_data)) * 100, 1)}%)")
    print(f"execution errors = {nr_execution_errors} {':D' if nr_execution_errors == 0 else ''}")
    print("")

    total_correct_patches = sum(len(data.result.population.correct_variants) for data in run_data)
    avg_evaluated_patches = avg([data.result.population.nr_unique_variants for data in run_data])
    avg_convergence_generation = avg([data.converged_at() for data in run_data])
    avg_max_new_variants = avg([data.max_new_variants() for data in run_data])
    fixes_244 = {project: 0 for project in D4J_224_PROJECTS}
    fixes_244['tot'] = 0
    for data in run_data:
        project = get_224_project(data.bug_name)
        if project is None:
            continue
        if len(data.result.population.correct_variants) > 0:
            fixes_244[project] += 1
            fixes_244['tot'] += 1

    print(f"total correct patches = {total_correct_patches}")
    print(f"avg evaluated patches = {avg_evaluated_patches}")
    print(f"avg convergence gen = {avg_convergence_generation}")
    print(f"avg max new variants = {avg_max_new_variants}")
    print(f"fixes for 224 subset = {fixes_244}")
    print(f"runtime stats (seconds, min med max avg): {runtime_stats.min}, {runtime_stats.median}, {runtime_stats.max}, {runtime_stats.avg}")
    print(f"infill time stats (seconds, min med max avg): {infill_stats.min}, {infill_stats.median}, {infill_stats.max}, {infill_stats.avg}")
    print(f"peak vram: {peak_vram} MiB ({round(peak_vram / 1024, 1)} GiB)")
    print(f"ingredient stats: {ingredient_stats}")
    print("")

    stats = [
        ('execution time (h)', 'bugs fixed', 'execution errors', 'total t/a patches', 'avg evaluated patches', 'avg convergence gen', 'avg max new variants', 'nr bugs'),
        (round(total_seconds / 3600, 1), nr_successful_runs, nr_execution_errors, total_correct_patches, avg_evaluated_patches, avg_convergence_generation, avg_max_new_variants,
         len(run_data))
    ]

    if markdown:
        print(tuples_to_markdown(stats))
    else:
        print(tuples_to_csv(stats))

    print('file path', results_dir)

    plm_log_path = os.path.join(results_dir, 'plm.log')
    if os.path.exists(plm_log_path) and os.path.getsize(plm_log_path) < 1000:
        print("WARNING: PLM log is very small, it might have crashed on startup")


if __name__ == "__main__":
    fire.Fire(main)
