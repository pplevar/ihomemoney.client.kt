package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("TransactionId") val id: String,
    @SerializedName("Date") val date: String, // Формат "2023-01-01T00:00:00"
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
    @SerializedName("CreateDate") val createDate: String, // Формат "2023-01-01T00:00:00"
    @SerializedName("CreateDateUnix") val createDateUnix: String
)