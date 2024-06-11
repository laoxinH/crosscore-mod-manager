package top.laoxin.modmanager

import com.google.gson.Gson
import org.junit.Test

import org.junit.Assert.*
import top.laoxin.modmanager.bean.GameInfo
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        val fromJson =
            Gson().fromJson<GameInfo>(File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\碧蓝航线示例配置.json").readText(), GameInfo::class.java)
        println(fromJson)

    }
}