import os

os.environ.setdefault("MPLCONFIGDIR", os.path.abspath("reports/mpl_cache"))

import matplotlib.pyplot as plt
import pandas as pd
from matplotlib import font_manager


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SUMMARY_CSV = os.path.join(ROOT, "results", "eetle_density_summary.csv")
OUT_DIR = os.path.join(ROOT, "results", "eetle_density_figures")


def ensure_font():
    for path in [
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]:
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
    svg = os.path.splitext(path)[0] + ".svg"
    plt.tight_layout()
    plt.savefig(path, bbox_inches="tight")
    plt.savefig(svg, bbox_inches="tight")
    plt.close()
    print(path)
    print(svg)


def error_plot(df, mean_col, std_col, ylabel, title, filename):
    plt.figure(figsize=(7.2, 4.5))
    plt.errorbar(df["density"], df[mean_col], yerr=df[std_col],
                 marker="o", capsize=4, linewidth=2.0)
    plt.xlabel("无人机密度 / 架·km$^{-2}$")
    plt.ylabel(ylabel)
    plt.title(title)
    plt.grid(True, linestyle="--", alpha=0.35)
    savefig(filename)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(os.environ["MPLCONFIGDIR"], exist_ok=True)
    ensure_font()
    df = pd.read_csv(SUMMARY_CSV)

    error_plot(
        df,
        "detection_rate_mean",
        "detection_rate_std",
        "恶意节点检测率",
        "节点密度对恶意节点检测率的影响",
        "density_fig1_detection_rate.png")
    error_plot(
        df,
        "false_positive_rate_mean",
        "false_positive_rate_std",
        "正常节点误判率",
        "节点密度对正常节点误判率的影响",
        "density_fig2_false_positive_rate.png")
    error_plot(
        df,
        "F1_mean",
        "F1_std",
        "F1 值",
        "节点密度对检测 F1 值的影响",
        "density_fig3_f1.png")
    error_plot(
        df,
        "malicious_leader_rate_mean",
        "malicious_leader_rate_std",
        "恶意 Leader 选举概率",
        "节点密度对恶意 Leader 选举概率的影响",
        "density_fig4_malicious_leader_rate.png")
    error_plot(
        df,
        "leader_switch_count_mean",
        "leader_switch_count_std",
        "Leader 切换次数",
        "节点密度对 Leader 切换次数的影响",
        "density_fig5_leader_switch_count.png")

    fig, ax1 = plt.subplots(figsize=(7.6, 4.7))
    ax1.errorbar(df["density"], df["communication_overhead_mean"],
                 yerr=df["communication_overhead_std"], marker="o",
                 capsize=4, linewidth=2.0, label="通信开销")
    ax1.set_xlabel("无人机密度 / 架·km$^{-2}$")
    ax1.set_ylabel("通信开销")
    ax1.grid(True, linestyle="--", alpha=0.35)
    if "runtime_mean" in df.columns:
        ax2 = ax1.twinx()
        ax2.errorbar(df["density"], df["runtime_mean"],
                     yerr=df["runtime_std"], marker="s", capsize=4,
                     color="#d62728", linewidth=2.0, label="运行时间")
        ax2.set_ylabel("运行时间 / s")
        lines, labels = ax1.get_legend_handles_labels()
        lines2, labels2 = ax2.get_legend_handles_labels()
        ax1.legend(lines + lines2, labels + labels2, loc="best")
    else:
        ax1.legend(loc="best")
    plt.title("节点密度对通信开销和运行时间的影响")
    savefig("density_fig6_overhead_runtime.png")

    plt.figure(figsize=(7.2, 4.5))
    plt.errorbar(df["density"], df["average_neighbor_degree_mean"],
                 yerr=df["average_neighbor_degree_std"], marker="o",
                 capsize=4, linewidth=2.0)
    plt.xlabel("无人机密度 / 架·km$^{-2}$")
    plt.ylabel("平均邻居数量")
    plt.title("节点密度与平均邻居数量")
    plt.grid(True, linestyle="--", alpha=0.35)
    savefig("density_fig0_average_neighbor_degree.png")


if __name__ == "__main__":
    main()
