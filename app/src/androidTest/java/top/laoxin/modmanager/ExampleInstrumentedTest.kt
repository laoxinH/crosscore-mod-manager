package top.laoxin.modmanager

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import net.sf.sevenzipjbinding.ArchiveFormat
import net.sf.sevenzipjbinding.IArchiveOpenCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import top.laoxin.modmanager.tools.ModTools
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )
//    @Test
//    fun useAppContext() {
//
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("top.laoxin.modmanager", appContext.packageName)
//        try {
//            val file = File(ModTools.ROOT_PATH + "/Download/Telegram/测.rar")
//           // SevenZip.initSevenZipFromPlatformJAR()
//            val randomAccessFile: RandomAccessFile = RandomAccessFile(file, "r")
//            val inStream: RandomAccessFileInStream = RandomAccessFileInStream(randomAccessFile)
//            val callback: ArchiveOpenCallback = ArchiveOpenCallback()
//            val inArchive: IInArchive =
//                SevenZip.openInArchive(ArchiveFormat.RAR5, inStream, callback)
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
//
//    private class ArchiveOpenCallback : IArchiveOpenCallback {
//        override fun setTotal(files: Long, bytes: Long) {
//            Log.i("测试", "Archive open, total work: $files files, $bytes bytes")
//        }
//
//        override fun setCompleted(files: Long, bytes: Long) {
//            Log.i("测试", "Archive open, completed: $files files, $bytes bytes")
//        }
//    }
}