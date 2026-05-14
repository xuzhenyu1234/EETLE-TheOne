import csv
import glob
import math
import os


EXPERIMENTS = [
    ("0.00", 0, "EETLERatio00_60", "reports/eetle_ratio_00_60"),
    ("0.10", 6, "EETLERatio10_60", "reports/eetle_ratio_10_60"),
    ("0.20", 12, "EETLERatio20_60", "reports/eetle_ratio_20_60"),
    ("0.30", 18, "EETLERatio30_60", "reports/eetle_ratio_30_60"),
    ("0.40", 24, "EETLERatio40_60", "reports/eetle_ratio_40_60"),
]

ATTACK_TYPES = [
    "BLACKHOLE",
    "ON_OFF",
    "FALSE_EVENT",
    "ENV_CAMOUFLAGE",
    "CROSS_REGION",
]

FIELDNAMES = [
    "ratio",
    "attackNodeCount",
    "normalNodeCount",
    "precision",
    "recall",
    "f1",
    "fpr",
    "TP",
    "FP",
    "FN",
    "TN",
    "normal_avg_gt",
    "malicious_avg_gt",
    "blackhole_avg_gt",
    "onoff_avg_gt",
    "false_event_avg_gt",
    "env_camouflage_avg_gt",
    "cross_region_avg_gt",
    "delivery_prob",
    "overhead_ratio",
    "latency_avg",
    "leaderChangeCount",
    "maliciousLeaderCount",
    "normalLeaderRatio",
    "finalLeader",
    "finalLeaderAttackType",
    "attackAttempts",
    "blackholeDrops",
    "onOffDrops",
    "falseEventsInjected",
    "envCamouflageDrops",
    "crossRegionDrops",
]


def find_report(report_dir, prefix, report_name):
    pattern = os.path.join(report_dir, prefix + "_" + report_name + "*.txt")
    matches = sorted(glob.glob(pattern))
    if not matches:
        return None
    return matches[-1]


def read_csv_report(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, newline="") as f:
        return list(csv.DictReader(f))


def as_float(value, default=None):
    try:
        if value is None or value == "" or value == "NaN":
            return default
        return float(value)
    except ValueError:
        return default


def as_int(value, default=0):
    try:
        if value is None or value == "":
            return default
        return int(float(value))
    except ValueError:
        return default


def fmt(value):
    if value is None:
        return "NA"
    if isinstance(value, str):
        return value
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        if math.isnan(value):
            return "NA"
        return "{:.4f}".format(value)
    return str(value)


def average(values):
    values = [v for v in values if v is not None]
    if not values:
        return None
    return sum(values) / len(values)


def last_window(rows):
    times = [as_float(r.get("time")) for r in rows]
    times = [t for t in times if t is not None]
    if not times:
        return []
    max_time = max(times)
    cutoff = max_time * 0.9
    return [r for r in rows if as_float(r.get("time"), -1.0) >= cutoff]


def load_attack_map(report_dir, prefix):
    attack_map = {}
    detection_rows = read_csv_report(
        find_report(report_dir, prefix, "DetectionReport"))
    for row in detection_rows:
        if row.get("recordType") != "NODE":
            continue
        node = row.get("node")
        attack_type = row.get("attackType")
        if node not in (None, "") and attack_type:
            attack_map[str(int(float(node)))] = attack_type

    if attack_map:
        return attack_map

    trust_rows = read_csv_report(
        find_report(report_dir, prefix, "EETLETrustReport"))
    for row in trust_rows:
        node = row.get("node")
        attack_type = row.get("attackType")
        if node not in (None, "") and attack_type:
            attack_map[str(int(float(node)))] = attack_type
    return attack_map


def summarize_detection(report_dir, prefix, attack_count):
    rows = read_csv_report(find_report(report_dir, prefix, "DetectionReport"))
    summary = [
        r for r in rows
        if r.get("recordType") == "SUMMARY" and r.get("attackType") == "ALL"
    ]
    window = last_window(summary)
    final = summary[-1] if summary else {}

    precision_values = [as_float(r.get("precision")) for r in window]
    recall_values = [as_float(r.get("recall")) for r in window]
    f1_values = [as_float(r.get("f1")) for r in window]
    fpr_values = [as_float(r.get("fpr")) for r in window]

    tp = as_int(final.get("TP"))
    fp = as_int(final.get("FP"))
    fn = as_int(final.get("FN"))
    tn = as_int(final.get("TN"))

    precision = average(precision_values)
    recall = average(recall_values)
    f1 = average(f1_values)
    fpr = average(fpr_values)

    if attack_count == 0:
        recall = None
        f1 = None
        if tp + fp == 0:
            precision = None

    return {
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "fpr": fpr,
        "TP": tp,
        "FP": fp,
        "FN": fn,
        "TN": tn,
    }


def summarize_global_trust(report_dir, prefix, attack_map, attack_count):
    rows = read_csv_report(find_report(report_dir, prefix, "GlobalTrustReport"))
    window = last_window(rows)
    values_by_type = {"NORMAL": []}
    for attack_type in ATTACK_TYPES:
        values_by_type[attack_type] = []

    for row in window:
        target = row.get("target")
        attack_type = attack_map.get(str(int(float(target)))) if target else None
        if not attack_type:
            continue
        gt = as_float(row.get("globalTrust"))
        if gt is None:
            continue
        values_by_type.setdefault(attack_type, []).append(gt)

    malicious_values = []
    for attack_type in ATTACK_TYPES:
        malicious_values.extend(values_by_type.get(attack_type, []))

    result = {
        "normal_avg_gt": average(values_by_type.get("NORMAL", [])),
        "malicious_avg_gt": average(malicious_values),
        "blackhole_avg_gt": average(values_by_type.get("BLACKHOLE", [])),
        "onoff_avg_gt": average(values_by_type.get("ON_OFF", [])),
        "false_event_avg_gt": average(values_by_type.get("FALSE_EVENT", [])),
        "env_camouflage_avg_gt": average(
            values_by_type.get("ENV_CAMOUFLAGE", [])),
        "cross_region_avg_gt": average(values_by_type.get("CROSS_REGION", [])),
    }

    if attack_count == 0:
        for key in [
            "malicious_avg_gt",
            "blackhole_avg_gt",
            "onoff_avg_gt",
            "false_event_avg_gt",
            "env_camouflage_avg_gt",
            "cross_region_avg_gt",
        ]:
            result[key] = None
    return result


def summarize_leader(report_dir, prefix):
    rows = read_csv_report(find_report(report_dir, prefix, "LeaderReport"))
    leaders = [r for r in rows if r.get("recordType") == "LEADER"]
    if not leaders:
        return {
            "leaderChangeCount": 0,
            "maliciousLeaderCount": 0,
            "normalLeaderRatio": None,
            "finalLeader": "NA",
            "finalLeaderAttackType": "NA",
        }

    final = leaders[-1]
    malicious_count = sum(
        1 for r in leaders
        if r.get("attackType") not in ("NORMAL", "UNKNOWN", "", None)
    )
    normal_count = sum(1 for r in leaders if r.get("attackType") == "NORMAL")
    return {
        "leaderChangeCount": as_int(final.get("leaderChangeCount")),
        "maliciousLeaderCount": malicious_count,
        "normalLeaderRatio": normal_count / float(len(leaders)),
        "finalLeader": final.get("currentLeader", "NA"),
        "finalLeaderAttackType": final.get("attackType", "NA"),
    }


def summarize_message_stats(report_dir, prefix):
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
        "delivery_prob": as_float(stats.get("delivery_prob")),
        "overhead_ratio": as_float(stats.get("overhead_ratio")),
        "latency_avg": as_float(stats.get("latency_avg")),
    }


def summarize_attacks(report_dir, prefix):
    rows = read_csv_report(find_report(report_dir, prefix, "EETLETrustReport"))
    if not rows:
        return {
            "attackAttempts": 0,
            "blackholeDrops": 0,
            "onOffDrops": 0,
            "falseEventsInjected": 0,
            "envCamouflageDrops": 0,
            "crossRegionDrops": 0,
        }

    final_rows = last_window(rows)
    max_time = max(as_float(r.get("time"), -1.0) for r in rows)
    final_rows = [r for r in rows if as_float(r.get("time"), -1.0) == max_time]

    return {
        "attackAttempts": sum(as_int(r.get("totalAttackAttempts"))
                              for r in final_rows),
        "blackholeDrops": sum(as_int(r.get("blackholeDrops"))
                              for r in final_rows),
        "onOffDrops": sum(as_int(r.get("onOffDrops")) for r in final_rows),
        "falseEventsInjected": sum(as_int(r.get("falseEventsInjected"))
                                   for r in final_rows),
        "envCamouflageDrops": sum(as_int(r.get("envCamouflageDrops"))
                                  for r in final_rows),
        "crossRegionDrops": sum(as_int(r.get("crossRegionDrops"))
                                for r in final_rows),
    }


def summarize_experiment(ratio, attack_count, prefix, report_dir):
    attack_map = load_attack_map(report_dir, prefix)
    actual_attack_count = sum(
        1 for t in attack_map.values()
        if t not in ("NORMAL", "UNKNOWN", "", None)
    )
    if actual_attack_count or attack_count == 0:
        attack_count = actual_attack_count

    row = {
        "ratio": ratio,
        "attackNodeCount": attack_count,
        "normalNodeCount": 60 - attack_count,
    }
    row.update(summarize_detection(report_dir, prefix, attack_count))
    row.update(summarize_global_trust(
        report_dir, prefix, attack_map, attack_count))
    row.update(summarize_message_stats(report_dir, prefix))
    row.update(summarize_leader(report_dir, prefix))
    row.update(summarize_attacks(report_dir, prefix))
    return row


def main():
    rows = []
    for ratio, expected_attack_count, prefix, report_dir in EXPERIMENTS:
        rows.append(summarize_experiment(
            ratio, expected_attack_count, prefix, report_dir))

    os.makedirs("results", exist_ok=True)
    output = os.path.join("results", "eetle_ratio_summary.csv")
    with open(output, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDNAMES)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: fmt(row.get(key)) for key in FIELDNAMES})

    print("ratio,precision,recall,f1,fpr,delivery_prob,"
          "leaderChangeCount,maliciousLeaderCount,normalLeaderRatio")
    for row in rows:
        print(",".join([
            fmt(row.get("ratio")),
            fmt(row.get("precision")),
            fmt(row.get("recall")),
            fmt(row.get("f1")),
            fmt(row.get("fpr")),
            fmt(row.get("delivery_prob")),
            fmt(row.get("leaderChangeCount")),
            fmt(row.get("maliciousLeaderCount")),
            fmt(row.get("normalLeaderRatio")),
        ]))
    print("Wrote " + output)


if __name__ == "__main__":
    main()
