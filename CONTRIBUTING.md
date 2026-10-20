# Contributing to PWDog

感谢你愿意参与 PWDog。这个项目是 Windows 优先的本地密码管理器，最重要的约束是：**数据必须留在本机、默认安全、失败时不能悄悄丢数据**。

## Before you start

- 对于较大的改动，请先开 Issue 说明目标与方案，避免做完才发现方向不一致。
- 安全问题请不要公开提 Issue，按 [SECURITY.md](SECURITY.md) 私下报告。
- 不要在仓库、Issue、截图或测试数据中使用真实密码、恢复密钥或配对令牌。

## Development setup

要求：

- Windows 10 / 11
- JDK 21
- 项目自带 Gradle Wrapper

```powershell
.\gradlew.bat :shared:jvmTest :desktopApp:test
.\gradlew.bat :desktopApp:run
```

建议使用 IntelliJ IDEA 打开仓库并运行 `com.pwdog.desktop.MainKt`。

### Use an isolated data directory

开发时不要把数据写进真实的 `%LOCALAPPDATA%\PWDogData`：

```powershell
$env:PWDOG_DATA_DIR="$PWD\.devdata"
.\gradlew.bat :desktopApp:seedDev
.\gradlew.bat :desktopApp:run
```

种子账户主密码为 `DevPassword123`，仅用于本地开发。`.devdata/` 已被 Git 忽略。

### Packaging

```powershell
.\gradlew.bat :desktopApp:packageMsiWithCleanup
```

该任务会构建按用户安装的 MSI，并应用自定义 WiX 模板：卸载时清理当前 Windows 用户的数据目录，升级时保留数据。

## What to test

提交 UI 相关改动前，请实际运行应用，至少检查：

- 创建账户 / 解锁 / 锁定
- 新增、同站新增、编辑、删除条目
- 搜索与条目分组
- CSV 导入预览与确认
- 设置中的主题、强调色、背景
- 深色与浅色模式
- 窄窗口布局
- 弹层可用 Esc 关闭

涉及加密、账户或数据库的改动必须补充或更新 `shared/src/jvmTest` 下的测试。

## Code style

- Kotlin 官方风格，4 空格缩进。
- 不在 UI 中输出密码或密钥。
- 不引入云服务、遥测或后台网络请求。
- 新依赖需要说明用途，并优先选择维护活跃、体积可控的库。
- 用户可见文案使用简体中文；代码标识符使用英文。
- 避免无意义的注释；只在约束不明显或原因非显然时写简短注释。

## Pull requests

- 保持 PR 聚焦，避免夹带无关重构。
- 描述中说明：改了什么、为什么、如何验证。
- UI 改动请附截图。
- 如果改变了数据格式、密钥协议、安装/卸载行为，请同时更新 README、CHANGELOG 和相关文档。
- 不要在 PR 中提交 `build/`、`.devdata/`、MSI 或任何个人数据。

## Commit messages

推荐使用简短的祈使句，例如：

```text
Add account-scoped deletion
Fix light title bar not updating
Document uninstall data cleanup
```

## License

提交贡献即表示你同意以 [MIT License](LICENSE) 授权你的贡献。
