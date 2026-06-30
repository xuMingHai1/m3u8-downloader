# Release Checklist

发布新版本前按以下步骤检查。

## 准备

- [ ] 更新 `pom.xml` 中的项目版本号。
- [ ] 更新 `CHANGELOG.md`。
- [ ] 本地运行 `./mvnw.cmd test` 或 `./mvnw test`。
- [ ] 如修改打包流程，至少验证一个平台的 `portable` profile。

## 创建 Tag

Tag 使用 `v主版本.次版本.修订号` 格式，例如：

```shell
git tag v0.0.3
git push origin v0.0.3
```

推送 `v*` tag 后，GitHub Actions 会自动：

- 运行测试
- 构建多平台 portable zip
- 生成 sha256 校验文件
- 创建或更新 GitHub Release

## 发布后检查

- [ ] Release 中包含所有目标平台 zip。
- [ ] 每个 zip 都有对应 `.sha256` 文件。
- [ ] Release notes 内容可读。
- [ ] 下载并启动至少一个平台产物。
