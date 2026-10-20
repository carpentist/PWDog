# 故障排查

## 卸载时报 Error 1926，提示 `E:\Config.Msi\*.rbf` 权限不足

这是 **Windows Installer 的回滚文件权限问题**，不是 PWDog 的密码库文件损坏。**0.2.2 起 MSI 已改为按所有用户安装并提权；如果仍然出现，优先使用便携版 ZIP。**

典型日志：

```text
Error 1926. Could not set file security for file 'E:\Config.Msi\71f1417.rbf'. Error: 5.
Verify that you have sufficient privileges to modify the security permissions for this file.
```

`Config.Msi` 是 Windows Installer 在安装盘根目录创建的临时回滚目录。把 PWDog 安装到 `E:\PWDog` 时，卸载程序需要在 `E:\Config.Msi` 写入/修改回滚文件；如果当前进程没有该目录的权限，就会出现这个错误。多数情况下产品仍然会被移除，但 Windows Installer 会弹出错误提示。

处理方式，任选一种：

1. **用管理员权限运行卸载程序**
   - 在“设置 → 应用 → 已安装的应用”中卸载时确认 UAC。
   - 或打开管理员命令提示符，运行卸载字符串（产品代码以注册表为准）：
     ```text
     MsiExec.exe /X{3BE6B571-D284-3ADD-90C5-EB481430632F}
     ```

2. **修复 `E:\Config.Msi` 权限**（管理员命令提示符）
   ```text
   takeown /f E:\Config.Msi /r /d y
   icacls E:\Config.Msi /reset /t /c
   ```
   确认没有其他安装/卸载正在进行后，也可以删除该临时目录，Windows Installer 会重新创建：
   ```text
   rmdir /s /q E:\Config.Msi
   ```

3. **安装到默认的系统盘路径**
   PWDog 0.2.2 的 MSI 支持选择任意盘符；如果 E: 盘的 `Config.Msi` 权限仍然异常，安装到 C: 的默认路径可以绕开该目录。

4. **使用便携版 ZIP**
   解压到 `E:\PWDog` 后直接运行 `PWDog.exe`，完全不经过 Windows Installer 和 `E:\Config.Msi`。便携版卸载只需退出程序并删除程序文件夹；需要清除全部数据时再删除 `%LOCALAPPDATA%\PWDogData`。

如果卸载已经完成（应用和“应用和功能”条目都不见了），这个错误通常只是回滚文件操作失败。先备份数据目录，再安装新版本即可。

## 数据目录仍然存在

卸载清理失败、或者程序重新启动后，数据可能仍在：

```text
%LOCALAPPDATA%\PWDogData
```

这是正常的数据目录。重新安装 PWDog 后，它仍会读取这里的账户。需要彻底删除时，先退出 PWDog，再手动删除该目录。

## 浏览器扩展找不到文件夹 / Firefox 无法加载

0.2.1 起安装包内置扩展；0.3.0 起同时解压 Chromium 和 Firefox 两套：

- 打开 **设置 → 浏览器自动填充**，分别点击“打开 Chrome / Edge 扩展文件夹”或“打开 Firefox 扩展文件夹”。
- **Chrome / Edge**：加载 `%LOCALAPPDATA%\PWDogData\extension\chromium`。
- **Firefox 109+**：打开 `about:debugging#/runtime/this-firefox`，选择“临时载入附加组件”，选中 `%LOCALAPPDATA%\PWDogData\extension\firefox\manifest.json`。
- Firefox 未签名扩展在浏览器重启后需要重新临时载入。要长期使用，请通过 `Sign Firefox Extension` 工作流生成签名 XPI，安装一次后即可跨重启保留；详见 [docs/FIREFOX_SIGNING.md](FIREFOX_SIGNING.md)。
- Release 同时提供 `PWDog-Extension-<version>.zip`（Chromium）和 `PWDog-Firefox-Extension-<version>.zip`（Firefox）。

## 浏览器扩展连接失败

- 确认 PWDog 桌面端已解锁。
- 确认扩展设置中的接口地址是 `http://127.0.0.1:27831`，令牌与配对页一致。
- 如果提示端口 27831 被占用，关闭其他正在运行的 PWDog 实例后重新启动。
- 扩展只查询当前页面 host 严格匹配的条目；请确认条目的网址包含正确域名。

## Windows Hello 不可用

- 需要在 Windows 设置中先配置 PIN / 指纹 / 人脸。
- 快速解锁数据绑定当前 Windows 用户；换用户或重置系统凭据后需要重新启用。
- 主密码始终可以作为后备解锁方式。

## 数据备份

**先完全退出 PWDog**（包括托盘），再复制整个 `%LOCALAPPDATA%\PWDogData`。运行中复制可能得到不完整的 WAL 状态。
