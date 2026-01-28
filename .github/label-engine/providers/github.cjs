const octokit = getOctokit(token);

const { owner, repo } = context.repo;
const prNumber = context.payload.pull_request.number;const yaml = require("js-yaml");

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
    pr_description: context.payload.pull_request.body || "",
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

  const ctx = { pr_stats, diff, pr_description };

  for (const group of Object.values(rules)) {
    for (const [label, condition] of Object.entries(group)) {
      try {
        if (eval(condition)) {
          labels.push(label);
        }
      } catch (e) {
        console.warn(`⚠️ Rule failed for ${label}:`, condition);
      }
    }
  }

  return [...new Set(labels)];
}


async function applyLabels(prData, labels) {
  if (!labels || labels.length === 0) {
    console.log("ℹ️ No labels to apply");
    return;
  }

  const { owner, repo, prNumber, octokit } = prData;

  console.log("🏷 Applying labels:", labels);

  await octokit.rest.issues.addLabels({
    owner,
    repo,
    issue_number: prNumber,
    labels,
  });
}


module.exports = {
  getPullRequestData,
  evaluateRules,
  applyLabels,
};

