package top.laoxin.modmanager.data.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.laoxin.modmanager.App
import top.laoxin.modmanager.constant.FileAccessType
import top.laoxin.modmanager.data.service.filetools.FileToolsManager
import top.laoxin.modmanager.domain.model.AppError
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.tools.ArchiveUtil
import top.laoxin.modmanager.tools.PasswordErrorException
import java.io.FileNotFoundException

/** FileService 实现 封装 FileToolsManager，根据权限自动选择正确的文件访问方式 所有操作返回 Result 类型以支持统一的错误处理 */
@Singleton
class FileServiceImpl @Inject constructor(
    private val fileToolsManager: FileToolsManager,

) :
    FileService {

    override suspend fun copyFile(srcPath: String, destPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsByPaths(srcPath, destPath)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                val srcAccessType = fileToolsManager.getFileAccessType(srcPath)
                val destAccessType = fileToolsManager.getFileAccessType(destPath)

                val success =
                    when (srcAccessType) {
                        FileAccessType.DOCUMENT_FILE if destAccessType ==
                                FileAccessType.STANDARD_FILE -> {
                            fileTools.copyFileByDF(srcPath, destPath)
                        }

                        FileAccessType.STANDARD_FILE if destAccessType ==
                                FileAccessType.DOCUMENT_FILE -> {
                            fileTools.copyFileByFD(srcPath, destPath)
                        }

                        else -> fileTools.copyFile(srcPath, destPath)
                    }

                if (success) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.CopyFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "复制文件失败"))
            }
        }

    override suspend fun deleteFile(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                if (fileTools.deleteFile(path)) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.DeleteFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Log.e("FileService", "删除文件失败", e)
                Result.Error(AppError.FileError.Unknown(e.message ?: "删除文件失败"))
            }
        }

    override suspend fun moveFile(
        srcPath: String,
        destPath: String,
        overwrite: Boolean
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsByPaths(srcPath, destPath)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                // 如果目标文件存在且需要覆盖，先删除
                if (overwrite && fileTools.isFileExist(destPath)) {
                    deleteFile(destPath)
                }

                if (copyFile(srcPath, destPath).isSuccess) {
                    // 删除源文件
                    deleteFile(srcPath)
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.MoveFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "移动文件失败"))
            }
        }

    override suspend fun isFileExist(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.isFileExist(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "检查文件存在失败"))
            }
        }

    override suspend fun isFile(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.isFile(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "检查是否为文件失败"))
            }
        }

    override suspend fun getFileNames(path: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.getFilesNames(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "获取文件名列表失败"))
            }
        }

    override suspend fun listFiles(path: String): Result<List<File>> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.listFiles(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "列出文件失败"))
            }
        }

    override suspend fun readFile(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.readFile(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.ReadFailed)
            }
        }

    override suspend fun writeFile(
        path: String,
        filename: String,
        content: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                if (fileTools.writeFile(path, filename, content)) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.WriteFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "写入文件失败"))
            }
        }

    override suspend fun createFileByStream(
        path: String,
        filename: String,
        inputStream: InputStream
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                if (fileTools.createFileByStream(path, filename, inputStream)) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.WriteFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "通过流创建文件失败"))
            }
        }

    override suspend fun createDirectory(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                if (fileTools.createDictionary(path)) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.CreateDirectoryFailed)
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "创建目录失败"))
            }
        }

    override suspend fun renameDirectory(path: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                if (fileTools.changDictionaryName(path, newName)) {
                    Result.Success(Unit)
                } else {
                    Result.Error(AppError.FileError.Unknown("重命名目录失败"))
                }
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "重命名目录失败"))
            }
        }

    override suspend fun getLastModified(path: String): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.isFileChanged(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "获取文件修改时间失败"))
            }
        }

    override suspend fun listFilesRecursively(
        path: String,
        extensions: Set<String>?
    ): Result<List<File>> =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) {
                    return@withContext Result.Error(AppError.FileError.FileNotFound(path))
                }

                val files = dir.walkTopDown()
                    .filter { it.isFile }
                    .filter { file ->
                        extensions == null || file.extension.lowercase() in extensions
                    }
                    .toList()

                Result.Success(files)
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "递归列出文件失败"))
            }
        }

    override suspend fun listFirstLevelDirectories(path: String): Result<List<File>> =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) {
                    return@withContext Result.Error(AppError.FileError.FileNotFound(path))
                }

                val directories = dir.listFiles()
                    ?.filter { it.isDirectory }
                    ?.toList() ?: emptyList()

                Result.Success(directories)
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            }catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "列出目录失败"))
            }
        }


    override suspend fun calculateFileMd5(path: String): Result<String> =

        withContext(Dispatchers.IO) {
            try {

                val fileTools =
                    fileToolsManager.getFileToolsBySinglePath(path)
                        ?: return@withContext Result.Error(
                            AppError.FileError.PermissionDenied
                        )

                Result.Success(fileTools.calculateFileMd5(path))
            } catch (e: SecurityException) {
                Result.Error(AppError.FileError.PermissionDenied)
            } catch (e: FileNotFoundException) {
                Result.Error(AppError.FileError.FileNotFound(path))
            }catch (e :IllegalStateException) {
                Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
            } catch (e: Exception) {
                Result.Error(AppError.FileError.Unknown(e.message ?: "读取MOD失败"))
            }
        }

    override fun getFileName(filePath: String): String {
        return File(filePath).name
    }


    override suspend fun getFileLength(absolutePath: String): Result<Long> = withContext(Dispatchers.IO){
        try {

            val fileTools =
                fileToolsManager.getFileToolsBySinglePath(absolutePath)
                    ?: return@withContext Result.Error(
                        AppError.FileError.PermissionDenied
                    )

            Result.Success(fileTools.getFileSize(absolutePath))
        } catch (e: SecurityException) {
            Result.Error(AppError.FileError.PermissionDenied)
        } catch (e: FileNotFoundException) {
            Result.Error(AppError.FileError.FileNotFound(absolutePath))
        } catch (e :IllegalStateException) {
            Result.Error(AppError.FileError.ShizukuDisconnected(e.message ?: "复制文件失败"))
        } catch (e: Exception) {
            Result.Error(AppError.FileError.Unknown(e.message ?: "读取MOD失败"))
        }
    }

}
