package top.laoxin.modmanager.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import top.laoxin.modmanager.bean.DownloadGameConfigBean
import top.laoxin.modmanager.bean.GameInfoBean
import top.laoxin.modmanager.bean.InfoBean
import top.laoxin.modmanager.bean.ThinksBean
import top.laoxin.modmanager.bean.UpdateBean

private const val BASE_URL =
    "https://gitee.com"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
val gson = GsonBuilder()
    //.disableHtmlEscaping()
    .create()
private val retrofit = Retrofit.Builder()
    //.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .addConverterFactory(GsonConverterFactory.create(gson))
    .baseUrl(BASE_URL)
    .build()

/**
 * Retrofit service object for creating api calls
 */
interface ModManagerApiService {
    @GET("/laoxinH/Mod_Manager/raw/main/update/update.json")
    suspend fun getUpdate(): UpdateBean

    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/api/gameConfig.json")
    suspend fun getGameConfigs(): List<DownloadGameConfigBean>

    // 下载游戏配置
    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/{name}.json")
    suspend fun downloadGameConfig(@Path("name") name: String): GameInfoBean

    // 获取感谢名单
    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/api/thinks.json")
    suspend fun getThinksList(): List<ThinksBean>

    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/api/information.json")
    suspend fun getInfo(): InfoBean

}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object ModManagerApi {
    val retrofitService: ModManagerApiService by lazy {
        retrofit.create(ModManagerApiService::class.java)
    }
}

