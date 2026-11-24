package com.anistream.app.domain.model

enum class ThemeMode {
    MIDNIGHT,
    DARK,
    AMOLED
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.MIDNIGHT,
    val autoplayNext: Boolean = true,
    val preferHighestQuality: Boolean = true,
    val searchHistory: List<String> = emptyList()
)
