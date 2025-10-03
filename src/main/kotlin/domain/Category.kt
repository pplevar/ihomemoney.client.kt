package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: String,
    // 0 - расход, 1 - доход
    @SerializedName("type") val type: Int,
    @SerializedName("Name") val name: String,
    @SerializedName("FullName") val fullName: String,
    @SerializedName("isArchive") val isArchive: Boolean,
    @SerializedName("isPinned") val isPinned: Boolean,
)
