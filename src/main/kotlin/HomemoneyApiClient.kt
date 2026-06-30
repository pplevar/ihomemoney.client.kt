package ru.levar

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.levar.api.HomemoneyApiService
import java.util.concurrent.TimeUnit

/**
 * Entry point to the iHomemoney API.
 *
 * The client is unauthenticated: its only operation is [authenticate], which exchanges
 * credentials for a [Session]. Authenticated operations live on [Session], so they are
 * unreachable without first authenticating — the "log in first" invariant is enforced by
 * the type system rather than by a runtime guard repeated in every method.
 */
class HomemoneyApiClient(baseUrl: String = AppConfig.serviceUri) {
    private val apiService: HomemoneyApiService

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

    /**
     * Exchanges credentials for an authenticated [Session].
     *
     * Returns the same typed seam as every other call: an [ApiResult.Err] carries *why*
     * authentication failed (wrong password vs. server error vs. malformed body) instead of
     * collapsing every cause into a bare `false`.
     */
    suspend fun authenticate(
        login: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ): ApiResult<Session> =
        interpret { apiService.login(login, password, clientId, clientSecret) }
            .map { Session(apiService, it.token) }

    /**
     * Builds a [Session] for a known token without a network round-trip.
     *
     * Module-internal test seam: it lets authenticated-call tests exercise [Session] directly
     * while keeping the public surface honest — library consumers can still only obtain a
     * [Session] through [authenticate].
     */
    internal fun sessionWith(token: String): Session = Session(apiService, token)
}
