package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class Currency(
    @SerializedName("id") val id: Int,
    @SerializedName("shortName") val shortName: String,
    @SerializedName("rate") val rate: Double,
    @SerializedName("balance") val balance: Double,
    @SerializedName("display") val display: Boolean
)
