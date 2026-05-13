import csv
import glob
import math
import os
import statistics


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REPORTS_ROOT = os.path.join(ROOT, "reports", "density")
RESULTS_DIR = os.path.join(ROOT, "results")
RUNTIME_CSV = os.path.join(RESULTS_DIR, "eetle_density_runtime.csv")
DETAIL_CSV = os.path.join(RESULTS_DIR, "eetle_density_results.csv")
SUMMARY_CSV = os.path.join(RESULTS_DIR, "eetle_density_summary.csv")

ATTACK_TYPES = set([
    "BLACKHOLE", "ON_OFF", "FALSE_EVENT", "ENV_CAMOUFLAGE",
    "CROSS_REGION"
])

DETAIL_FIELDS = [
    "density", "N", "seed",
    "average_neighbor_degree",
    "average_GT_normal", "average_GT_malicious",
    "detection_rate", "false_positive_rate", "F1",
    "false_message_detection_rate",
    "malicious_leader_rate", "leader_switch_count",
    "average_leader_score",
    "communication_overhead", "delivery_prob", "latency_avg",
    "runtime"
]

SUMMARY_FIELDS = ["density", "N", "runs"]
for field in DETAIL_FIELDS[3:]:
    SUMMARY_FIELDS.append(field + "_mean")
    SUMMARY_FIELDS.append(field + "_std")


def read_csv(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, newline="") as f:
        return list(csv.DictReader(f))


def find_report(report_dir, prefix, report_name):
    pattern = os.path.join(report_dir, prefix + "_" + report_name + "*.txt")
    matches = sorted(glob.glob(pattern))
    return matches[-1] if matches else None


def as_float(value, default=None):
    try:
        if value in (None, "", "NA", "NaN"):
            return default
        return float(value)
    except ValueError:
        return default


def as_int(value, default=0):
    try:
        if value in (None, "", "NA", "NaN"):
            return default
        return int(float(value))
    except ValueError:
        return default


def average(values):
    values = [v for v in values if v is not None]
    return sum(values) / len(values) if values else None


def std(values):
    values = [v for v in values if v is not None]
    if len(values) < 2:
        return 0.0 if len(values) == 1 else None
    return statistics.stdev(values)


def last_window(rows):
    times = [as_float(r.get("time")) for r in rows]
    times = [t for t in times if t is not None]
    if not times:
        return []
    cutoff = max(times) * 0.9
    return [r for r in rows if as_float(r.get("time"), -1.0) >= cutoff]


def fmt(value):
    if value is None:
        return "NA"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        if math.isnan(value):
            return "NA"
        return "%.4f" % value
    return str(value)


def parse_experiment_dir(path):
    name = os.path.basename(path.rstrip("\\/"))
    # expected n60_s01
    try:
        parts = name.split("_")
        n = int(parts[0][1:])
        seed = int(parts[1][1:])
        prefix = "EETLEDensityN%dS%02d" % (n, seed)
        return n, seed, prefix
    except Exception:
        return None


def load_runtime():
    runtime = {}
    for row in read_csv(RUNTIME_CSV):
        n = as_int(row.get("N"))
        seed = as_int(row.get("seed"))
        runtime[(n, seed)] = as_float(row.get("runtime"))
    return runtime


def attack_map_from_detection(report_dir, prefix):
    rows = read_csv(find_report(report_dir, prefix, "DetectionReport"))
    attack = {}
    for row in rows:
        if row.get("recordType") != "NODE":
            continue
        node = row.get("node")
        if node not in (None, ""):
            attack[str(int(float(node)))] = row.get("attackType")
    return attack


def summarize_detection(report_dir, prefix):
    rows = read_csv(find_report(report_dir, prefix, "DetectionReport"))
    summaries = [r for r in rows if r.get("recordType") == "SUMMARY" and
                 r.get("attackType") == "ALL"]
    window = last_window(summaries)
    return {
        "detection_rate": average([as_float(r.get("recall")) for r in window]),
        "false_positive_rate": average([as_float(r.get("fpr")) for r in window]),
        "F1": average([as_float(r.get("f1")) for r in window]),
    }


def summarize_neighbor_degree(report_dir, prefix):
    rows = read_csv(find_report(report_dir, prefix, "NeighborDegreeReport"))
    window = last_window(rows)
    return average([as_float(r.get("averageNeighborDegree")) for r in window])


def summarize_global_trust(report_dir, prefix, attack_map):
    rows = read_csv(find_report(report_dir, prefix, "GlobalTrustReport"))
    window = last_window(rows)
    normal = []
    malicious = []
    for row in window:
        target = row.get("target")
        if target in (None, ""):
            continue
        attack_type = attack_map.get(str(int(float(target))))
        gt = as_float(row.get("globalTrust"))
        if gt is None:
            continue
        if attack_type == "NORMAL":
            normal.append(gt)
        elif attack_type in ATTACK_TYPES:
            malicious.append(gt)
    return average(normal), average(malicious)


def summarize_false_message(report_dir, prefix):
    rows = read_csv(find_report(report_dir, prefix, "EventTrustReport"))
    false_rows = [r for r in rows if str(r.get("falseReport")).lower() == "true"]
    if not false_rows:
        return None
    penalties = [
        r for r in false_rows
        if str(r.get("appliedPenalty")).lower() == "true"
    ]
    return len(penalties) / float(len(false_rows))


def summarize_leader(report_dir, prefix):
    rows = read_csv(find_report(report_dir, prefix, "LeaderReport"))
    leaders = [r for r in rows if r.get("recordType") == "LEADER"]
    if not leaders:
        return None, None, None
    malicious = [
        r for r in leaders
        if r.get("attackType") not in ("NORMAL", "UNKNOWN", "", None)
    ]
    scores = [as_float(r.get("finalScore")) for r in leaders]
    final = leaders[-1]
    return (
        len(malicious) / float(len(leaders)),
        as_int(final.get("leaderChangeCount")),
        average(scores),
    )


def summarize_message_stats(report_dir, prefix):
    path = find_report(report_dir, prefix, "MessageStatsReport")
    stats = {}
    if path and os.path.exists(path):
        with open(path) as f:
            for line in f:
                if ":" in line:
                    key, value = line.strip().split(":", 1)
                    stats[key.strip()] = value.strip()
    return (
        as_float(stats.get("overhead_ratio")),
        as_float(stats.get("delivery_prob")),
        as_float(stats.get("latency_avg")),
    )


def summarize_one(report_dir, n, seed, prefix, runtime_map):
    attack_map = attack_map_from_detection(report_dir, prefix)
    row = {
        "density": n,  # fixed 1 km^2 area
        "N": n,
        "seed": seed,
        "runtime": runtime_map.get((n, seed)),
    }
    row["average_neighbor_degree"] = summarize_neighbor_degree(
        report_dir, prefix)
    row.update(summarize_detection(report_dir, prefix))
    normal_gt, malicious_gt = summarize_global_trust(
        report_dir, prefix, attack_map)
    row["average_GT_normal"] = normal_gt
    row["average_GT_malicious"] = malicious_gt
    row["false_message_detection_rate"] = summarize_false_message(
        report_dir, prefix)
    malicious_leader_rate, switch_count, avg_score = summarize_leader(
        report_dir, prefix)
    row["malicious_leader_rate"] = malicious_leader_rate
    row["leader_switch_count"] = switch_count
    row["average_leader_score"] = avg_score
    overhead, delivery, latency = summarize_message_stats(report_dir, prefix)
    row["communication_overhead"] = overhead
    row["delivery_prob"] = delivery
    row["latency_avg"] = latency
    return row


def write_detail(rows):
    os.makedirs(RESULTS_DIR, exist_ok=True)
    with open(DETAIL_CSV, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=DETAIL_FIELDS)
        writer.writeheader()
        for row in rows:
            writer.writerow({k: fmt(row.get(k)) for k in DETAIL_FIELDS})


def write_summary(rows):
    grouped = {}
    for row in rows:
        grouped.setdefault(row["N"], []).append(row)
    summary_rows = []
    for n in sorted(grouped.keys()):
        group = grouped[n]
        out = {"density": n, "N": n, "runs": len(group)}
        for field in DETAIL_FIELDS[3:]:
            values = [r.get(field) for r in group]
            out[field + "_mean"] = average(values)
            out[field + "_std"] = std(values)
        summary_rows.append(out)

    with open(SUMMARY_CSV, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=SUMMARY_FIELDS)
        writer.writeheader()
        for row in summary_rows:
            writer.writerow({k: fmt(row.get(k)) for k in SUMMARY_FIELDS})


def main():
    runtime_map = load_runtime()
    rows = []
    for report_dir in sorted(glob.glob(os.path.join(REPORTS_ROOT, "n*_s*"))):
        parsed = parse_experiment_dir(report_dir)
        if not parsed:
            continue
        n, seed, prefix = parsed
        rows.append(summarize_one(report_dir, n, seed, prefix, runtime_map))

    write_detail(rows)
    write_summary(rows)
    print("Wrote " + DETAIL_CSV)
    print("Wrote " + SUMMARY_CSV)


if __name__ == "__main__":
    main()
