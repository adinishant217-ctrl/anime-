package com.anistream.app.ui.screen.episodes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.ui.component.EpisodeRow
import com.anistream.app.ui.component.ErrorState
import com.anistream.app.ui.component.LoadingState
import com.anistream.app.ui.component.ScreenHeader

@Composable
fun EpisodeListScreen(
    state: EpisodeListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEpisodeClick: (Episode) -> Unit
) {
    val detail = state.detail
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Episodes", onBack = onBack)

        when {
            state.loading && detail == null -> LoadingState(
                message = "Loading episodes…",
                modifier = Modifier.weight(1f)
            )

            state.error != null && detail == null -> ErrorState(
                message = state.error,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )

            detail != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${detail.episodes.size} episodes · newest episode first in the catalogue, oldest first here",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (detail.episodes.isEmpty()) {
                    Text(
                        text = "No episodes are available for this title.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        items(detail.episodes, key = { it.slug }) { episode ->
                            val history: HistoryEntry? = state.history[episode.number]
                            EpisodeRow(
                                episode = episode,
                                watched = history?.completed == true,
                                progress = history?.progressFraction ?: 0f,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }
            }
        }
    }
}
