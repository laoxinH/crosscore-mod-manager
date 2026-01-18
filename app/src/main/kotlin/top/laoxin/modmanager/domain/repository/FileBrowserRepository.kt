package top.laoxin.modmanager.domain.repository

import java.io.File
import top.laoxin.modmanager.domain.model.Result

interface FileBrowserRepository {
    /**
     * Get files for a specific path.
     * Supports standard directories and ZIP archives.
     */
    suspend fun getFiles(path: String, searchQuery: String? = null): Result<List<File>>
}
