package top.laoxin.modmanager

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.LongSerializationPolicy
import com.google.gson.reflect.TypeToken
import org.junit.Test

import org.junit.Assert.*
import top.laoxin.modmanager.bean.GameInfo
import java.io.File
import java.math.BigDecimal
import java.util.HashMap
import com.google.gson.*
import java.lang.reflect.Type
import java.text.DecimalFormat

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    data class HotUpdate(
        var fullPack: FullPack = FullPack(),
        var versionId: String = "",
        var abInfos: MutableList<AbInfo> = mutableListOf(),
        var countOfTypedRes : String = "",
        var packInfos : MutableList<AbInfo> = mutableListOf()
    )
    data class PersistentRes(
        val abInfos: MutableList<AbInfo> = mutableListOf(),

    )

    data class FullPack(
        val totalSize: Long = 0,
        val abSize : Long = 0,
        val type : String = "",
        val cid : Int = -1,
        )

    data class AbInfo(
        val name : String? ,
        val hash : String? ,
        val md5 : String?,
        val totalSize : Long?,
        val abSize : Long?,
        val thash : String?,
        val type : String?,
        val pid : String?,
        val cid : Int?,


    )
    @Test
    fun addition_isCorrect() {

        assertEquals(4, 2 + 2)
        val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
      /*  val gson = GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Long::class.java, LongTypeAdapter())
            .create()*/
        val gson = GsonBuilder()
            //.disableHtmlEscaping()
            //.registerTypeAdapter(Long::class.java, LongTypeAdapter()) // 注册 LongTypeAdapter
            .create()
        val fromJson : /*MutableMap<String, Any>*/ HotUpdate =
            gson.fromJson(File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\hot_update_list.json").readText(), HotUpdate::class.java)
        val any = fromJson.abInfos/*["abInfos"] as List<MutableMap<String, Any>>*/
         var index1 = -1
   /*     any.forEachIndexed { index, mutableMap ->
            if (mutableMap.name == "activity/[uc]act1access.ab") {
                index1 = index
            }
            *//*mutableMap["abSize"] = *//**//*formatScientificNumberToNormal((mutableMap["abSize"] as Double).toLong())*//**//*13131313
            mutableMap["totalSize"] = formatScientificNumberToNormal((mutableMap["totalSize"] as Double).toLong())
            println(mutableMap["abSize"])
            println(mutableMap["totalSize"])*//*
            //
        }
        val a = 461366113466161*/
        any[1] = any[1].copy(
            name = "activity/[uc]act1access.ab测试",
            abSize = 461366113466161

        )
       // any[index1]["abSize"] = 461366113466161
        println(index1)
        val toJson = gson.toJson(fromJson, HotUpdate::class.java)
        val file = File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\test.json")
        file.createNewFile()
        file.writeText(toJson/*.replace(".0","")*/)

        println("===========================")
        //println(toJson)
        println("===========================")

        val toJson1 : PersistentRes = gson.fromJson(
            File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\persistent_res_list.json").readText(),
            PersistentRes::class.java
        )
        val file1 = File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\test1.json")
        file1.createNewFile()
        file1.writeText(gson.toJson(toJson1,PersistentRes::class.java)/*.replace(".0","")*/)

    }
    fun formatScientificNumberToNormal(num: Long): String {
        val formatter = DecimalFormat("#")
        return formatter.format(num)
    }

}