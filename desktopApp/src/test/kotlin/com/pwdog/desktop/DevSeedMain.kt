package com.pwdog.desktop

import com.pwdog.core.account.AccountService
import com.pwdog.core.account.AppPaths
import com.pwdog.core.entry.EntryRepository
import com.pwdog.desktop.security.WindowsQuickUnlock

fun main() {
    val baseDir = AppPaths.defaultBaseDir()
    val password = "DevPassword123".toByteArray()

    try {
        AccountService(baseDir).use { service ->
            val existing = service.listAccounts().firstOrNull { it.displayName == "DevUser" }
            val session = if (existing == null) {
                service.createAccount("DevUser", password).session
            } else {
                service.unlock(existing.id, password)
            }

            session.use {
                val repository = EntryRepository(it.connection)
                if (repository.count() == 0) {
                    repository.create(
                        title = "GitHub · Work",
                        url = "https://github.com",
                        username = "dev@work.example",
                        password = "gh-work-secret",
                        notes = "工作账号，已开启双重验证。",
                    )
                    repository.create(
                        title = "GitHub · Personal",
                        url = "https://github.com",
                        username = "dev@example.com",
                        password = "gh-secret-1",
                        notes = "个人项目与开源仓库。",
                    )
                    repository.create(
                        title = "Figma",
                        url = "https://www.figma.com",
                        username = "designer@example.com",
                        password = "figma-demo-pass",
                        notes = "设计协作账号。",
                    )
                    repository.create(
                        title = "Proton Mail",
                        url = "https://account.proton.me",
                        username = "dev@proton.me",
                        password = "mail-demo-pass",
                        notes = "",
                    )
                    repository.create(
                        title = "Linear",
                        url = "https://linear.app",
                        username = "dev@work.example",
                        password = "linear-demo-pass",
                        notes = "工作区管理员。",
                    )
                    repository.create(
                        title = "Cloudflare",
                        url = "https://dash.cloudflare.com",
                        username = "ops@company.com",
                        password = "cf-demo-pass",
                        notes = "只用于公司域名。",
                    )
                    repository.create(
                        title = "公司 VPN",
                        url = "https://vpn.example.com",
                        username = "devuser",
                        password = "vpn-secret-3",
                        notes = "仅测试",
                    )
                }
                if (
                    System.getenv("PWDOG_SEED_QUICK_UNLOCK") == "1" &&
                    it.account.quickUnlockBlob == null
                ) {
                    service.enableQuickUnlock(it, WindowsQuickUnlock::protect)
                }
                println("seeded ${repository.count()} entries at $baseDir")
            }
        }
    } finally {
        password.fill(0)
    }
}
