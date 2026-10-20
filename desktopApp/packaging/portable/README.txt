PWDog 便携版
============

1. 将整个 PWDog 文件夹放到任意位置，例如 E:\PWDog。
2. 双击 PWDog.exe 运行，不需要安装，也不需要管理员权限。
3. 数据保存在当前 Windows 用户的：
   %LOCALAPPDATA%\PWDogData
   浏览器扩展在：设置 → 浏览器自动填充 → 打开扩展文件夹。
4. 卸载：
   - 退出 PWDog（托盘 → Exit）。
   - 删除 PWDog 文件夹。
   - 如需清除全部密码库和设置，再删除 %LOCALAPPDATA%\PWDogData。
5. 便携版不会创建 Windows Installer 卸载项，也不会使用 E:\Config.Msi。

注意：数据目录仍按 Windows 用户隔离；同一台电脑的不同 Windows 用户各自拥有独立的 PWDogData。
