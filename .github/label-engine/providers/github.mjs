import { context, getOctokit } from "@actions/github";
import yaml from "js-yaml";

function getStats(files) {
  let additions = 0;
  let deletions = 0;

  files.forEach(f => {
    additions += f.additions;
    deletions += f.deletions;
  });

  return { lines_changed: additions + deletions };
}

export async function getPullRequestData() {
  const octokit = getOctokit(process.env.GITHUB_TOKEN);

  const { owner, repo } = context.repo;
  const prNumber = context.payload.pull_request.number;

  const files = await octokit.rest.pulls.listFiles({
    owner,
    repo,
    pull_number: prNumber,
  });

  const diff = files.data.map(f => f.patch || "").join("\n");

  return {
    diff,
    pr_description: context.payload.pull_request.body || "",
    pr_stats: getStats(files.data),
    octokit,
    owner,
    repo,
    prNumber,
  };
}

function evaluateRules(prData, rulesYaml) {
  const rules = yaml.load(rulesYaml);
  const labels = [];

  const { pr_stats, diff, pr_description } = prData;
  const text = `${diff}\n${pr_description}`.toLowerCase();

  // SIZE LABEL
  const lines = pr_stats.lines_changed;
  const size = rules.size;

  if (lines <= size.XS) labels.push("size/XS");
  else if (lines <= size.S) labels.push("size/S");
  else if (lines <= size.M) labels.push("size/M");
  else if (lines <= size.L) labels.push("size/L");
  else labels.push("size/XL");

  // KEYWORD LABELS
  for (const label of Object.keys(rules.keywords)) {
    const keywords = rules.keywords[label];
    if (keywords.some(k => text.includes(k))) {
      labels.push(label);
    }
  }

  return [...new Set(labels)];
}


export async function applyLabels(prData, labels) {
  if (!labels.length) return;

  await prData.octokit.rest.issues.addLabels({
    owner: prData.owner,
    repo: prData.repo,
    issue_number: prData.prNumber,
    labels,
  });
}

export default {
  getPullRequestData,
  evaluateRules,
  applyLabels,
};
