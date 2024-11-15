package top.laoxin.modmanager

import top.laoxin.modmanager.constant.FileType
import java.io.File
import java.io.FileInputStream
import java.io.IOException


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
        var countOfTypedRes: String = "",
        var packInfos: MutableList<AbInfo> = mutableListOf()
    )

    data class PersistentRes(
        val abInfos: MutableList<AbInfo> = mutableListOf(),

        )

    data class FullPack(
        val totalSize: Long = 0,
        val abSize: Long = 0,
        val type: String = "",
        val cid: Int = -1,
    )

    data class AbInfo(
        val name: String?,
        val hash: String?,
        val md5: String?,
        val totalSize: Long?,
        val abSize: Long?,
        val thash: String?,
        val type: String?,
        val pid: String?,
        val cid: Int?,


        )
//    @Test
//    fun addition_isCorrect() {
//
//        assertEquals(4, 2 + 2)
//        val mapType = object : TypeToken<MutableMap<String, Any>>() {}.type
//      /*  val gson = GsonBuilder()
//            .disableHtmlEscaping()
//            .registerTypeAdapter(Long::class.java, LongTypeAdapter())
//            .create()*/
//        val gson = GsonBuilder()
//            //.disableHtmlEscaping()
//            //.registerTypeAdapter(Long::class.java, LongTypeAdapter()) // 注册 LongTypeAdapter
//            .create()
//        val fromJson : /*MutableMap<String, Any>*/ HotUpdate =
//            gson.fromJson(File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\hot_update_list.json").readText(), HotUpdate::class.java)
//        val any = fromJson.abInfos/*["abInfos"] as List<MutableMap<String, Any>>*/
//         var index1 = -1
//   /*     any.forEachIndexed { index, mutableMap ->
//            if (mutableMap.name == "activity/[uc]act1access.ab") {
//                index1 = index
//            }
//            *//*mutableMap["abSize"] = *//**//*formatScientificNumberToNormal((mutableMap["abSize"] as Double).toLong())*//**//*13131313
//            mutableMap["totalSize"] = formatScientificNumberToNormal((mutableMap["totalSize"] as Double).toLong())
//            println(mutableMap["abSize"])
//            println(mutableMap["totalSize"])*//*
//            //
//        }
//        val a = 461366113466161*/
//        any[1] = any[1].copy(
//            name = "activity/[uc]act1access.ab测试",
//            abSize = 461366113466161
//
//        )
//       // any[index1]["abSize"] = 461366113466161
//        println(index1)
//        val toJson = gson.toJson(fromJson, HotUpdate::class.java)
//        val file = File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\test.json")
//        file.createNewFile()
//        file.writeText(toJson/*.replace(".0","")*/)
//
//        println("===========================")
//        //println(toJson)
//        println("===========================")
//
//        val toJson1 : PersistentRes = gson.fromJson(
//            File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\persistent_res_list.json").readText(),
//            PersistentRes::class.java
//        )
//        val file1 = File("C:\\Users\\thixi\\Documents\\GitHub\\crosscore-mod-manager\\app\\src\\test\\java\\top\\laoxin\\modmanager\\test1.json")
//        file1.createNewFile()
//        file1.writeText(gson.toJson(toJson1,PersistentRes::class.java)/*.replace(".0","")*/)
//
//    }
//    fun formatScientificNumberToNormal(num: Long): String {
//        val formatter = DecimalFormat("#")
//        return formatter.format(num)
//    }
//
//
//    @Test
//
//    fun test7z(){
//        try {
//            val file = File("C:\\Users\\thixi\\Desktop\\示例\\dynchars中文测试.7z")
//            SevenZip.initSevenZipFromPlatformJAR()
//            val randomAccessFile: RandomAccessFile = RandomAccessFile(file, "r")
//            val inStream: RandomAccessFileInStream = RandomAccessFileInStream(randomAccessFile)
//            val callback: ArchiveOpenCallback = ArchiveOpenCallback()
//            val inArchive: IInArchive =
//                SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream, callback)
//
//            val format: ArchiveFormat = inArchive.getArchiveFormat()
//            Log.i("测试", "Archive format: " + format.getMethodName())
//
//            val itemCount: Int = inArchive.getNumberOfItems()
//            Log.i("测试", "Items in archive: $itemCount")
//            for (i in 0 until itemCount) {
//                Log.i(
//                    "测试",
//                    ("File " + i + ": " + inArchive.getStringProperty(
//                        i,
//                        PropID.PATH
//                    )).toString() + " : " + inArchive.getStringProperty(i, PropID.SIZE)
//                )
//            }
//
//            inArchive.close()
//            inStream.close()
//        } catch (e: FileNotFoundException) {
//            Log.e("测试", e.message!!)
//        } catch (e: SevenZipException) {
//            //Log.e("测试", e.message)
//        } catch (e: IOException) {
//            Log.e("测试", e.message!!)
//        }
//    }
//    private class ArchiveOpenCallback : IArchiveOpenCallback {
//        override fun setTotal(files: Long, bytes: Long) {
//            Log.i("测试", "Archive open, total work: $files files, $bytes bytes")
//        }
//
//        override fun setCompleted(files: Long, bytes: Long) {
//            Log.i("测试", "Archive open, completed: $files files, $bytes bytes")
//        }
//    }

    /**
     * 获取文件真实类型
     *
     * @param file 要获取类型的文件。
     * @return 文件类型枚举。
     */
    private fun getFileType(file: File): FileType {
        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(file)
            val head = ByteArray(4)
            if (-1 == inputStream.read(head)) {
                return FileType.UNKNOWN
            }
            var headHex = 0
            for (b in head) {
                headHex = headHex shl 8
                headHex = headHex or b.toInt()
            }
            return when (headHex) {
                0x504B0304 -> FileType.ZIP
                -0x51 -> FileType._7z
                0x52617221 -> FileType.RAR
                else -> FileType.UNKNOWN
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return FileType.UNKNOWN
    }
}