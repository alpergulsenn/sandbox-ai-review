## <a id="readme-top"></a> 
# 🤖 sandbox-ai-review (AI powered code review agent)

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
<li><a href="#nasil-kurulur">🚀 Nasıl Kurulur (Bitbucket & GitHub)</a></li>
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

## <a id="nasil-kurulur"></a> 🚀 Nasıl Kurulur (Bitbucket & GitHub)

Bu repository, **AI Code Review** sistemini kendi projenize entegre etmeniz için gereken tüm hazır yapılandırmaları içerir. Kurulum için bu repository'deki ilgili dosyaları kendi projenize kopyalamanız yeterlidir.

### 1. Bitbucket Kurulumu

Bitbucket kullanıyorsanız, aşağıdaki dosyaları projenizin ana dizinine kopyalayın:

1.  **Konfigürasyon Dosyaları:**
    *   `.pr_agent.toml` dosyasını ana dizine kopyalayın.
    *   `.bitbucket/pr_rules/` klasörünü ve içindeki tüm dosyaları, kendi projenizde de aynı yola (`.bitbucket/pr_rules/`) olacak şekilde kopyalayın.
    *   `bitbucket-pipelines.yml` dosyasını ana dizine kopyalayın.

2.  **Repository Variables Ayarlayın:**
    Bitbucket repository'nizde **Repository settings > Pipelines > Repository variables** kısmına gidin ve şu değişkenleri ekleyin:
    *   `GCP_PROJECT_ID`: Google Cloud Proje ID'niz.
    *   `GCP_LOCATION`: (Örn: `us-central1`).
    *   `GCP_CREDENTIALS_JSON`: Google Service Account JSON dosyanızın içeriği.
    *   `BITBUCKET_USERNAME`: Bitbucket mail adresiniz.
    *   `BITBUCKET_APP_PASSWORD`: Bitbucket'tan alacağınız Api Token. [https://id.atlassian.com/manage-profile/security/api-tokens]

### 2. GitHub Kurulumu

GitHub kullanıyorsanız, aşağıdaki dosyaları projenize kopyalayın:

1.  **Konfigürasyon Dosyaları:**
    *   `.pr_agent.toml` dosyasını ana dizine kopyalayın.
    *   `.github/pr_rules/` klasörünü ve içindeki tüm dosyaları, kendi projenizde de aynı yola (`.github/pr_rules/`) olacak şekilde kopyalayın.
    *   `.github/workflows/ai_review.yml` dosyasını, `.github/workflows/` klasörüne kopyalayın.

2.  **Secrets Ayarlayın:**
    Repository'nizde **Settings > Secrets and variables > Actions** kısmına gidin:
    *   `GCP_PROJECT_ID`: Google Cloud Proje ID'niz.
    *   `GCP_SA_KEY`: Google Service Account JSON dosyanızın içeriği.

<p align="right">(<a href="#readme-top">yukarı çık</a>)</p>

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

## <a id="context-decision-logic"></a>  Context & Decision Logic

As requested by the engineering lead, the agent operates on a **"Contextual Reasoning"** model:

1. **Persona-Driven Review:** The agent acts as a `Senior Software Architect`, prioritizing system stability and maintainability over minor formatting.
2. **Strategic Filtering:** Uses `minimal_severity: Medium` and `IGNORE_FILE_EXTENSIONS` to eliminate "noise" and focus on high-impact changes.
3. **Semantic Analysis:** Unlike linters, it simulates runtime behavior to catch logic errors like infinite loops or inefficient data filtering.
4. **Integrated Labeling:** Synchronizes with `.pr_agent.toml` to automatically categorize PRs by complexity (`size/XS` to `size/XL`) and risk.

---

##  Getting Started

Follow these steps to deploy the **sandbox-ai-review** agent in your repository. This setup ensures your code is audited by **Gemini 2.0 Flash** with senior-level precision.

###  Prerequisites

Before installation, ensure you have the following:

* **Google Cloud Project:** A GCP project with **Vertex AI API** enabled.
* **Service Account:** A service account with the **Vertex AI User** role.
* **Authentication:** A downloaded **JSON key** for the service account.
* **Secrets Access:** Permission to add **Actions Secrets** to your GitHub repository.

---

###  Installation

1. **Configure GitHub Secrets**
Navigate to **Settings > Secrets and variables > Actions** and add:
* `GCP_SA_KEY`: The full content of your Service Account JSON key.
* `GCP_PROJECT_ID`: Your unique Google Cloud Project ID.


2. **Add the Workflow**
Create `.github/workflows/ai_review.yml` and paste the provided workflow configuration. This triggers the agent on `pull_request` events.
3. **Define Custom Labels**
Add `.pr_agent.toml` to your root directory. This file defines the logic for categories like `security-review`, `performance`, and `Bug-fix`.
4. **Set Token Limits**
Ensure `CONFIG.CUSTOM_MODEL_MAX_TOKENS` is set to **8192** in your YAML to allow the agent to process complex files without truncation.

---

###  Understanding Label Automation

The agent is designed for **High-Signal Feedback**. To maintain a clean PR history, labeling follows a hybrid automation model:

* **Automatic Labels:** On PR creation, only the **Review Effort** label (e.g., `Effort: Low/High`) is applied automatically.
* **Manual Trigger (Custom Labels):** To apply your specific project labels defined in `.pr_agent.toml` (like `size/M`, `database`, or `security-review`), simply comment on the PR:
> `/generate_labels`


* **Why?** This prevents labels from being misapplied before the developer has finished their initial commits, ensuring the final categorization is accurate.

---

###  Pro Tip for the Demo

 By using `/generate_labels` manually, we ensure we only spend tokens on labeling once the PR is "Review Ready," saving approximately **20% in unnecessary token costs** per PR lifecycle.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

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

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## <a id="cost-analysis"></a>  Cost Analysis (January 2026)

Based on real-world monitoring, the Gemini 2.0 Flash implementation provides industry-leading efficiency:

| Metric | Value (Turkish Lira - TL) |
| --- | --- |
| **Avg. Cost per PR** | **~0.11 TL** (0,0023 $) |
| **Daily Peak (Jan 27)** | 1.6 TL -> 0,036$ (for 12 PRs)  |
| **Input Cost** | Higher due to context-heavy diff reading |
| **Output Cost** | Lower due to strict `Medium` severity filtering |

*Note: Costs are optimized by excluding non-code files like `.json`, `.css`, and `.svg`.*

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## <a id="best-prompts"></a>  Interactive Prompts (/ask)

Beyond automated reviews, the agent serves as an interactive engineering partner via the `/ask` command:

| English Prompt (Interactive Reasoning) | Türkçesi (Etkileşimli Sorgulama) |
| --- | --- |
| `"If I add 10 more payment types, how should I refactor this using a Strategy Pattern?"` | `"Gelecekte 10 ödeme tipi daha eklersem, bunu Strategy Pattern ile nasıl refactor etmeliyim?"` |
| `"Under a load of 1 million concurrent requests, where is the most likely bottleneck?"` | `"1 milyon eşzamanlı istek altında, en muhtemel darboğaz neresidir?"` |
| `"Explain this logic to a Junior Developer. What is the mental model?"` | `"Bu mantığı bir Junior yazılımcıya açıkla. Buradaki zihinsel model nedir?"` |
| `"Generate 5 edge cases (nulls, timeouts) that are NOT handled in this PR."` | `"Bu PR'da ele alınmayan 5 uç durum (null, zaman aşımı) üret."` |

### 🌐 Reference-Based Analysis (External Link Reasoning)
You can force the agent to evaluate code against specific industry standards or documentation:

| English Prompt (External Context) | Türkçesi (Harici Bağlam Sorgulama) |
| :--- | :--- |
| `"Based on the standards in <URL>, does this code align with their 'Best Practices'?"` | `"<URL> linkindeki standartlara göre, bu kod oradaki 'En İyi Pratikler' ile uyumlu mu?"` |
| `"Act as an expert in the framework in <URL>. Evaluate my PR using their tuning guide."` | `"<URL> linkindeki framework'te uzman biri gibi davran. PR'ımı oradaki ayar rehberine göre değerlendir."` |
| `"Compare my logic with the article in <URL>. Are there any missing edge cases?"` | `"Mantığımı <URL> makalesiyle karşılaştır. Eksik bir uç durum (edge case) var mı?"` |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Contact

**Alper** - Computer Engineering Intern at Abralabs  : [LinkedIn](https://www.linkedin.com/in/alpergulsenn/)

---

## Acknowledgments

* [Haktan Enes Biçer - Abralabs Technical Lead]
* [Codium-ai/pr-agent Documentation]
* [Google Cloud Vertex AI Reference]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

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
