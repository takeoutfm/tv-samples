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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(
    @field: Json(name = "accessToken")
    val accessToken: String,

    @field: Json(name = "mediaToken")
    val mediaToken: String,

    @field: Json(name = "refreshToken")
    val refreshToken: String,

    @field: Json(name = "endpoint")
    val endpoint: String,

    @field: Json(name = "displayName")
    val displayName: String
)
