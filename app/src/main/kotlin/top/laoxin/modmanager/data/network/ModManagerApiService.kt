package top.laoxin.modmanager.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import top.laoxin.modmanager.domain.bean.DownloadGameConfigBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.bean.InfoBean
import top.laoxin.modmanager.domain.bean.ThanksBean

private const val API_URL_JSDELIVR =
    "https://cdn.jsdelivr.net"

val gsonForApi: Gson = GsonBuilder().create()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gsonForApi))
    .baseUrl(API_URL_JSDELIVR)
    .build()

interface ModManagerApiService {
    // 获取游戏配置列表
    @GET("gh/laoxinH/crosscore-mod-manager@main/gameConfig/api/gameConfig.json")
    suspend fun getGameConfigs(): List<DownloadGameConfigBean>

    // 下载游戏配置
    @GET("gh/laoxinH/crosscore-mod-manager@main/gameConfig/{name}.json")
    suspend fun downloadGameConfig(@Path("name") name: String): GameInfoBean

    // 获取感谢名单
    @GET("gh/laoxinH/crosscore-mod-manager@main/gameConfig/api/thanks.json")
    suspend fun getThanksList(): List<ThanksBean>

    // 获取最新信息
    @GET("gh/laoxinH/crosscore-mod-manager@main/gameConfig/api/information.json")
    suspend fun getInfo(): InfoBean

}

object ModManagerApi {
    val retrofitService: ModManagerApiService by lazy {
        retrofit.create(ModManagerApiService::class.java)
    }
}

