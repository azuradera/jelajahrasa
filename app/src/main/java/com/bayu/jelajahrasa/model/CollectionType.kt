package com.bayu.jelajahrasa.model

import androidx.annotation.StringRes
import com.bayu.jelajahrasa.R

enum class CollectionType(
    val value: String,
    @StringRes val titleResId: Int
) {
    WATCHLIST(
        value = "WATCHLIST",
        titleResId = R.string.collection_watchlist
    ),
    WATCHED(
        value = "WATCHED",
        titleResId = R.string.collection_watched
    ),
    FAVORITE(
        value = "FAVORITE",
        titleResId = R.string.collection_favorite
    )
}