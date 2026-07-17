package com.example.smbplayer.data.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites_paths")
    }

    val favorites: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY]?.toList() ?: emptyList()
    }

    suspend fun addFavorite(path: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            current.add(path)
            preferences[FAVORITES_KEY] = current
        }
    }

    suspend fun removeFavorite(path: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(path)
            preferences[FAVORITES_KEY] = current
        }
    }

    fun isFavorite(path: String): Flow<Boolean> =
        favorites.map { paths -> path in paths }

    suspend fun toggleFavorite(path: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            if (path in current) current.remove(path) else current.add(path)
            preferences[FAVORITES_KEY] = current
        }
    }
}
