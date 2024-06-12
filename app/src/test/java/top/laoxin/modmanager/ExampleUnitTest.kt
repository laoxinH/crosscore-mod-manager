package top.laoxin.modmanager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test

import org.junit.Assert.*
import top.laoxin.modmanager.bean.GameInfo
import java.io.File
import java.util.HashMap

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
        val fromJson : MutableMap<String, Any> =
            Gson().fromJson(File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\hot_update_list.json").readText(), mapType)
        val any = fromJson["abInfos"] as List<MutableMap<String, Any>>
         var index1 = -1
        any.forEachIndexed { index, mutableMap ->
            if (mutableMap["name"] == "activity/[uc]act1access.ab") {
                index1 = index
            }
        }
        any[index1]["name"] = "activity/[uc]act1access.ab测试"
        println(index1)
        val toJson = Gson().toJson(fromJson, mapType)
        val file = File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\test.json")
        file.createNewFile()
        file.writeText(toJson)

        println("===========================")
        //println(toJson)
        println("===========================")

    }
}