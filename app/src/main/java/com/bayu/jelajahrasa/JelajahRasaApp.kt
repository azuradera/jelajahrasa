package com.bayu.jelajahrasa

import android.app.Application
import com.bayu.jelajahrasa.data.local.AppDatabase
import com.bayu.jelajahrasa.data.remote.MovieRemoteDataSource
import com.bayu.jelajahrasa.data.repository.AuthRepository
import com.bayu.jelajahrasa.data.repository.MovieRepository

class JelajahRasaApp : Application() {

    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val movieRepository: MovieRepository by lazy {
        MovieRepository(
            database = database,
            remoteDataSource = MovieRemoteDataSource()
        )
    }
}