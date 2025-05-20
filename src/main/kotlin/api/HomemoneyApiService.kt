package ru.levar.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import ru.levar.domain.AccountGroup
import ru.levar.domain.Category
import ru.levar.domain.ErrorType
import ru.levar.domain.Transaction

interface HomemoneyApiService {

    @GET("TokenPassword")
    suspend fun login(
        @Query("username") login: String,
        @Query("password") password: String,
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String = "password",
    ): Response<AuthResponse>

    data class AuthResponse(
        @SerializedName("Error") val error: ErrorType,
        @SerializedName("access_token") val token: String,
        @SerializedName("refresh_token") val refreshToken: String
    )


    // Работа со счетами
    @GET("BalanceList")
    suspend fun getAccountGroups(
        @Query("Token") token: String,
    ): Response<BalanceListResponse>

    data class BalanceListResponse(
        @SerializedName("defaultcurrency") val defaultCurrencyId: String,
        @SerializedName("name") val currencyShortName: String,
        @SerializedName("ListGroupInfo") val listAccountGroupInfo: List<AccountGroup>,
        @SerializedName("Error") val error: ErrorType
    )

    // Работа с категориями
    @GET("CategoryList")
    suspend fun getCategories(
        @Query("Token") token: String,
    ): Response<CategoryListResponse>

    data class CategoryListResponse(
        @SerializedName("ListCategory") val listCategory: List<Category>,
        @SerializedName("Error") val error: ErrorType
    )

    // Работа с транзакциями
    @GET("TransactionList")
    suspend fun getTransactions(
        @Query("Token") token: String,
        @Query("TopCount") topCount: Int?
    ): Response<TransactionListResponse>

    data class TransactionListResponse(
        @SerializedName("ListTransaction") val listTransaction: List<Transaction>,
        @SerializedName("Error") val error: ErrorType
    )


//    @POST("Transactions/CreateTransaction")
//    suspend fun createTransaction(
//        @Header("Authorization") token: String,
//        @Body transaction: Transaction
//    ): Response<ApiResponse<String>> // Возвращает ID созданной транзакции

    // Работа с валютами
//    @GET("Currencies/GetCurrencies")
//    suspend fun getCurrencies(
//        @Header("Authorization") token: String
//    ): Response<ApiResponse<List<Currency>>>
}