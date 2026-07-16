package com.splitbill.routes

import com.splitbill.service.StatsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.statsRoutes(statsService: StatsService) {

    route("/stats") {

        // GET /api/stats/me - Lấy thống kê chi tiêu cá nhân
        get("/me") {
            val userId = call.currentUserId()
            val stats = statsService.getUserStats(userId)
            call.respond(HttpStatusCode.OK, stats)
        }

        // GET /api/stats/group/{groupId} - Lấy thống kê chi tiêu của nhóm
        get("/group/{groupId}") {
            val userId = call.currentUserId()
            val groupId = call.parameters["groupId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Thiếu groupId")
            val stats = statsService.getGroupStats(groupId, userId)
            call.respond(HttpStatusCode.OK, stats)
        }
    }
}
