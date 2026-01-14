# TRADITIONAL MOD 扫描逻辑设计文档

## 概述

本文档描述 TRADITIONAL 形式 MOD 的扫描与构建逻辑。TRADITIONAL MOD 是通过被动扫描压缩包或文件夹，识别其中包含的游戏替换文件来构建的 MOD 形式。

---

## 核心实体

### GameInfoBean (游戏配置)
```kotlin
data class GameInfoBean(
    val packageName: String,           // 游戏包名
    val gamePath: String,              // 游戏数据路径
    val gameFilePath: List<String>,    // 游戏文件目录列表 (如 ["Custom/", "Lihui/"])
    val modType: List<String>,         // 对应的 MOD 类型列表 (如 ["皮肤", "立绘"])
    val isGameFileRepeat: Boolean,     // 游戏目录中是否存在同名文件
)
```

### ModBean (构建的 MOD)
```kotlin
data class ModBean(
    val id: Int = 0,
    val name: String = "",              // MOD 名称
    val path: String = "",              // 压缩包/文件夹完整路径
    val date: Long = 0L,                // 物理修改时间
    val modDate: Long = 0L,             // [新增] MOD 创建/更新时间
    val virtualPaths: String? = null,   // 整合包虚拟路径
    val modFiles: List<String>,         // MOD 文件路径列表 (压缩包为相对路径，文件夹为完整路径)
    val gameFilesPath: List<String>,    // 游戏目标路径列表
    val modType: String = "",           // MOD 类型
    val isEncrypted: Boolean = false,   // 是否加密
    val icon: String? = null,           // 缩略图路径
    val images: List<String>? = null,   // 预览图列表
    val description: String = "",       // readme 内容
    val readmePath: String? = null,     // readme 路径
    val fileReadmePath: String? = null, // 整合包根目录 readme
    val isZipFile: Boolean = true,      // 是否为压缩包
    // ... 其他字段
)
```

---

## 扫描同步逻辑

### 完整同步流程
```
1. 扫描目录获取所有压缩包及文件夹列表
2. 从数据库获取当前游戏的所有 ModBean
3. 对比执行：
   - 新增：物理文件存在，数据库不存在 → 创建 ModBean
   - 更新：物理修改时间 > ModBean.date → 重新扫描更新
   - 删除：数据库存在，物理文件不存在 → 删除 ModBean 及相关缓存
```

---

## 字段映射规则

| 字段 | 正常压缩包 | 加密压缩包 | 文件夹 MOD (非压缩包) |
|------|-----------|-----------|---------------------|
| `path` | 压缩包完整路径 | 同左 | 文件夹完整路径 |
| `isZipFile` | `true` | `true` | `false` |
| `name` | `"名称(路径|Custom)"` | 同左 | `"名称(路径|Custom)"` |
| `date` | 文件修改时间 | 同左 | 文件夹修改时间 |
| `modDate` | MOD 创建/更新时间 | 同左 | 同左 |
| `virtualPaths` | 整合包路径或 `null` | 同左 | 同左 |
| `modFiles` | **相对路径列表** | 同左 | **完整文件路径列表** |
| `isEncrypted` | `false` | `true` | `false` |
| `icon` | **提取后的缓存路径** | 相对路径 | **完整文件路径** |
| `images` | **提取后的缓存路径列表** | 相对路径列表 | **完整文件路径列表** |
| `description` | **readme 文本内容** | `"MOD已加密"` | **readme 文本内容** |
| `readmePath` | 相对路径 | 同左 | **完整文件路径** |
| `fileReadmePath`| 相对路径 | 同左 | **完整文件路径** |

---

## 命名规则

| 来源 | 内部路径 | name | virtualPaths |
|-----|---------|------|--------------|
| `xxx.zip` | `Custom/abc.dat` | `"xxx"` | `null` |
| `xxx.zip` | `皮肤/Custom/abc.dat` | `"xxx(皮肤\|Custom)"` | `null` |
| `xxx.zip` | 整合包多 MOD | `"xxx(路径1\|路径2\|Custom)"` | `完整路径` |
| `MyFolder`| `Custom/abc.dat` | `"MyFolder"` | `null` |
| `MyFolder`| `皮肤/Custom/abc.dat` | `"MyFolder(皮肤\|Custom)"` | `null` |

### 命名算法
```kotlin
fun generateModName(
    sourceName: String,      // 压缩包名或文件夹名
    modRelativePath: String   // MOD 相对路径，如 "皮肤/人物1/Custom"
): String {
    val parts = modRelativePath.split("/").filter { it.isNotEmpty() }
    return when {
        parts.size <= 1 -> sourceName
        else -> "$sourceName(${parts.joinToString("|")})"
    }
}
```

---

## 文件夹 MOD 扫描 (非压缩包)

### 扫描范围
- 目录: `[selectedDirectory]/[GamePackageName]/` 下的**直接子文件夹**
- 每个直接子文件夹视为一个“MOD 包”（类似于一个 zip 文件）

### 识别逻辑
- 逻辑与压缩包一致，只是操作对象从 ZipEntry 变为 File
- 同样支持 `isGameFileRepeat` 的两种模式

---

## MOD 识别规则

### isGameFileRepeat = true
- 必须包含游戏目录名（如 `Custom/`、`Lihui/`）
- 每个游戏目录 = 一个独立 MOD

### isGameFileRepeat = false
- 不需要包含游戏目录名
- 通过文件名匹配游戏文件
- 包含匹配文件的最近父目录 = 一个 MOD

---

## 加密压缩包处理

| 场景 | 处理 |
|------|------|
| fileHeader 加密，无法读取文件列表 | **跳过**，不创建 ModBean |
| 可读取文件列表，但内容加密 | 创建 ModBean，`isEncrypted = true` |
| 用户输入密码启用时 | 重新提取 icon/images，更新 description |

---

## 图片提取 (压缩包)

- **缩略图**: `PathConstants.MODS_ICON_PATH`
- **预览图**: `PathConstants.MODS_IMAGE_PATH`
- 命名: `"${archiveName}_${hash}.ext"`

---

## 服务依赖

| 服务 | 用途 |
|------|------|
| `ArchiveService` | 读取压缩包 |
| `FileService` | 文件操作、复制 |
| `PermissionService` | 权限检查 |
| `UserPreferencesRepository` | 配置获取 |
| `ModRepository` | 数据库操作 |

---

## 待实现接口

```kotlin
interface ModScanService {
    /** 扫描压缩包 */
    suspend fun scanArchive(archivePath: String, gameInfo: GameInfoBean): Result<List<ModBean>>
    
    /** 扫描文件夹 MOD */
    suspend fun scanDirectoryMod(folderPath: String, gameInfo: GameInfoBean): Result<List<ModBean>>

    /** 判断是否包含有效 MOD */
    suspend fun isModSource(path: String, gameInfo: GameInfoBean): Result<Boolean>

    /** 同步扫描 */
    suspend fun syncMods(scanPaths: List<String>, gameInfo: GameInfoBean): Result<SyncResult>
}
```
