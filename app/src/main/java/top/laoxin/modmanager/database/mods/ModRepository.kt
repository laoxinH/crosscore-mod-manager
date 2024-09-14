package top.laoxin.modmanager.database.mods

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ModBean

interface ModRepository {

    // 插入数据
    suspend fun insertMod(mod: ModBean)

    // 更新数据
    suspend fun updateMod(mod: ModBean)

    // 删除数据
    suspend fun deleteMod(mod: ModBean)

    // 获取所有数据
    fun getAllIModsStream(): Flow<List<ModBean>>

    // 通过gamePackageName和名字模糊查询
    fun search(name: String, gamePackageName: String): Flow<List<ModBean>>

    // 通过List<ModBean>插入数据
    suspend fun insertAll(mods: List<ModBean>)

    //通过gamePackageName 查询关闭的mod
    fun getDisableMods(gamePackageName: String): Flow<List<ModBean>>

    // 通过list批量删除mods
    suspend fun deleteAll(mods: List<ModBean>)

    // 通过List<modbean>更新
    suspend fun updateAll(mods: List<ModBean>)

    // // 通过gamePackageName查询mods
    fun getModsByGamePackageName(gamePackageName: String): Flow<List<ModBean>>

    // // 通过gamePackageName查询mods数量
    fun getModsCountByGamePackageName(gamePackageName: String): Flow<Int>

    // 通过gamePackageName查询已开启的mods
    fun getEnableMods(gamePackageName: String): Flow<List<ModBean>>


    // 通过path和gamePackageName查询mods
    fun getModsByPathAndGamePackageName(path: String, gamePackageName: String): Flow<List<ModBean>>

    //通过包名查询已开启的mods数量
    fun getEnableModsCountByGamePackageName(gamePackageName: String): Flow<Int>

    // 通过ids查询mods
    fun getModsByIds(ids: List<Int>): Flow<List<ModBean>>


    // 通过path查询mod数量
    fun getModsCountByPath(path: String): Flow<Int>


}