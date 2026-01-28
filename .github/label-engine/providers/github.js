const core = require("@actions/core");
const github = require("@actions/github");
const yaml = require("js-yaml");

function getStats(files) {
  let additions = 0;
  let deletions = 0;

  files.forEach(f => {
    additions += f.additions;
    deletions += f.deletions;
  });

  return {
    lines_changed: additions + deletions
  };
}

async function getPullRequestData() {
  const token = process.env.GITHUB_TOKEN;
  const octokit = github.getOctokit(token);

  const { owner, repo } = github.context.repo;
  const prNumber = github.context.payload.pull_request.number;

  const files = await octokit.rest.pulls.listFiles({
    owner,
    repo,
    pull_number: prNumber,
  });

  const diff = files.data.map(f => f.patch || "").join("\n");

  return {
    diff,
    pr_description: github.context.payload.pull_request.body || "",
    pr_stats: getStats(files.data),
    context: github.context,
    octokit,
    owner,
    repo,
    prNumber
  };
}
function evaluateRules(prData, rulesYaml) {
  const rules = yaml.load(rulesYaml);
  const labels = [];

  const { pr_stats, diff, pr_description } = prData;

  function add_label(label) {
    labels.push(label);
  }

  function contains(text, keyword) {
    return text.includes(keyword);
  }

  // 🔥 RULES EXECUTION

  eval(`
    ${rules}
  `);

  return [...new Set(labels)];
}

async function applyLabels(labels) {
  if (labels.length === 0) return;

  const { owner, repo, prNumber, octokit } = globalThis.prData || {};

  console.log("🏷 Applying labels:", labels);

  await octokit.rest.issues.addLabels({
    owner,
    repo,
    issue_number: prNumber,
    labels
  });
}

module.exports = {
  getPullRequestData,
  evaluateRules,
  applyLabels
};

