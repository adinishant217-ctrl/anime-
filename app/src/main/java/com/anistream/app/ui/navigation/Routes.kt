package com.anistream.app.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val EXPLORE_BASE = "explore"
    const val EXPLORE = "explore?mode={mode}"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val DETAIL = "anime/{slug}"
    const val EPISODES = "anime/{slug}/episodes"
    const val PLAYER = "player/{animeSlug}/{episodeSlug}"

    fun explore(mode: String): String = "explore?mode=$mode"

    fun detail(slug: String): String = "anime/${Uri.encode(slug)}"

    fun episodes(slug: String): String = "anime/${Uri.encode(slug)}/episodes"

    fun player(animeSlug: String, episodeSlug: String): String =
        "player/${Uri.encode(animeSlug)}/${Uri.encode(episodeSlug)}"
}
