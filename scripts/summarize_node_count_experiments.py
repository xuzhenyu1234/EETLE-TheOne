import csv
import glob
import math
import os


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUTPUT_CSV = os.path.join(ROOT, "reports", "node_count_summary.csv")
OUTPUT_MD = os.path.join(ROOT, "reports", "node_count_summary.md")

EXPERIMENTS = [
    (30, "EETLENodes30DualRegion",
     os.path.join(ROOT, "reports", "eetle_nodes_30_dual_region")),
    (60, "EETLENodes60DualRegion",
     os.path.join(ROOT, "reports", "eetle_nodes_60_dual_region")),
    (90, "EETLENodes90DualRegion",
     os.path.join(ROOT, "reports", "eetle_nodes_90_dual_region")),
    (120, "EETLENodes120DualRegion",
     os.path.join(ROOT, "reports", "eetle_nodes_120_dual_region")),
]

ATTACK_TYPES = [
    "NORMAL",
    "BLACKHOLE",
    "ON_OFF",
    "FALSE_EVENT",
    "ENV_CAMOUFLAGE",
    "CROSS_REGION",
]

FIELDNAMES = [
    "nodeCount",
    "sim_time",
    "created",
    "delivered",
    "delivery_prob",
    "overhead_ratio",
    "latency_avg",
    "latency_med",
    "dropped",
    "relayed",
    "finalLeader",
    "finalLeaderAttackType",
    "finalLeaderTrust",
    "leaderChangeCount",
    "maliciousLeaderRecords",
    "detectionRate",
    "falsePositiveRate",
    "accuracy",
    "TP",
    "FP",
    "TN",
    "FN",
    "normalLeaderRatio",
    "crossRegionLeaderCount",
    "avgGlobalTrust_NORMAL",
    "avgGlobalTrust_BLACKHOLE",
    "avgGlobalTrust_ON_OFF",
    "avgGlobalTrust_FALSE_EVENT",
    "avgGlobalTrust_ENV_CAMOUFLAGE",
    "avgGlobalTrust_CROSS_REGION",
]


def find_report(report_dir, prefix, report_name):
    pattern = os.path.join(report_dir, prefix + "_" + report_name + "*.txt")
    matches = sorted(glob.glob(pattern))
    return matches[-1] if matches else None


def read_csv_report(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, newline="") as f:
        return list(csv.DictReader(f))


def as_float(value, default=None):
    try:
        if value in (None, "", "NA", "NaN"):
            return default
        value = float(value)
        if math.isnan(value) or math.isinf(value):
            return default
        return value
    except ValueError:
        return default


def as_int(value, default=0):
    try:
        if value in (None, "", "NA", "NaN"):
            return default
        return int(float(value))
    except ValueError:
        return default


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


def average(values):
    values = [v for v in values if v is not None]
    return sum(values) / len(values) if values else None


def last_window(rows):
    times = [as_float(r.get("time")) for r in rows]
    times = [t for t in times if t is not None]
    if not times:
        return []
    cutoff = max(times) * 0.9
    return [r for r in rows if as_float(r.get("time"), -1.0) >= cutoff]


def read_message_stats(report_dir, prefix):
    path = find_report(report_dir, prefix, "MessageStatsReport")
    stats = {}
    if path and os.path.exists(path):
        with open(path) as f:
            for line in f:
                if ":" not in line:
                    continue
                key, value = line.strip().split(":", 1)
                stats[key.strip()] = value.strip()
    return {
        "sim_time": as_float(stats.get("sim_time")),
        "created": as_int(stats.get("created")),
        "delivered": as_int(stats.get("delivered")),
        "delivery_prob": as_float(stats.get("delivery_prob")),
        "overhead_ratio": as_float(stats.get("overhead_ratio")),
        "latency_avg": as_float(stats.get("latency_avg")),
        "latency_med": as_float(stats.get("latency_med")),
        "dropped": as_int(stats.get("dropped")),
        "relayed": as_int(stats.get("relayed")),
    }


def load_attack_map(report_dir, prefix):
    attack_map = {}
    rows = read_csv_report(find_report(report_dir, prefix, "DetectionReport"))
    for row in rows:
        if row.get("recordType") != "NODE":
            continue
        node = row.get("node")
        if node not in (None, ""):
            attack_map[str(int(float(node)))] = row.get("attackType")
    if attack_map:
        return attack_map

    rows = read_csv_report(find_report(report_dir, prefix, "EETLETrustReport"))
    for row in rows:
        node = parse_node(row.get("node"))
        if node is not None:
            attack_map[str(node)] = row.get("attackType")
    return attack_map


def parse_node(value):
    if value in (None, ""):
        return None
    digits = ""
    for ch in str(value):
        if ch.isdigit():
            digits += ch
    if digits == "":
        return None
    return int(digits)


def summarize_detection(report_dir, prefix):
    rows = read_csv_report(find_report(report_dir, prefix, "DetectionReport"))
    summaries = [
        r for r in rows
        if r.get("recordType") == "SUMMARY" and r.get("attackType") == "ALL"
    ]
    window = last_window(summaries)
    final = summaries[-1] if summaries else {}
    tp = as_int(final.get("TP"))
    fp = as_int(final.get("FP"))
    fn = as_int(final.get("FN"))
    tn = as_int(final.get("TN"))
    total = tp + fp + fn + tn
    return {
        "detectionRate": average([as_float(r.get("recall")) for r in window]),
        "falsePositiveRate": average([as_float(r.get("fpr"))
                                      for r in window]),
        "accuracy": (tp + tn) / float(total) if total > 0 else None,
        "TP": tp,
        "FP": fp,
        "TN": tn,
        "FN": fn,
    }


def summarize_leader(report_dir, prefix):
    rows = read_csv_report(find_report(report_dir, prefix, "LeaderReport"))
    leaders = [r for r in rows if r.get("recordType") == "LEADER"]
    if not leaders:
        return {
            "finalLeader": "NA",
            "finalLeaderAttackType": "NA",
            "finalLeaderTrust": None,
            "leaderChangeCount": 0,
            "maliciousLeaderRecords": 0,
            "normalLeaderRatio": None,
            "crossRegionLeaderCount": 0,
        }

    final = leaders[-1]
    malicious = [
        r for r in leaders
        if r.get("attackType") not in ("NORMAL", "UNKNOWN", "", None)
    ]
    normal = [r for r in leaders if r.get("attackType") == "NORMAL"]
    cross_region = [
        r for r in leaders if r.get("attackType") == "CROSS_REGION"
    ]
    return {
        "finalLeader": final.get("currentLeader", "NA"),
        "finalLeaderAttackType": final.get("attackType", "NA"),
        "finalLeaderTrust": as_float(final.get("leaderTrust")),
        "leaderChangeCount": as_int(final.get("leaderChangeCount")),
        "maliciousLeaderRecords": len(malicious),
        "normalLeaderRatio": len(normal) / float(len(leaders)),
        "crossRegionLeaderCount": len(cross_region),
    }


def summarize_global_trust(report_dir, prefix, attack_map):
    rows = read_csv_report(find_report(report_dir, prefix, "GlobalTrustReport"))
    window = last_window(rows)
    values = {}
    for attack_type in ATTACK_TYPES:
        values[attack_type] = []

    for row in window:
        target = row.get("target")
        if target in (None, ""):
            continue
        attack_type = attack_map.get(str(int(float(target))))
        gt = as_float(row.get("globalTrust"))
        if attack_type in values and gt is not None:
            values[attack_type].append(gt)

    result = {}
    for attack_type in ATTACK_TYPES:
        result["avgGlobalTrust_" + attack_type] = average(values[attack_type])
    return result


def summarize_experiment(node_count, prefix, report_dir):
    attack_map = load_attack_map(report_dir, prefix)
    row = {"nodeCount": node_count}
    row.update(read_message_stats(report_dir, prefix))
    row.update(summarize_leader(report_dir, prefix))
    row.update(summarize_detection(report_dir, prefix))
    row.update(summarize_global_trust(report_dir, prefix, attack_map))
    return row


def write_csv(rows):
    with open(OUTPUT_CSV, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDNAMES)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: fmt(row.get(key)) for key in FIELDNAMES})


def write_md(rows):
    with open(OUTPUT_MD, "w") as f:
        f.write("# Node Count Summary\n\n")
        f.write("| " + " | ".join(FIELDNAMES) + " |\n")
        f.write("| " + " | ".join(["---"] * len(FIELDNAMES)) + " |\n")
        for row in rows:
            values = [fmt(row.get(key)) for key in FIELDNAMES]
            f.write("| " + " | ".join(values) + " |\n")


def main():
    os.makedirs(os.path.dirname(OUTPUT_CSV), exist_ok=True)
    rows = [
        summarize_experiment(node_count, prefix, report_dir)
        for node_count, prefix, report_dir in EXPERIMENTS
    ]
    write_csv(rows)
    write_md(rows)
    print("Wrote " + OUTPUT_CSV)
    print("Wrote " + OUTPUT_MD)


if __name__ == "__main__":
    main()
