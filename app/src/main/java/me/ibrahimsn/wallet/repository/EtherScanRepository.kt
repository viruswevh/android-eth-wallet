package me.ibrahimsn.wallet.repository

import com.google.gson.Gson
import io.reactivex.Single
import me.ibrahimsn.wallet.entity.EtherScanResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

class EtherScanRepository @Inject constructor(okHttpClient: OkHttpClient, gson: Gson,
                                              networkRepository: EthereumNetworkRepository) {

    private val etherScanApiClient: EtherScanApiClient

    private interface EtherScanApiClient {
        @GET("/api?module=account&action=txlist")
        fun fetchTransactions(@Query("address") address: String,
                              @Query("page") page: Int,
                              @Query("offset") offset: Int,
                              @Query("sort") sort: String,
                              @Query("apikey") apiKey: String): Single<EtherScanResponse>
    }

    init {
        etherScanApiClient = Retrofit.Builder()
                .baseUrl(networkRepository.getDefaultNetwork().backendUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(EtherScanApiClient::class.java)
    }

    fun fetchTransaction(address: String, page: Int, offset: Int): Single<EtherScanResponse> {
        return etherScanApiClient.fetchTransactions(address, page, offset, "ASC",
                "SGPX7HN5MJNWMMYDFUKUW7XTM21EDG2T1N")
    }
}