package com.wy.diary.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// 扩展属性：Context.userDataStore
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs" // DataStore 存储文件名，对应 files/datastore/user_prefs.preferences_pb
)
