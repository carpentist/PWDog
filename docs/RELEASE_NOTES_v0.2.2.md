# PWDog 0.2.2

支持安装到 E: 等非系统盘，并修复 Windows Installer 在 `Config.Msi` 上的权限错误。

## 安装包变化

- **MSI 改为按所有用户安装并提权**：安装和卸载都会请求管理员权限。
- 安装向导可以选择任意盘符，例如 `E:\PWDog`。
- 账户数据仍然按 Windows 用户隔离，保存在 `%LOCALAPPDATA%\PWDogData`。
- 新增 **便携版 ZIP**：解压到任意目录运行，不经过 Windows Installer，也不会使用 `E:\Config.Msi`。

## 为什么修改

0.2.0 / 0.2.1 的 MSI 按当前用户安装，如果之前用管理员权限安装到 `E:\PWDog`，可能出现用户级与机器级安装混杂，卸载时 Windows Installer 无法修改 `E:\Config.Msi` 中回滚文件的权限，弹出：

```text
Error 1926. Could not set file security for file 'E:\Config.Msi\*.rbf'. Error: 5.
```

0.2.2 的 MSI 统一按所有用户安装并提权，卸载时由管理员上下文处理；便携版则完全绕开 MSI。

## 安装

### MSI

1. 右键 `PWDog-0.2.2.msi`，以管理员身份运行。
2. 选择安装目录，例如 `E:\PWDog`。
3. 卸载：Windows 设置 → 应用 → 已安装的应用 → PWDog → 卸载（需要管理员权限）。

### 便携版

1. 解压 `PWDog-0.2.2-portable.zip` 到任意目录，例如 `E:\PWDog`。
2. 双击 `PWDog.exe` 运行。
3. 卸载：退出 PWDog，删除程序文件夹；如需清除全部数据，再删除 `%LOCALAPPDATA%\PWDogData`。

## 升级说明

- 如果已安装 0.2.1（按当前用户安装），请先备份 `%LOCALAPPDATA%\PWDogData`，从 Windows 设置卸载 0.2.1，再安装 0.2.2。
- 如果仍遇到 `Config.Msi` 权限错误，请使用便携版，或参考 `docs/TROUBLESHOOTING.md` 修复该目录权限。
- 0.2.1 的扩展文件夹修复已包含在 0.2.2 中：设置 → 浏览器自动填充 → 打开扩展文件夹。

## 发布资产

- `PWDog-0.2.2.msi`
- `PWDog-0.2.2-portable.zip`
- `PWDog-Extension-0.2.2.zip`
- `SHA256SUMS.txt`

安装包未签名，SmartScreen 可能提示未知发布者。完整变更见 [CHANGELOG.md](https://github.com/carpentist/PWDog/blob/main/CHANGELOG.md)。
