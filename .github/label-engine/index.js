import fs from "fs";
import yaml from "js-yaml";
import { getPRContext, applyLabels } from "./github.js";

function loadRules() {
  return yaml.load(
    fs.readFileSync(".github/label-rules.yml", "utf8")
  );
}

function computeSizeLabel(config, linesChanged) {
  for (const [size, limit] of Object.entries(config.size.thresholds)) {
    if (linesChanged <= limit) {
      return config.size.prefix + size;
    }
  }
  return null;
}

function collectText(rule, context) {
  return (rule.sources || [])
    .map(src => {
      if (src === "filenames") return context.filenames.join(" ");
      return context[src] || "";
    })
    .join(" ")
    .toLowerCase();
}

async function main() {
  const config = loadRules();
  const context = await getPRContext();
  const labels = new Set();

  const sizeLabel = computeSizeLabel(config, context.linesChanged);
  if (sizeLabel) labels.add(sizeLabel);

  for (const rule of config.rules) {
    if (rule.match) {
      const text = collectText(rule, context);
      const regex = new RegExp(rule.match.join("|"), "i");
      if (regex.test(text)) {
        labels.add(rule.label);
      }
    }

    if (rule.conditions?.min_lines_changed) {
      if (context.linesChanged >= rule.conditions.min_lines_changed) {
        labels.add(rule.label);
      }
    }
  }

  if (labels.size > 0) {
    await applyLabels(context, [...labels]);
  }

  console.log("Applied labels:", [...labels]);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
