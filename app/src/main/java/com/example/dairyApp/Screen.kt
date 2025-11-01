package com.example.dairyApp

sealed class Screen(val route: String) {
    object PhotoCaption : Screen("photo_caption?eventId={eventId}&diaryPageName={diaryPageName}&entryId={entryId}") {
        fun createRoute(eventId: String? = null, diaryPageName: String? = null, entryId: String? = null): String {
            val parts = mutableListOf<String>()
            eventId?.let { parts.add("eventId=$it") }
            diaryPageName?.let { parts.add("diaryPageName=${it}") }
            entryId?.let { parts.add("entryId=$it") }
            return if (parts.isEmpty()) {
                "photo_caption"
            } else {
                "photo_caption?" + parts.joinToString("&")
            }
        }
        const val eventIdArg = "eventId" // optional
        const val diaryPageNameArg = "diaryPageName" // optional: when provided we add entry directly to this page
        const val entryIdArg = "entryId" // optional, for editing existing entry
    }

    object DiaryHome : Screen("diary_home") // Consider if this is still needed

    object CreateEvent : Screen("create_event") // Renamed from CreateSupergroup
    object EventList : Screen("event_list")     // Renamed from SupergroupList

    // Renamed GroupList to DiaryPageList, and updated arguments/route segments
    object DiaryPageList : Screen("event_detail/{eventId}/diary_pages") {
        fun createRoute(eventId: String) = "event_detail/$eventId/diary_pages"
        const val eventIdArg = "eventId"
    }

    // Renamed EntryList to DiaryEntryList, and updated arguments/route segments
    object DiaryEntryList : Screen("event_detail/{eventId}/diary_page/{diaryPageName}/entries") {
        fun createRoute(eventId: String, diaryPageName: String) = "event_detail/$eventId/diary_page/$diaryPageName/entries"
        const val eventIdArg = "eventId"
        const val diaryPageNameArg = "diaryPageName" // Renamed from groupNameArg
    }

    // This might represent an old concept or a specific detail view. Review if still needed.
    object TripDetail : Screen("trip_detail/{tripId}") { 
        fun createRoute(tripId: String) = "trip_detail/$tripId"
        // If DiaryTrip is now Event, this tripId might be eventId.
        const val tripIdArg = "tripId" // or eventIdArg depending on its new meaning
    }
}
