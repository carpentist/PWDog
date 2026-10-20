# Firefox 扩展持久安装

Firefox 正式版只允许**已由 Mozilla 签名**的扩展永久安装。`about:debugging` 的“临时载入附加组件”在浏览器重启后会消失，这是 Firefox 的平台限制，不是 PWDog 的配置问题。

要让扩展安装一次后长期保留，需要生成一个签名 XPI。签名版 manifest 使用 Firefox 140+ 的 `data_collection_permissions` 字段，因此签名 XPI 需要 Firefox 140 或更高版本；未签名的开发包仍可按 README 的临时方式在 Firefox 109+ 加载。

## 一次性配置

1. 登录 [Mozilla Add-ons](https://addons.mozilla.org/)。
2. 打开 [Developer Hub → API keys](https://addons.mozilla.org/developers/addon/api/key/)。
3. 生成 JWT issuer 和 JWT secret。
4. 在 GitHub 仓库 `carpentist/PWDog` → Settings → Secrets and variables → Actions 中添加：
   - `AMO_JWT_ISSUER`
   - `AMO_JWT_SECRET`
5. 打开 Actions → **Sign Firefox Extension** → Run workflow，填写要附加的 Release 标签，例如 `v0.3.0`。

工作流会：

- 组装 `extension/manifest.firefox.json`；
- 通过 `web-ext sign --channel unlisted` 请求 Mozilla 签名；
- 将 `PWDog-Firefox-Extension-<version>.xpi` 和 SHA-256 校验文件上传到对应 Release。

不要把 AMO API 密钥贴到 Issue、聊天或代码中，只放在 GitHub Actions Secrets 里。

## 安装签名 XPI

1. 从 Release 下载 `PWDog-Firefox-Extension-<version>.xpi`。
2. Firefox 打开 `about:addons`。
3. 点击右上角齿轮 → **从文件安装附加组件**。
4. 选择下载的 `.xpi`。

安装后扩展会像普通扩展一样写入 Firefox 配置，重启浏览器后仍然保留，不再需要 `about:debugging`。

## 更新方式

- **Unlisted（自签名）XPI**：持久安装，但不会自动更新。发布新版本后重新下载并安装新的 XPI 即可覆盖旧版。
- **AMO Listed（推荐给长期使用者）**：提交到 AMO 审核并通过后，可以从 AMO 安装，Firefox 会自动更新。当前仓库尚未上架 AMO。

## 本地签名

如果不想使用 GitHub Actions，可以在本机执行：

```powershell
New-Item -ItemType Directory -Force build/ext-firefox | Out-Null
Copy-Item extension\* build/ext-firefox -Recurse -Force -Exclude manifest.json
Copy-Item extension\manifest.firefox.json build/ext-firefox\manifest.json -Force
Remove-Item build/ext-firefox\manifest.firefox.json -Force -ErrorAction SilentlyContinue

npx --yes web-ext sign `
  --source-dir build/ext-firefox `
  --api-key $env:AMO_JWT_ISSUER `
  --api-secret $env:AMO_JWT_SECRET `
  --channel unlisted
```

`build/ext-firefox` 需要是 Firefox 版本：把 `extension/manifest.firefox.json` 复制为 `manifest.json`，并保持其他扩展文件不变。

签名后的 `.xpi` 可以直接通过 `about:addons` 安装。

## 注意事项

- 不要使用 `xpinstall.signatures.required=false` 绕过签名；这仅适合临时开发环境，不是正式使用方案。
- 签名 XPI 的版本号必须递增；发布新扩展前同步更新 `extension/manifest.firefox.json` 中的 `version`。
- 签名请求会把扩展包发送给 Mozilla Add-ons 进行签名。扩展不包含遥测，也不会连接远程服务器，但请知悉这一流程。
