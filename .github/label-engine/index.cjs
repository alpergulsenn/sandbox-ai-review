const fs = require("fs");
const path = require("path");

const provider = process.env.PROVIDER || "github";

async function run() {
  console.log("🔍 Label engine started");
  console.log("📦 Provider:", provider);

  const providerImpl = require(
    path.join(__dirname, "providers", `${provider}.cjs`)
  );

  const rulesPath = path.join(__dirname, "label-rules.yml");
  const rules = fs.readFileSync(rulesPath, "utf8");

  const prData = await providerImpl.getPullRequestData();
  const labels = providerImpl.evaluateRules(prData, rules);

  await providerImpl.applyLabels(prData, labels);
}

run().catch((err) => {
  console.error("❌ Label engine failed", err);
  process.exit(1);
});
