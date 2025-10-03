package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class AccountCurrencyInfo(
    @SerializedName("id") val id: String,
    @SerializedName("shortName") val shortName: String,
    @SerializedName("rate") val rate: Double,
    @SerializedName("balance") val balance: Double,
    @SerializedName("display") val display: Boolean,
)
