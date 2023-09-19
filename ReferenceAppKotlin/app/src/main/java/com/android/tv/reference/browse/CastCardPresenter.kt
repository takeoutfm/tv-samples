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
package com.android.tv.reference.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import com.android.tv.reference.shared.datamodel.Cast
import com.android.tv.reference.shared.datamodel.Video
import com.takeoutfm.tv.R
import com.takeoutfm.tv.databinding.PresenterCastCardBinding
import com.squareup.picasso.Picasso
import timber.log.Timber

/**
 * Presents a [Video] as an [ImageCardView] with descriptive text based on the Video's type.
 */
class CastCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val binding = PresenterCastCardBinding.inflate(LayoutInflater.from(context), parent, false)

        // Set the image size ahead of time since loading can take a while.
        val resources = context.resources
        binding.root.setMainImageDimensions(
            resources.getDimensionPixelSize(R.dimen.image_card_width),
            resources.getDimensionPixelSize(R.dimen.image_card_height)
        )

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        checkNotNull(item)
        val cast = item as Cast
        val binding = PresenterCastCardBinding.bind(viewHolder.view)
        binding.root.titleText = cast.person.name
        binding.root.contentText = cast.character

        Picasso.get().load(cast.person.thumbnailUri).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).into(binding.root.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val binding = PresenterCastCardBinding.bind(viewHolder.view)
        binding.root.mainImage = null
    }
}
