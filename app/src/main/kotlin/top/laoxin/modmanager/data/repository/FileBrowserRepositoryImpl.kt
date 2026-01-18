package top.laoxin.modmanager.data.repository

import android.util.Log
import top.laoxin.modmanager.domain.model.AppError
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import top.laoxin.modmanager.domain.model.Result
import top.laoxin.modmanager.domain.repository.FileBrowserRepository
import top.laoxin.modmanager.domain.service.ArchiveService

@Singleton
class FileBrowserRepositoryImpl @Inject constructor(
    private val archiveService: ArchiveService
) : FileBrowserRepository {

    companion object {
        private const val TAG = "FileBrowserRepository"
    }

    // Cache for ZIP files to avoid re-reading significantly
    private var currentZipPath = ""
    private var currentArchiveFiles = emptyList<File>()

    override suspend fun getFiles(path: String, searchQuery: String?): Result<List<File>> {
        return try {
            val file = File(path)
            var filesToShow: List<File> = emptyList()

            if (file.isDirectory) {
                // Normal Directory
                filesToShow = file.listFiles()
                    ?.toList()
                    ?.sortedWith(
                        compareBy<File> { !it.isDirectory }
                            .thenByDescending { it.lastModified() }
                            .thenBy { it.name }
                    )
                    ?: emptyList()

                /*filesToShow.filter {
                    if (file.isDirectory) return@filter true
                    // 2. Check if it is a supported archive
                    archiveService.isArchive(file.path).getOrDefault(false)

                }*/



            } else {
                // Inside Zip / Virtual Path logic
                // Try to find if path is inside a zip or reuse cache
                if (currentZipPath.isNotEmpty() && path.contains(currentZipPath)) {
                     if (currentArchiveFiles.isEmpty()) {
                        getArchiveFiles(currentZipPath).onSuccess { 
                            // method getArchiveFiles updates currentArchiveFiles as side effect or returns it?
                            // let's make it return it and update cache.
                        }
                    }
                    filesToShow = currentArchiveFiles.filter { it.parent == path }
                } else {
                     val zipParent = findZipParent(path)
                     if (zipParent != null) {
                         currentZipPath = zipParent
                         getArchiveFiles(currentZipPath)
                         filesToShow = currentArchiveFiles.filter { it.parent == path }
                     } else {
                         filesToShow = emptyList()
                     }
                }
            }

            if (!searchQuery.isNullOrEmpty()) {
                filesToShow = filesToShow.filter {
                    it.name.lowercase().contains(searchQuery.lowercase())
                }
            }
            
            Result.Success(filesToShow)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting files for path: $path", e)
            Result.Error(AppError.FileError.Unknown(e.message ?: "Unknown error"))
        }
    }

    private fun findZipParent(path: String): String? {
        var current = File(path)
        while (current.parent != null) {
            if (current.exists() && current.isFile) {
                return current.path
            }
            current = current.parentFile ?: return null
        }
        return null
    }

    private suspend fun getArchiveFiles(path: String): Result<List<File>> {
        return archiveService.listFiles(path).map { fileHeaders ->
             val files = fileHeaders.map { File("$path/$it") }
             currentArchiveFiles = files
             files
        }
    }
}
