package top.laoxin.modmanager.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import top.laoxin.modmanager.bean.DownloadGameConfigBean
import top.laoxin.modmanager.bean.GameInfo
import top.laoxin.modmanager.bean.ThinksBean
import top.laoxin.modmanager.bean.UpdateBean

private const val BASE_URL =
    "https://gitee.com"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; isLenient = true; allowSpecialFloatingPointValues = true; useArrayPolymorphism = false; allowStructuredMapKeys = true; prettyPrint = true; useAlternativeNames = false; classDiscriminator = "type" }
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
    suspend fun getGameConfigs() : List<DownloadGameConfigBean>

    // 下载游戏配置
    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/{name}.json")
    suspend fun downloadGameConfig(@Path("name")name : String) : GameInfo
    // 获取感谢名单
    @GET("/laoxinH/Mod_Manager/raw/main/gameConfig/api/thinks.json")
    suspend fun getThinksList() : List<ThinksBean>

}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object ModManagerApi {
    val retrofitService: ModManagerApiService by lazy {
        retrofit.create(ModManagerApiService::class.java)
    }
}

