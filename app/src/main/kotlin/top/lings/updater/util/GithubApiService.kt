package top.lings.updater.util

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import top.laoxin.modmanager.bean.GithubBean

private const val BASE_URL_GITHUB =
    "https://api.github.com"

val gsonForGithub = GsonBuilder().create()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gsonForGithub))
    .baseUrl(BASE_URL_GITHUB)
    .build()

interface GithubApiService {
    @GET("repos/laoxinH/crosscore-mod-manager/releases/latest")
    suspend fun getLatestRelease(): GithubBean

}

object GithubApi {
    val retrofitService: GithubApiService by lazy {
        retrofit.create(GithubApiService::class.java)
    }
}

