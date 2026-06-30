package ru.levar

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
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
import java.io.EOFException
import java.util.concurrent.TimeUnit

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
    ): ApiResult<Unit> =
        when (val result = interpret { apiService.login(login, password, clientId, clientSecret) }) {
            is ApiResult.Ok -> {
                token = result.value.token
                ApiResult.Ok(Unit)
            }
            is ApiResult.Err -> result
        }

    private fun requireAuthentication() {
        require(_token.isNotBlank()) { "Authentication required. Call login() first." }
    }

    suspend fun getAccountGroups(): ApiResult<List<AccountGroup>> {
        requireAuthentication()
        return interpret { apiService.getAccountGroups(token) }.map { it.listAccountGroupInfo }
    }

    suspend fun getAccounts(): ApiResult<List<Account>> = getAccountGroups().map { groups -> groups.flatMap { it.listAccountInfo } }

    suspend fun getCategories(): ApiResult<List<Category>> {
        requireAuthentication()
        return interpret { apiService.getCategories(token) }.map { it.listCategory }
    }

    suspend fun getTransactions(topCount: Int?): ApiResult<List<Transaction>> {
        requireAuthentication()
        return interpret { apiService.getTransactions(token, topCount) }.map { it.listTransaction }
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

    /**
     * The single point where a raw HTTP response is interpreted into an [ApiResult].
     *
     * Every failure mode is decided here once: a non-2xx status, a null/empty body,
     * an API-level error on HTTP 200, and a Gson parse failure (including the empty-body
     * "End of input"). Transport errors (e.g. connection refused/timeout) are not modelled
     * by [ApiFailure] and propagate as thrown exceptions.
     */
    private suspend fun <T : ApiEnvelope> interpret(call: suspend () -> Response<T>): ApiResult<T> {
        val response =
            try {
                call()
            } catch (e: Exception) {
                if (e is JsonParseException || e is MalformedJsonException || e is EOFException) {
                    return ApiResult.Err(ApiFailure.Malformed(e))
                }
                throw e
            }

        if (!response.isSuccessful) {
            return ApiResult.Err(ApiFailure.Http(response.code()))
        }

        val body = response.body() ?: return ApiResult.Err(ApiFailure.EmptyBody)

        if (body.error.code != 0) {
            return ApiResult.Err(ApiFailure.Api(body.error.code, body.error.message))
        }

        return ApiResult.Ok(body)
    }
}
