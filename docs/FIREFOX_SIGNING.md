# Firefox 扩展持久安装

Firefox 正式版只允许**已由 Mozilla 签名**的扩展永久安装。`about:debugging` 的临时载入方式在浏览器重启后会消失，这是 Firefox 的限制。

需要做的只有一件事：把扩展包交给 Mozilla 签名，得到一个 `.xpi`，安装一次即可长期保留。

## 最简单的方式：在 Mozilla 网页上签名

不需要命令行、不需要 API key、不需要 GitHub 配置。

### 1. 下载 Firefox 扩展包

从 Release 下载：

```text
PWDog-Firefox-Extension-0.3.0.zip
```

不要解压。这个 ZIP 里已经是 Firefox 版 `manifest.json`。

### 2. 登录 Mozilla Add-ons 开发者后台

打开：

```text
https://addons.mozilla.org/developers/
```

使用 Mozilla 账号登录；没有账号就免费注册一个。

### 3. 提交新扩展

1. 点击 **Submit a New Add-on**。
2. 选择 **On your own site**（自行分发 / self-distribution）。这个方式不需要公开上架，也不需要人工审核。
3. 上传刚才下载的 `PWDog-Firefox-Extension-0.3.0.zip`。
4. 按页面提示继续。代码是未压缩的普通 JS，如果询问是否使用构建工具，选择“否 / 没有使用构建工具”；如果要求上传源码，上传同一个 ZIP 即可。
5. 等待 Mozilla 完成自动验证并签名。

### 4. 下载签名后的 XPI

签名完成后，在开发者后台的扩展页面中下载 signed `.xpi` 文件。文件名通常类似：

```text
pwdog@carpentist.github.io-0.3.0-an+fx.xpi
```

### 5. 安装

1. Firefox 打开 `about:addons`。
2. 点击右上角齿轮 → **从文件安装附加组件**。
3. 选择刚下载的签名 `.xpi`。

安装后扩展会像普通扩展一样保留，重启 Firefox 不需要再临时载入。

## 更新方式

- **自行分发的签名 XPI**：可以永久安装，但不会自动更新。发布新版本后，重新下载新的签名 XPI 并覆盖安装即可。
- **AMO 正式上架**：如果希望 Firefox 自动更新，在第 3 步选择 **On this site**，提交公开上架审核。审核通过后用户可以直接从 AMO 安装并自动更新。

## 可选：自动化签名

仓库还提供了 `Sign Firefox Extension` 工作流，适合之后重复发布版本时自动签名。它需要：

1. 在 Mozilla Add-ons 的 [API keys](https://addons.mozilla.org/developers/addon/api/key/) 页面生成 JWT issuer / secret。
2. 在 GitHub 仓库 Settings → Secrets and variables → Actions 中添加：
   - `AMO_JWT_ISSUER`
   - `AMO_JWT_SECRET`
3. 运行 Actions → Sign Firefox Extension，tag 填当前 Release 标签，例如 `v0.3.0`。

工作流会生成签名 XPI 并附加到 Release。

如果只是想自己用，推荐直接用上面的网页签名方式，不需要配置 API key。

## 注意

- 不要把 API key、Mozilla 密码贴到聊天、Issue 或代码中。
- 签名 XPI 的版本号必须递增；发布新扩展前同步更新 `extension/manifest.firefox.json` 中的 `version`。
- 签名版 manifest 使用 `data_collection_permissions`，要求 Firefox 140 或更高版本；未签名的开发包仍可按 README 的临时方式在 Firefox 109+ 加载。
