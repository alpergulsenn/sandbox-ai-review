# 🤖 AI-Powered Code Review Agent

[][contributors-url]
[][forks-url]
[][stars-url]
[][issues-url]
[][linkedin-url]

<br />
<div align="center">
<h3 align="center">Vertex AI & Gemini 2.0 Flash Code Reviewer</h3>

<p align="center">
An automated, context-aware AI agent that evaluates architectural patterns, security risks, and code quality directly within GitHub Pull Requests.
<br />
<a href="#usage"><strong>Explore the commands »</strong></a>
<br />
<br />
<a href="#live-demo">View Demo</a>
·
<a href="#cost-analysis">Cost Analysis</a>
·
<a href="#best-prompts">Interactive Prompts</a>
</p>
</div>

<details>
<summary>Table of Contents</summary>
<ol>
<li>
<a href="#about-the-project">About The Project</a>
<ul>
<li><a href="#built-with">Built With</a></li>
<li><a href="#context--decision-logic">Context & Decision Logic</a></li>
</ul>
</li>
<li>
<a href="#getting-started">Getting Started</a>
<ul>
<li><a href="#prerequisites">Prerequisites</a></li>
<li><a href="#installation">Installation</a></li>
</ul>
</li>
<li><a href="#usage">Usage</a></li>
<li><a href="#cost-analysis">Cost Analysis</a></li>
<li><a href="#best-prompts">Best Prompts (/ask)</a></li>
<li><a href="#contact">Contact</a></li>
</ol>
</details>

---

## About The Project

This project addresses the gap between traditional linters and senior-level architectural reviews. While linters catch syntax errors, this **AI Agent** identifies structural flaws, security leaks, and violations of SOLID principles.

**Why sandbox-ai-review?**

* **Architectural Insight:** Goes beyond "Parsing Errors" to find "Architectural Debt" like SRP violations and Callback Hell.
* **Extreme Cost Efficiency:** Runs at approximately **0.10 TL per PR** using Gemini 2.0 Flash.
* **Dynamic Language Support:** Detects user intent and communicates in both English and Turkish while preserving technical terms.

### Built With

* [][GCP-url]
* [][GitHub-url]
* [][Gemini-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🧠 Context & Decision Logic

As requested by the engineering lead, the agent operates on a **"Contextual Reasoning"** model:

1. **Persona-Driven Review:** The agent acts as a `Senior Software Architect`, prioritizing system stability and maintainability over minor formatting.
2. **Strategic Filtering:** Uses `minimal_severity: Medium` and `IGNORE_FILE_EXTENSIONS` to eliminate "noise" and focus on high-impact changes.
3. **Semantic Analysis:** Unlike linters, it simulates runtime behavior to catch logic errors like infinite loops or inefficient data filtering.
4. **Integrated Labeling:** Synchronizes with `.pr_agent.toml` to automatically categorize PRs by complexity (`size/XS` to `size/XL`) and risk.

---

## Getting Started

### Prerequisites

* A **Google Cloud Project** with **Vertex AI API** enabled.
* A **Service Account** with `Vertex AI User` permissions.
* GitHub Repository secrets: `GCP_SA_KEY` (JSON key) and `GCP_PROJECT_ID`.

### Installation

1. **Configure GitHub Secrets:** Add your GCP Service Account JSON to `GCP_SA_KEY`.
2. **Add Workflow:** Place `ai_review.yml` in `.github/workflows/`.
3. **Define Rules:** Create `.pr_agent.toml` in the root directory to define your `custom_labels` and tool permissions.
4. **Set Environment:** Ensure `CONFIG.CUSTOM_MODEL_MAX_TOKENS` is set to `8192` to optimize Gemini 2.0 Flash performance.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Usage

The agent triggers automatically on PR creation or via manual commands:

* **`/describe`**: Generates a structured summary and walkthrough.
* **`/review`**: Provides deep architectural and security analysis.
* **`/generate_labels`**: Force-updates PR labels based on custom TOML rules.
* **`/ask [question]`**: Interactively consult the Senior Architect persona.

### 📺 Live Demo Instructions

To demonstrate the "Context Logic" during a review:

1. Create a new branch in **sandbox-ai-review** and push a code change containing an **SRP violation** or a **Hardcoded Secret**.
2. Open a PR; the agent will automatically flag these as `Medium` severity issues.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 💰 Cost Analysis (January 2026)

Based on real-world monitoring, the Gemini 2.0 Flash implementation provides industry-leading efficiency:

| Metric | Value (Turkish Lira - TL) |
| --- | --- |
| **Avg. Cost per PR** | **~0.11 TL** |
| **Daily Peak (Jan 27)** | 1.6 TL (for 12 PRs) |
| **Input Cost** | Higher due to context-heavy diff reading |
| **Output Cost** | Lower due to strict `Medium` severity filtering |

*Note: Costs are optimized by excluding non-code files like `.json`, `.css`, and `.svg`.*

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## 🧠 Interactive Prompts (/ask)

Beyond automated reviews, the agent serves as an interactive engineering partner via the `/ask` command:

| English Prompt (Interactive Reasoning) | Türkçesi (Etkileşimli Sorgulama) |
| --- | --- |
| `"If I add 10 more payment types, how should I refactor this using a Strategy Pattern?"` | `"Gelecekte 10 ödeme tipi daha eklersem, bunu Strategy Pattern ile nasıl refactor etmeliyim?"` |
| `"Under a load of 1 million concurrent requests, where is the most likely bottleneck?"` | `"1 milyon eşzamanlı istek altında, en muhtemel darboğaz neresidir?"` |
| `"Explain this logic to a Junior Developer. What is the mental model?"` | `"Bu mantığı bir Junior yazılımcıya açıkla. Buradaki zihinsel model nedir?"` |
| `"Generate 5 edge cases (nulls, timeouts) that are NOT handled in this PR."` | `"Bu PR'da ele alınmayan 5 uç durum (null, zaman aşımı) üret."` |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Contact

**Alper** - Computer Engineering Intern at Abralabs

Project Link: [https://github.com/alpergulsenn/sandbox-ai-review](https://www.google.com/search?q=https://github.com/alpergulsenn/sandbox-ai-review)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Acknowledgments

* [Haktan Enes Biçer - Abralabs Technical Lead]
* [Codium-ai/pr-agent Documentation]
* [Google Cloud Vertex AI Reference]

---


[]: #
[contributors-url]: https://www.google.com/search?q=%5Bhttps://github.com/alpergulsenn/sandbox-ai-review/graphs/contributors%5D
[]: #
[forks-url]: https://www.google.com/search?q=%5Bhttps://github.com/alpergulsenn/sandbox-ai-review/network/members%5D
[]: #
[stars-url]: https://www.google.com/search?q=%5Bhttps://github.com/alpergulsenn/sandbox-ai-review/stargazers%5D
[]: #
[issues-url]: https://www.google.com/search?q=%5Bhttps://github.com/alpergulsenn/sandbox-ai-review/issues%5D
[]: #
[linkedin-url]: https://www.google.com/search?q=%5Bhttps://linkedin.com/in/alpergulsenn%5D
[]: #
[gcp-url]: https://www.google.com/search?q=%5Bhttps://cloud.google.com/%5D
[]: #
[github-url]: https://www.google.com/search?q=%5Bhttps://github.com/features/actions%5D
[]: #
[gemini-url]: https://www.google.com/search?q=%5Bhttps://ai.google.dev/%5D