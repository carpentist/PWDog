package com.pwdog.core.account

import java.sql.Connection

internal object VaultSchema {
    fun create(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                create table if not exists entries (
                  id text primary key,
                  title text not null,
                  url text not null default '',
                  username text not null default '',
                  password text not null,
                  notes text not null default '',
                  created_at integer not null,
                  updated_at integer not null
                )
                """.trimIndent(),
            )
            statement.executeUpdate(
                "create index if not exists idx_entries_title on entries(title collate nocase)",
            )
            statement.executeUpdate(
                "create index if not exists idx_entries_url on entries(url collate nocase)",
            )
            statement.execute("PRAGMA user_version = 1")
        }
    }
}
