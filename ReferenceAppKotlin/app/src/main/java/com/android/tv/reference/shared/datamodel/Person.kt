package com.android.tv.reference.shared.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Person(
    val id: String,
    val name: String,
    val bio: String,
    val birthplace: String,
    val birthday: Int,
    val thumbnailUri: String,
) : Parcelable
