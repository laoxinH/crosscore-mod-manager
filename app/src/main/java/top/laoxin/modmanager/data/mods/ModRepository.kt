package top.laoxin.modmanager.data.mods

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.bean.ModBean

interface ModRepository {

    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllIModsStream(): Flow<List<ModBean>>

    /**
     * Retrieve an item from the given data source that matches with the [id].
     */
    fun getModStream(id: Int): Flow<ModBean?>

    /**
     * Insert item in the data source
     */
    suspend fun insertMod(mod: ModBean)

    /**
     * Delete item from the data source
     */
    suspend fun deleteMod(mod: ModBean)

    /**
     * Update item in the data source
     */
    suspend fun updateMod(mod: ModBean)

    // 通过名字模糊查询
    fun search(name: String): Flow<List<ModBean>>

    // 获取已开启的mod
    fun getEnableMods(): Flow<List<ModBean>>

    // 插入所有数据
    suspend fun insertAll(mods: List<ModBean>)

    // 查询mod数量
    fun getModCount(): Flow<Int>

    // 查询已开启mod数量
    fun getEnableModCount(): Flow<Int>

    // 查询关闭的mod
    fun getDisableMods(): Flow<List<ModBean>>

    // 通过list批量删除
    suspend fun deleteAll(mods: List<ModBean>)

    // 通过path查询
    fun getByPath(path: String): Flow<List<ModBean>>

    // 通过list<modben>更新数据
    suspend fun updateAll(mods: List<ModBean>)
}