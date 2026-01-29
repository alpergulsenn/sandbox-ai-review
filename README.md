# 🤖 AI-Powered Code Review Agent

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

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
<li><a href="#context-decision-logic">Context & Decision Logic</a></li>
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
* **Extreme Cost Efficiency:** Runs at approximately **0.10 TL per PR** (0,0023 $) using Gemini 2.0 Flash.
* **Dynamic Language Support:** Detects user intent and communicates in both English and Turkish while preserving technical terms.

### Built With

[![GCP][GCP-shield]][GCP-url]

[![GitHubActions][GitHub-shield]][GitHub-url]

[![Gemini][Gemini-shield]][Gemini-url]

---

## <a id="context-decision-logic"></a> 🧠 Context & Decision Logic

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

---

## Usage

The agent triggers automatically on PR creation or via manual commands:

* **`/describe`**: Generates a structured summary and walkthrough.
* **`/review`**: Provides deep architectural and security analysis.
* **`/generate_labels`**: Force-updates PR labels based on custom TOML rules.
* **`/ask [question]`**: Interactively consult the Senior Architect persona.

### <a id="live-demo"></a> 📺 Live Demo Instructions

To demonstrate the "Context Logic" during a review:

1. Create a new branch in **sandbox-ai-review** and push a code change containing an **SRP violation** or a **Hardcoded Secret**.
2. Open a PR; the agent will automatically flag these as `Medium` severity issues.

---

## <a id="cost-analysis"></a> 💰 Cost Analysis (January 2026)

Based on real-world monitoring, the Gemini 2.0 Flash implementation provides industry-leading efficiency:

| Metric | Value (Turkish Lira - TL) |
| --- | --- |
| **Avg. Cost per PR** | **~0.11 TL** (0,0023 $) |
| **Daily Peak (Jan 27)** | 1.6 TL -> 0,036$ (for 12 PRs)  |
| **Input Cost** | Higher due to context-heavy diff reading |
| **Output Cost** | Lower due to strict `Medium` severity filtering |

*Note: Costs are optimized by excluding non-code files like `.json`, `.css`, and `.svg`.*

---

## <a id="best-prompts"></a> 🧠 Interactive Prompts (/ask)

Beyond automated reviews, the agent serves as an interactive engineering partner via the `/ask` command:

| English Prompt (Interactive Reasoning) | Türkçesi (Etkileşimli Sorgulama) |
| --- | --- |
| `"If I add 10 more payment types, how should I refactor this using a Strategy Pattern?"` | `"Gelecekte 10 ödeme tipi daha eklersem, bunu Strategy Pattern ile nasıl refactor etmeliyim?"` |
| `"Under a load of 1 million concurrent requests, where is the most likely bottleneck?"` | `"1 milyon eşzamanlı istek altında, en muhtemel darboğaz neresidir?"` |
| `"Explain this logic to a Junior Developer. What is the mental model?"` | `"Bu mantığı bir Junior yazılımcıya açıkla. Buradaki zihinsel model nedir?"` |
| `"Generate 5 edge cases (nulls, timeouts) that are NOT handled in this PR."` | `"Bu PR'da ele alınmayan 5 uç durum (null, zaman aşımı) üret."` |

---

## Contact

**Alper** - Computer Engineering Intern at Abralabs  : [LinkedIn](https://www.google.com/search?q=https://www.linkedin.com/in/alpergulsenn/)

Project Link: [https://github.com/alpergulsenn/sandbox-ai-review](https://www.google.com/search?q=https://github.com/alpergulsenn/sandbox-ai-review)

---

## Acknowledgments

* [Haktan Enes Biçer - Abralabs Technical Lead]
* [Codium-ai/pr-agent Documentation]
* [Google Cloud Vertex AI Reference]

[contributors-shield]: https://img.shields.io/github/contributors/alpergulsenn/sandbox-ai-review.svg?style=for-the-badge
[contributors-url]: https://github.com/alpergulsenn/sandbox-ai-review/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/alpergulsenn/sandbox-ai-review.svg?style=for-the-badge
[forks-url]: https://github.com/alpergulsenn/sandbox-ai-review/network/members

[stars-shield]: https://img.shields.io/github/stars/alpergulsenn/sandbox-ai-review.svg?style=for-the-badge
[stars-url]: https://github.com/alpergulsenn/sandbox-ai-review/stargazers

[issues-shield]: https://img.shields.io/github/issues/alpergulsenn/sandbox-ai-review.svg?style=for-the-badge
[issues-url]: https://github.com/alpergulsenn/sandbox-ai-review/issues

[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/alpergulsenn

[GCP-shield]: https://img.shields.io/badge/Google_Cloud-4285F4?style=for-the-badge&logo=google-cloud&logoColor=white
[GCP-url]: https://cloud.google.com/

[GitHub-shield]: https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white
[GitHub-url]: https://github.com/features/actions

[Gemini-shield]: https://img.shields.io/badge/Gemini_2.0_Flash-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white
[Gemini-url]: https://ai.google.dev/