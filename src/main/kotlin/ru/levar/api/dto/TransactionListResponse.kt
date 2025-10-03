package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.ErrorType
import ru.levar.domain.Transaction

data class TransactionListResponse(
    @SerializedName("ListTransaction") val listTransaction: List<Transaction>,
    @SerializedName("Error") val error: ErrorType,
)
