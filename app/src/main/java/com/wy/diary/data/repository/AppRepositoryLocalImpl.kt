package com.wy.diary.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wy.diary.util.userDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppRepositoryLocalImpl @Inject constructor(@ApplicationContext private val context: Context): AppRepository {

    private val LOGIN_KEY = booleanPreferencesKey("login")
    private val TOKEN_KEY = stringPreferencesKey("user_token")

    override suspend fun saveLoginStatus(value: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[LOGIN_KEY] = value
        }
    }

    override suspend fun getLoginStatus(): Boolean {
        return context.userDataStore.data
            .map { preferences -> preferences[LOGIN_KEY] ?: false }
            .first()
    }

    override suspend fun saveToken(value: String) {
        context.userDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = value
        }
    }

    override suspend fun getToken(): String {
        return context.userDataStore.data
            .map { preferences -> preferences[TOKEN_KEY] ?: "" }
            .first()
    }
}