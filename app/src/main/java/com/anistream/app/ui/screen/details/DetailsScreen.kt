package com.anistream.app.ui.screen.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.ui.component.EpisodeRow
import com.anistream.app.ui.component.ErrorState
import com.anistream.app.ui.component.InfoRow
import com.anistream.app.ui.component.LoadingState
import com.anistream.app.ui.component.MetaPill
import com.anistream.app.ui.component.PosterImage
import com.anistream.app.ui.component.ScreenHeader
import com.anistream.app.ui.theme.WarningAmber

@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onViewAllEpisodes: () -> Unit
) {
    val detail = state.detail
    when {
        state.loading && detail == null -> Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Anime", onBack = onBack)
            LoadingState(message = "Loading anime details…")
        }

        state.error != null && detail == null -> Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Anime", onBack = onBack)
            ErrorState(message = state.error, onRetry = onRetry)
        }

        detail != null -> DetailsContent(
            state = state,
            detail = detail,
            onBack = onBack,
            onToggleWatchlist = onToggleWatchlist,
            onPlayEpisode = onPlayEpisode,
            onViewAllEpisodes = onViewAllEpisodes
        )
    }
}

@Composable
private fun DetailsContent(
    state: DetailsUiState,
    detail: AnimeDetail,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onViewAllEpisodes: () -> Unit
) {
    var synopsisExpanded by rememberSaveable(detail.slug) { mutableStateOf(false) }
    val resumeEntry = state.resumeEntry
    val resumeEpisode = resumeEntry?.let { entry ->
        detail.episodes.firstOrNull { it.number == entry.episodeNumber }
    }
    val primaryEpisode = resumeEpisode ?: detail.episodes.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(392.dp)
            ) {
                PosterImage(
                    url = detail.coverUrl,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.35f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    detail.japaneseTitle?.let { japanese ->
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = japanese,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        detail.score?.let { score ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = score,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        detail.status?.let { status ->
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        detail.type?.let { type ->
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }

        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { primaryEpisode?.let(onPlayEpisode) },
                    enabled = primaryEpisode != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (resumeEpisode != null) {
                            "Resume episode ${resumeEpisode.number}"
                        } else {
                            "Watch now"
                        }
                    )
                }
                Spacer(Modifier.width(10.dp))
                FilledTonalIconButton(
                    onClick = onToggleWatchlist,
                    enabled = !state.watchlistBusy
                ) {
                    Icon(
                        imageVector = if (state.isInWatchlist) {
                            Icons.Filled.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        contentDescription = if (state.isInWatchlist) {
                            "Remove from watchlist"
                        } else {
                            "Add to watchlist"
                        }
                    )
                }
            }
        }

        item(key = "meta") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${detail.episodes.size} episode${if (detail.episodes.size == 1) "" else "s"} available",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                detail.synopsis?.let { synopsis ->
                    Spacer(Modifier.height(18.dp))
                    Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = { synopsisExpanded = !synopsisExpanded },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Text(if (synopsisExpanded) "Show less" else "Read more")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        detail.type?.let { InfoRow("Type", it) }
                        detail.status?.let { InfoRow("Status", it) }
                        detail.totalEpisodes?.let { InfoRow("Total episodes", it) }
                        detail.duration?.let { InfoRow("Duration", it) }
                        detail.studio?.let { InfoRow("Studio", it) }
                        detail.releaseDate?.let { InfoRow("Released", it) }
                        detail.producers?.let { InfoRow("Producers", it) }
                        if (
                            detail.type == null && detail.status == null && detail.totalEpisodes == null &&
                            detail.duration == null && detail.studio == null && detail.releaseDate == null &&
                            detail.producers == null
                        ) {
                            Text(
                                text = "The catalogue did not provide additional metadata for this title.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (detail.genres.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Genres", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(detail.genres, key = { it.slug.ifBlank { it.name } }) { genre ->
                            MetaPill(text = genre.name)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Episodes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (detail.episodes.isNotEmpty()) {
                        TextButton(onClick = onViewAllEpisodes) {
                            Text("View all")
                        }
                    }
                }

                if (detail.episodes.isEmpty()) {
                    Text(
                        text = "No episodes are listed for this title yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    detail.episodes.take(12).forEach { episode ->
                        val history = state.history[episode.number]
                        EpisodeRow(
                            episode = episode,
                            watched = history?.completed == true,
                            progress = history?.progressFraction ?: 0f,
                            onClick = { onPlayEpisode(episode) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Metadata and episode identifiers come from the catalogue API. " +
                        "AniStream only plays direct, compatible sources and never bypasses embed providers.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
