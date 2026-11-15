package com.bayu.jelajahrasa.model

data class MoodRecommendation(
    val mood: Mood,
    val genreIds: String,
    val title: String
) {
    companion object {
        fun create(mood: Mood): MoodRecommendation {
            return MoodRecommendation(
                mood = mood,
                genreIds = mood.getGenreIds(),
                title = mood.name
            )
        }
    }
}