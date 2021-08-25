package com.android.tv.reference.shared.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Cast(
    val id: String,
    val person: Person,
    val character: String,
) : Parcelable
