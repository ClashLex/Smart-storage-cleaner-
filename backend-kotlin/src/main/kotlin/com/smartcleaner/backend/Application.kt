package com.smartcleaner.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

data class ScanRequest(val categories: List<StorageCategory> = emptyList())

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 3001
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {
            serializeNulls()
            disableHtmlEscaping()
        }
    }
    routing {
        get("/health") {
            call.respond(mapOf("status" to "OK", "service" to "smart-storage-cleaner-kotlin", "migrationStage" to "core-analysis"))
        }
        post("/api/scan/analyze") {
            val request = runCatching { call.receive<ScanRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid scan request body."))
                return@post
            }
            val analysis = runCatching { StorageAnalyzer.analyze(request.categories) }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (it.message ?: "Invalid scan data.")))
                return@post
            }
            call.respond(mapOf(
                "totalSizeBytes" to analysis.totalSizeBytes,
                "totalSizeFormatted" to analysis.totalSizeFormatted,
                "recommendations" to analysis.recommendations,
            ))
        }
    }
}
