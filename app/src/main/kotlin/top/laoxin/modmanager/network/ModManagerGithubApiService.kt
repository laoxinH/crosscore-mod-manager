package top.laoxin.modmanager.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import top.laoxin.modmanager.bean.GithubBean

private const val BASE_URL_GITHUB =
    "https://api.github.com"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
val gsonForGithub = GsonBuilder()
    //.disableHtmlEscaping()
    .create()
private val retrofit = Retrofit.Builder()
    //.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .addConverterFactory(GsonConverterFactory.create(gsonForGithub))
    .baseUrl(BASE_URL_GITHUB)
    .build()

/**
 * Retrofit service object for creating api calls
 */
interface ModManagerGithubApiService {
    @GET("repos/laoxinH/crosscore-mod-manager/releases/latest")
    suspend fun getLatestRelease(): GithubBean

}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object ModManagerGithubApi {
    val retrofitService: ModManagerGithubApiService by lazy {
        retrofit.create(ModManagerGithubApiService::class.java)
    }
}

