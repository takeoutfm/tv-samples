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

import com.android.tv.reference.shared.util.Result
import com.defsub.takeout.client.Client

/**
 * The authority that verifies the user's identity based on an existing token or credentials.
 * The verification is expected to be server-side.
 */
interface AuthClient {
    suspend fun validateToken(token: String): Result<UserInfo>
    suspend fun authWithPassword(
        endpoint: String,
        username: String,
        password: String
    ): Result<UserInfo>

    suspend fun authWithGoogleIdToken(idToken: String): Result<UserInfo>
    suspend fun invalidateToken(token: String): Result<Unit>
}

sealed class AuthClientError(message: String) : Exception(message) {
    object AuthenticationError : AuthClientError("Error authenticating user")
    data class ServerError(
        val errorCause: Exception
    ) : AuthClientError("Server error: ${errorCause.message}")
}

class TakeoutAuthClient : AuthClient {

    override suspend fun validateToken(token: String): Result<UserInfo> {
        return Result.Error(AuthClientError.AuthenticationError)
    }

    override suspend fun authWithPassword(
        endpoint: String,
        username: String,
        password: String
    ): Result<UserInfo> {
        val client = Client(endpoint)
        val tokens = client.login(username, password)
        if (tokens != null) {
            return Result.Success(
                UserInfo(
                    accessToken = tokens.accessToken,
                    mediaToken = tokens.mediaToken,
                    refreshToken = tokens.refreshToken,
                    endpoint = endpoint,
                    displayName = username
                )
            )
        }
        return Result.Error(AuthClientError.AuthenticationError)
    }

    override suspend fun authWithGoogleIdToken(idToken: String): Result<UserInfo> {
        return Result.Error(AuthClientError.AuthenticationError)
    }

    override suspend fun invalidateToken(token: String): Result<Unit> = Result.Success(Unit)
}
