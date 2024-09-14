package top.laoxin.modmanager

import android.util.Log
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.io.IOUtils
import top.laoxin.modmanager.constant.FileType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files


object ArchiveUtil {
    val TAG = "ArchiveUtil"

    /**
     * 解压文件
     * 支持的文件格式: 7z, ar, arj, cpio, dump, tar, zip
     *
     * @param srcFile  需要解压的文件位置
     * @param destDir  解压的目标位置
     * @param password 密码
     * @param charset  编码格式
     */
    fun decompression(srcFile: String, destDir: String, password: String?) {
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive =
                SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile), password)
            inArchive.extract(null, false, ExtractCallback(inArchive, destDir))
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }


    fun decompression(srcFile: String, destDir: String) {
        decompression(srcFile, destDir, null)
    }

    @Throws(SevenZipException::class)
    fun extractSpecificFile(
        srcFile: String,
        files: List<String>,
        destDir: String,
        password: String?
    ) {
        var extractPath = destDir
        if (!destDir.endsWith("/") && !destDir.endsWith("\\")) {
            extractPath += File.separator
        }
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive =
                SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile), password)

            val indices = IntArray(files.size)
            for (i in 0 until inArchive.numberOfItems) {
                var filePath = inArchive.getStringProperty(i, PropID.PATH)
                filePath = formatString(filePath)
                if (filePath in files) {
                    indices[i] = i
                }
            }

            inArchive.extract(indices, false, object : IArchiveExtractCallback {

                override fun setTotal(total: Long) {}

                override fun setCompleted(complete: Long) {}

                @Throws(SevenZipException::class)
                override fun getStream(
                    index: Int,
                    extractAskMode: ExtractAskMode
                ): ISequentialOutStream {
                    return ISequentialOutStream { data ->
                        var filename = inArchive.getStringProperty(index, PropID.PATH)
                        filename = formatString(filename)
                        FileOutputStream(extractPath + filename).use { it.write(data) }
                        data.size
                    }
                }

                override fun prepareOperation(extractAskMode: ExtractAskMode) {}

                override fun setOperationResult(extractOperationResult: ExtractOperationResult) {}
            })
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    fun extractSpecificFile(srcFile: String, files: List<String>, destDir: String) {
        extractSpecificFile(srcFile, files, destDir, null)
    }

    private class ExtractCallback(inArchive: IInArchive?, extractPath: String) :
        IArchiveExtractCallback {
        private val inArchive: IInArchive?

        private val extractPath: String

        init {
            var extractPath = extractPath
            this.inArchive = inArchive
            if (!extractPath.endsWith("/") && !extractPath.endsWith("\\")) {
                extractPath += File.separator
            }
            this.extractPath = extractPath
        }

        override fun setTotal(total: Long) {
        }

        override fun setCompleted(complete: Long) {
        }

        @Throws(SevenZipException::class)
        override fun getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream {
            return ISequentialOutStream { data: ByteArray ->
                var filePath = inArchive!!.getStringProperty(index, PropID.PATH)
                filePath = formatString(filePath)
                try {
                    val path = File(extractPath + filePath)

                    if (path.parentFile?.exists() == false) {
                        path.parentFile?.mkdirs()
                    }

                    if (!path.exists()) {
                        path.createNewFile()
                    }
                    FileOutputStream(path, true).use {
                        it.write(data)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "IOException while extracting $filePath")
                }
                data.size
            }
        }

        override fun prepareOperation(extractAskMode: ExtractAskMode) {
        }

        override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
        }
    }


    // 列出压缩包内容
    fun listInArchiveFiles(srcFile: String): List<String> {
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null
        val list = mutableListOf<String>()
        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            for (i in 0 until inArchive.numberOfItems) {
                var filePath = inArchive.getStringProperty(i, PropID.PATH)
                filePath = formatString(filePath)
                list.add(filePath)
            }
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
        return list
    }
    /**
     * 解压zip
     * 支持的文件格式: ar, arj, cpio, dump, tar, zip
     *
     * @param srcFile  需要解压的文件位置
     * @param destDir  解压的目标位置
     * @param password 密码
     * @param charset  编码格式
     */


    /**
     * 解压7z
     *
     * @param srcFile  需要解压的文件位置
     * @param destDir  解压的目标位置
     * @param password 密码，没有密码的时候，输入null
     * @param charset  编码格式
     */
    fun un7z(srcFile: String, destDir: String, password: String?) {
        val passwordChars = password?.toCharArray()

        try {
            var sevenZFile =
                SevenZFile.Builder().setFile((File(srcFile))).setPassword(passwordChars)
                    .setCharset(StandardCharsets.UTF_8).get()
            val destDir = File(destDir)
            val fileHeaders = sevenZFile.entries.map { it.name }
            if (isRandomCode(fileHeaders)) {
                sevenZFile.close()
                sevenZFile = SevenZFile.Builder().setFile(File(srcFile)).setPassword(passwordChars)
                    .setCharset(Charset.forName("GBK")).get()
            }
            sevenZFile.use { sZFile ->
                sZFile.entries.forEach {
                    val entry = it
                    val entryName = entry.name
                    val f = File(destDir, entryName)
                    if (entry.isDirectory) {
                        if (!f.isDirectory && !f.mkdirs()) {
                            throw IOException("failed to create directory $f")
                        }
                    } else {
                        val parent = f.parentFile
                        if (!parent.isDirectory && !parent.mkdirs()) {
                            throw IOException("failed to create directory $parent")
                        }
                        Files.newOutputStream(f.toPath()).use { o ->
                            sZFile.getInputStream(entry).use { i ->
                                IOUtils.copy(i, o)
                            }
                        }
                    }

                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

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

    fun formatString(fileHeader: String): String {

        val canEnCode = Charset.forName("GBK").newEncoder().canEncode(fileHeader)
        if (!canEnCode) { //canEnCode为true，表示不是乱码。false.表示乱码。是乱码则需要重新设置编码格式
            return fileHeader
        } else {
            val bytes = fileHeader.toByteArray(Charset.forName("Cp437"))
            return String(bytes, Charset.forName("GBK"))
        }


    }

    fun isRandomCode(fileHeaders: List<String>): Boolean {
        for (i in fileHeaders.indices) {
            val fileHeader = fileHeaders[i]
            val canEnCode = Charset.forName("GBK").newEncoder().canEncode(fileHeader)
            if (!canEnCode) { //canEnCode为true，表示不是乱码。false.表示乱码。是乱码则需要重新设置编码格式
                return true
            }
        }
        return false
    }
}
