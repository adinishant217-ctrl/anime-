package com.anistream.app.ui.screen.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.ui.component.AnimeGridCard
import com.anistream.app.ui.component.EmptyState
import com.anistream.app.ui.component.ErrorState
import com.anistream.app.ui.component.LoadingState
import com.anistream.app.ui.component.PosterImage
import kotlinx.coroutines.flow.collect

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onModeSelected: (ExploreMode) -> Unit,
    onGenreSelected: (Genre) -> Unit,
    onClearGenre: () -> Unit,
    onAnimeClick: (AnimeSummary) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Browse the live catalogue by status, genre or broadcast day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExploreMode.values().forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode && state.selectedGenre == null,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
            if (state.genres.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all-genres") {
                        FilterChip(
                            selected = state.selectedGenre == null,
                            onClick = onClearGenre,
                            label = { Text("All genres") }
                        )
                    }
                    listItems(state.genres, key = { it.slug }) { genre ->
                        FilterChip(
                            selected = state.selectedGenre?.slug == genre.slug,
                            onClick = { onGenreSelected(genre) },
                            label = { Text(genre.name) }
                        )
                    }
                }
            }
            if (state.selectedGenre != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Genre: ${state.selectedGenre.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            state.loading -> LoadingState(
                message = "Loading catalogue…",
                modifier = Modifier.weight(1f)
            )

            state.error != null && state.items.isEmpty() && state.schedule.isEmpty() -> ErrorState(
                message = state.error,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )

            state.mode == ExploreMode.SCHEDULE && state.selectedGenre == null -> ScheduleList(
                days = state.schedule,
                onAnimeClick = onAnimeClick,
                modifier = Modifier.weight(1f)
            )

            state.items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Explore,
                title = "Nothing here yet",
                message = "The catalogue returned no titles for this filter.",
                modifier = Modifier.weight(1f)
            )

            else -> AnimeGrid(
                animeList = state.items,
                hasNext = state.hasNext,
                loadingMore = state.loadingMore,
                error = state.error,
                onAnimeClick = onAnimeClick,
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AnimeGrid(
    animeList: List<AnimeSummary>,
    hasNext: Boolean,
    loadingMore: Boolean,
    error: String?,
    onAnimeClick: (AnimeSummary) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, animeList.size, hasNext) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (lastVisible >= animeList.size - 4 && hasNext) onLoadMore()
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        gridItems(animeList, key = { it.slug }) { anime ->
            AnimeGridCard(anime = anime, onClick = { onAnimeClick(anime) })
        }

        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        } else if (hasNext) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onLoadMore) { Text("Load more") }
                }
            }
        } else if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun ScheduleList(
    days: List<ScheduleDay>,
    onAnimeClick: (AnimeSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Explore,
            title = "Schedule unavailable",
            message = "The catalogue did not return a weekly schedule right now.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { day ->
            item(key = "day-${day.key}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${day.items.size} titles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            listItems(day.items, key = { "${day.key}-${it.slug}" }) { anime ->
                ScheduleRow(anime = anime, onClick = { onAnimeClick(anime) })
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    anime: AnimeSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(9.dp))
        ) {
            PosterImage(
                url = anime.coverUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun ExploreMode.label(): String = when (this) {
    ExploreMode.ONGOING -> "Ongoing"
    ExploreMode.COMPLETED -> "Completed"
    ExploreMode.SCHEDULE -> "Schedule"
}
