package com.antigravity.webtoonhub.api

data class WebtoonItem(
    val titleId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val linkUrl: String,
    val platform: String,
    val isUpdate: Boolean = false
)
