# 仓库指南

## 项目结构与模块组织

本项目是基于 Maven、Java 26、JavaFX 的模块化 M3U8 视频下载桌面应用。主代码位于 `src/main/java/xyz/xuminghai/m3u8_downloader`：`download` 处理下载请求与状态，`task` 执行 M3U8 下载任务，`m3u8` 负责播放列表解析模型，`http` 封装网络请求，`view`/`viewmodel` 承载 JavaFX 界面，`config`、`control`、`util` 存放配置、控件和工具类。资源位于 `src/main/resources`，其中 `css` 放样式，`img` 放运行时图片；`src/main/jpackage` 放 jpackage 图标等打包资源。README 图片放在 `md_data`，构建产物只应出现在 `target`。

## 构建、测试与打包命令

- `mvn clean package`：清理并构建 jlink 运行时压缩包。
- `mvn clean verify -P portable`：生成当前操作系统对应的 jpackage 便携版应用目录和压缩包。
- `mvn test`：运行 Maven test 阶段；当前仓库保留 `src/test` 目录，但没有测试类和显式测试依赖。
- `mvn clean`：删除 `target` 下的生成文件。

平台 profile（如 `windows-x86_64`、`linux-x86_64`、`macosx-arm64`）按当前系统自动设置 JavaFX 与 JavaCPP FFmpeg classifier。`dev` profile 只用于引入 DevToolsFX；应用还需要以 `-Dapp.devModel=true` 启动才会打开开发工具。

## 编码风格与命名约定

项目默认语言为简体中文，文档、注释和用户可见文本优先使用简体中文。源码使用 UTF-8、Java 26 和包根 `xyz.xuminghai.m3u8_downloader`。保持现有风格：4 空格缩进，左花括号同行，类名 `PascalCase`，方法和字段 `camelCase`，枚举常量全大写。新增 Java 源文件应遵循现有 GPL-3.0 文件头风格。Lombok 已作为 provided 依赖引入，只在能明显减少样板代码时使用。

## 测试指南

当前测试目录为空，且 `pom.xml` 未直接声明 JUnit/AssertJ 等 test scope 依赖。新增测试时，先补齐测试依赖，再在 `src/test/java` 中镜像生产包结构。测试类命名为 `*Test`，方法使用行为描述式命名，例如 `shouldRejectIllegalTransitions`。下载状态机、请求校验、M3U8 解析、重试/取消、文件路径和自动合并 MP4 等逻辑变更，应优先补测试。

## 提交与 Pull Request 要求

历史提交信息简短直接，常见形式包括优化说明、依赖升级和问题修复，例如 `fix #1`。每个提交聚焦一个变更；涉及 issue 时在提交或 PR 中引用。PR 应包含变更摘要、执行过的验证命令、影响平台；涉及 JavaFX 界面或打包产物时，附截图、录屏或产物路径说明。

## 安全、配置与本地文件

不要提交下载生成的视频、运行日志、IDE 状态、个人代理配置、临时脚本、`.agents` 本地工具目录或 `target` 产物。只有服务于项目构建、发布或团队协作的脚本才应进入仓库。修改代理、文件路径、并发重试、AES-128 密钥处理、FFmpeg 合并或 jpackage 配置时要格外谨慎；新增用户可见配置时，同步更新 `README.md`。
