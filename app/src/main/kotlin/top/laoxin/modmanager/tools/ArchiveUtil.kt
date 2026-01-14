package top.laoxin.modmanager.tools

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
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
import top.laoxin.modmanager.constant.ArchiveFileType

object ArchiveUtil {
    const val TAG = "ArchiveUtil"
    // var progressUpdateListener: ProgressUpdateListener? = null

    // 带密码解压
    fun decompression(
            srcFile: String,
            destDir: String,
            password: String?,
            overwrite: Boolean = false,
            onProgress: (progress: Int) -> Unit
    ): Boolean {
        return when (getFileType(File(srcFile))) {
            ArchiveFileType.ZIP -> {
                // 有密码时使用 7z 库处理（解决中文密码兼容性问题）
                if (!password.isNullOrEmpty()) {
                    decompressionBy7z(srcFile, destDir, password, overwrite, onProgress)
                } else {
                    extractZip(srcFile, destDir, password, overwrite, onProgress)
                }
            }
            ArchiveFileType._7z, ArchiveFileType.RAR -> {
                decompressionBy7z(srcFile, destDir, password, overwrite, onProgress)
            }
            else -> false
        }
    }

    // 无密码解压指定文件
    fun extractSpecificFile(
            srcFile: String,
            files: List<String>,
            destDir: String,
            overwrite: Boolean = false,
            onProgress: (progress: Int) -> Unit
    ): Boolean {
        // 判断压缩文件类型`
        return extractSpecificFile(srcFile, files, destDir, null, overwrite, onProgress)
    }

    // 有密码解压指定文件
    fun extractSpecificFile(
            srcFile: String,
            files: List<String>,
            destDir: String,
            password: String?,
            overwrite: Boolean = false,
            onProgress: (progress: Int) -> Unit
    ): Boolean {
        return when (getFileType(File(srcFile))) {
            ArchiveFileType.ZIP -> {
                // 有密码时使用 7z 库处理（解决中文密码兼容性问题）
                if (!password.isNullOrEmpty()) {
                    extractSpecificFileBy7z(srcFile, files, destDir, password, overwrite, onProgress)
                } else {
                    extractSpecificZipFile(srcFile, files, destDir, password, overwrite, onProgress)
                }
            }
            ArchiveFileType._7z, ArchiveFileType.RAR -> {
                extractSpecificFileBy7z(srcFile, files, destDir, password, overwrite, onProgress)
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
            ArchiveFileType.ZIP -> {
                listZipFiles(srcFile)
            }
            ArchiveFileType._7z -> {
                listInArchiveFilesBy7z(srcFile)
            }
            ArchiveFileType.RAR -> {
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
            ArchiveFileType.ZIP -> {
                // 有密码时使用 7z 库处理（解决中文密码兼容性问题）
                if (!password.isNullOrEmpty()) {
                    getArchiveItemInputStreamBy7z(archivePath, itemName, password)
                } else {
                    getZipFileInputStream(archivePath, itemName, password)
                }
            }
            ArchiveFileType._7z -> {
                getArchiveItemInputStreamBy7z(archivePath, itemName, password)
            }
            ArchiveFileType.RAR -> {
                getArchiveItemInputStreamBy7z(archivePath, itemName, password)
            }
            else -> {
                Log.e(TAG, "不支持的文件类型")
                null
            }
        }
    }

    /**
     * 通过zip4j解压zip 支持的文件格式: ar, arj, cpio, dump, tar, zip
     *
     * @param srcFile 需要解压的文件位置
     * @param destDir 解压的目标位置
     * @param password 密码
     * @param charset 编码格式
     */
    private fun extractZip(
            srcFile: String,
            destDir: String,
            password: String?,
            overwrite: Boolean,
            onProgress: (progress: Int) -> Unit
    ): Boolean {
        Log.i(TAG, "解压文件: $srcFile---$destDir---$password")

        if (File(destDir).exists() && !overwrite) return true
        var zipFile = ZipFile(srcFile)
        zipFile.charset = Charset.forName("UTF-8")

        if (isRandomCode(zipFile.fileHeaders)) {
            zipFile.close()
            zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("GBK")
        }

        // 尝试多种密码编码（解决中文密码兼容性问题）
        val passwordToUse = findWorkingPassword(zipFile, password)
        passwordToUse?.let { zipFile.setPassword(it.toCharArray()) }

        // 尝试检查是否需要密码但没提供有效密码
        if (zipFile.isEncrypted && (passwordToUse.isNullOrEmpty() || !validateZipPassword(zipFile))) {
            return false
        }

        zipFile.isRunInThread = true
        zipFile.extractAll(destDir)
        val progressMonitor = zipFile.progressMonitor
        while (true) {
            Log.i(TAG, "Progress: ${progressMonitor.percentDone}")
            onProgress(progressMonitor.percentDone)
            Thread.sleep(100)
            if (progressMonitor.result == ProgressMonitor.Result.SUCCESS) {
                return true
            }
            if (progressMonitor.result == ProgressMonitor.Result.ERROR) {
                if (progressMonitor.exception?.message?.contains("password", ignoreCase = true) == true) {
                    Log.e(TAG, "解压失败: 密码错误 - ${progressMonitor.exception?.message}")
                } else {
                    Log.e(TAG, "解压失败: ${progressMonitor.exception?.message}")
                }
                return false
            }
        }
    }

    // 验证ZIP密码是否正确
    private fun validateZipPassword(zipFile: ZipFile): Boolean {

        if (!zipFile.isEncrypted) return true

        // 尝试获取第一个文件头，这会在密码错误时抛出异常
        val fileHeaders = zipFile.fileHeaders
        if (fileHeaders.isNotEmpty()) {
            val firstHeader = fileHeaders[0]
            if (firstHeader.isEncrypted) {
                // 尝试用提供的密码打开流，如果密码错误会抛出异常
                val inputStream = zipFile.getInputStream(firstHeader)
                inputStream.close()
            }
            return true
        } else {

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
            overwrite: Boolean,
            onProgress: (progress: Int) -> Unit
    ): Boolean {

        if (files.map { File(destDir, it) }.all { it.exists() } && !overwrite) return true
        var zipFile = ZipFile(srcFile)
        zipFile.charset = Charset.forName("UTF-8")

        if (isRandomCode(zipFile.fileHeaders)) {
            zipFile.close()
            zipFile = ZipFile(srcFile)
            zipFile.charset = Charset.forName("GBK")
        }
        Log.d(TAG, "extractSpecificZipFile: ${zipFile.fileHeaders.map { it.fileName }}")
        
        // 尝试多种密码编码
        val passwordToUse = findWorkingPassword(zipFile, password)
        passwordToUse?.let { zipFile.setPassword(it.toCharArray()) }
        
        for (file in files) {
            zipFile.extractFile(file, destDir)
        }
        return true
    }

    /**
     * 查找有效的密码编码
     * 针对中文密码在不同压缩软件间的兼容性问题
     */
    private fun findWorkingPassword(zipFile: ZipFile, password: String?): String? {
        if (password.isNullOrEmpty()) return password
        if (!zipFile.isEncrypted) return password
        
        // 尝试多种编码
        val passwordVariants = listOfNotNull(
            password,
            try {
                String(password.toByteArray(Charset.forName("GBK")), Charset.forName("ISO-8859-1"))
            } catch (e: Exception) {
                null
            }
        ).distinct()
        
        for (pwd in passwordVariants) {
            try {
                zipFile.setPassword(pwd.toCharArray())
                // 尝试获取第一个加密文件的流来验证密码
                val encryptedHeader = zipFile.fileHeaders.firstOrNull { it.isEncrypted }
                if (encryptedHeader != null) {
                    zipFile.getInputStream(encryptedHeader).use { it.read() }
                    return pwd  // 密码正确
                }
            } catch (e: Exception) {
                Log.d(TAG, "Password variant failed: ${e.message}")
            }
        }
        
        return password  // 回退到原始密码
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
        
        val fileHeader = zipFile.fileHeaders.find { it.fileName == fileName } ?: return null
        
        // 如果文件未加密或没有密码，直接返回
        if (!fileHeader.isEncrypted || password.isNullOrEmpty()) {
            password?.let { zipFile.setPassword(it.toCharArray()) }
            return zipFile.getInputStream(fileHeader)
        }
        
        // 尝试多种密码编码方式（解决中文密码兼容性问题）
        return tryPasswordWithEncodings(zipFile, fileHeader, password)
    }

    /**
     * 尝试多种编码方式设置密码
     * 不同压缩软件对中文密码使用不同编码（UTF-8、GBK等）
     */
    private fun tryPasswordWithEncodings(
            zipFile: ZipFile, 
            fileHeader: FileHeader,
            password: String
    ): InputStream? {
        // 编码尝试顺序：原始 -> UTF-8 显式转换 -> GBK 转换
        val passwordVariants = listOf(
            password,  // 原始密码
            password.toByteArray(Charset.forName("UTF-8")).toString(Charset.forName("UTF-8")),
            // 尝试 GBK 编码的密码（某些 Windows 压缩软件使用）
            try { 
                String(password.toByteArray(Charset.forName("GBK")), Charset.forName("ISO-8859-1"))
            } catch (e: Exception) { 
                null 
            }
        ).filterNotNull().distinct()

        for (pwd in passwordVariants) {
            try {
                zipFile.setPassword(pwd.toCharArray())
                val inputStream = zipFile.getInputStream(fileHeader)
                // 尝试读取一点数据来验证密码是否正确
                if (inputStream.available() >= 0) {
                    return inputStream
                }
            } catch (e: Exception) {
                Log.d(TAG, "Password encoding variant failed: ${e.message}")
                // 继续尝试下一个编码
            }
        }
        
        // 所有尝试都失败，使用原始密码再试一次（让异常抛出）
        zipFile.setPassword(password.toCharArray())
        return zipFile.getInputStream(fileHeader)
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
               // Log.i(TAG, "filePath原名: $filePath")
                filePath = formatString(filePath)
                //Log.i(TAG, "filePath格式化: $filePath")

                list.add(filePath)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
        return list
    }

    /**
     * 通过7z解压文件 支持的文件格式: 7z, zip, rar(zip中文乱码)
     *
     * @param srcFile 需要解压的文件位置
     * @param destDir 解压的目标位置
     * @param password 密码
     */
    private fun decompressionBy7z(
            srcFile: String,
            destDir: String,
            password: String?,
            overwrite: Boolean,
            onProgress: (progress: Int) -> Unit
    ): Boolean {
        Log.i(TAG, "解压文件: $srcFile---$destDir---$password")
        if (File(destDir).exists() && !overwrite) return true
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        return try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")

            // 先验证密码是否正确
            if (!validate7zPassword(srcFile, password)) {
                throw PasswordErrorException("Password Error, unable to extract")
            }

            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            inArchive.extract(
                    null,
                    false,
                    ExtractCallback(inArchive, destDir, password ?: "", onProgress)
            )

            // 验证解压后的文件是否存在（至少有一个文件）
            val extractedFiles = File(destDir).listFiles()
            if (extractedFiles == null || extractedFiles.isEmpty()) {
                throw PasswordErrorException("Password Error, unable to extract")
            }
            true
        } catch (e: Exception) {
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    /** 验证7z压缩包密码是否正确 */
    fun validate7zPassword(srcFile: String, password: String?): Boolean {
        if (password.isNullOrEmpty()) {
            // 如果文件需要密码但未提供，则返回false
            if (is7zEncrypted(srcFile)) {
                return false
            }
            // 如果文件不需要密码，则返回true
            return true
        }

        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(File(srcFile), "r")
            // 尝试使用密码打开档案
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            // 获取第一个文件索引
            if (inArchive.numberOfItems > 0) {
                // 创建一个测试解压缓冲区
                val testBuffer = ByteArrayOutputStream()
                val outStream = ISequentialOutStream { data ->
                    testBuffer.write(data)
                    data.size
                }

                // 尝试提取第一个加密项目
                for (i in 0 until inArchive.numberOfItems) {
                    // 使用正确的API获取加密状态
                    if (inArchive.getProperty(i, PropID.ENCRYPTED).toString().toBoolean()) {
                        try {
                            // 使用ICryptoGetTextPassword接口提供密码
                            val passwordProvider =
                                    object : ICryptoGetTextPassword {
                                        override fun cryptoGetTextPassword(): String = password
                                    }
                            inArchive.extractSlow(
                                    i,
                                    outStream,
                                    passwordProvider.cryptoGetTextPassword()
                            )

                            // 如果能成功解压，则密码正确
                            return true
                        } catch (e: Exception) {
                            // 如果解压失败，则密码错误
                            if (e.message?.contains("password", ignoreCase = true) == true ||
                                            e.message?.contains(
                                                    "Wrong Password",
                                                    ignoreCase = true
                                            ) == true
                            ) {
                                return false
                            }
                            // 如果是其他错误，继续尝试下一个文件
                        }
                    }
                }
                // 如果没有找到加密项或所有项都成功解压，则密码正确或不需要密码
                return true
            }

            // 如果档案为空，则认为不需要密码
            return true
        } catch (e: Exception) {
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    /** 检查7z文件是否加密 */
    fun is7zEncrypted(archiveFile: String): Boolean {
        var inArchive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            randomAccessFile = RandomAccessFile(File(archiveFile), "r")
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            // 检查是否有任何加密的项目
            for (i in 0 until inArchive.numberOfItems) {
                // 使用正确的API获取加密状态
                val encrypted = inArchive.getProperty(i, PropID.ENCRYPTED).toString().toBoolean()
                if (encrypted) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            // 如果出现与密码相关的错误，可能是因为文件加密且需要密码
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    // 判断是否加密
    fun isArchiveEncrypted(archiveFile: String): Boolean {
        var randomAccessFile: RandomAccessFile? = null
        var inArchive: IInArchive? = null

        try {
            randomAccessFile = RandomAccessFile(File(archiveFile), "r")
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

            // 检查是否有任何加密的项目
            for (i in 0 until inArchive.numberOfItems) {
                val encrypted = inArchive.getProperty(i, PropID.ENCRYPTED).toString().toBoolean()
                if (encrypted) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    // 7z解压回调
    private class ExtractCallback(
            private val inArchive: IInArchive?,
            extractPath: String,
            private val password: String,
            private val onProgress: (progress: Int) -> Unit
    ) : IArchiveExtractCallback, ICryptoGetTextPassword {

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
            val progress =
                    if (total > 0) {
                        complete.toDouble() / total.toDouble() * 100
                    } else {
                        0.0
                    }

            // progressUpdateListener?.onProgressUpdate("${progress.toInt()}%")
            onProgress(progress.toInt())
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
                    FileOutputStream(path, true).use { it.write(data) }
                } catch (_: IOException) {
                    Log.e(TAG, "IOException while extracting $filePath")
                }
                data.size
            }
        }

        override fun prepareOperation(extractAskMode: ExtractAskMode) {}

        override fun setOperationResult(extractOperationResult: ExtractOperationResult) {}

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
            overwrite: Boolean,
            onProgress: (progress: Int) -> Unit
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
            inArchive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))

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
                            val progress =
                                    if (total > 0) {
                                        complete.toDouble() / total.toDouble() * 100
                                    } else {
                                        0.0
                                    }
                            // progressUpdateListener?.onProgressUpdate("${progress.toInt()}%")
                            onProgress(progress.toInt())
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
                                } catch (_: IOException) {
                                    Log.e(TAG, "IOException while extracting")
                                }
                                data.size
                            }
                        }

                        override fun prepareOperation(extractAskMode: ExtractAskMode) {}

                        override fun setOperationResult(
                                extractOperationResult: ExtractOperationResult
                        ) {}
                        override fun cryptoGetTextPassword(): String {
                            return password ?: ""
                        }
                    }
            )
            return true
        } catch (e: Exception) {
            throw e
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }
    }

    /** 通过7z获取指定文件的输入流 */
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
                    SevenZip.openInArchive(
                            null,
                            RandomAccessFileInStream(randomAccessFile),
                            password
                    )

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
            val outStream = ISequentialOutStream { data ->
                byteArrayOutputStream.write(data)
                data.size
            }

            // 使用带密码的 extractSlow 重载方法
            if (!password.isNullOrEmpty()) {
                inArchive.extractSlow(itemIndex, outStream, password)
            } else {
                inArchive.extractSlow(itemIndex, outStream)
            }
        } finally {
            inArchive?.close()
            randomAccessFile?.close()
        }

        return byteArrayOutputStream.toByteArray().inputStream()
    }

    /** 判断是否为压缩包 */
    fun isArchive(file: String): Boolean {
        if (!File(file).exists()) return false
        if (File(file).isDirectory) return false
        return when (getFileType(File(file))) {
            ArchiveFileType.ZIP, ArchiveFileType._7z, ArchiveFileType.RAR -> true
            else -> false
        }
    }

    // 判断文件头是否为乱码
    private fun isRandomCode(fileHeaders: List<FileHeader>): Boolean {
        for (element in fileHeaders) {
            val fileHeader: FileHeader = element
            val canEnCode = Charset.forName("GBK").newEncoder().canEncode(fileHeader.fileName)
            if (!canEnCode) { // canEnCode为true，表示不是乱码。false.表示乱码。是乱码则需要重新设置编码格式
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
                val bytes =
                        fileHeader
                                .toByteArray(Charset.forName("CP437"))
                                .toString(Charset.forName(encoding))
                if (Charset.forName(encoding).newEncoder().canEncode(bytes)) {
                    return bytes
                }
            }
        }

        // 如果所有的编码都无法将字符串解码为可视化字符串，返回原始字符串
        return fileHeader
    }

    /** 判断压缩包类型 */
    fun getFileType(file: File): ArchiveFileType {

        // 判断是否为apk文件,无视其直接返回
        if (file.extension.equals("apk", ignoreCase = true)) {
            return ArchiveFileType.UNKNOWN
        }

        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(file)
            val head = ByteArray(4)
            if (-1 == inputStream.read(head)) {
                return ArchiveFileType.UNKNOWN
            }
            var headHex = 0
            for (b in head) {
                headHex = headHex shl 8
                headHex = headHex or b.toInt()
            }
            return when (headHex) {
                0x504B0304 -> ArchiveFileType.ZIP
                -0x51 -> ArchiveFileType._7z
                0x52617221 -> ArchiveFileType.RAR
                else -> ArchiveFileType.UNKNOWN
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
        return ArchiveFileType.UNKNOWN
    }
}

// 实现PasswordErrorException
class PasswordErrorException(message: String) : Exception(message)
