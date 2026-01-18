package top.laoxin.modmanager.ui.view.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Console : Route

    @Serializable data object ModList : Route

    @Serializable data object Settings : Route
}
