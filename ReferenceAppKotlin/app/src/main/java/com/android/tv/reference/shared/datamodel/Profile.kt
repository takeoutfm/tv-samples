package com.android.tv.reference.shared.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Profile(
    val id: String,
    val person: Person,
    val videos: List<Video>,
) : Parcelable
