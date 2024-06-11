package top.laoxin.modmanager.network

import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import top.laoxin.modmanager.bean.UpdateBean

private const val BASE_URL =
    "https://raw.githubusercontent.com"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

/**
 * Retrofit service object for creating api calls
 */
interface ModManagerApiService {
    @GET("/laoxinH/crosscore-mod-manager/main/update/update.json")
    suspend fun getUpdate(): UpdateBean
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object ModManagerApi {
    val retrofitService: ModManagerApiService by lazy {
        retrofit.create(ModManagerApiService::class.java)
    }
}

