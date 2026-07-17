# FFmpeg 外部库许可证

本文件记录 JavaCPP Presets 1.5.13 构建 FFmpeg 8.0.1 native 二进制时启用并链接的外部库。
版本和启用参数以同目录的 `BUILD-CONFIGURATION.txt` 为准。

各组件仍适用其自身许可证。随附原文保留了上游版权声明、许可条件和免责声明；源码链接指向构建时
使用的对应版本，便于获取和重新构建对应源代码。

## 组件清单

- zlib 1.3.2
  - 许可证：Zlib
  - 原文：`external/zlib/LICENSE`
  - 源码：https://github.com/madler/zlib/tree/v1.3.2
- LAME 3.100
  - 许可证：LGPL-2.0-or-later
  - 原文：`external/lame/COPYING`
  - 源码：https://sourceforge.net/projects/lame/files/lame/3.100/
- Speex 1.2.1
  - 许可证：BSD-3-Clause
  - 原文：`external/speex/COPYING`
  - 源码：https://github.com/xiph/speex/tree/Speex-1.2.1
- Opus 1.3.1
  - 许可证：BSD-3-Clause
  - 原文：`external/opus/COPYING`
  - 源码：https://github.com/xiph/opus/tree/v1.3.1
- OpenCORE AMR 0.1.6
  - 许可证：Apache-2.0
  - 原文：`external/opencore-amr/LICENSE`
  - 源码：https://sourceforge.net/p/opencore-amr/code/ci/v0.1.6/tree/
- VisualOn AMR-WB 0.1.3
  - 许可证：Apache-2.0
  - 原文：`external/vo-amrwbenc/COPYING`
  - 源码：https://sourceforge.net/p/opencore-amr/vo-amrwbenc/ci/v0.1.3/tree/
- OpenSSL 3.5.5
  - 许可证：Apache-2.0
  - 原文：`external/openssl/LICENSE.txt`
  - 源码：https://github.com/openssl/openssl/tree/openssl-3.5.5
- OpenH264 2.6.0
  - 许可证：BSD-2-Clause
  - 原文：`external/openh264/LICENSE`
  - 源码：https://github.com/cisco/openh264/tree/v2.6.0
- libvpx 1.15.2
  - 许可证：BSD-3-Clause
  - 原文：`external/libvpx/LICENSE`
  - 源码：https://github.com/webmproject/libvpx/tree/v1.15.2
- FreeType 2.14.1
  - 许可证：FTL（本发布物采用该许可路径）
  - 原文：`external/freetype/FTL.TXT`
  - 源码：https://github.com/freetype/freetype/tree/VER-2-14-1
- HarfBuzz 12.3.0
  - 许可证：MIT
  - 原文：`external/harfbuzz/COPYING`
  - 源码：https://github.com/harfbuzz/harfbuzz/tree/12.3.0
- mfx_dispatch 1.35.1
  - 许可证：BSD-3-Clause
  - 原文：`external/mfx-dispatch/LICENSE`
  - 源码：https://github.com/lu-zero/mfx_dispatch/tree/1.35.1
- nv-codec-headers 13.0.19.0
  - 许可证：MIT
  - 原文：`external/nv-codec-headers/LICENSE.txt`
  - 源码：https://github.com/FFmpeg/nv-codec-headers/tree/n13.0.19.0
- libxml2 2.9.12
  - 许可证：MIT
  - 原文：`external/libxml2/Copyright`
  - 源码：https://github.com/GNOME/libxml2/tree/v2.9.12
- SRT 1.5.4
  - 许可证：MPL-2.0
  - 原文：`external/srt/LICENSE`
  - 源码：https://github.com/Haivision/srt/tree/v1.5.4
- libwebp 1.6.0
  - 许可证：BSD-3-Clause
  - 原文：`external/libwebp/COPYING`
  - 源码：https://github.com/webmproject/libwebp/tree/v1.6.0
- libaom 3.9.1
  - 许可证：BSD-2-Clause
  - 原文：`external/libaom/LICENSE`
  - 源码：https://aomedia.googlesource.com/aom/+/refs/tags/v3.9.1
- SVT-AV1 3.1.2
  - 许可证：BSD-3-Clause-Clear
  - 原文：`external/svt-av1/LICENSE.md`
  - 源码：https://gitlab.com/AOMediaCodec/SVT-AV1/-/tree/v3.1.2
- zimg 3.0.6
  - 许可证：WTFPL-2.0
  - 原文：`external/zimg/COPYING`
  - 源码：https://github.com/sekrit-twc/zimg/tree/release-3.0.6

## 系统提供的接口

构建参数还启用了 Vulkan、CUDA、CUVID 和 NVENC 接口。portable 包不携带显卡驱动或对应的系统
运行库；这些能力仅在用户系统已经提供兼容运行环境时可用。nv-codec-headers 属于构建时使用的
接口头文件，已在上表列出。
