package com.bayu.jelajahrasa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bayu.jelajahrasa.data.local.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow
import com.bayu.jelajahrasa.data.local.entity.MovieEntity


@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("""
        SELECT movies.* FROM movies
        INNER JOIN collections ON movies.movieId = collections.movieId
        WHERE collections.userId = :userId
        AND collections.collectionType = :collectionType
        ORDER BY 
            CASE WHEN :sortOrder = 'ASC' THEN collections.addedAt END ASC,
            CASE WHEN :sortOrder = 'DESC' THEN collections.addedAt END DESC
    """)

    fun getCollections(
        userId: String,
        collectionType: String,
        sortOrder: String = "DESC"
    ): Flow<List<MovieEntity>>

    @Query("""
        DELETE FROM collections
        WHERE userId = :userId
        AND movieId = :movieId
        AND collectionType = :collectionType
    """)
    suspend fun removeCollection(
        userId: String,
        movieId: Int,
        collectionType: String
    )

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM collections
            WHERE userId = :userId
            AND movieId = :movieId
            AND collectionType = :collectionType
        )
    """)
    suspend fun isInCollection(
        userId: String,
        movieId: Int,
        collectionType: String
    ): Boolean

    @Query("""
        SELECT movieId FROM collections
        WHERE userId = :userId
        AND collectionType = :collectionType
    """)
    suspend fun getCollectionMovieIds(
        userId: String,
        collectionType: String
    ): List<Int>

    @Query("""
        SELECT COUNT(*) FROM collections
        WHERE userId = :userId
        AND collectionType = :collectionType
    """)
    fun getCollectionCount(
        userId: String,
        collectionType: String
    ): Flow<Int>

    @Query("DELETE FROM collections WHERE userId = :userId")
    suspend fun clearUserCollections(userId: String)
}