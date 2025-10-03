package ru.levar.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.levar.api.dto.AuthResponse
import ru.levar.api.dto.BalanceListResponse
import ru.levar.api.dto.CategoryListResponse
import ru.levar.api.dto.TransactionListResponse

interface HomemoneyApiService {
    @GET("TokenPassword")
    suspend fun login(
        @Query("username") login: String,
        @Query("password") password: String,
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String = "password",
    ): Response<AuthResponse>

    // Работа со счетами
    @GET("BalanceList")
    suspend fun getAccountGroups(
        @Query("Token") token: String,
    ): Response<BalanceListResponse>

    // Работа с категориями
    @GET("CategoryList")
    suspend fun getCategories(
        @Query("Token") token: String,
    ): Response<CategoryListResponse>

    // Работа с транзакциями
    @GET("TransactionList")
    suspend fun getTransactions(
        @Query("Token") token: String,
        @Query("TopCount") topCount: Int?,
    ): Response<TransactionListResponse>
}
