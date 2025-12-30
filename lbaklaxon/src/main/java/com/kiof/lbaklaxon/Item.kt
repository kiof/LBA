package com.kiof.lbaklaxon

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class Item(
    val sectionText: String,
    @DrawableRes val sectionImage: Int,
    val buttonText: String,
    @DrawableRes val buttonImage: Int,
    @RawRes val buttonSound: Int
)
