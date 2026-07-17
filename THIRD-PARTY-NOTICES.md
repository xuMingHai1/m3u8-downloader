# 第三方软件声明

本文件列出 `m3u8-downloader` portable 发布包直接携带的主要第三方运行时组件，以及随源码仓库
分发的构建工具。项目自身代码仍按 `GPL-3.0-or-later` 发布；第三方组件分别适用其自身许可证。

许可证原文位于 `third-party-licenses`。由 jlink 生成的 OpenJDK 运行时还会在 `runtime/legal` 中保留
JDK 自带的模块级法律文件。

## Java 运行时与界面组件

### OpenJDK 26.0.1

- 用途：portable 包内的 Java 运行时。
- 许可证：GPL-2.0-only with Classpath Exception；部分模块含额外第三方条款。
- 法律文件：发布包内的 `runtime/legal`。
- 项目地址：https://openjdk.org/projects/jdk/26/

### OpenJFX 26.0.1

- 组件：`javafx-base`、`javafx-graphics`、`javafx-controls`。
- 许可证：GPL-2.0-only with Classpath Exception。
- 许可证文件：`third-party-licenses/openjfx`。
- 项目地址：https://github.com/openjdk/jfx

### AtlantaFX 2.1.0

- 组件：`io.github.mkpaz:atlantafx-base:2.1.0`。
- 许可证：MIT。
- 许可证文件：`third-party-licenses/atlantafx/LICENSE`。
- 项目地址：https://github.com/mkpaz/atlantafx

## 日志组件

### Logback 1.5.38

- 组件：`logback-classic`、`logback-core`。
- 许可证：EPL-2.0 或 LGPL-2.1；本项目按 LGPL-2.1 条款使用和分发。
- 许可证文件：`third-party-licenses/logback`。
- 项目地址：https://logback.qos.ch/

### SLF4J 2.0.17

- 组件：`org.slf4j:slf4j-api:2.0.17`。
- 许可证：MIT。
- 许可证文件：`third-party-licenses/slf4j/LICENSE.txt`。
- 项目地址：https://www.slf4j.org/

## FFmpeg 与 JavaCPP

### JavaCPP 1.5.13

- 组件：`org.bytedeco:javacpp:1.5.13`。
- 可选许可证包括 Apache-2.0、GPL-2.0-or-later 和 GPL-2.0-or-later with Classpath Exception；
  本项目选择 Apache-2.0。
- 许可证文件：`third-party-licenses/javacpp/LICENSE.txt`。
- 项目地址：https://github.com/bytedeco/javacpp

### JavaCPP Presets for FFmpeg 8.0.1-1.5.13

- 组件：`org.bytedeco:ffmpeg:8.0.1-1.5.13` 及对应平台 native JAR。
- Java 包装代码按 JavaCPP Presets 的多许可证条款发布；本项目选择 Apache-2.0。
- 许可证文件：`third-party-licenses/javacpp-presets/LICENSE.txt`。
- 项目地址：https://github.com/bytedeco/javacpp-presets/tree/1.5.13/ffmpeg

### FFmpeg 8.0.1 native 二进制

- 当前普通平台构建启用了 `--enable-version3`，未启用 `--enable-gpl` 或 `--enable-nonfree`，按 LGPLv3
  路径分发。
- FFmpeg 许可证说明和 LGPLv3 原文位于 `third-party-licenses/ffmpeg`。
- 实际构建参数及启用的外部库版本见 `third-party-licenses/ffmpeg/BUILD-CONFIGURATION.txt`。
- 外部库许可证、版权声明和精确源码入口见
  `third-party-licenses/ffmpeg/EXTERNAL-LIBRARIES.md` 及其 `external` 子目录。
- FFmpeg 源码：https://github.com/FFmpeg/FFmpeg/tree/n8.0.1
- JavaCPP Presets 构建源码：https://github.com/bytedeco/javacpp-presets/tree/1.5.13/ffmpeg

FFmpeg native 二进制还启用了 zlib、LAME、Speex、Opus、OpenCORE AMR、VisualOn AMR-WB、OpenSSL、
OpenH264、libvpx、FreeType、HarfBuzz、mfx_dispatch、libxml2、SRT、libwebp、libaom、SVT-AV1、zimg
等外部库。这些库仍分别适用其上游许可证。

### Independent JPEG Group 署名

FFmpeg 的部分离散余弦变换代码来源于 Independent JPEG Group（IJG）。本项目对 FFmpeg 8.0.1
源码没有额外修改；对应上游文件及其修改历史以 FFmpeg n8.0.1 源码为准。本声明用于满足 FFmpeg
许可证说明中针对仅分发可执行文件时保留 IJG 署名和修改信息的要求。

## 数据库组件

### SQLite JDBC 3.53.2.0

- 组件：`org.xerial:sqlite-jdbc:3.53.2.0`。
- 许可证：Apache-2.0；其中还包含 SQLite 公共领域代码及 Zentus BSD 风格许可代码。
- 许可证文件：`third-party-licenses/sqlite-jdbc`。
- 项目地址：https://github.com/xerial/sqlite-jdbc

## 仓库构建工具

### Apache Maven Wrapper 3.3.4

- 文件：`mvnw`、`mvnw.cmd`；本仓库使用 only-script 模式，不携带 Maven Wrapper JAR。
- 用途：从源码仓库启动项目构建，不属于应用运行时组件。
- 许可证：Apache-2.0。
- 许可证及署名文件：`third-party-licenses/maven-wrapper`。
- 项目地址：https://maven.apache.org/tools/wrapper/

## 不在 portable 清单中的依赖

JUnit、AssertJ 仅用于测试，Lombok 仅用于编译，DevToolsFX 仅在显式启用 `dev` profile 时加入；
它们不属于默认 portable 发布包，因此未列入本发布清单。
