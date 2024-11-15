package top.laoxin.modmanager.tools

import android.util.Log
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.progress.ProgressMonitor
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.ICryptoGetTextPassword
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import top.laoxin.modmanager.constant.FileType
import top.laoxin.modmanager.listener.ProgressUpdateListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset


object ArchiveUtil {
    const val TAG = "ArchiveUtil"
    var progressUpdateListener: ProgressUpdateListener? = null


    // 测试
    fun test() {
        val listInArchiveFiles =
            listInArchiveFiles(ModTools.ROOT_PATH + "/Download/Telegram/1.7z")
        decompression(
            ModTools.ROOT_PATH + "/Download/Telegram/测.rar",
            ModTools.ROOT_PATH + "/Download/Telegram/全部解压",
        )
        decompression(
            ModTools.ROOT_PATH + "/Download/Telegram/1.7z",
            ModTools.ROOT_PATH + "/Download/Telegram/密码解压",
            "12"
        )
        Log.i(TAG, listInArchiveFiles.toString())
        var archiveEncrypted = isArchiveEncrypted(ModTools.ROOT_PATH + "/Download/Telegram/1.7z")
        Log.i(TAG, "是否加密$archiveEncrypted")
        archiveEncrypted = isArchiveEncrypted(ModTools.ROOT_PATH + "/Download/Telegram/测.7z")
        Log.i(TAG, archiveEncrypted.toString())
        extractSpecificFile(
            ModTools.ROOT_PATH + "/Download/Telegram/测.rar",
            listOf("乌丝怀亚_去衣_玩具插入哎嘿颜/预览.png"),
            ModTools.ROOT_PATH + "/Download/Telegram/部分解压",
            null
        )

    }


    /**
     * 无需密码解压文件
     */
    fun decompression(srcFile: String, destDir: String, overwrite: Boolean = false): Boolean {
        // 判断压缩包类型
        return decompression(srcFile, destDir, null, overwrite)
    }

    /**
     * 带密码解压
     */
    fun decompression(
        srcFile: String,
        destDir: String,
        password: String?,
        overwrite: Boolean = false
    ): Boolean {
        return when (getFileType(File(srcFile))) {
            FileType.ZIP -> {
                extractZip(srcFile, destDir, password, overwrite)
            }

            FileType._7z -> {
                decompressionBy7z(srcFile, destDir, password, overwrite)
            }

            FileType.RAR -> {
                decompressionBy7z(srcFile, destDir, password, overwrite)
            }

            else -> false
        }
    }


    /**
     * 无密码解压指定文件
     */
    fun extractSpecificFile(
        srcFile: String,
        files: List<String>,
        destDir: String,
        overwrite: Boolean = false
    ): Boolean {
        // 判断压缩文件类型`
        return extractSpecificFile(srcFile, files, destDir, null, overwrite)
    }

    /**
     * 有密码解压指定文件
     */
    fun extractSpecificFile(
        srcFile: String,
        files: List<String>,
        destDir: String,
        password: String?,
        overwrite: Boolean = false
    ): Boolean {
        return when (getFileType(File(srcFile))) {
            FileType.ZIP -> {
                extractSpecificZipFile(srcFile, files, destDir, password, overwrite)
            }

            FileType._7z -> {
                extractSpecificFileBy7z(srcFile, files, destDir, password, overwrite)
            }

            FileType.RAR -> {
                extractSpecificFileBy7z(srcFile, files, destDir, password, overwrite)
            }

            else -> {
                Log.e(TAG, "不支持的文件类型")
                false
            }
        }
    }

    // 读取压缩文件列表
    fun listInArchiveFiles(srcFile: String): List<String> {
        return when (getFileType(File(srcFile))) {
            FileType.ZIP -> {
                listZipFiles(srcFile)
            }

            FileType._7z -> {
                listInArchiveFilesBy7z(srcFile)
            }

            FileType.RAR -> {
                listInArchiveFilesBy7z(srcFile)
            }

            else -> {
                Log.e(TAG, "不支持的文件类型")
                emptyList()
            }
        }
    }

    // 读取指定文件流
    fun getArchiveItemInputStream(
        archivePath: String,
        itemName: String,
        password: String?
    ): InputStream? {
        return when (getFileType(File(archivePath))) {
            FileType.ZIP -> {
                getZipFileInputStream(archivePath, itemName, password)
            }

            FileType._7z -> {
                getArchiveItemInputStreamBy7z(archivePath, itemName, password)
            }

            FileType.RAR -> {
                getArchiveItemInputStreamBy7z(archivePath, itemName, password)
            }

            else -> {
                Log.e(TAG, "不支持的文件类型")
                null
            }
        }
    }

    // 不带密码读取指定文件流
    fun getArchiveItemInputStream(archivePath: String, itemName: String): InputStream? {
        return getArchiveItemInputStream(archivePath, itemName, null)
    }


    /**
     * 通过zip4j解压zip
     * 支持的文件格式: ar, arj, cpio, dump, tar, zip
     *
     * @param srcFile  需要解压的文件位置
     * @param destDir  解压的目标位置
     * @param password 密码
     * @param charset  编码格式
     */
    private fun extractZip(
        srcFile: String,
        destDir: String,
        password: String?,
        overwrite: Boolean
    ): Boolean {
        Log.i(TAG, "解压文件: $srcFile---$destDir---$password")
        try {
            if (File(destDir).exists() && !overwrite) return true
            var zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("UTF-8")

            if (isRandomCode(zipFile.fileHeaders)) {
                zipFile.close()
                zipFile = ZipFile(srcFile)
                zipFile.charset = Charset.forName("GBK")
            }
            password?.let {
                zipFile.setPassword(it.toCharArray())
            }
            zipFile.isRunInThread = true
            zipFile.extractAll(destDir)
            val progressMonitor = zipFile.progressMonitor
            while (true) {
                Log.i(TAG, "Progress: ${progressMonitor.percentDone}")
                progressUpdateListener?.onProgressUpdate("${progressMonitor.percentDone}%")
                Thread.sleep(100)
                if (progressMonitor.result == ProgressMonitor.Result.SUCCESS) {
                    return true
                }
                if (progressMonitor.result == ProgressMonitor.Result.ERROR) {
                    return false
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
            LogTools.logRecord("解压失败: $e")
            return false
        }

    }

    // 通过zip4j列出zip文件内容
    private fun listZipFiles(srcFile: String): List<String> {

        var zipFile = ZipFile(srcFile)
        zipFile.charset = Charset.forName("UTF-8")
        if (isRandomCode(zipFile.fileHeaders)) {
            zipFile.close()
            zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("GBK")
        }
        return zipFile.fileHeaders.map { it.fileName }
    }

    // 通过zip4j解压zip文件中的指定文件
    private fun extractSpecificZipFile(
        srcFile: String,
        files: List<String>,
        destDir: String,
        password: String?,
        overwrite: Boolean
    ): Boolean {
        try {
            if (files.map { File(destDir, it) }.all { it.exists() } && !overwrite) return true
            var zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("UTF-8")

            if (isRandomCode(zipFile.fileHeaders)) {
                zipFile.close()
                zipFile = ZipFile(srcFile)
                zipFile.charset = Charset.forName("GBK")
            }
            password?.let {
                zipFile.setPassword(it.toCharArray())
            }
            for (file in files) {
                zipFile.extractFile(file, destDir)
            }
            return true

        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
            return false
        }

    }

    // 通过zip4j解压zip获取文件流
    private fun getZipFileInputStream(
        srcFile: String,
        fileName: String,
        password: String?
    ): InputStream? {
        var zipFile = ZipFile(srcFile)
        zipFile.charset = Charset.forName("UTF-8")

        if (isRandomCode(zipFile.fileHeaders)) {
            zipFile.close()
            zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("GBK")
        }
        password?.let {
            zipFile.setPassword(it.toCharArray())
        }
        val fileHeader = zipFile.fileHeaders.find { it.fileName == fileName }
        return fileHeader?.let { zipFile.getInputStream(it) }
    }

    // 通过7z列出压缩包内容
    private fun listInArchiveFilesBy7z(srcFile: String): List<String> {
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null
        val list = mutableListOf<String>()
        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            for (i in 0 until inArchive.numberOfItems) {
                var filePath = inArchive.getStringProperty(i, PropID.PATH)
                Log.i(TAG, "filePath原名: $filePath")
                filePath = formatString(filePath)
                Log.i(TAG, "filePath格式化: $filePath")

                list.add(filePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
        return list
    }

    /**
     * 通过7z解压文件
     * 支持的文件格式: 7z, zip, rar(zip中文乱码)
     *
     * @param srcFile  需要解压的文件位置
     * @param destDir  解压的目标位置
     * @param password 密码
     */
    private fun decompressionBy7z(
        srcFile: String,
        destDir: String,
        password: String?,
        overwrite: Boolean
    ): Boolean {
        Log.i(TAG, "解压文件: $srcFile---$destDir---$password")
        if (File(destDir).exists() && !overwrite) return true
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        return try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive =
                SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            inArchive.extract(null, false, ExtractCallback(inArchive, destDir, password ?: ""))
            true
        } catch (
            e: Exception
        ) {
            Log.e(TAG, e.message!!)
            false
        } finally {
            inArchive?.close()
            randomAccessFile?.close()

        }
    }


    // 7z解压回调
    private class ExtractCallback(
        private val inArchive: IInArchive?,
        extractPath: String,
        private val password: String
    ) :
        IArchiveExtractCallback, ICryptoGetTextPassword {

        private var extractPath: String

        init {
            if (!extractPath.endsWith("/") && !extractPath.endsWith("\\")) {
                this.extractPath = extractPath + File.separator
            } else {
                this.extractPath = extractPath
            }
        }

        private var total: Long = 0
        override fun setTotal(total: Long) {
            this.total = total
        }

        override fun setCompleted(complete: Long) {
            val progress = if (total > 0) {
                complete.toDouble() / total.toDouble() * 100
            } else {
                0.0
            }
            progressUpdateListener?.onProgressUpdate("${progress.toInt()}%")
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

        override fun cryptoGetTextPassword(): String {

            // Return the password when needed
            return password
        }
    }

    // 通过7z获取指定文件的输入流
    fun extractSpecificFileBy7z(
        srcFile: String,
        files: List<String>,
        destDir: String,
        password: String?,
        overwrite: Boolean
    ): Boolean {
        if (files.map { File(destDir, it) }.all { it.exists() } && !overwrite) return true
        var extractPath = destDir
        if (!destDir.endsWith("/") && !destDir.endsWith("\\")) {
            extractPath += File.separator
        }
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            inArchive =
                SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            val indices = ArrayList<Int>()
            for (i in 0 until inArchive.numberOfItems) {
                var filePath = inArchive.getStringProperty(i, PropID.PATH)
                filePath = formatString(filePath)
                if (filePath in files) {
                    indices.add(i)
                }
            }
            Log.i(TAG, "indices: $indices")


            inArchive.extract(
                indices.toIntArray(),
                false,
                object : IArchiveExtractCallback, ICryptoGetTextPassword {

                    private var total: Long = 0
                    override fun setTotal(total: Long) {
                        this.total = total
                    }

                    override fun setCompleted(complete: Long) {
                        val progress = if (total > 0) {
                            complete.toDouble() / total.toDouble() * 100
                        } else {
                            0.0
                        }
                        progressUpdateListener?.onProgressUpdate("${progress.toInt()}%")
                    }

                    @Throws(SevenZipException::class)
                    override fun getStream(
                        index: Int,
                        extractAskMode: ExtractAskMode
                    ): ISequentialOutStream {
                        return ISequentialOutStream { data ->
                            try {
                                var filename = inArchive.getStringProperty(index, PropID.PATH)
                                filename = formatString(filename)
                                val file = File(extractPath + filename)
                                file.parentFile?.mkdirs()
                                if (!file.exists()) {
                                    file.createNewFile()
                                }
                                FileOutputStream(file, true).use {
                                    it.write(data)
                                    it.flush()
                                }
                            } catch (e: IOException) {
                                Log.e(TAG, "IOException while extracting")
                            }
                            data.size
                        }
                    }

                    override fun prepareOperation(extractAskMode: ExtractAskMode) {}

                    override fun setOperationResult(extractOperationResult: ExtractOperationResult) {}
                    override fun cryptoGetTextPassword(): String {
                        return password ?: ""
                    }
                })
            return true
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            return false

        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    /**
     * 通过7z获取指定文件的输入流
     */
    private fun getArchiveItemInputStreamBy7z(
        archivePath: String,
        itemName: String,
        password: String?
    ): InputStream? {
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null
        var byteArrayOutputStream: ByteArrayOutputStream? = null

        try {
            randomAccessFile = RandomAccessFile(File(archivePath), "r")
            inArchive =
                SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile), password)

            var itemIndex = -1
            for (i in 0 until inArchive.numberOfItems) {
                val stringProperty = inArchive.getStringProperty(i, PropID.PATH)
                if (formatString(stringProperty) == itemName) {
                    itemIndex = i
                    break
                }
            }

            if (itemIndex == -1) {
                throw SevenZipException("Item not found: $itemName")
            }

            byteArrayOutputStream = ByteArrayOutputStream()
            val outStream: ISequentialOutStream = ISequentialOutStream { data ->
                byteArrayOutputStream.write(data)
                data.size
            }

            inArchive.extractSlow(itemIndex, outStream)
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }

        return byteArrayOutputStream?.toByteArray()?.inputStream()
    }

    // 判断是否加密
    fun isArchiveEncrypted(archiveFile: String): Boolean {
        val randomAccessFile = RandomAccessFile(File(archiveFile), "r")
        val inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
        inArchive.use {
            return inArchive.simpleInterface.archiveItems.any { it.isEncrypted }
        }
    }

    /**
     * 判断是否为压缩包
     */
    fun isArchive(file: String): Boolean {
        if (!File(file).exists()) return false
        if (File(file).isDirectory) return false
        return when (getFileType(File(file))) {
            FileType.ZIP, FileType._7z, FileType.RAR -> true
            else -> false
        }
    }

    // 判断文件头是否为乱码
    private fun isRandomCode(fileHeaders: List<FileHeader>): Boolean {
        for (element in fileHeaders) {
            val fileHeader: FileHeader = element
            val canEnCode = Charset.forName("GBK").newEncoder().canEncode(fileHeader.fileName)
            if (!canEnCode) { //canEnCode为true，表示不是乱码。false.表示乱码。是乱码则需要重新设置编码格式
                return true
            }
        }
        return false
    }

    // 格式化字乱码符串,目前没有乱用
    fun formatString(fileHeader: String): String {
        val encodings = listOf("GBK", "UTF-8", "GB2312", "Big5", "ISO-8859-1")

        for (encoding in encodings) {
            if (Charset.forName(encoding).newEncoder().canEncode(fileHeader)) {
                return fileHeader
            } else {
                val bytes = fileHeader.toByteArray(Charset.forName("CP437"))
                    .toString(Charset.forName(encoding))
                if (Charset.forName(encoding).newEncoder().canEncode(bytes)) {
                    return bytes
                }
            }
        }

        // 如果所有的编码都无法将字符串解码为可视化字符串，返回原始字符串
        return fileHeader
    }


    /**
     * 判断压缩包类型
     */
    private fun getFileType(file: File): FileType {

        // 判断是否为apk文件,无视其直接返回
        if (file.extension.equals("apk", ignoreCase = true)) {
            return FileType.UNKNOWN
        }

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
