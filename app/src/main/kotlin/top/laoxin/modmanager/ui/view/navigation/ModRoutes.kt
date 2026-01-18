package top.laoxin.modmanager.ui.view.navigation

import kotlinx.serialization.Serializable

sealed interface ModRoute

@Serializable data object ModListRoute : ModRoute

@Serializable data class ModBrowserRoute(val path: String? = null) : ModRoute
