---
name: git-workflow
description: Git workflow management including branching, commits, PRs, and conflict resolution
license: MIT
compatibility: opencode
metadata:
  audience: all-developers
  vcs: git
---

## What I do
- Create and manage Git branches for feature development
- Stage, commit, and amend changes with proper messages
- Resolve merge conflicts and rebase branches
- Create and manage pull requests via GitHub CLI
- Sync with remote repositories (push, pull, fetch)
- Inspect history and diagnose issues

## When to use me
Use me when working with Git, especially when:
- Creating feature branches or hotfixes
- Committing changes with descriptive messages
- Resolving merge or rebase conflicts
- Managing pull requests and code reviews
- Syncing work with teammates
- Investigating issues in commit history

## Common commands
- `git status` - Show working tree status
- `git diff` - Show unstaged changes
- `git diff --staged` - Show staged changes
- `git add .` - Stage all changes
- `git commit -m "message"` - Commit staged changes
- `git commit --amend` - Modify last commit
- `git branch feature-name` - Create branch
- `git checkout -b feature-name` - Create and checkout branch
- `git checkout main` - Switch to main branch
- `git merge feature-name` - Merge branch
- `git rebase main` - Rebase current branch onto main
- `git push -u origin feature-name` - Push branch with upstream
- `git pull --rebase` - Pull with rebase
- `git log --oneline -10` - Show last 10 commits
- `git log -p --follow file` - Show file history

## Branching strategies
- Feature branches: `feature/feature-name`
- Bugfix branches: `bugfix/issue-description`
- Hotfix branches: `hotfix/critical-fix`
- Release branches: `release/version-number`
- Delete branch after merge: `git branch -d feature-name`

## Commit message conventions
- Use imperative mood: "Add feature" not "Added feature"
- Keep first line under 50 characters
- Leave blank line between summary and body
- Reference issues: `Closes #123` or `Refs #456`
- Explain "why" not "what" in body

## Conflict resolution
- Run `git status` to find conflicts
- Open files with conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`)
- Resolve by keeping desired code
- Mark resolved with `git add file`
- Complete merge/rebase
- Test thoroughly before committing

## GitHub CLI patterns
- `gh pr create` - Create pull request
- `gh pr list` - List pull requests
- `gh pr view 123` - View PR details
- `gh pr merge 123` - Merge PR
- `gh issue list` - List issues
- `gh repo clone org/repo` - Clone repository

## Rebasing vs merging
- Rebase: cleaner linear history, avoid for shared branches
- Merge: preserve branch history, safer for collaboration
- Interactive rebase: `git rebase -i HEAD~3` to edit commits
- Squash: combine multiple commits into one

## Safety checks
- Check `git status` before destructive commands
- Never force push to shared branches (`main`, `master`)
- Use `--dry-run` flag with caution
- Backup branches with tags before major operations
- Verify with `git reflog` if you lose commits

## Interactive operations
- `git add -p` - Stage changes interactively (hunks)
- `git commit -v` - Show diff when committing
- `git rebase -i` - Interactive rebase to edit/squash commits
- `git clean -fd` - Remove untracked files and directories (use with caution)
- `git restore file` - Discard changes in working directory
