package com.bayu.jelajahrasa.model

import androidx.annotation.StringRes
import androidx.annotation.ColorRes
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.data.local.entity.MovieEntity

enum class Mood(
    @StringRes val titleResId: Int,
    @ColorRes val colorResId: Int,
    val genreIds: List<Int>,
    val positiveGenres: Map<Int, Int>,
    val negativeGenres: Map<Int, Int>,
    val positiveKeywords: List<String>,
    val negativeKeywords: List<String>
) {

    HAPPY(
        titleResId = R.string.mood_happy,
        colorResId = R.color.mood_happy,
        genreIds = listOf(35, 10751, 16, 10402),
        positiveGenres = mapOf(35 to 3, 10751 to 3, 16 to 2, 10402 to 2),
        negativeGenres = mapOf(27 to 3, 53 to 3, 80 to 2, 18 to 1),
        positiveKeywords = listOf("friendship", "happy ending", "feel good", "celebration", "family relationship", "summer", "dance", "comedy"),
        negativeKeywords = listOf("tragedy", "death", "serial killer", "depression", "suicide", "war", "murder")
    ),

    SAD(
        titleResId = R.string.mood_sad,
        colorResId = R.color.mood_sad,
        genreIds = listOf(18, 10749),
        positiveGenres = mapOf(18 to 3, 10749 to 2, 36 to 1),
        negativeGenres = mapOf(35 to 3, 16 to 2, 28 to 2),
        positiveKeywords = listOf("tragedy", "grief", "loss of loved one", "tearjerker", "cancer", "heartbreak", "loneliness", "melancholy"),
        negativeKeywords = listOf("slapstick comedy", "party", "superhero", "magic", "happy ending", "funny")
    ),

    ENERGETIC(
        titleResId = R.string.mood_energetic,
        colorResId = R.color.mood_energetic,
        genreIds = listOf(28, 12),
        positiveGenres = mapOf(28 to 3, 12 to 3, 878 to 2, 53 to 1),
        negativeGenres = mapOf(10749 to 3, 18 to 2, 10751 to 2),
        positiveKeywords = listOf("hero", "epic", "battle", "survival", "quest", "explosion", "race", "underdog", "action"),
        negativeKeywords = listOf("depression", "boring", "slow burn", "melancholy", "dialogue driven")
    ),

    TENSE(
        titleResId = R.string.mood_tense,
        colorResId = R.color.mood_tense,
        genreIds = listOf(53, 27, 80),
        positiveGenres = mapOf(53 to 3, 27 to 3, 80 to 3, 9648 to 2),
        negativeGenres = mapOf(35 to 3, 10751 to 3, 16 to 3, 10749 to 2),
        positiveKeywords = listOf("suspense", "murder", "killer", "jumpscare", "survival", "investigation", "paranormal", "psychopath"),
        negativeKeywords = listOf("romance", "fairy tale", "family vacation", "musical", "happy ending")
    ),

    ROMANTIC(
        titleResId = R.string.mood_romantic,
        colorResId = R.color.mood_romantic,
        genreIds = listOf(10749, 18),
        positiveGenres = mapOf(10749 to 3, 18 to 2, 35 to 1),
        negativeGenres = mapOf(28 to 3, 27 to 3, 53 to 3, 80 to 3),
        positiveKeywords = listOf("love", "romance", "first love", "couple", "relationship", "wedding", "kiss", "unrequited love"),
        negativeKeywords = listOf("serial killer", "alien invasion", "zombie", "war", "murder")
    ),

    CURIOUS(
        titleResId = R.string.mood_curious,
        colorResId = R.color.mood_curious,
        genreIds = listOf(9648, 878, 14),
        positiveGenres = mapOf(9648 to 3, 878 to 3, 14 to 2, 53 to 1),
        negativeGenres = mapOf(35 to 2, 10751 to 2, 10749 to 1),
        positiveKeywords = listOf("mystery", "twist ending", "puzzle", "conspiracy", "time travel", "secret", "detective", "mind bend"),
        negativeKeywords = listOf("predictable", "simple plot", "slapstick")
    );

    fun getGenreIds(): String {
        return genreIds.joinToString("|")
    }

    fun calculateScore(movie: MovieEntity, keywords: List<String>): Double {
        val movieGenres = movie.genreIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
        var score = 0.0

        for (genre in movieGenres) {
            score += positiveGenres[genre] ?: 0
            score -= negativeGenres[genre] ?: 0
        }

        for (kw in keywords) {
            if (positiveKeywords.any { kw.contains(it, ignoreCase = true) }) {
                score += 2.0
            }
            if (negativeKeywords.any { kw.contains(it, ignoreCase = true) }) {
                score -= 2.0
            }
        }

        score += (movie.voteAverage / 2.0)

        return score
    }
}

fun MovieEntity.determineMood(): Mood {
    val movieGenres = genreIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
    for (mood in Mood.entries) {
        if (movieGenres.any { mood.genreIds.contains(it) }) {
            return mood
        }
    }
    return Mood.HAPPY
}
