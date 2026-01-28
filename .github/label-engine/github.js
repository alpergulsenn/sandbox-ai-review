import { getOctokit, context } from "@actions/github";

export async function getPRContext() {
  const token = process.env.GITHUB_TOKEN;
  const octokit = getOctokit(token);
  const { owner, repo } = context.repo;
  const pr = context.payload.pull_request;

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
    diff: "",
    linesChanged
  };
}

export async function applyLabels(contextData, labels) {
  const token = process.env.GITHUB_TOKEN;
  const octokit = getOctokit(token);

  await octokit.rest.issues.addLabels({
    owner: contextData.owner,
    repo: contextData.repo,
    issue_number: contextData.prNumber,
    labels
  });
}
