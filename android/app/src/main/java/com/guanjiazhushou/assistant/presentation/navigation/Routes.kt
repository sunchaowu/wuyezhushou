package com.guanjiazhushou.assistant.presentation.navigation

import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.StatisticsScope

object Routes {
    const val BUILDING_GRAPH = "building_graph"
    const val BUILDING_LIST = "building_list"
    const val BUILDING_FORM = "building_form"
    const val BUILDING_FORM_EDIT = "building_form/{buildingId}"
    const val BUILDING_GRID = "building_grid/{buildingId}?roomId={roomId}"
    const val ROOM_COMMUNICATION = "room_communication/{roomId}"
    const val SEARCH = "search"

    const val STATISTICS_GRAPH = "statistics_graph"
    const val STATISTICS = "statistics"
    const val ROOM_LIST = "room_list/{filterType}/{scopeType}"

    const val PROFILE_GRAPH = "profile_graph"
    const val PROFILE = "profile"
    const val THEME = "theme"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val GROUP = "group"
    const val ABOUT = "about"
    const val VERSION_DETAILS = "version_details"
    const val LEGAL_DOCUMENT = "legal/{documentType}"
    const val EVENT_LIST = "event_list"

    fun legalDocument(documentType: LegalDocumentType) = "legal/${documentType.name}"

    fun buildingFormEdit(buildingId: Long) = "building_form/$buildingId"
    fun buildingGrid(buildingId: Long, roomId: Long = NO_ROOM_HIGHLIGHT) =
        "building_grid/$buildingId?roomId=$roomId"

    const val NO_ROOM_HIGHLIGHT = -1L
    fun roomCommunication(roomId: Long) = "room_communication/$roomId"
    fun roomList(filter: RoomListFilter, scope: StatisticsScope) =
        "room_list/${filter.name}/${scope.name}"
}

enum class BottomTab(
    val route: String,
    val label: String,
) {
    BUILDING(Routes.BUILDING_GRAPH, "楼栋"),
    STATISTICS(Routes.STATISTICS_GRAPH, "统计"),
    PROFILE(Routes.PROFILE_GRAPH, "我的"),
}
