package ru.levar.domain

import com.google.gson.annotations.SerializedName


// Модель для аутентификации
data class AuthRequest(
    @SerializedName("username") val login: String,
    @SerializedName("password") val password: String,
    @SerializedName("client_id") val deviceId: String = "5",
    @SerializedName("client_secret") val appKey: String = "demo"
)

data class ErrorType(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
)

