# PWDog 0.2.0

PWDog 的第一个公开版本：一个安静、克制的 Windows 本地密码库。

## 主要更新

- **Quiet Vault 界面**：重新设计的登录页、密码库分栏布局、详情面板和右侧编辑弹层。
- **本地多账户**：每个账户拥有独立主密码、恢复密钥和加密数据库。
- **同站多账户**：同一网站下的账号自动归组。
- **现代密钥体系**：Argon2id + 随机 DEK + AES-256-GCM 密钥包装 + SQLCipher。
- **Windows Hello 快速解锁**：由 DPAPI 保护，绑定当前 Windows 用户。
- **浏览器自动填充**：Chrome / Edge MV3 扩展，按需返回匹配条目，不自动提交。
- **CSV 导入**：支持 Chrome / Edge、Firefox、Bitwarden 与通用 CSV，带预览与去重。
- **外观**：深色 / 浅色 / 跟随系统、五种强调色、三种背景、头像上传。
- **桌面细节**：托盘常驻、开机自启、快捷键、15 秒剪贴板清理。
- **应用图标**：沿用 Quiet Vault 的渐变锁形图标。

## 安装

1. 下载 `PWDog-0.2.0.msi`。
2. 使用 `SHA256SUMS.txt` 校验文件。
3. 双击安装。按当前 Windows 用户安装，默认路径 `%LOCALAPPDATA%\PWDog`，无需管理员权限。

当前安装包未做代码签名，Windows SmartScreen 可能显示“未知发布者”。请确认文件来自本仓库 Release。

## 升级

从 0.1.0 升级到 0.2.0 会保留已有账户和密码库。升级过程中不会触发数据清理。

## 卸载

从 0.2.0 开始，卸载会删除当前 Windows 用户下的：

```text
%LOCALAPPDATA%\PWDogData
```

包括所有本地账户、加密密码库、头像、设置和扩展配对令牌，同时移除开机自启项。

**卸载前请先退出 PWDog，并备份 `PWDogData`。** 该操作不可恢复。

## 发布资产

- `PWDog-0.2.0.msi` — Windows 10/11 x64 安装包
- `PWDog-Extension-0.2.0.zip` — Chrome / Edge 扩展（开发者模式加载）
- `SHA256SUMS.txt` — SHA-256 校验值

## 已知限制

- 尚未经过独立安全审计。
- 安装包未签名，暂无自动更新。
- 目前仅支持 Windows；Android 在规划中。
- 浏览器扩展未上架扩展商店。
- JVM 无法保证内存中的密钥被彻底擦除。

## 完整变更

见 [CHANGELOG.md](https://github.com/carpentist/PWDog/blob/main/CHANGELOG.md)。

## 许可证

MIT
