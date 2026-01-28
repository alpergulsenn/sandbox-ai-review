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

export function evaluateRules(prData, rulesYaml) {
  const rules = yaml.load(rulesYaml);
  const labels = [];

  for (const group of Object.values(rules)) {
    for (const [label, condition] of Object.entries(group)) {
      if (eval(condition)) labels.push(label);
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
