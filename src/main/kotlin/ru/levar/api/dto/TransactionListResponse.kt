package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.ErrorType
import ru.levar.domain.Transaction

internal data class TransactionListResponse(
    @SerializedName("ListTransaction") val listTransaction: List<Transaction>,
    @SerializedName("Error") override val error: ErrorType,
) : ApiEnvelope
