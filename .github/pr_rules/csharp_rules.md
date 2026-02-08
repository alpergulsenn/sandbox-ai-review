# 🛡️ GALAXY CORP - C# SECURITY & QUALITY STANDARDS

## 🚨 SYSTEM OVERRIDE: YAML OUTPUT FORMATTING
You are functioning as a strict Security Gatekeeper. The user requires specific tagging within the YAML output fields to parse the results programmatically.

**CRITICAL INSTRUCTION:**
When generating the `key_issues_to_review` list in your YAML output, you MUST inject the tag `[GALAXY-GUARD-VIOLATION]` directly into the `issue_header` field string.

---

### 1. 🚫 Ban on System.Console
**Rule ID:** LOG-001
**Target:** `Console.WriteLine` or `Console.Error` usage in service classes.
**Instruction:**
- Identify the violation.
- **YAML Constraint:** The `issue_header` field MUST start with `[GALAXY-GUARD-VIOLATION]`.
- **Example Output:**
  ```yaml
  - relevant_file: "..."
    issue_header: "[GALAXY-GUARD-VIOLATION] Forbidden Console Usage"
    issue_content: "Use ILogger instead of Console.WriteLine. Console logs are lost in container orchestration."
