# PWDog 0.3.0

新增 Firefox 扩展支持。

## 扩展

- **Chrome / Edge / Firefox 共用一套扩展代码**。
- 新增 `browser-api.js`，在 Chrome 使用回调 API，在 Firefox 使用 Promise API。
- 安装版会同时解压两套扩展：
  - `%LOCALAPPDATA%\PWDogData\extension\chromium`
  - `%LOCALAPPDATA%\PWDogData\extension\firefox`
- 配对页分别提供“打开 Chrome / Edge 扩展文件夹”和“打开 Firefox 扩展文件夹”。
- 扩展新增图标、`options_ui`，版本统一为 0.3.0。

## Firefox 加载方式

1. PWDog → 设置 → 浏览器自动填充 → 打开 Firefox 扩展文件夹。
2. 打开 `about:debugging#/runtime/this-firefox`。
3. 选择“临时载入附加组件”，选中 `firefox` 文件夹中的 `manifest.json`。
4. 填入配对页显示的接口地址和令牌。

Firefox 扩展目前未签名，浏览器重启后需要重新临时载入。后续可供 AMO 签名后永久安装。

## Chrome / Edge

与之前相同：扩展管理页启用开发者模式，加载 `chromium` 文件夹。

## 安装包

- `PWDog-0.3.0.msi` — 按所有用户安装，需要管理员权限，可选择 E: 等盘符。
- `PWDog-0.3.0-portable.zip` — 解压到任意目录运行，不经过 Windows Installer。
- `PWDog-Extension-0.3.0.zip` — Chromium 扩展。
- `PWDog-Firefox-Extension-0.3.0.zip` — Firefox 扩展。

数据仍保存在 `%LOCALAPPDATA%\PWDogData`，按 Windows 用户隔离。卸载/删除数据前请先备份。

## 升级

从 0.2.2 升级会保留数据。若旧版是按当前用户安装的 0.2.1，请先备份并卸载旧版，再安装 0.3.0。

完整变更见 [CHANGELOG.md](https://github.com/carpentist/PWDog/blob/main/CHANGELOG.md)。
