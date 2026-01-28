const github = require("@actions/github");

async function getPRContext() {
  const token = process.env.GITHUB_TOKEN;
  const octokit = github.getOctokit(token);
  const { owner, repo } = github.context.repo;
  const pr = github.context.payload.pull_request;

  const files = await octokit.rest.pulls.listFiles({
    owner,
    repo,
    pull_number: pr.number,
    per_page: 100
  });

  const filenames = files.data.map(f => f.filename);

  const linesChanged = files.data.reduce(
    (sum, f) => sum + f.additions + f.deletions,
    0
  );

  return {
    owner,
    repo,
    prNumber: pr.number,
    title: pr.title || "",
    description: pr.body || "",
    filenames,
    diff: "", // şimdilik boş
    linesChanged
  };
}

async function applyLabels(context, labels) {
  const token = process.env.GITHUB_TOKEN;
  const octokit = github.getOctokit(token);

  await octokit.rest.issues.addLabels({
    owner: context.owner,
    repo: context.repo,
    issue_number: context.prNumber,
    labels
  });
}

module.exports = { getPRContext, applyLabels };
