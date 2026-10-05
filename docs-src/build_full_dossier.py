#!/usr/bin/env python3
"""
H8 EMS Complete System Dossier Compiler
Compiles all 11 chapters into a unified 60-70 page technical dossier in both Markdown and HTML.
"""

import os
import re

DOCS_DIR = os.path.dirname(os.path.abspath(__file__))
CHAPTERS = [
    "01_executive_summary_and_overview.md",
    "02_software_requirements_specification.md",
    "03_system_architecture_and_design.md",
    "04_algorithmic_specifications_and_math.md",
    "05_complete_uml_modeling_suite.md",
    "06_database_schemas_and_kafka_contracts.md",
    "07_api_reference_and_service_specifications.md",
    "08_cryptographic_audit_and_security.md",
    "09_frontend_pwa_and_ui_architecture.md",
    "10_verification_testing_and_benchmarks.md",
    "11_production_deployment_and_ops_manual.md"
]

OUTPUT_MD = os.path.join(DOCS_DIR, "H8_EMS_COMPLETE_SYSTEM_DOSSIER.md")
OUTPUT_HTML = os.path.join(DOCS_DIR, "H8_EMS_COMPLETE_SYSTEM_DOSSIER.html")

def escape_html(text):
    return (text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;"))

def markdown_to_html_basic(md_text):
    """Simple parser converting markdown headers, codeblocks, tables, and lists to styled HTML."""
    lines = md_text.splitlines()
    html = []
    in_code = False
    code_lang = ""
    code_buf = []
    in_table = False
    table_buf = []

    for line in lines:
        if line.startswith("```"):
            if in_code:
                in_code = False
                raw_code = "\n".join(code_buf)
                if code_lang == "mermaid":
                    html.append(f'<div class="mermaid">\n{raw_code}\n</div>')
                else:
                    html.append(f'<pre><code class="language-{code_lang}">{escape_html(raw_code)}</code></pre>')
                code_buf = []
                code_lang = ""
            else:
                in_code = True
                code_lang = line[3:].strip()
            continue

        if in_code:
            code_buf.append(line)
            continue

        # Handle tables
        if line.strip().startswith("|") and line.strip().endswith("|"):
            if not in_table:
                in_table = True
                table_buf = [line]
            else:
                table_buf.append(line)
            continue
        elif in_table:
            in_table = False
            # Render table
            html.append('<div class="table-container"><table>')
            is_header = True
            for t_line in table_buf:
                if re.match(r'^\s*\|(?:\s*:?-+:?\s*\|)+\s*$', t_line):
                    is_header = False
                    continue
                cells = [c.strip() for c in t_line.strip().strip('|').split('|')]
                tag = 'th' if is_header else 'td'
                row_html = "".join(f"<{tag}>{c}</{tag}>" for c in cells)
                html.append(f"<tr>{row_html}</tr>")
            html.append('</table></div>')
            table_buf = []

        # Headers
        if line.startswith("# "):
            html.append(f'<h1 class="chapter-title">{line[2:].strip()}</h1>')
        elif line.startswith("## "):
            html.append(f'<h2>{line[3:].strip()}</h2>')
        elif line.startswith("### "):
            html.append(f'<h3>{line[4:].strip()}</h3>')
        elif line.startswith("#### "):
            html.append(f'<h4>{line[5:].strip()}</h4>')
        elif line.startswith("* ") or line.startswith("- "):
            html.append(f'<li>{line[2:].strip()}</li>')
        elif line.strip() == "---":
            html.append('<hr/>')
        elif line.strip():
            # Paragraph
            html.append(f'<p>{line}</p>')

    return "\n".join(html)

def build():
    all_content = []
    total_words = 0

    print("Reading chapters...")
    for ch in CHAPTERS:
        path = os.path.join(DOCS_DIR, ch)
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
                word_count = len(content.split())
                total_words += word_count
                all_content.append(content)
                print(f"  + {ch}: {word_count} words")
        else:
            print(f"  ! Missing {ch}")

    # Build master Markdown
    dossier_md = "\n\n---\n<div class=\"page-break\"></div>\n\n".join(all_content)
    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write(dossier_md)
    print(f"\nWrote master Markdown: {OUTPUT_MD} ({total_words} total words)")

    # Build HTML
    html_body = []
    for i, content in enumerate(all_content):
        chapter_html = markdown_to_html_basic(content)
        html_body.append(f'<section class="chapter-section page-break" id="chapter-{i+1}">\n{chapter_html}\n</section>')

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H8 Capability-Aware EMS Dispatch System — Engineering Dossier</title>
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;600&family=Inter:wght@300;400;500;600;700;800&family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,600;1,6..72,400&display=swap" rel="stylesheet">
    <!-- Mermaid JS for UML rendering -->
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <script>
        mermaid.initialize({{
            startOnLoad: true,
            theme: 'neutral',
            flowchart: {{ useMaxWidth: true, htmlLabels: true }},
            sequence: {{ useMaxWidth: true }},
            themeVariables: {{
                fontFamily: 'Inter, sans-serif',
                primaryColor: '#e0f2fe',
                primaryTextColor: '#0f172a',
                primaryBorderColor: '#38bdf8',
                lineColor: '#64748b'
            }}
        }});
    </script>
    <style>
        :root {{
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --text-dark: #0f172a;
            --text-muted: #475569;
            --bg-light: #f8fafc;
            --code-bg: #1e293b;
            --border: #e2e8f0;
        }}

        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            font-family: 'Newsreader', Georgia, serif;
            font-size: 15px;
            line-height: 1.7;
            color: var(--text-dark);
            background: #ffffff;
            padding: 0;
            margin: 0;
        }}

        .dossier-container {{
            max-width: 900px;
            margin: 0 auto;
            padding: 3rem 2.5rem;
        }}

        /* Cover Page Styling */
        .cover-page {{
            min-height: 90vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            border: 8px double var(--primary-dark);
            padding: 4rem 2rem;
            margin-bottom: 4rem;
            background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
            box-shadow: 0 10px 25px rgba(0,0,0,0.05);
        }}
        .cover-badge {{
            display: inline-block;
            background: var(--primary);
            color: #fff;
            padding: 0.35rem 1.2rem;
            border-radius: 9999px;
            font-family: 'Inter', sans-serif;
            font-size: 0.85rem;
            font-weight: 700;
            letter-spacing: 0.1em;
            text-transform: uppercase;
            margin-bottom: 2rem;
        }}
        .cover-title {{
            font-family: 'Inter', sans-serif;
            font-size: 2.85rem;
            font-weight: 800;
            color: #0c4a6e;
            line-height: 1.15;
            margin-bottom: 1.5rem;
        }}
        .cover-subtitle {{
            font-size: 1.35rem;
            color: var(--text-muted);
            max-width: 700px;
            margin-bottom: 3rem;
            font-style: italic;
        }}
        .cover-meta {{
            font-family: 'Inter', sans-serif;
            font-size: 0.95rem;
            color: #334155;
            line-height: 1.8;
            border-top: 2px solid #bae6fd;
            padding-top: 2rem;
            width: 80%;
        }}

        /* Typography */
        h1, h2, h3, h4 {{
            font-family: 'Inter', sans-serif;
            color: #0f172a;
            margin-top: 2.2rem;
            margin-bottom: 0.8rem;
        }}
        .chapter-title {{
            font-size: 2.1rem;
            font-weight: 800;
            color: #0369a1;
            border-bottom: 3px solid #38bdf8;
            padding-bottom: 0.6rem;
            margin-top: 3.5rem;
        }}
        h2 {{ font-size: 1.45rem; font-weight: 700; color: #1e293b; border-left: 4px solid var(--primary); padding-left: 0.65rem; }}
        h3 {{ font-size: 1.15rem; font-weight: 600; color: #334155; }}
        h4 {{ font-size: 1.0rem; font-weight: 600; color: #475569; }}
        p {{ margin-bottom: 1rem; text-align: justify; }}
        li {{ margin-left: 1.75rem; margin-bottom: 0.4rem; }}
        hr {{ border: 0; height: 1px; background: var(--border); margin: 2rem 0; }}

        /* Code Blocks */
        pre {{
            background: var(--code-bg);
            color: #f8fafc;
            padding: 1.25rem;
            border-radius: 8px;
            overflow-x: auto;
            font-family: 'Fira Code', monospace;
            font-size: 0.84rem;
            line-height: 1.55;
            margin: 1.5rem 0;
            box-shadow: inset 0 2px 6px rgba(0,0,0,0.3);
        }}
        code {{
            font-family: 'Fira Code', monospace;
            font-size: 0.87em;
            background: #f1f5f9;
            color: #0284c7;
            padding: 0.15rem 0.35rem;
            border-radius: 4px;
        }}
        pre code {{ background: transparent; color: inherit; padding: 0; }}

        /* Tables */
        .table-container {{
            overflow-x: auto;
            margin: 1.5rem 0;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            font-family: 'Inter', sans-serif;
            font-size: 0.85rem;
        }}
        th, td {{
            padding: 0.75rem 0.9rem;
            border: 1px solid var(--border);
            text-align: left;
        }}
        th {{
            background: #f0f9ff;
            color: #0369a1;
            font-weight: 700;
        }}
        tr:nth-child(even) {{ background: #f8fafc; }}

        /* Mermaid Diagrams Container */
        .mermaid {{
            margin: 2rem auto;
            text-align: center;
            background: #ffffff;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            padding: 1.5rem;
            box-shadow: 0 4px 12px rgba(0,0,0,0.03);
            overflow-x: auto;
        }}

        /* Print & Page Break Styles */
        @media print {{
            body {{ font-size: 11pt; }}
            .dossier-container {{ max-width: 100%; padding: 0; }}
            .page-break {{ page-break-before: always; }}
            pre, table, .mermaid {{ page-break-inside: avoid; }}
            @page {{
                margin: 2cm;
                @bottom-right {{
                    content: counter(page);
                }}
            }}
        }}
    </style>
</head>
<body>
    <div class="dossier-container">
        <!-- Executive Cover Page -->
        <div class="cover-page">
            <span class="cover-badge">Engineering &amp; Architecture Dossier</span>
            <h1 class="cover-title">H8 Capability-Aware Emergency Medical Services (EMS) Platform</h1>
            <p class="cover-subtitle">Complete System Requirements, Mathematical Formulations, UML Models, API Specifications &amp; Operational Runbooks</p>
            <div class="cover-meta">
                <p><strong>Architecture Version:</strong> 1.0.0-SNAPSHOT (Production Release)</p>
                <p><strong>Core Stack:</strong> Java 21 LTS &bull; Spring Boot 3 &bull; PostgreSQL 15 (PostGIS 3.4) &bull; Apache Kafka 3.7 &bull; Redis 7 &bull; Keycloak 25 &bull; GraphHopper &bull; PWA</p>
                <p><strong>Target Deployment:</strong> Metropolitan Pilot &bull; Jaipur, Rajasthan, India &bull; 14 Active Units &bull; 5 Tier-1 Receiving Centers</p>
                <p><strong>Document Verification:</strong> Cryptographically Signed &bull; jqwik Invariant Verified &bull; Zero Secrets Baseline</p>
            </div>
        </div>

        <!-- Table of Contents Summary -->
        <section class="page-break" style="margin-bottom: 3rem; padding: 2rem; background: #f8fafc; border-radius: 8px; border: 1px solid var(--border);">
            <h2 style="margin-top: 0;">Table of Contents</h2>
            <ol style="line-height: 2; font-family: 'Inter', sans-serif; font-size: 0.95rem; margin-left: 2rem; color: #1e293b;">
                <li><strong>Chapter 1:</strong> Executive Summary &amp; Operational Scope</li>
                <li><strong>Chapter 2:</strong> Software Requirements Specification (SRS)</li>
                <li><strong>Chapter 3:</strong> System Architecture &amp; Hexagonal Design (ADD)</li>
                <li><strong>Chapter 4:</strong> Algorithmic Specifications &amp; Mathematical Foundations</li>
                <li><strong>Chapter 5:</strong> Complete UML Modeling Suite (All 14 UML Diagrams)</li>
                <li><strong>Chapter 6:</strong> Database Schemas, Relational DDL &amp; Kafka Event Contracts</li>
                <li><strong>Chapter 7:</strong> API Reference &amp; Microservice Endpoints</li>
                <li><strong>Chapter 8:</strong> Cryptographic Audit Ledger &amp; Security Architecture</li>
                <li><strong>Chapter 9:</strong> Frontend Architecture &amp; PWA Specifications</li>
                <li><strong>Chapter 10:</strong> Verification, Testing &amp; Performance Benchmarks</li>
                <li><strong>Chapter 11:</strong> Production Deployment, Operations &amp; Disaster Recovery Manual</li>
            </ol>
        </section>

        <!-- Chapter Contents -->
        {"\n".join(html_body)}
    </div>
</body>
</html>
"""

    with open(OUTPUT_HTML, "w", encoding="utf-8") as f:
        f.write(full_html)
    print(f"Wrote compiled HTML: {OUTPUT_HTML}")
    print("\nCompilation successfully complete!")

if __name__ == "__main__":
    build()
