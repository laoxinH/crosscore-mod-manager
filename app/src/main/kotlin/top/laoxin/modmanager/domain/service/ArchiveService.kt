package top.laoxin.modmanager.domain.service

import top.laoxin.modmanager.domain.model.Result
import java.io.InputStream

interface ArchiveService {

    suspend fun extract(
        srcPath: String,
        destPath: String,
        password: String? = null,
        overwrite: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit>

    suspend fun extractSpecificFiles(
        srcPath: String,
        files: List<String>,
        destPath: String,
        password: String? = null,
        overwrite: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit>

    suspend fun listFiles(archivePath: String): Result<List<String>>

    suspend fun getFileInputStream(
        archivePath: String,
        itemName: String,
        password: String? = null
    ): Result<InputStream>

    suspend fun isArchive(path: String): Result<Boolean>

    suspend fun isEncrypted(archivePath: String): Result<Boolean>

    suspend fun validatePassword(archivePath: String, password: String): Result<Boolean>

    suspend fun clearTempDirectory()


}
