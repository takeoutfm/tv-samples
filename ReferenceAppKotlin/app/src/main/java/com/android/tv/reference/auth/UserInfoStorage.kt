/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.reference.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.*
import timber.log.Timber

/**
 * An identity storage persists identity data such as user information and ID token locally.
 */
interface UserInfoStorage {
    fun readUserInfo(): UserInfo?
    fun writeUserInfo(userInfo: UserInfo)
    fun clearUserInfo()
}

/**
 * SharedPreferences-backed identity storage. Production apps should prefer a more sophisticated
 * mechanism such as Room.
 */
class DefaultUserInfoStorage(context: Context) : UserInfoStorage {
//    private val KEY_PREFIX = "userInfo_"
    private val KEY_PREFIX = ""

    private var masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private var sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        "userInfo",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

//    private val sharedPreferences: SharedPreferences =
//        PreferenceManager.getDefaultSharedPreferences(context)

    override fun readUserInfo(): UserInfo? {
        Timber.d("readUserInfo!")
        val accessToken = getString("accessToken") ?: ""
        val mediaToken = getString("mediaToken") ?: ""
        val refreshToken = getString("refreshToken") ?: ""
        val endpoint = getString("endpoint") ?: ""
        val displayName = getString("displayName")
        return if (accessToken.isNotEmpty() && mediaToken.isNotEmpty() && refreshToken.isNotEmpty() && endpoint.isNotEmpty()) {
            UserInfo(accessToken, mediaToken, refreshToken, endpoint ?: "", displayName ?: "")
        } else {
            null
        }
    }

    override fun writeUserInfo(userInfo: UserInfo) {
        Timber.d("writeUserInfo %s", userInfo.toString())
        putStrings(
            mapOf(
                "accessToken" to userInfo.accessToken,
                "mediaToken" to userInfo.mediaToken,
                "refreshToken" to userInfo.refreshToken,
                "endpoint" to userInfo.endpoint,
                "displayName" to userInfo.displayName
            )
        )
    }

    override fun clearUserInfo() {
        putStrings(
            mapOf(
                "accessToken" to null,
                "mediaToken" to null,
                "refreshToken" to null,
                "endpoint" to null,
                "displayName" to null
            )
        )
    }

    private fun getString(key: String): String? =
        sharedPreferences.getString("${KEY_PREFIX}$key", null)

    private fun xputString(key: String, value: String?) = sharedPreferences.edit(true) {
        putString("${KEY_PREFIX}$key", value)
    }

    private fun putStrings(strings: Map<String, String?>) = sharedPreferences.edit(true) {
        strings.forEach {
            xputString(it.key, it.value)
        }
    }
}
