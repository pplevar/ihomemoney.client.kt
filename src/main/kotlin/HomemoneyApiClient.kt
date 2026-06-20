package ru.levar

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.levar.api.HomemoneyApiService
import ru.levar.api.dto.ApiEnvelope
import ru.levar.domain.Account
import ru.levar.domain.AccountGroup
import ru.levar.domain.Category
import ru.levar.domain.Transaction
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class HomemoneyApiClient(baseUrl: String = AppConfig.serviceUri) {
    private val apiService: HomemoneyApiService
    private var _token: String = ""

    var token: String
        get() = _token
        set(value) {
            require(value.isNotBlank()) { "Token cannot be blank" }
            _token = value
        }

    init {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

        val client =
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        apiService = retrofit.create(HomemoneyApiService::class.java)
    }

    suspend fun login(
        login: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ): Boolean {
        return try {
            val auth = handleResponse(apiService.login(login, password, clientId, clientSecret))
            token = auth.token
            true
        } catch (e: Exception) {
            logger.error(e) { "Authentication failed: ${e.message}" }
            false
        }
    }

    private fun requireAuthentication() {
        require(_token.isNotBlank()) { "Authentication required. Call login() first." }
    }

    suspend fun getAccountGroups(): List<AccountGroup> {
        requireAuthentication()
        val response = apiService.getAccountGroups(token)
        return handleResponse(response).listAccountGroupInfo
    }

    suspend fun getAccounts(): List<Account> = getAccountGroups().flatMap { it.listAccountInfo }

    suspend fun getCategories(): List<Category> {
        requireAuthentication()
        val response = apiService.getCategories(token)
        return handleResponse(response).listCategory
    }

    suspend fun getTransactions(topCount: Int?): List<Transaction> {
        requireAuthentication()
        val response = apiService.getTransactions(token, topCount)
        return handleResponse(response).listTransaction
    }

//    suspend fun getTransactions(
//        startDate: String? = null,
//        endDate: String? = null,
//        accountId: String? = null,
//        categoryId: String? = null,
//        page: Int? = null,
//        pageSize: Int? = null
//    ): List<Transaction> {
//        val response = apiService.getTransactions(
//            token,
//            startDate,
//            endDate,
//            accountId,
//            categoryId,
//            page,
//            pageSize
//        )
//        return handleResponse(response)
//    }

//    suspend fun createTransaction(transaction: Transaction): String {
//        val response = apiService.createTransaction(token, transaction)
//        return handleResponse(response)
//    }

//    suspend fun getCurrencies(): List<Currency> {
//        val response = apiService.getCurrencies(token)
//        return handleResponse(response)
//    }

    private fun <T : ApiEnvelope> handleResponse(response: Response<T>): T {
        if (!response.isSuccessful) {
            throw Exception("Request failed with code ${response.code()}: ${response.message()}")
        }

        val body =
            response.body()
                ?: throw Exception("Request failed: empty response body")

        if (body.error.code != 0) {
            throw Exception("API error ${body.error.code}: ${body.error.message}")
        }

        return body
    }
}
