package com.anistream.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.ui.component.AnimePosterCard
import com.anistream.app.ui.component.ContinueWatchingCard
import com.anistream.app.ui.component.ErrorState
import com.anistream.app.ui.component.HomeSkeleton
import com.anistream.app.ui.component.PosterImage
import com.anistream.app.ui.component.SectionHeader
import com.anistream.app.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAnimeClick: (AnimeSummary) -> Unit,
    onContinueClick: (HistoryEntry) -> Unit,
    onBrowseOngoing: () -> Unit,
    onBrowseCompleted: () -> Unit,
    onRetry: () -> Unit
) {
    when {
        state.loading && state.featured == null -> HomeSkeleton()
        state.error != null && state.featured == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ErrorState(message = state.error, onRetry = onRetry)
        }

        else -> HomeContent(
            state = state,
            onAnimeClick = onAnimeClick,
            onContinueClick = onContinueClick,
            onBrowseOngoing = onBrowseOngoing,
            onBrowseCompleted = onBrowseCompleted
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAnimeClick: (AnimeSummary) -> Unit,
    onContinueClick: (HistoryEntry) -> Unit,
    onBrowseOngoing: () -> Unit,
    onBrowseCompleted: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        state.featured?.let { featured ->
            item(key = "featured") {
                FeaturedBanner(anime = featured, onClick = { onAnimeClick(featured) })
            }
        }

        if (state.continueWatching.isNotEmpty()) {
            item(key = "continue-header") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Continue watching")
            }
            item(key = "continue-row") {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.continueWatching, key = { "${it.animeSlug}-${it.episodeNumber}" }) { entry ->
                        ContinueWatchingCard(entry = entry, onClick = { onContinueClick(entry) })
                    }
                }
            }
        }

        if (state.ongoing.isNotEmpty()) {
            item(key = "ongoing-header") {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Ongoing",
                    actionLabel = "View all",
                    onAction = onBrowseOngoing
                )
            }
            item(key = "ongoing-row") {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.ongoing, key = { it.slug }) { anime ->
                        AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime) })
                    }
                }
            }
        }

        if (state.completed.isNotEmpty()) {
            item(key = "completed-header") {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Completed & highly rated",
                    actionLabel = "View all",
                    onAction = onBrowseCompleted
                )
            }
            item(key = "completed-row") {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.completed, key = { it.slug }) { anime ->
                        AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime) })
                    }
                }
            }
        }

        item(key = "footer") {
            Spacer(Modifier.height(30.dp))
            Text(
                text = "Catalogue data by ShivraAPI · AniStream native player",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun FeaturedBanner(
    anime: AnimeSummary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clickable(onClick = onClick)
    ) {
        PosterImage(
            url = anime.coverUrl,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.32f to Color.Transparent,
                        0.6f to Color.Black.copy(alpha = 0.35f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = "AniStream",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "NATIVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "FEATURED · ${anime.status?.uppercase() ?: "ONGOING"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = anime.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                anime.rating?.let { rating ->
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
                            text = rating,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
                val episode = anime.episodeLabel ?: anime.totalEpisodes
                if (!episode.isNullOrBlank()) {
                    Text(
                        text = episode,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                anime.day?.let { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View episodes")
            }
        }
    }
}
