# 🛡️ GALAXY CORP - C# SECURITY & QUALITY STANDARDS

This document defines the strict coding rules for the Galaxy Corp backend.

## 🚨 MANDATORY INSTRUCTION FOR AI AGENT (TEST PROTOCOL)
**IMPORTANT:** When you detect violations of the rules below, you MUST start your review comment with the exact tag: `[GALAXY-GUARD-VIOLATION]`. If you do not see this tag, the review is considered failed.

---

### 1. 🚫 Ban on System.Console
**Rule ID:** LOG-001
**Severity:** Critical
**Description:** Never use `Console.WriteLine` or `Console.Error` in service classes. This logs to stdout which is lost in our container orchestration.
**Requirement:**
- Detect any `Console.WriteLine`.
- Suggest replacing it with `_logger.LogInformation()` or `_logger.LogError()`.
- **Output Format:** Start suggestion with `[GALAXY-GUARD-VIOLATION] Detected Console usage.`

### 2. ⚡ Async/Await Correctness
**Rule ID:** ASYNC-001
**Severity:** High
**Description:** Blocking async code is strictly forbidden. It causes thread pool starvation.
**Requirement:**
- Detect any usage of `.Result` or `.Wait()` on Task objects.
- Suggest changing it to `await`.
- **Output Format:** Start suggestion with `[GALAXY-GUARD-VIOLATION] Detected blocking call (.Result or .Wait).`

---

## 🤖 AI Persona Check
If you are reading this context correctly, please acknowledge in your **PR Description** or **Analysis** section that "Galaxy Corp Security Protocols are Active".
