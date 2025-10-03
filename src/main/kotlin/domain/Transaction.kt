package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("TransactionId") val id: String,
    // Формат "2023-01-01T00:00:00"
    @SerializedName("Date") val date: String,
    @SerializedName("DateUnix") val dateUnix: String,
    @SerializedName("CategoryId") val categoryId: Int,
    @SerializedName("CategoryFullName") val categoryFullName: String,
    @SerializedName("Description") val description: String,
    @SerializedName("isPlan") val isPlan: Boolean,
    @SerializedName("type") val type: Int,
    @SerializedName("Total") val total: Double,
    @SerializedName("AccountId") val accountId: String,
    @SerializedName("CurrencyId") val currencyId: Int,
    @SerializedName("TransTotal") val transTotal: Double,
    @SerializedName("TransAccountId") val transAccountId: String,
    @SerializedName("TransCurrencyId") val transCurrencyId: Int,
    @SerializedName("GUID") val comment: String,
    // Формат "2023-01-01T00:00:00"
    @SerializedName("CreateDate") val createDate: String,
    @SerializedName("CreateDateUnix") val createDateUnix: String,
)
