package com.bayu.jelajahrasa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }

        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    suspend fun register(email: String, password: String): FirebaseUser {
        return firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .await()
            .user ?: throw IllegalStateException("Pendaftaran gagal.")
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        return firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
            .user ?: throw IllegalStateException("Login gagal.")
    }

    suspend fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}