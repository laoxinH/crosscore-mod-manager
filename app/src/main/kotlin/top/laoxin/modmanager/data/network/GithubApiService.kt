package top.laoxin.modmanager.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import top.laoxin.modmanager.domain.bean.GithubBean

private const val GITHUB_URL =
    "https://api.github.com"

val gsonForGithub: Gson = GsonBuilder().create()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gsonForGithub))
    .baseUrl(GITHUB_URL)
    .build()

interface GithubApiService {
    // 获取最新版本
    @GET("repos/laoxinH/crosscore-mod-manager/releases/latest")
    suspend fun getLatestRelease(): GithubBean

}

object GithubApi {
    val retrofitService: GithubApiService by lazy {
        retrofit.create(GithubApiService::class.java)
    }
}

