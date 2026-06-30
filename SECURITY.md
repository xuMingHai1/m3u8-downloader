# Security Policy

## Supported Versions

当前仅维护最新发布版本和主分支上的安全问题。

| Version | Supported |
| --- | --- |
| latest | Yes |
| older releases | No |

## Reporting a Vulnerability

请不要通过公开 issue 披露安全漏洞。

推荐方式：

1. 优先使用 GitHub Security Advisories 私下报告。
2. 如果不可用，请通过项目主页中的维护者联系方式私下反馈。

报告时请尽量包含：

- 影响版本
- 复现步骤
- 影响范围
- 相关日志或截图
- 是否已经公开传播

## Scope

以下问题属于安全敏感范围：

- 任意文件写入或路径穿越
- 下载链接、代理凭据或 AES 密钥泄露
- 恶意 M3U8 内容导致异常执行或资源耗尽
- 自动合并、解密或文件处理过程中的安全问题

以下问题通常不作为安全漏洞处理：

- 普通下载失败
- UI 显示问题
- 非敏感日志格式问题
- 本地环境配置错误
