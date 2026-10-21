# PWDog 0.3.0

新增 Firefox 扩展支持，并提供已签名的 Firefox XPI。

## 扩展

- **Chrome / Edge / Firefox 共用一套扩展代码**。
- 新增 `browser-api.js`，在 Chrome 使用回调 API，在 Firefox 使用 Promise API。
- 安装版会同时解压两套扩展目录：
  - `%LOCALAPPDATA%\PWDogData\extension\chromium`
  - `%LOCALAPPDATA%\PWDogData\extension\firefox`
- 扩展新增图标、`options_ui`，版本统一为 0.3.0。

## Firefox 持久安装

普通 Firefox 只允许 Mozilla 签名的扩展永久安装。本 Release 附带了已签名的 XPI：

1. 下载 `PWDog-Firefox-Extension-0.3.0.xpi`。
2. Firefox 打开 `about:addons`。
3. 右上角齿轮 → **从文件安装附加组件**。
4. 选择下载的 XPI。

安装后扩展会跨浏览器重启保留，不再需要 `about:debugging` 临时载入。要求 Firefox 140 或更高版本。

这是 AMO 自行分发（unlisted）签名：可以永久安装，但**不会自动更新**。发布新版本时，重新安装签名 XPI 即可；扩展的配对信息会保留。

## Chrome / Edge

在扩展管理页启用开发者模式，选择“加载已解压的扩展程序”，指向 `chromium` 文件夹。Chrome / Edge 的已解压扩展会跨重启保留。

## 安装包

- `PWDog-0.3.0.msi` — 按所有用户安装，需要管理员权限，可选择 E: 等盘符。
- `PWDog-0.3.0-portable.zip` — 解压到任意目录运行，不经过 Windows Installer。
- `PWDog-Extension-0.3.0.zip` — Chromium 扩展。
- `PWDog-Firefox-Extension-0.3.0.xpi` — Firefox 签名扩展。

数据仍保存在 `%LOCALAPPDATA%\PWDogData`，按 Windows 用户隔离。卸载/删除数据前请先备份。

扩展不包含遥测，不连接远程服务器；配对令牌只保存在本机，PWDog 锁定时不会返回密码。

## 升级

从 0.2.2 升级会保留数据。若旧版是按当前用户安装的 0.2.1，请先备份并卸载旧版，再安装 0.3.0。

完整变更见 [CHANGELOG.md](https://github.com/carpentist/PWDog/blob/main/CHANGELOG.md)。
