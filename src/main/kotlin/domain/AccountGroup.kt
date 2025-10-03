package ru.levar.domain

import com.google.gson.annotations.SerializedName

data class AccountGroup(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("hasAccounts") val hasAccounts: Boolean,
    @SerializedName("hasShowAccounts") val hasShowAccounts: Boolean,
    @SerializedName("order") val order: Long,
    @SerializedName("ListAccountInfo") val listAccountInfo: List<Account>,
)
