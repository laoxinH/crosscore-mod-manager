package top.laoxin.modmanager.domain.usercase.file

import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.FileBrowserRepository
import top.laoxin.modmanager.domain.repository.ModRepository

class GetPathFilesUseCase @Inject constructor(
    private val repository: FileBrowserRepository,
    private val modRepository: ModRepository
) {
    operator fun invoke(path: String, searchQuery: String? = null): Flow<List<File>> = flow {
        repository.getFiles(path, searchQuery).onSuccess {files->
            /*files.filter { file ->
                allMods.any{
                    it.path == file.absolutePath || it.virtualPaths?.contains(file.path) == true
                }

            }*/
            emit(files)
        }.onError {
            // In case of error, emit empty list or handle it. 
            // For now, consistent with previous behavior, emit empty list.
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}
