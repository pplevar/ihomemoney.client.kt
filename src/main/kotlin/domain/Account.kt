package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("isDefault") val isDefault: Boolean,
    @SerializedName("display") val display: Boolean,
    @SerializedName("includeBalance") val includeBalance: Boolean,
    @SerializedName("hasOpenCurrencies") val hasOpenCurrencies: Boolean,
    @SerializedName("ListCurrencyInfo") val listCurrencyInfo: List<AccountCurrencyInfo>,
    @SerializedName("isShowInGroup") val isDeleted: Boolean,
)
