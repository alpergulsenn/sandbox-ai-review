import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

// __dirname ESM karşılığı
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const provider = process.env.PROVIDER || "github";

async function run() {
  console.log("🔍 Label engine started");
  console.log("📦 Provider:", provider);

  // ✅ ESM uyumlu provider import
  const providerModule = await import(`./providers/${provider}.mjs`);

  const rulesPath = path.join(__dirname, "label-rules.yml");
  const rules = fs.readFileSync(rulesPath, "utf8");

  const prData = await providerModule.getPullRequestData();
  const labels = providerModule.evaluateRules(prData, rules);

  await providerModule.applyLabels(prData, labels);
}

run().catch(err => {
  console.error("❌ Label engine failed");
  console.error(err);
  process.exit(1);
});

