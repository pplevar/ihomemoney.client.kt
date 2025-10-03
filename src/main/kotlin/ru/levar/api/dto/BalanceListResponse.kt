package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.AccountGroup
import ru.levar.domain.ErrorType

data class BalanceListResponse(
    @SerializedName("defaultcurrency") val defaultCurrencyId: String,
    @SerializedName("name") val currencyShortName: String,
    @SerializedName("ListGroupInfo") val listAccountGroupInfo: List<AccountGroup>,
    @SerializedName("Error") val error: ErrorType,
)
