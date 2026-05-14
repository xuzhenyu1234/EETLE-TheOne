import os

os.environ.setdefault("MPLCONFIGDIR", os.path.abspath("reports/mpl_cache"))

import matplotlib.pyplot as plt
import pandas as pd
from matplotlib import font_manager


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(ROOT, "results", "eetle_paper_figures")
RATIO_CSV = os.path.join(ROOT, "results", "eetle_ratio_summary.csv")
ABLATION_CSV = os.path.join(ROOT, "reports", "eetle_paper_ablation_summary.csv")
NORMAL_VALIDATION = os.path.join(
    ROOT,
    "reports",
    "eetle_paper_full_60",
    "EETLEPaperFull60_NormalDetectionValidationReport0000.txt",
)
DETECTION_REPORT = os.path.join(
    ROOT,
    "reports",
    "eetle_paper_full_60",
    "EETLEPaperFull60_DetectionReport0000.txt",
)
EVENT_REPORT = os.path.join(
    ROOT,
    "reports",
    "eetle_paper_full_60",
    "EETLEPaperFull60_EventTrustReport0000.txt",
)


def ensure_font():
    candidates = [
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]
    for path in candidates:
        if os.path.exists(path):
            font_manager.fontManager.addfont(path)
            name = font_manager.FontProperties(fname=path).get_name()
            plt.rcParams["font.sans-serif"] = [name, "DejaVu Sans"]
            break
    plt.rcParams["axes.unicode_minus"] = False
    plt.rcParams["figure.dpi"] = 120
    plt.rcParams["savefig.dpi"] = 300


def savefig(name):
    path = os.path.join(OUT_DIR, name)
    svg_path = os.path.splitext(path)[0] + ".svg"
    plt.tight_layout()
    plt.savefig(path, bbox_inches="tight")
    plt.savefig(svg_path, bbox_inches="tight")
    plt.close()
    print(path)
    print(svg_path)


def ratio_label(series):
    return (series.astype(float) * 100).round(0).astype(int).astype(str) + "%"


def plot_detection_metrics(ratio):
    df = ratio[ratio["ratio"] > 0].copy()
    x = ratio_label(df["ratio"])
    plt.figure(figsize=(7.2, 4.5))
    plt.plot(x, df["precision"], marker="o", label="Precision")
    plt.plot(x, df["recall"], marker="s", label="Recall")
    plt.plot(x, df["f1"], marker="^", label="F1")
    plt.plot(x, df["fpr"], marker="D", label="FPR")
    plt.ylim(-0.05, 1.08)
    plt.xlabel("恶意节点比例")
    plt.ylabel("指标值")
    plt.title("不同恶意节点比例下的检测性能")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend(ncol=2)
    savefig("fig1_detection_metrics_vs_ratio.png")


def plot_trust_gap(ratio):
    df = ratio[ratio["ratio"] > 0].copy()
    x = ratio_label(df["ratio"])
    plt.figure(figsize=(7.2, 4.5))
    plt.plot(x, df["normal_avg_gt"], marker="o", linewidth=2.2,
             label="正常节点平均全局信任")
    plt.plot(x, df["malicious_avg_gt"], marker="s", linewidth=2.2,
             label="恶意节点平均全局信任")
    plt.fill_between(range(len(df)), df["malicious_avg_gt"],
                     df["normal_avg_gt"], alpha=0.12)
    plt.ylim(0, 1.0)
    plt.xlabel("恶意节点比例")
    plt.ylabel("平均全局信任")
    plt.title("正常节点与恶意节点的全局信任区分")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend()
    savefig("fig2_normal_malicious_trust_gap.png")


def plot_attack_type_trust(ratio):
    df = ratio[ratio["ratio"] > 0].copy()
    x = ratio_label(df["ratio"])
    cols = [
        ("blackhole_avg_gt", "黑洞"),
        ("onoff_avg_gt", "开关"),
        ("false_event_avg_gt", "虚假事件"),
        ("env_camouflage_avg_gt", "环境隐蔽"),
        ("cross_region_avg_gt", "跨域"),
    ]
    plt.figure(figsize=(8.0, 4.8))
    for col, label in cols:
        plt.plot(x, df[col], marker="o", label=label)
    plt.ylim(0, 1.0)
    plt.xlabel("恶意节点比例")
    plt.ylabel("平均全局信任")
    plt.title("不同攻击类型节点的平均全局信任")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend(ncol=3)
    savefig("fig3_attack_type_global_trust.png")


def plot_network_performance(ratio):
    x = ratio_label(ratio["ratio"])
    fig, axes = plt.subplots(1, 3, figsize=(12.0, 3.8))
    axes[0].plot(x, ratio["delivery_prob"], marker="o", color="#1f77b4")
    axes[0].set_ylim(0.96, 1.01)
    axes[0].set_title("投递率")
    axes[0].set_ylabel("Delivery probability")

    axes[1].plot(x, ratio["overhead_ratio"], marker="s", color="#ff7f0e")
    axes[1].set_title("开销比")
    axes[1].set_ylabel("Overhead ratio")

    axes[2].plot(x, ratio["latency_avg"], marker="^", color="#2ca02c")
    axes[2].set_title("平均时延")
    axes[2].set_ylabel("Latency / s")

    for ax in axes:
        ax.set_xlabel("恶意节点比例")
        ax.grid(True, linestyle="--", alpha=0.35)
    fig.suptitle("不同恶意节点比例下的网络传输性能", y=1.03)
    savefig("fig4_network_performance_vs_ratio.png")


def plot_leader_security(ratio):
    x = ratio_label(ratio["ratio"])
    fig, ax1 = plt.subplots(figsize=(7.6, 4.6))
    ax1.bar(x, ratio["leaderChangeCount"], alpha=0.55, label="Leader切换次数")
    ax1.bar(x, ratio["maliciousLeaderCount"], alpha=0.75,
            label="恶意Leader次数")
    ax1.set_ylabel("次数")
    ax1.set_xlabel("恶意节点比例")
    ax1.grid(True, axis="y", linestyle="--", alpha=0.35)
    ax2 = ax1.twinx()
    ax2.plot(x, ratio["normalLeaderRatio"], color="#d62728", marker="o",
             linewidth=2.2, label="正常Leader比例")
    ax2.set_ylabel("正常Leader比例")
    ax2.set_ylim(0, 1.08)
    lines, labels = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines + lines2, labels + labels2, loc="lower left")
    plt.title("不同恶意节点比例下的Leader安全性")
    savefig("fig5_leader_security_vs_ratio.png")


def plot_ablation_trust(ablation):
    configs = ablation["config"].tolist()
    trust_cols = ["NORMAL", "BLACKHOLE", "ON_OFF", "FALSE_EVENT",
                  "ENV_CAMOUFLAGE", "CROSS_REGION"]
    labels = ["正常", "黑洞", "开关", "虚假事件", "环境隐蔽", "跨域"]
    data = ablation[trust_cols].astype(float)
    fig, ax = plt.subplots(figsize=(11.5, 5.4))
    width = 0.12
    x = range(len(configs))
    for i, (col, label) in enumerate(zip(trust_cols, labels)):
        ax.bar([v + (i - 2.5) * width for v in x], data[col],
               width=width, label=label)
    ax.set_xticks(list(x))
    ax.set_xticklabels(configs, rotation=25, ha="right")
    ax.set_ylim(0, 1.0)
    ax.set_ylabel("平均全局信任")
    ax.set_title("消融实验中不同类型节点的平均全局信任")
    ax.grid(True, axis="y", linestyle="--", alpha=0.3)
    ax.legend(ncol=3)
    savefig("fig6_ablation_global_trust.png")


def plot_ablation_leader(ablation):
    configs = ablation["config"].tolist()
    fig, ax1 = plt.subplots(figsize=(8.8, 4.8))
    ax1.bar(configs, ablation["leaderChangeCount"], alpha=0.55,
            label="Leader切换次数")
    ax1.bar(configs, ablation["maliciousLeaderCount"], alpha=0.75,
            label="恶意Leader次数")
    ax1.set_ylabel("次数")
    ax1.tick_params(axis="x", rotation=25)
    ax1.grid(True, axis="y", linestyle="--", alpha=0.3)
    ax2 = ax1.twinx()
    ax2.plot(configs, ablation["normalLeaderRatio"], color="#d62728",
             marker="o", linewidth=2.2, label="正常Leader比例")
    ax2.set_ylim(0, 1.08)
    ax2.set_ylabel("正常Leader比例")
    lines, labels = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines + lines2, labels + labels2, loc="upper right")
    plt.title("消融实验中的Leader选举安全性")
    savefig("fig7_ablation_leader_security.png")


def plot_normal_validation():
    df = pd.read_csv(NORMAL_VALIDATION)
    plt.figure(figsize=(8.2, 4.6))
    plt.plot(df["time"], df["FPR"], label="正常节点误判率(FPR)",
             linewidth=2.0)
    plt.plot(df["time"], df["avgNormalGlobalTrust"],
             label="正常节点平均全局信任", linewidth=2.0)
    plt.xlabel("仿真时间 / s")
    plt.ylabel("指标值")
    plt.ylim(-0.03, 1.05)
    plt.title("完整模型下正常节点误判率与平均全局信任")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend()
    savefig("fig8_normal_fpr_and_trust_over_time.png")


def plot_detection_over_time():
    df = pd.read_csv(DETECTION_REPORT)
    summary = df[(df["recordType"] == "SUMMARY") &
                 (df["attackType"] == "ALL")].copy()
    plt.figure(figsize=(8.2, 4.6))
    plt.plot(summary["time"], summary["precision"], label="Precision")
    plt.plot(summary["time"], summary["recall"], label="Recall")
    plt.plot(summary["time"], summary["f1"], label="F1")
    plt.plot(summary["time"], summary["fpr"], label="FPR")
    plt.xlabel("仿真时间 / s")
    plt.ylabel("指标值")
    plt.ylim(-0.05, 1.08)
    plt.title("完整模型下恶意节点检测指标随时间变化")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend(ncol=2)
    savefig("fig9_detection_metrics_over_time.png")


def plot_event_penalty_over_time():
    df = pd.read_csv(EVENT_REPORT)
    if df.empty:
        return
    grouped = df.groupby("time").agg(
        false_reports=("falseReport", lambda s: (s.astype(str) == "true").sum()),
        penalties=("appliedPenalty", lambda s: (s.astype(str) == "true").sum()),
        rewards=("appliedReward", lambda s: (s.astype(str) == "true").sum()),
    ).reset_index()
    grouped["cum_false_reports"] = grouped["false_reports"].cumsum()
    grouped["cum_penalties"] = grouped["penalties"].cumsum()
    grouped["cum_rewards"] = grouped["rewards"].cumsum()
    plt.figure(figsize=(8.2, 4.6))
    plt.plot(grouped["time"], grouped["cum_false_reports"],
             label="累计虚假报告")
    plt.plot(grouped["time"], grouped["cum_penalties"],
             label="累计事件惩罚")
    plt.plot(grouped["time"], grouped["cum_rewards"],
             label="累计事件奖励")
    plt.xlabel("仿真时间 / s")
    plt.ylabel("累计次数")
    plt.title("事件一致性奖惩随时间变化")
    plt.grid(True, linestyle="--", alpha=0.35)
    plt.legend()
    savefig("fig10_event_consistency_over_time.png")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(os.environ["MPLCONFIGDIR"], exist_ok=True)
    ensure_font()

    ratio = pd.read_csv(RATIO_CSV, na_values=["NA"])
    ablation = pd.read_csv(ABLATION_CSV)
    for col in ablation.columns:
        if col != "config":
            ablation[col] = pd.to_numeric(ablation[col], errors="coerce")

    plot_detection_metrics(ratio)
    plot_trust_gap(ratio)
    plot_attack_type_trust(ratio)
    plot_network_performance(ratio)
    plot_leader_security(ratio)
    plot_ablation_trust(ablation)
    plot_ablation_leader(ablation)
    plot_normal_validation()
    plot_detection_over_time()
    plot_event_penalty_over_time()


if __name__ == "__main__":
    main()
