package com.notes.client.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    val title: String
}

@Serializable
object NoteListRoute : AppRoute {
    override val title: String = "All Notes"
}

@Serializable
data class NoteDetailRoute(val noteId: String? = null) : AppRoute {
    override val title: String = "Text Editor"
}

@Serializable
data class CanvasRoute(val noteId: String? = null) : AppRoute {
    override val title: String = "Canvas"
}

@Serializable
object SettingsRoute : AppRoute {
    override val title: String = "Settings"
}
