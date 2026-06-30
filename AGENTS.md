# 仓库指南

## 项目结构与模块组织

本项目是基于 Java 26、JavaFX、Maven Wrapper 的模块化 M3U8 视频下载桌面应用。主代码位于 `src/main/java/xyz/xuminghai/m3u8_downloader`：`download` 处理下载请求与状态，`task` 执行下载任务，`m3u8` 负责播放列表解析模型，`http` 封装网络请求，`view`/`viewmodel` 承载界面，`config`、`control`、`util` 存放配置、控件和工具类。资源位于 `src/main/resources`，`css` 放样式，`img` 放运行时图片；`src/main/jpackage` 放 jpackage 图标等打包资源。`docs` 存放发布和分支保护说明，`.github` 存放 CI、PR 模板和 issue 模板，`md_data` 存放 README 图片。

## 构建、测试与打包命令

- Windows：`./mvnw.cmd test` 运行 CI 同款测试阶段。
- Windows：`./mvnw.cmd clean verify -P portable,windows-x86_64` 构建 Windows 便携版。
- Linux/macOS：使用 `./mvnw` 替代 `./mvnw.cmd`，并按平台选择 `linux-x86_64`、`linux-arm64`、`macosx-x86_64` 或 `macosx-arm64`。
- `./mvnw.cmd clean package` 只生成 jlink 运行时压缩包；`target` 下内容均为构建产物。

优先使用 Maven Wrapper，不要求贡献者本机单独安装 Maven。`.mvn/jvm.config` 已固定 UTF-8 相关 JVM 参数。`dev` profile 只用于引入 DevToolsFX；应用还需要以 `-Dapp.devModel=true` 启动才会打开开发工具。GitHub Actions 在 PR 上运行测试，在非 PR 推送和 `v*` tag 上构建多平台 portable zip。

## 编码风格与命名约定

项目默认语言为简体中文，文档、注释和用户可见文本优先使用简体中文。遵守 `.editorconfig`：UTF-8、LF、末尾换行、去除行尾空格，Java 使用 4 空格缩进，XML/YAML/CSS/Markdown 使用 2 空格缩进，最大行宽 120。Java 包根为 `xyz.xuminghai.m3u8_downloader`，类名使用 `PascalCase`，方法和字段使用 `camelCase`，枚举常量全大写。新增 Java 源文件应遵循现有 GPL-3.0 文件头风格。Lombok 为 provided 依赖，只在能明显减少样板代码时使用。

## 测试指南

当前仓库保留 `src/test` 目录，但没有项目自有测试类，也没有直接声明 JUnit/AssertJ 等 test scope 依赖。新增测试时，先补齐测试依赖，再在 `src/test/java` 中镜像生产包结构。测试类命名为 `*Test`，方法使用行为描述式命名，例如 `shouldRejectIllegalTransitions`。下载状态机、请求校验、M3U8 解析、重试/取消、路径处理、AES-128 解密和自动合并 MP4 等逻辑变更，应优先补测试。提交前至少运行 `./mvnw.cmd test` 或 `./mvnw test`。

## 提交与 Pull Request 要求

遵守 `CONTRIBUTING.md` 和 `.github/pull_request_template.md`。提交信息保持简短直接，可使用中文说明，也可使用范围前缀，例如 `build: 添加 jlink+jpackage 便携版打包流程`、`fix #1`。每个 PR 聚焦一个主题，说明变更摘要、验证方式、风险范围和关联 issue。涉及 JavaFX 界面时附截图或录屏；涉及打包流程时说明已验证的平台和产物路径。发布前参考 `docs/release-checklist.md`，安全问题按 `SECURITY.md` 私下报告。

## 安全、配置与本地文件

不要提交下载生成的视频、运行日志、IDE 状态、个人代理配置、临时脚本、`.agents` 本地工具目录或 `target` 产物。只有服务于项目构建、发布或团队协作的脚本才应进入仓库；Maven Wrapper、CI、发布文档属于可提交项目基础设施。修改代理、文件路径、并发重试、AES-128 密钥处理、FFmpeg 合并、JavaCPP classifier 或 jpackage 配置时要格外谨慎；新增用户可见配置时，同步更新 `README.md`、`CHANGELOG.md` 或相关 `docs` 文档。
