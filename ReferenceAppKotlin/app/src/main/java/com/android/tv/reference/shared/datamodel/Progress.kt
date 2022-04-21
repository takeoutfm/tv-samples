package com.android.tv.reference.shared.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Progress(
    val id: String,
    val position: Long,
    val duration: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
