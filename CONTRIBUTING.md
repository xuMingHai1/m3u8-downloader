# Contributing

感谢你关注这个项目。提交 issue 或 pull request 前，请先阅读以下约定。

## 开发环境

- JDK 26
- Maven Wrapper
- 推荐使用 IntelliJ IDEA

首次构建前无需单独安装 Maven，项目内置 Maven Wrapper：

```shell
./mvnw.cmd test
```

Linux 或 macOS：

```shell
./mvnw test
```

## 常用命令

```shell
./mvnw.cmd test
./mvnw.cmd clean verify -P portable
```

Linux 或 macOS 使用 `./mvnw` 替代 `./mvnw.cmd`。

## 分支与提交

- 每个 pull request 聚焦一个主题。
- 提交信息保持简短直接，例如 `fix #1`、`build: add portable package workflow`。
- 涉及行为变化时，在 PR 中说明影响范围和验证方式。

## Pull Request 检查

提交 PR 前请确认：

- 已运行相关测试。
- 修改打包流程时，已验证至少一个目标平台。
- 修改 JavaFX 界面时，已附截图或录屏。
- 没有提交 `target`、日志、下载产物或本地 IDE 状态。

## 代码风格

- Java 代码使用 4 空格缩进。
- 文件编码使用 UTF-8。
- 包根为 `xyz.xuminghai.m3u8_downloader`。
- UI 逻辑放在 `view` 或 `viewmodel`，下载流程放在 `download` 或 `task`，解析和网络逻辑不要混入界面类。

## 安全注意事项

提交日志、截图或复现链接前，请先脱敏：

- 下载链接中的 token
- 代理地址和账号
- AES-128 密钥
- 本地文件路径
