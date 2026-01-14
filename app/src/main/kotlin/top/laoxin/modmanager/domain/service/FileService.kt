package top.laoxin.modmanager.domain.service

import java.io.File
import java.io.InputStream
import top.laoxin.modmanager.domain.model.Result

/** 文件服务接口 Domain 层定义，隐藏底层文件访问实现细节（File/DocumentFile/Shizuku） 所有操作返回 Result 类型以支持统一的错误处理 */
interface FileService {

        /**
         * 复制文件
         * @param srcPath 源文件路径
         * @param destPath 目标文件路径
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun copyFile(srcPath: String, destPath: String): Result<Unit>

        /**
         * 删除文件
         * @param path 文件路径
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun deleteFile(path: String): Result<Unit>

        /**
         * 移动文件
         * @param srcPath 源文件路径
         * @param destPath 目标文件路径
         * @param overwrite 是否覆盖已存在的目标文件
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun moveFile(
                srcPath: String,
                destPath: String,
                overwrite: Boolean = false
        ): Result<Unit>

        /**
         * 判断文件是否存在
         * @param path 文件路径
         * @return Result<Boolean> 成功返回是否存在，失败返回 FileError
         */
        suspend fun isFileExist(path: String): Result<Boolean>

        /**
         * 判断是否为文件（非目录）
         * @param path 路径
         * @return Result<Boolean> 成功返回是否为文件，失败返回 FileError
         */
        suspend fun isFile(path: String): Result<Boolean>

        /**
         * 获取目录下的文件名列表
         * @param path 目录路径
         * @return Result<List<String>> 成功返回文件名列表，失败返回 FileError
         */
        suspend fun getFileNames(path: String): Result<List<String>>

        /**
         * 列出目录下的所有文件
         * @param path 目录路径
         * @return Result<List<File>> 成功返回文件列表，失败返回 FileError
         */
        suspend fun listFiles(path: String): Result<List<File>>

        /**
         * 读取文件内容
         * @param path 文件路径
         * @return Result<String> 成功返回文件内容，失败返回 FileError
         */
        suspend fun readFile(path: String): Result<String>

        /**
         * 写入文件
         * @param path 目录路径
         * @param filename 文件名
         * @param content 内容
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun writeFile(path: String, filename: String, content: String): Result<Unit>

        /**
         * 通过输入流创建文件
         * @param path 目录路径
         * @param filename 文件名
         * @param inputStream 输入流
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun createFileByStream(
                path: String,
                filename: String,
                inputStream: InputStream
        ): Result<Unit>

        /**
         * 创建目录
         * @param path 目录路径
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun createDirectory(path: String): Result<Unit>

        /**
         * 重命名目录
         * @param path 目录路径
         * @param newName 新名称
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */
        suspend fun renameDirectory(path: String, newName: String): Result<Unit>

        /**
         * 获取文件最后修改时间
         * @param path 文件路径
         * @return Result<Long> 成功返回时间戳，失败返回 FileError
         */
        suspend fun getLastModified(path: String): Result<Long>

        /**
         * 递归列出目录下的所有文件（包含子目录中的文件）
         * @param path 目录路径
         * @param extensions 可选，文件扩展名过滤（如 ["zip", "rar", "7z"]）
         * @return Result<List<File>> 成功返回文件列表，失败返回 FileError
         */
        suspend fun listFilesRecursively(
                path: String,
                extensions: Set<String>? = null
        ): Result<List<File>>

        /**
         * 列出目录下的第一层子目录
         * @param path 目录路径
         * @return Result<List<File>> 成功返回目录列表，失败返回 FileError
         */
        suspend fun listFirstLevelDirectories(path: String): Result<List<File>>


        /**
         * 计算文件 MD5 值
         * @param path 文件路径
         * @return Result<String> 成功返回 MD5 哈希字符串，失败返回 FileError
         */
        suspend fun calculateFileMd5(path: String): Result<String>
        
        /**
         * 获取文件名
         * @param filePath 文件路径
         * @return 文件名
         */
        fun getFileName(filePath: String): String
        
        /**
         * 删除目录（递归删除目录及其所有内容）
         * @param path 目录路径
         * @return Result<Unit> 成功返回 Unit，失败返回 FileError
         */

        /**
         * 获取文件长度
         * @param absolutePath 文件绝对路径
         * @return Result<Long> 获取文件长度成功返回长度，失败返回 FileError
         */
    suspend  fun getFileLength(absolutePath: String): Result<Long>

    /**
     * 获取文件名
     */

}
