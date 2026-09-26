package com.lugat.kelime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lugat.kelime.domain.Content

/** Grup başlıklı (Okul, Sınavlar, Üniversite…) iki sütunlu deste ızgarası. */
@Composable
fun DeckGrid(
    content: Content,
    learnedOf: (String) -> Int,
    selected: Set<String>?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (LazyGridScope.() -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        header?.invoke(this)
        content.groups.forEach { group ->
            item(key = "g:$group", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    group,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                )
            }
            items(content.decks.filter { it.group == group }, key = { it.id }) { deck ->
                DeckTile(
                    deck = deck,
                    learned = learnedOf(deck.id),
                    total = deck.words.size,
                    selected = selected?.let { deck.id in it },
                    onClick = { onClick(deck.id) },
                )
            }
        }
    }
}
