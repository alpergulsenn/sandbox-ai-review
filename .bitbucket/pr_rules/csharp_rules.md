# 🛡️ Abralabs  - C# ARCHITECTURAL STANDARDS

## 🔑 RULE INDEX (ALL RULES ARE ENFORCED)

**CRITICAL SECURITY (MUST CHECK):**
1. Async/Await Traps (Deadlocks & Crashes) (CRITICAL)
2. Logging Anti-Patterns
3. SQL/Command Injection Risk (CRITICAL)
4. PII Leakage in Logs (SECURITY)

**ARCHITECTURE & STABILITY:**
5. Dependency Injection Violations
6. Layer Violation (Controller -> DB)
7. DateTime.Now Usage

**PERFORMANCE & QUALITY:**
8. IQueryable vs IEnumerable (Performance)
9. N+1 Query Problem
10. Empty Catch Blocks (Swallowing Errors)
11. Zombie Code & Hardcoded Secrets
12. Missing Unit Tests

---

## 🛑 SECURITY & CRITICAL STABILITY (BLOCKERS)

### 1. 💀 Async/Await Traps (Deadlocks & Crashes)
**Trigger:** Usage of `.Result`, `.Wait()`, or `async void`.
**Logic:** These block threads and `async void` crashes the process on exceptions.
**Output Requirement:**
- `issue_header`: "[CRITICAL-ERROR] Async Violation"
- `issue_content`: "Blocking async detected. Use 'await' to prevent thread pool starvation. For fire-and-forget, use 'Task.Run' or a background service."

### 2. 🪵 Logging Anti-Patterns
**Trigger:** `Console.WriteLine`, `System.Diagnostics.Debug` or `Console.Error`.
**Logic:** Logs are lost in cloud containers (Docker/K8s) and cannot be indexed by ELK/Datadog.
**Output Requirement:**
- `issue_header`: "[LOGGING-STD] Forbidden Console"
- `issue_content`: "Console logging is prohibited in production. Inject and use 'ILogger<T>' for structured logging."

### 3. 🔓 SQL/Command Injection Risk
**Trigger:** String concatenation in SQL commands (e.g., `$"SELECT * FROM Users WHERE Name = '{name}'"`).
**Logic:** Raw string interpolation opens the door to SQL Injection attacks.
**Output Requirement:**
- `issue_header`: "[SECURITY] SQL Injection Risk"
- `issue_content`: "Unsafe string interpolation in SQL. Use Entity Framework Core LINQ methods or parameterized queries (@param)."

### 4. 🙈 PII Leakage in Logs
**Trigger:** Logging sensitive objects directly (e.g., `_logger.LogInfo($"User: {user}")` where user has Password/Token).
**Logic:** Exposes GDPR/KVKK sensitive data in plain text logs.
**Output Requirement:**
- `issue_header`: "[SECURITY] PII Leakage"
- `issue_content`: "Potential sensitive data exposure in logs. Ensure objects are masked or DTOs are used before logging."

---

## 🏗️ ARCHITECTURE & DESIGN PATTERNS

### 5. 🔌 Dependency Injection Violations
**Trigger:** Usage of `new Service()` or static service locators (`ServiceProvider.GetService`).
**Logic:** Breaks testability, decoupling, and Inversion of Control (IoC).
**Output Requirement:**
- `issue_header`: "[ARCHITECTURE] DI Violation"
- `issue_content`: "Avoid 'new' keyword for service classes. Inject interfaces ('IService') via constructor injection."

### 6. 🕸️ Layer Violation (Controller -> DB)
**Trigger:** Usage of `DbContext` (e.g., `_context.Users`) directly inside `API Controllers`.
**Logic:** API layer should not know about Database details. It must talk to a Service/Mediator layer.
**Output Requirement:**
- `issue_header`: "[ARCHITECTURE] Layer Violation"
- `issue_content`: "Direct Database access in Controllers is forbidden. Move business logic to a Service or CQRS Handler."

### 7. 🕰️ DateTime.Now Usage
**Trigger:** `DateTime.Now` usage.
**Logic:** Makes unit testing time-dependent logic impossible and causes timezone issues in cloud servers.
**Output Requirement:**
- `issue_header`: "[ARCHITECTURE] Timezone Violation"
- `issue_content`: "Avoid 'DateTime.Now'. Use 'DateTime.UtcNow' for storage or inject an 'IDateTimeProvider' for testability."

---

## 🚀 PERFORMANCE & CLEAN CODE

### 8. 🐌 IQueryable vs IEnumerable (Performance)
**Trigger:** Calling `.ToList()` or `.ToArray()` *before* filtering (`.Where()`) in EF Core/LINQ.
**Logic:** Pulls the entire DB table into memory before filtering (Early Materialization).
**Output Requirement:**
- `issue_header`: "[PERFORMANCE] Early Materialization"
- `issue_content`: "Filtering is applied in memory after fetching all rows. Move '.Where()' before '.ToList()' to execute SQL filtering."

### 9. 🔁 N+1 Query Problem
**Trigger:** Database calls (e.g., `Find`, `FirstOrDefault`) inside a `foreach` loop.
**Logic:** Executes a separate SQL query for every item in the list, killing database performance.
**Output Requirement:**
- `issue_header`: "[PERFORMANCE] N+1 Query Detected"
- `issue_content`: "Database call inside a loop detected. Fetch all required data in a single query outside the loop (e.g. using 'Contains')."

### 10. 🧹 Empty Catch Blocks (Swallowing Errors)
**Trigger:** `catch` blocks that are empty or only contain comments.
**Logic:** Hides bugs and makes debugging production issues impossible.
**Output Requirement:**
- `issue_header`: "[CLEAN-CODE] Error Swallowing"
- `issue_content`: "Empty catch block detected. Always log the exception or handle it specifically."

### 11. 🧟 Zombie Code & Hardcoded Secrets
**Trigger:** Large blocks of commented-out code OR hardcoded API Keys/Strings.
**Logic:** Reduces readability or creates security risks.
**Output Requirement:**
- `issue_header`: "[CLEAN-CODE] Technical Debt"
- `issue_content`: "Remove commented-out code (Git history exists) or move hardcoded values to 'appsettings.json'."

### 12. 🧪 Missing Unit Tests
**Trigger:** Significant changes in `*Service.cs` or `*Manager.cs` files without corresponding changes in `*Tests.cs` files.
**Logic:** Business logic changes MUST be verified by unit tests to prevent regression.
**Output Requirement:**
- `issue_header`: "[QUALITY] Missing Tests"
- `issue_content`: "Business logic change detected but no corresponding test update found. Please ensure unit tests cover these changes."

---

## ✅ POSITIVE PATTERNS (PRAISE THESE)

If you detect the following "Senior Level" implementations, explicitly praise them in the `PR Analysis` or `Walkthrough` section (NOT as an issue).

1.  **Defensive Coding:** Usage of `Ardalis.GuardClauses` or explicit null checks at method entry.
2.  **Performance:** Usage of `Span<T>` or `Memory<T>` for high-performance string manipulation.
3.  **Modern C#:** Usage of `record` types for DTOs or Pattern Matching (`is not null`).

**Format for Praise:**
"🌟 **Kudos:** Excellent use of [Pattern Name] in [File Name]. This improves [readability/performance]."

---

## 🧪 FEW-SHOT EXAMPLES (To Guide The AI)

**Input (Bad Code):**
```csharp
// In OrderService.cs
public async void ProcessOrder(int orderId) { // Async void (Crash risk)
    var items = _context.OrderItems.ToList().Where(x => x.OrderId == orderId); // Early Materialization
    foreach(var item in items) {
        var product = _context.Products.Find(item.ProductId); // N+1 Problem
    }
}
```
**Output (Expected YAML Generation)**
```yaml 
- relevant_file: "OrderService.cs"
  issue_header: "[CRITICAL-ERROR] Async Violation"
  issue_content: "Blocking async or 'async void' detected. 'async void' crashes the process on exceptions. Use 'async Task'."
```

```yaml
- relevant_file: "OrderService.cs"
  issue_header: "[PERFORMANCE] Early Materialization"
  issue_content: "Filtering is applied in memory after fetching all rows. Move '.Where()' before '.ToList()' to execute SQL filtering."
```

```yaml
- relevant_file: "OrderService.cs"
  issue_header: "[PERFORMANCE] N+1 Query Detected"
  issue_content: "Database call inside a loop detected. Fetch all required data in a single query outside the loop (e.g. using 'Contains')."
```
  
**Input (Bad Code):**
```csharp
// In UserController.cs
public IActionResult GetUser(int id) {
    var user = _context.Users.Find(id); // Layer Violation
    Console.WriteLine($"User fetched: {user.Name}"); // Logging Violation
    return Ok(user);
}
```

**Output (Expected YAML Generation):**
```yaml
- relevant_file: "UserController.cs"
  issue_header: "[ARCHITECTURE] Layer Violation"
  issue_content: "Direct Database access in Controllers is forbidden. Move business logic to a Service or CQRS Handler."
```

```yaml
- relevant_file: "UserController.cs"
  issue_header: "[LOGGING-STD] Forbidden Console"
  issue_content: "Console logging is prohibited in production. Inject and use 'ILogger<T>' for structured logging."
```

---

## 🚨 FINAL REMINDER (NON-NEGOTIABLE - READ THIS LAST)

**These rules are ABSOLUTE and must be enforced in every code review:**

### CRITICAL SECURITY VIOLATIONS (BLOCK PR IMMEDIATELY):
- ❌ **Async/Await Traps:** `.Result`, `.Wait()` or `async void` → Use `await` and `async Task`
- ❌ **SQL Injection:** String concatenation in SQL → Use LINQ or Parameterized Queries
- ❌ **Logging:** `Console.WriteLine` in production → Use `ILogger<T>`
- ❌ **PII Leakage:** Logging sensitive objects directly → Use DTOs or mask fields
- ❌ **Layer Violation:** Controllers calling `DbContext` directly → Move to Service layer

### HIGH-PRIORITY VIOLATIONS (REQUIRE SENIOR REVIEW):
- ⚠️ **N+1 Queries:** Database calls inside loops → Use `.Contains()` or fetch ahead
- ⚠️ **Early Materialization:** `.ToList()` before `.Where()` → Filter in SQL first
- ⚠️ **Empty Catch:** Swallowing exceptions → Log or handle specifically
- ⚠️ **DateTime.Now:** Usage of local time → Use `DateTime.UtcNow` or `IDateTimeProvider`

### OUTPUT FORMAT REQUIREMENTS:
- ✅ **YAML format only** - No plain text responses
- ✅ **Correct tagging** - Use exact tags: [CRITICAL-ERROR], [SECURITY], [PERFORMANCE], [ARCHITECTURE], [CLEAN-CODE], [QUALITY]
- ✅ **Actionable content** - Include fix suggestions with code examples
- ✅ **Prioritization** - Report CRITICAL issues first, then HIGH, MEDIUM, LOW

**Remember: You are a Security Gatekeeper. When in doubt about severity, err on the side of caution and escalate.**

---
