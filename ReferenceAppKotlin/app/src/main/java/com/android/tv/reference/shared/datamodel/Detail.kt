package com.android.tv.reference.shared.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Detail(
    val id: String,
    val video: Video,
    val genres: List<String>,
    val cast: List<Cast>,
    val related: List<Video>,
) : Parcelable
