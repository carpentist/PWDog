package com.pwdog.desktop.autofill

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pwdog.core.account.VaultSession
import com.pwdog.core.entry.EntryRepository
import com.pwdog.core.entry.HostMatcher
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class MatchRequest(val host: String? = null)

data class FillRequest(
    val host: String? = null,
    val entryId: String? = null,
)

data class EntrySummary(
    val id: String,
    val title: String,
    val username: String,
    val url: String,
)

data class MatchResponse(
    val host: String,
    val entries: List<EntrySummary>,
)

data class FillResponse(
    val username: String,
    val password: String,
)

data class HealthResponse(
    val status: String = "ok",
    val locked: Boolean,
    val account: String? = null,
)

data class ErrorResponse(val error: String)

class AutofillServer(
    private val port: Int,
    private val token: String,
    private val sessionProvider: () -> VaultSession?,
) : AutoCloseable {
    private val gson = Gson()
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null

    fun start() {
        check(server == null) { "autofill server already started" }

        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
        httpServer.createContext("/health", handler { exchange -> handleHealth(exchange) })
        httpServer.createContext("/match", handler { exchange -> handleMatch(exchange) })
        httpServer.createContext("/fill", handler { exchange -> handleFill(exchange) })

        val workerPool = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "pwdog-autofill").apply { isDaemon = true }
        }
        httpServer.executor = workerPool
        httpServer.start()

        executor = workerPool
        server = httpServer
    }

    override fun close() {
        server?.stop(0)
        server = null
        executor?.shutdownNow()
        executor = null
    }

    private fun handleHealth(exchange: HttpExchange) {
        if (!prepare(exchange)) return
        if (exchange.requestMethod != "GET") {
            send(exchange, 405, ErrorResponse("method-not-allowed"))
            return
        }
        val session = sessionProvider()
        send(
            exchange,
            200,
            HealthResponse(
                locked = session == null,
                account = session?.account?.displayName,
            ),
        )
    }

    private fun handleMatch(exchange: HttpExchange) {
        if (!prepare(exchange)) return
        if (exchange.requestMethod != "POST") {
            send(exchange, 405, ErrorResponse("method-not-allowed"))
            return
        }

        val request = readJson(exchange, MatchRequest::class.java) ?: return
        val host = HostMatcher.normalizeHost(request.host)
        if (host == null) {
            send(exchange, 400, ErrorResponse("invalid-host"))
            return
        }

        val session = sessionProvider()
        if (session == null) {
            send(exchange, 423, ErrorResponse("locked"))
            return
        }

        val matches = EntryRepository(session.connection)
            .findByHost(host)
            .map {
                EntrySummary(
                    id = it.id,
                    title = it.title,
                    username = it.username,
                    url = it.url,
                )
            }

        send(exchange, 200, MatchResponse(host = host, entries = matches))
    }

    private fun handleFill(exchange: HttpExchange) {
        if (!prepare(exchange)) return
        if (exchange.requestMethod != "POST") {
            send(exchange, 405, ErrorResponse("method-not-allowed"))
            return
        }

        val request = readJson(exchange, FillRequest::class.java) ?: return
        val host = HostMatcher.normalizeHost(request.host)
        val entryId = request.entryId?.trim().orEmpty()
        if (host == null || entryId.isEmpty()) {
            send(exchange, 400, ErrorResponse("invalid-request"))
            return
        }

        val session = sessionProvider()
        if (session == null) {
            send(exchange, 423, ErrorResponse("locked"))
            return
        }

        val entry = EntryRepository(session.connection).get(entryId)
        if (entry == null) {
            send(exchange, 404, ErrorResponse("entry-not-found"))
            return
        }
        if (!HostMatcher.matches(entry.url, host)) {
            send(exchange, 403, ErrorResponse("host-mismatch"))
            return
        }

        send(exchange, 200, FillResponse(username = entry.username, password = entry.password))
    }

    private fun prepare(exchange: HttpExchange): Boolean {
        val origin = exchange.requestHeaders.getFirst("Origin")
        if (origin != null && !origin.startsWith("chrome-extension://")) {
            send(exchange, 403, ErrorResponse("forbidden-origin"))
            return false
        }

        exchange.responseHeaders.add("Access-Control-Allow-Origin", origin ?: "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Authorization, Content-Type")
        exchange.responseHeaders.add("Access-Control-Max-Age", "600")
        exchange.responseHeaders.add("Cache-Control", "no-store")
        exchange.responseHeaders.add("X-Content-Type-Options", "nosniff")

        if (exchange.requestMethod == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            return false
        }

        if (!isAuthorized(exchange)) {
            send(exchange, 401, ErrorResponse("unauthorized"))
            return false
        }

        return true
    }

    private fun isAuthorized(exchange: HttpExchange): Boolean {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return false
        return MessageDigest.isEqual(
            header.toByteArray(StandardCharsets.UTF_8),
            "Bearer $token".toByteArray(StandardCharsets.UTF_8),
        )
    }

    private fun <T : Any> readJson(exchange: HttpExchange, type: Class<T>): T? {
        val bytes = exchange.requestBody.readNBytes(MAX_BODY_BYTES + 1)
        if (bytes.isEmpty() || bytes.size > MAX_BODY_BYTES) {
            send(exchange, 400, ErrorResponse("invalid-request"))
            return null
        }

        return try {
            gson.fromJson(String(bytes, StandardCharsets.UTF_8), type)
                ?: run {
                    send(exchange, 400, ErrorResponse("invalid-request"))
                    null
                }
        } catch (_: JsonSyntaxException) {
            send(exchange, 400, ErrorResponse("invalid-request"))
            null
        }
    }

    private fun send(exchange: HttpExchange, status: Int, payload: Any) {
        val bytes = gson.toJson(payload).toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun handler(block: (HttpExchange) -> Unit): HttpHandler = HttpHandler { exchange ->
        try {
            block(exchange)
        } catch (_: Throwable) {
            runCatching { send(exchange, 500, ErrorResponse("internal-error")) }
        } finally {
            exchange.close()
        }
    }

    private companion object {
        const val MAX_BODY_BYTES = 4096
    }
}
