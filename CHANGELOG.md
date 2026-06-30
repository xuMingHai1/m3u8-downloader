# Changelog

本项目的所有重要变更都会记录在此文件中。

版本号建议使用 `v主版本.次版本.修订号`，例如 `v0.0.2`。发布 GitHub tag 后，CI 会自动构建多平台便携包并创建或更新 Release。

## [Unreleased]

### Added

- 新增基于 GitHub Actions 的测试、打包和发布流程。
- 新增 Maven Wrapper，固定 Maven 版本。
- 新增 `jlink + jpackage` 便携版打包流程。

### Changed

- 优化 Maven POM 中的依赖、插件和 profile 配置。

## [0.0.2] - 2026-06-30

### Added

- 支持生成无需安装的便携版应用包。
- 支持 Windows、Linux、macOS 多平台打包配置。
