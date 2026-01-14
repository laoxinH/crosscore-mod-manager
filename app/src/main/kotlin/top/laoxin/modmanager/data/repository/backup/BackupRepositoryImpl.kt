package top.laoxin.modmanager.data.repository.backup

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.repository.BackupRepository

class BackupRepositoryImpl
@Inject
constructor(
        database: ModManagerDatabase,
//    private val permissionTools: PermissionTools,
//    private val fileToolsManager: FileToolsManager,
//    private val appPathsManager: AppPathsManager,

) : BackupRepository {

    private val backupDao = database.backupDao()

    override suspend fun getByModPath(modPath: String): Flow<List<BackupBean>> {
        // return backupDao.getByModPath(modPath)
        return backupDao.getByModId(1)
    }

    override suspend fun insertAll(backups: List<BackupBean>) {
        backupDao.insertAll(backups)
    }

    override fun deleteAllBackups() {
        backupDao.deleteAll()
    }

    override suspend fun deleteByGamePackageName(gamePackageName: String) {
        backupDao.deleteByGamePackageName(gamePackageName)
    }

    override fun getByModNameAndGamePackageName(
            modName: String,
            gamePackageName: String
    ): Flow<List<BackupBean>> {
        // return backupDao.getByModNameAndGamePackageName(modName, gamePackageName)
        return backupDao.getByModId(1)
    }

    override suspend fun backupModOriginalFiles(
            modBean: ModBean,
            onProgress: suspend (progress: Int, total: Int) -> Unit
    ): List<BackupBean> {
        //        val gameModPath = modBean.gameModPath!!
        //        val list: MutableList<BackupBean> = mutableListOf()
        //        // 通过ZipTools解压文件到modTempPath
        //        val checkPermission = permissionTools.checkPermission(gameModPath)
        //        val fileTools = fileToolsManager.getFileTools(checkPermission)
        //        val backups = backupDao.getByModNameAndGamePackageName(
        //            modBean.name!!,
        //            modBean.gamePackageName!!
        //        ).first()
        //        modBean.modFiles?.forEachIndexed { index: Int, it: String ->
        //            val file = File(it)
        //            val backupPath = spliceBackupPath(modBean, file.name)
        //                //appPathsManager.getBackupPath() + gameBackupPath +
        // File(modBean.gameModPath!!).name + "/" + file.name
        //            val gameFilePath = gameModPath + file.name
        //            if (!File(backupPath).exists()) {
        //                if (if (checkPermission == PathType.DOCUMENT) {
        //                        fileTools?.copyFileByDF(gameFilePath, backupPath) == true
        //                    } else {
        //                        fileTools?.copyFile(gameFilePath, backupPath) == true
        //                    } && fileTools?.isFileExist(backupPath) == true
        //                ) {
        //                    onProgress(index + 1, modBean.modFiles.size)
        //
        //                }
        //            }
        //            if (backups.isEmpty()) {
        //                list.add(
        //                    BackupBean(
        //                        id = 0,
        //                        filename = file.name,
        //                        gamePath = modBean.gameModPath,
        //                        gameFilePath = gameFilePath,
        //                        backupPath = backupPath,
        //                        gamePackageName = modBean.gamePackageName,
        //                        modName = modBean.name
        //                    )
        //                )
        //            } else {
        //                val backup = backups.find { it.filename == file.name }
        //                if (backup == null) {
        //                    list.add(
        //                        BackupBean(
        //                            id = 0,
        //                            filename = file.name,
        //                            gamePath = modBean.gameModPath,
        //                            gameFilePath = gameFilePath,
        //                            backupPath = backupPath,
        //                            gamePackageName = modBean.gamePackageName,
        //                            modName = modBean.name
        //                        )
        //                    )
        //                }
        //            }
        //        }
        return emptyList()
    }

    override suspend fun getByModIdSync(id: Int): List<BackupBean> {
        return backupDao.getByModIdSync(id)
    }

    override suspend fun deleteByModId(id: Int) {
        backupDao.deleteByModId(id)
    }

    override suspend fun insert(backupBean: BackupBean) {
        backupDao.insert(backupBean)
    }

    override suspend fun countByBackupPath(backupPath: String): Int {
        return backupDao.countByBackupPath(backupPath)
    }

    // 拼接备份路径
    private fun spliceBackupPath(mod: ModBean, filename: String): String {
        // return appPathsManager.getBackupPath() + mod.gamePackageName + "/" +
        // File(mod.gameModPath!!).name + "/" + filename
        return ""
    }
}
