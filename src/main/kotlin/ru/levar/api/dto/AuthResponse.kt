package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.ErrorType

data class AuthResponse(
    @SerializedName("Error") val error: ErrorType,
    @SerializedName("access_token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String,
)
