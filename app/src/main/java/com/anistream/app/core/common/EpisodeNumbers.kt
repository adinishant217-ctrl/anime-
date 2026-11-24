package com.anistream.app.core.common

/**
 * Episode numbers are not a separate API field: the catalogue embeds them in
 * titles such as "Gensou Suikoden Episode 8 Subtitle Indonesia".
 */
object EpisodeNumbers {

    private val episodePattern = Regex("""(?:episode|eps?\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val numberPattern = Regex("""\d+""")
    private val subtitlePattern = Regex("""\s*subtitle\s+indonesia\s*""", RegexOption.IGNORE_CASE)
    private val whitespacePattern = Regex("""\s+""")

    fun extract(title: String): Int {
        episodePattern.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        return numberPattern.findAll(title).lastOrNull()?.value?.toIntOrNull() ?: 0
    }

    /** Removes the "Episode N" and "Subtitle Indonesia" noise for compact UI rows. */
    fun cleanTitle(title: String, number: Int): String {
        var cleaned = title.replace(subtitlePattern, " ")
        if (number > 0) {
            cleaned = cleaned.replace(
                Regex("""\s*(?:episode|eps?\.?)\s*$number\b""", RegexOption.IGNORE_CASE),
                " "
            )
        }
        cleaned = cleaned.replace(whitespacePattern, " ").trim()
        return cleaned.ifBlank { title.trim() }
    }
}
