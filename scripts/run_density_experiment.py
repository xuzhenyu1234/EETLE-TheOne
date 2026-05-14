import argparse
import csv
import os
import subprocess
import time


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
TEMPLATE = os.path.join(ROOT, "settings", "eetle_paper_full_60.txt")
SETTINGS_DIR = os.path.join(ROOT, "settings", "density")
REPORTS_DIR = os.path.join(ROOT, "reports", "density")
RESULTS_DIR = os.path.join(ROOT, "results")
RUNTIME_CSV = os.path.join(RESULTS_DIR, "eetle_density_runtime.csv")

DEFAULT_NODE_COUNTS = [20, 40, 60, 80, 100, 120]
DEFAULT_RUNS = 20
AREA_M2 = 1000.0 * 1000.0


def read_template():
    with open(TEMPLATE, "r") as f:
        return f.read()


def replace_setting(text, key, value):
    lines = text.splitlines()
    replaced = False
    prefix = key + " ="
    for i in range(len(lines)):
        if lines[i].strip().startswith(prefix):
            lines[i] = key + " = " + str(value)
            replaced = True
            break
    if not replaced:
        lines.append(key + " = " + str(value))
    return "\n".join(lines) + "\n"


def ensure_neighbor_report(text):
    lines = text.splitlines()
    report_numbers = []
    has_neighbor = False
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("Report.report") and " = " in stripped:
            left, right = stripped.split("=", 1)
            right = right.strip()
            if right == "NeighborDegreeReport":
                has_neighbor = True
            suffix = left.replace("Report.report", "").strip()
            try:
                report_numbers.append(int(suffix))
            except ValueError:
                pass

    if has_neighbor:
        return text

    next_report = max(report_numbers) + 1 if report_numbers else 1
    text = replace_setting(text, "Report.nrofReports", next_report)
    text += "Report.report%d = NeighborDegreeReport\n" % next_report
    text += "NeighborDegreeReport.interval = 100.0\n"
    return text


def build_config(node_count, seed):
    scenario = "EETLEDensityN%dS%02d" % (node_count, seed)
    report_dir = "reports/density/n%d_s%02d/" % (node_count, seed)
    text = read_template()
    text = replace_setting(text, "Scenario.name", scenario)
    text = replace_setting(text, "Group.nrofHosts", node_count)
    text = replace_setting(text, "MovementModel.rngSeed", seed)
    text = replace_setting(text, "EETLERouter.attackAssignmentMode",
                           "randomRatio")
    text = replace_setting(text, "EETLERouter.attackSeed", 10000 + seed)
    text = replace_setting(text, "EETLERouter.attackRatio", "0.20")
    text = replace_setting(text, "Report.reportDir", report_dir)
    text = replace_setting(text, "Report.prefix", scenario)
    # Keep a stable source set while preventing invalid host indexes for N=20.
    max_source = min(19, node_count - 1)
    text = replace_setting(text, "Events1.hosts", "0,%d" % max_source)
    text = ensure_neighbor_report(text)
    return scenario, report_dir, text


def write_config(node_count, seed):
    os.makedirs(SETTINGS_DIR, exist_ok=True)
    scenario, report_dir, text = build_config(node_count, seed)
    path = os.path.join(SETTINGS_DIR, "eetle_density_n%d_s%02d.txt" %
                        (node_count, seed))
    with open(path, "w") as f:
        f.write(text)
    return path, scenario, report_dir


def append_runtime(row):
    os.makedirs(RESULTS_DIR, exist_ok=True)
    exists = os.path.exists(RUNTIME_CSV)
    with open(RUNTIME_CSV, "a", newline="") as f:
        fieldnames = ["N", "density", "seed", "scenario", "settings",
                      "reportDir", "runtime"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        if not exists:
            writer.writeheader()
        writer.writerow(row)


def run_one(settings_path):
    start = time.time()
    subprocess.check_call(["cmd", "/c", "one.bat", "-b", "1",
                           settings_path], cwd=ROOT)
    return time.time() - start


def main():
    parser = argparse.ArgumentParser(
        description="Generate and optionally run EETLE density experiments.")
    parser.add_argument("--runs", type=int, default=DEFAULT_RUNS,
                        help="random seeds per density level")
    parser.add_argument("--nodes", default=",".join(
                        str(n) for n in DEFAULT_NODE_COUNTS),
                        help="comma-separated node counts")
    parser.add_argument("--execute", action="store_true",
                        help="run simulations after generating settings")
    parser.add_argument("--compile", action="store_true",
                        help="compile before executing simulations")
    args = parser.parse_args()

    node_counts = [int(x.strip()) for x in args.nodes.split(",")
                   if x.strip()]
    os.makedirs(os.path.join(ROOT, "reports", "density"), exist_ok=True)

    if args.execute and args.compile:
        subprocess.check_call(["cmd", "/c", "compile.bat"], cwd=ROOT)

    generated = []
    for node_count in node_counts:
        for seed in range(1, args.runs + 1):
            settings_path, scenario, report_dir = write_config(node_count, seed)
            generated.append(settings_path)
            if args.execute:
                runtime = run_one(settings_path)
                append_runtime({
                    "N": node_count,
                    "density": node_count / (AREA_M2 / 1000000.0),
                    "seed": seed,
                    "scenario": scenario,
                    "settings": os.path.relpath(settings_path, ROOT),
                    "reportDir": report_dir,
                    "runtime": "%.4f" % runtime,
                })
                print("finished %s runtime=%.2fs" % (scenario, runtime))

    print("generated %d settings under %s" % (len(generated), SETTINGS_DIR))
    if not args.execute:
        print("dry run only. Add --execute to run simulations.")


if __name__ == "__main__":
    main()
