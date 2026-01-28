import fs from "fs";
import path from "path";
import githubProvider from "./providers/github.mjs";

console.log("🔍 Label engine started");
console.log("📦 Provider: github");

const rulesPath = path.resolve(".github/label-engine/label-rules.yml");
const rules = fs.readFileSync(rulesPath, "utf8");

const prData = await githubProvider.getPullRequestData();
const labels = githubProvider.evaluateRules(prData, rules);
await githubProvider.applyLabels(prData, labels);
