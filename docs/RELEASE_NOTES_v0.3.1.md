# PWDog 0.3.1

降低空闲内存占用，并加入单实例运行。

## 桌面端

- **单实例**：再次点击快捷方式时会唤醒已有窗口并置前，不再启动第二个 JVM；多开造成的重复内存占用被消除。
- **限制 JVM 常驻**：
  - 堆内存上限 256 MB，初始 16 MB
  - Metaspace 上限 128 MB
  - 代码缓存 64 MB
  - 直接内存 192 MB
  - 使用 Serial GC 降低 GC 线程和常驻开销
- **隐藏/最小化后回收**：关闭到托盘约 2.5 秒后、标题栏最小化约 1.5 秒后，会执行 GC 并回收工作集。

实测同一进程：

```text
启动后工作集约 250 MB
关闭到托盘后约 22 MB
最小化后约 12 MB
恢复窗口后约 55 MB
```

任务管理器中的“提交大小”仍可能保持在约 310 MB，这是 JVM/Skia 已提交但不活跃的内存；工作集回收后不再占用同样的物理内存。

## 扩展

扩展代码没有变化，继续使用 0.3.0 版本：

- `PWDog-Extension-0.3.0.zip` — Chrome / Edge
- `PWDog-Firefox-Extension-0.3.0.xpi` — Firefox 140+，AMO unlisted 签名

0.3.1 的本地接口、配对令牌和自动填充协议没有变化，已安装的扩展无需重新加载或重新配对。

## 安装包

- `PWDog-0.3.1.msi` — 按所有用户安装，需要管理员权限，可选择 E: 等盘符。
- `PWDog-0.3.1-portable.zip` — 解压到任意目录运行，不经过 Windows Installer。

升级会保留现有账户和密码库。卸载/删除数据前请先备份 `%LOCALAPPDATA%\PWDogData`。

## 验证

- `shared` 与 `desktopApp` 测试通过。
- 实测第二次启动只保留一个 JVM，并唤起已有窗口。
- 实测隐藏、最小化后的工作集回收。
- Firefox 签名 XPI 与 Chromium 扩展版本保持不变。

完整变更见 [CHANGELOG.md](https://github.com/carpentist/PWDog/blob/main/CHANGELOG.md)。
