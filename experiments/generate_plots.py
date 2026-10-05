"""
H8 EMS Platform — Phase 8 Publication Chart Generator (Native SVG)
Generates publication-quality vector charts (SVG) from simulation experiment CSVs.
"""

import csv
import os
from pathlib import Path

RESULTS_DIR = Path("experiments/results")
PLOTS_DIR = Path("experiments/plots")
PAPER_FIG_DIR = Path("docs/paper/figures")

PLOTS_DIR.mkdir(parents=True, exist_ok=True)
PAPER_FIG_DIR.mkdir(parents=True, exist_ok=True)

def load_aggregated():
    csv_file = RESULTS_DIR / "aggregated_metrics.csv"
    data = []
    with open(csv_file, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            data.append({
                "scenario": row["scenario"],
                "policy": row["policy"],
                "meanResponseSec": float(row["meanResponseSec"]),
                "ci95Lower": float(row["ci95Lower"]),
                "ci95Upper": float(row["ci95Upper"]),
                "p90ResponseSec": float(row["p90ResponseSec"]),
                "withinTargetPct": float(row["withinTargetPct"]) * 100.0,
                "meanHandoverSec": float(row["meanHandoverSec"]),
                "alsMatchPct": float(row["alsMatchPct"]) * 100.0,
            })
    return data

def generate_svg_response_times(data, output_paths):
    scenarios = ["S1", "S2", "S3", "S4", "S5"]
    policies = ["B1", "B2", "P1", "P2", "P3"]
    colors = {
        "B1": "#94a3b8",      # Slate grey (Baseline 1)
        "B2": "#64748b",      # Dark grey (Baseline 2)
        "P1": "#38bdf8",      # Sky blue (P1)
        "P2": "#0284c7",      # Blue (P2)
        "P3": "#10b981",      # Emerald Green (P3 Full)
    }

    width = 900
    height = 450
    margin_l = 80
    margin_r = 160
    margin_t = 60
    margin_b = 60

    plot_w = width - margin_l - margin_r
    plot_h = height - margin_t - margin_b

    max_val = 260.0

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" style="background-color: #0f172a; font-family: Inter, system-ui, sans-serif;">')
    
    # Title
    svg.append(f'<text x="{margin_l}" y="35" fill="#f8fafc" font-size="18" font-weight="700">Figure 1: Mean Response Time with 95% Confidence Intervals Across Scenarios</text>')
    
    # Grid lines & Y-axis labels
    for y_val in range(0, 261, 50):
        y_pos = margin_t + plot_h - (y_val / max_val * plot_h)
        svg.append(f'<line x1="{margin_l}" y1="{y_pos}" x2="{margin_l + plot_w}" y2="{y_pos}" stroke="#334155" stroke-dasharray="4,4" stroke-width="1"/>')
        svg.append(f'<text x="{margin_l - 12}" y="{y_pos + 4}" fill="#94a3b8" font-size="12" text-anchor="end">{y_val}s</text>')

    # Axes
    svg.append(f'<line x1="{margin_l}" y1="{margin_t + plot_h}" x2="{margin_l + plot_w}" y2="{margin_t + plot_h}" stroke="#64748b" stroke-width="2"/>')
    svg.append(f'<line x1="{margin_l}" y1="{margin_t}" x2="{margin_l}" y2="{margin_t + plot_h}" stroke="#64748b" stroke-width="2"/>')
    svg.append(f'<text x="{margin_l - 45}" y="{margin_t + plot_h // 2}" fill="#cbd5e1" font-size="13" font-weight="600" transform="rotate(-90 {margin_l - 45} {margin_t + plot_h // 2})" text-anchor="middle">Response Time (Seconds)</text>')

    # Grouped Bars
    group_width = plot_w / len(scenarios)
    bar_width = group_width * 0.14
    gap = group_width * 0.03

    scen_titles = {
        "S1": "S1: Urban",
        "S2": "S2: Surge",
        "S3": "S3: Rural",
        "S4": "S4: Overcrowd",
        "S5": "S5: Ablation"
    }

    for s_idx, sc in enumerate(scenarios):
        sc_center = margin_l + (s_idx + 0.5) * group_width
        svg.append(f'<text x="{sc_center}" y="{margin_t + plot_h + 25}" fill="#f1f5f9" font-size="13" font-weight="600" text-anchor="middle">{scen_titles[sc]}</text>')

        for p_idx, pol in enumerate(policies):
            matching = [row for row in data if row["scenario"] == sc and row["policy"] == pol]
            if not matching:
                continue
            item = matching[0]
            val = item["meanResponseSec"]
            ci_low = item["ci95Lower"]
            ci_high = item["ci95Upper"]

            bar_x = sc_center - (len(policies) * (bar_width + gap) - gap) / 2 + p_idx * (bar_width + gap)
            bar_h = (val / max_val) * plot_h
            bar_y = margin_t + plot_h - bar_h

            # Bar
            color = colors.get(pol, "#3b82f6")
            svg.append(f'<rect x="{bar_x}" y="{bar_y}" width="{bar_width}" height="{bar_h}" rx="3" fill="{color}" opacity="0.9"/>')
            
            # Error bar (CI 95%)
            ci_y_high = margin_t + plot_h - (ci_high / max_val) * plot_h
            ci_y_low = margin_t + plot_h - (ci_low / max_val) * plot_h
            center_x = bar_x + bar_width / 2
            svg.append(f'<line x1="{center_x}" y1="{ci_y_low}" x2="{center_x}" y2="{ci_y_high}" stroke="#ffffff" stroke-width="1.5"/>')
            svg.append(f'<line x1="{center_x - 3}" y1="{ci_y_high}" x2="{center_x + 3}" y2="{ci_y_high}" stroke="#ffffff" stroke-width="1.5"/>')
            svg.append(f'<line x1="{center_x - 3}" y1="{ci_y_low}" x2="{center_x + 3}" y2="{ci_y_low}" stroke="#ffffff" stroke-width="1.5"/>')

    # Legend
    leg_x = width - margin_r + 20
    leg_y = margin_t + 20
    svg.append(f'<rect x="{leg_x}" y="{leg_y - 15}" width="125" height="150" rx="6" fill="#1e293b" stroke="#334155" stroke-width="1"/>')
    svg.append(f'<text x="{leg_x + 10}" y="{leg_y}" fill="#94a3b8" font-size="12" font-weight="700">POLICIES</text>')
    for p_idx, pol in enumerate(policies):
        item_y = leg_y + 22 + p_idx * 24
        svg.append(f'<rect x="{leg_x + 10}" y="{item_y - 10}" width="14" height="14" rx="2" fill="{colors[pol]}"/>')
        svg.append(f'<text x="{leg_x + 32}" y="{item_y + 2}" fill="#e2e8f0" font-size="12">{pol}</text>')

    svg.append('</svg>')
    svg_content = "\n".join(svg)

    for p in output_paths:
        with open(p, "w", encoding="utf-8") as f:
            f.write(svg_content)
    print(f"Generated Figure 1: {output_paths[0]}")

def generate_svg_als_match(data, output_paths):
    scenarios = ["S1", "S2", "S3", "S4", "S5"]
    policies = ["B1", "B2", "P3"]
    colors = { "B1": "#94a3b8", "B2": "#64748b", "P3": "#10b981" }

    width = 850
    height = 420
    margin_l = 80
    margin_r = 160
    margin_t = 60
    margin_b = 60
    plot_w = width - margin_l - margin_r
    plot_h = height - margin_t - margin_b
    max_val = 100.0

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" style="background-color: #0f172a; font-family: Inter, system-ui, sans-serif;">')
    svg.append(f'<text x="{margin_l}" y="35" fill="#f8fafc" font-size="18" font-weight="700">Figure 2: ALS Clinical Appropriateness Rate (%) by Policy</text>')

    for y_val in range(0, 101, 20):
        y_pos = margin_t + plot_h - (y_val / max_val * plot_h)
        svg.append(f'<line x1="{margin_l}" y1="{y_pos}" x2="{margin_l + plot_w}" y2="{y_pos}" stroke="#334155" stroke-dasharray="4,4" stroke-width="1"/>')
        svg.append(f'<text x="{margin_l - 12}" y="{y_pos + 4}" fill="#94a3b8" font-size="12" text-anchor="end">{y_val}%</text>')

    svg.append(f'<line x1="{margin_l}" y1="{margin_t + plot_h}" x2="{margin_l + plot_w}" y2="{margin_t + plot_h}" stroke="#64748b" stroke-width="2"/>')
    svg.append(f'<line x1="{margin_l}" y1="{margin_t}" x2="{margin_l}" y2="{margin_t + plot_h}" stroke="#64748b" stroke-width="2"/>')
    svg.append(f'<text x="{margin_l - 45}" y="{margin_t + plot_h // 2}" fill="#cbd5e1" font-size="13" font-weight="600" transform="rotate(-90 {margin_l - 45} {margin_t + plot_h // 2})" text-anchor="middle">ALS Match Rate (%)</text>')

    group_width = plot_w / len(scenarios)
    bar_width = group_width * 0.22
    gap = group_width * 0.04

    scen_titles = {"S1": "S1: Urban", "S2": "S2: Surge", "S3": "S3: Rural", "S4": "S4: Overcrowd", "S5": "S5: Ablation"}

    for s_idx, sc in enumerate(scenarios):
        sc_center = margin_l + (s_idx + 0.5) * group_width
        svg.append(f'<text x="{sc_center}" y="{margin_t + plot_h + 25}" fill="#f1f5f9" font-size="13" font-weight="600" text-anchor="middle">{scen_titles[sc]}</text>')

        for p_idx, pol in enumerate(policies):
            matching = [row for row in data if row["scenario"] == sc and row["policy"] == pol]
            if not matching: continue
            val = matching[0]["alsMatchPct"]
            bar_x = sc_center - (len(policies) * (bar_width + gap) - gap) / 2 + p_idx * (bar_width + gap)
            bar_h = (val / max_val) * plot_h
            bar_y = margin_t + plot_h - bar_h

            svg.append(f'<rect x="{bar_x}" y="{bar_y}" width="{bar_width}" height="{bar_h}" rx="3" fill="{colors[pol]}" opacity="0.9"/>')
            svg.append(f'<text x="{bar_x + bar_width / 2}" y="{bar_y - 6}" fill="#f8fafc" font-size="11" font-weight="600" text-anchor="middle">{val:.1f}%</text>')

    leg_x = width - margin_r + 20
    leg_y = margin_t + 20
    svg.append(f'<rect x="{leg_x}" y="{leg_y - 15}" width="125" height="110" rx="6" fill="#1e293b" stroke="#334155" stroke-width="1"/>')
    svg.append(f'<text x="{leg_x + 10}" y="{leg_y}" fill="#94a3b8" font-size="12" font-weight="700">POLICIES</text>')
    for p_idx, pol in enumerate(policies):
        item_y = leg_y + 22 + p_idx * 24
        svg.append(f'<rect x="{leg_x + 10}" y="{item_y - 10}" width="14" height="14" rx="2" fill="{colors[pol]}"/>')
        svg.append(f'<text x="{leg_x + 32}" y="{item_y + 2}" fill="#e2e8f0" font-size="12">{pol}</text>')

    svg.append('</svg>')
    svg_content = "\n".join(svg)

    for p in output_paths:
        with open(p, "w", encoding="utf-8") as f:
            f.write(svg_content)
    print(f"Generated Figure 2: {output_paths[0]}")

def generate_svg_ablations(data, output_paths):
    ablations = ["P3", "P3-no-redeploy", "P3-no-hosp", "P3-no-coverage", "P3-no-fatigue"]
    sc = "S5" # Ablation baseline
    colors = ["#10b981", "#f59e0b", "#ef4444", "#3b82f6", "#8b5cf6"]

    width = 750
    height = 380
    margin_l = 150
    margin_r = 50
    margin_t = 60
    margin_b = 60
    plot_w = width - margin_l - margin_r
    plot_h = height - margin_t - margin_b

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" style="background-color: #0f172a; font-family: Inter, system-ui, sans-serif;">')
    svg.append(f'<text x="{margin_l}" y="35" fill="#f8fafc" font-size="18" font-weight="700">Figure 3: Ablation Analysis (Scenario S5 Parameter Effects)</text>')

    max_val = 140.0
    for x_val in range(0, 141, 20):
        x_pos = margin_l + (x_val / max_val * plot_w)
        svg.append(f'<line x1="{x_pos}" y1="{margin_t}" x2="{x_pos}" y2="{margin_t + plot_h}" stroke="#334155" stroke-dasharray="4,4" stroke-width="1"/>')
        svg.append(f'<text x="{x_pos}" y="{margin_t + plot_h + 18}" fill="#94a3b8" font-size="11" text-anchor="middle">{x_val}s</text>')

    bar_height = plot_h / len(ablations) * 0.55
    bar_gap = plot_h / len(ablations)

    for idx, abl in enumerate(ablations):
        matching = [row for row in data if row["scenario"] == sc and row["policy"] == abl]
        if not matching: continue
        val = matching[0]["meanResponseSec"]
        ci_l = matching[0]["ci95Lower"]
        ci_h = matching[0]["ci95Upper"]

        y_pos = margin_t + idx * bar_gap + (bar_gap - bar_height) / 2
        bar_w = (val / max_val) * plot_w

        svg.append(f'<text x="{margin_l - 12}" y="{y_pos + bar_height / 2 + 4}" fill="#cbd5e1" font-size="12" font-weight="500" text-anchor="end">{abl}</text>')
        svg.append(f'<rect x="{margin_l}" y="{y_pos}" width="{bar_w}" height="{bar_height}" rx="3" fill="{colors[idx]}" opacity="0.88"/>')
        svg.append(f'<text x="{margin_l + bar_w + 12}" y="{y_pos + bar_height / 2 + 4}" fill="#f8fafc" font-size="12" font-weight="600">{val:.1f}s</text>')

    svg.append('</svg>')
    svg_content = "\n".join(svg)
    for p in output_paths:
        with open(p, "w", encoding="utf-8") as f:
            f.write(svg_content)
    print(f"Generated Figure 3: {output_paths[0]}")

if __name__ == "__main__":
    data = load_aggregated()
    generate_svg_response_times(data, [PLOTS_DIR / "fig1_response_times_ci.svg", PAPER_FIG_DIR / "fig1_response_times_ci.svg"])
    generate_svg_als_match(data, [PLOTS_DIR / "fig2_als_appropriateness.svg", PAPER_FIG_DIR / "fig2_als_appropriateness.svg"])
    generate_svg_ablations(data, [PLOTS_DIR / "fig3_ablation_study.svg", PAPER_FIG_DIR / "fig3_ablation_study.svg"])
    print("All figures successfully created!")
