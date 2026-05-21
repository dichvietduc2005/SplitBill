package com.splitbill

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Welcome to Split Bill API!")
        }
    }
}
