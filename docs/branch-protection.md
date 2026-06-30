# Branch Protection

建议在 GitHub 仓库 `Settings -> Branches` 或 `Settings -> Rules -> Rulesets` 中配置主分支保护。

## 推荐规则

目标分支：

- `main`
- `master`

推荐设置：

- Require a pull request before merging
- Require status checks to pass before merging
- Require branches to be up to date before merging
- Required status check: `Test`
- Restrict deletions
- Block force pushes

## Tag 发布约定

- Release tag 使用 `v*`，例如 `v0.0.3`。
- tag 应从主分支上已经通过 CI 的 commit 创建。
- 不建议覆盖已发布 tag；如需修复，发布新的 patch 版本。

## 注意

分支保护是 GitHub 仓库设置，不会因为提交本文件自动生效。维护者需要在 GitHub 页面手动开启。
