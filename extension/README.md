# PWDog Browser Extension

Chrome / Edge / Firefox Manifest V3 扩展，用于从本地 PWDog 桌面端获取密码并填充当前页面。

## 安装

### Chrome / Edge

1. 打开 PWDog 设置 → 浏览器自动填充。
2. 点击“打开 Chrome / Edge 扩展文件夹”，应用会解压到 `%LOCALAPPDATA%\PWDogData\extension\chromium`。
3. 在 `chrome://extensions` 或 `edge://extensions` 启用“开发者模式”。
4. 选择“加载已解压的扩展程序”，选中 `chromium` 文件夹。
5. 打开扩展设置，填写桌面端提供的接口地址与配对令牌。

### Firefox 109+

1. 打开 PWDog 设置 → 浏览器自动填充。
2. 点击“打开 Firefox 扩展文件夹”，应用会解压到 `%LOCALAPPDATA%\PWDogData\extension\firefox`。
3. 打开 `about:debugging#/runtime/this-firefox`。
4. 选择“临时载入附加组件”，选中 `firefox` 文件夹中的 `manifest.json`。
5. 打开扩展设置，填写桌面端提供的接口地址与配对令牌。

Firefox 未签名扩展在浏览器重启后需要重新临时载入；后续可供 AMO 签名后永久安装。

从源码运行时，Chrome / Edge 使用本目录；Firefox 需要把 `manifest.firefox.json` 复制为 `manifest.json` 后再加载，或使用 Release 中的 `PWDog-Firefox-Extension-*.zip`。

配对信息在 PWDog 桌面端：**设置 → 浏览器自动填充**。

## 工作方式

- 仅在用户点击扩展时请求匹配条目。
- 只查询当前页面 host 严格匹配的条目。
- 填充用户名和密码，触发页面输入事件，但**不会自动提交表单**。
- 桌面端锁定时，本地接口返回 `423 locked`，扩展不会获得密码。
- 接口只监听 `127.0.0.1:27831`，并校验 Bearer 令牌与 Origin。

`browser-api.js` 同时兼容 Chrome 的回调 API 和 Firefox 的 Promise API。

## 权限

| 权限 | 用途 |
| --- | --- |
| `activeTab` | 仅在用户操作时访问当前标签页 |
| `scripting` | 注入填充脚本 |
| `storage` | 保存本地接口地址与配对令牌 |
| `http://127.0.0.1:27831/*` | 访问本机 PWDog 接口 |

扩展不包含遥测，不连接任何远程服务器。
