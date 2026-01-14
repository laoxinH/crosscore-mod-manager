# FileTools 和 Permission 架构重构设计

## 目标

将 `FileToolsManager` 和 `PermissionTools` 重构为符合 Clean Architecture 的分层设计。

---

## 目录结构变更

```
app/src/main/kotlin/top/laoxin/modmanager/
├── data/
│   ├── repository/
│   │   ├── FileRepositoryImpl.kt           [新增]
│   │   └── PermissionRepositoryImpl.kt     [新增]
│   └── service/
│       ├── filetools/                       [从 tools/ 移动]
│       │   ├── BaseFileTools.kt
│       │   ├── FileToolsManager.kt
│       │   └── impl/
│       │       ├── FileTools.kt
│       │       ├── DocumentFileTools.kt
│       │       └── ShizukuFileTools.kt
│       └── PermissionService.kt             [从 PermissionTools 重构]
│
├── domain/
│   └── repository/
│       ├── FileRepository.kt                [新增]
│       └── PermissionRepository.kt          [新增]
│
├── constant/
│   └── FileAccessType.kt                    [新增，替代 PathType]
```

---

## 新增文件

### 1. FileAccessType 枚举
```kotlin
// constant/FileAccessType.kt
enum class FileAccessType {
    STANDARD_FILE,    // 标准 File API
    DOCUMENT_FILE,    // DocumentFile API (SAF)
    SHIZUKU,          // Shizuku API
    NONE              // 无权限
}
```

### 2. FileRepository 接口
```kotlin
// domain/repository/FileRepository.kt
interface FileRepository {
    suspend fun copyFile(src: String, dest: String): Boolean
    suspend fun deleteFile(path: String): Boolean
    suspend fun listFiles(path: String): List<File>
    suspend fun isFileExist(path: String): Boolean
    suspend fun readFile(path: String): String
    suspend fun writeFile(path: String, filename: String, content: String): Boolean
    suspend fun moveFile(src: String, dest: String): Boolean
    suspend fun createDirectory(path: String): Boolean
}
```

### 3. PermissionRepository 接口
```kotlin
// domain/repository/PermissionRepository.kt
interface PermissionRepository {
    fun getFileAccessType(path: String): FileAccessType
    fun hasPermissionFor(path: String): Boolean
    fun isShizukuAvailable(): Boolean
    fun hasShizukuPermission(): Boolean
    fun requestShizukuPermission()
}
```

### 4. PermissionService
```kotlin
// data/service/PermissionService.kt
@Singleton
class PermissionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 原 PermissionTools 的所有方法实现
}
```

### 5. FileRepositoryImpl
```kotlin
// data/repository/FileRepositoryImpl.kt
@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileToolsManager: FileToolsManager,
    private val permissionService: PermissionService
) : FileRepository {
    // 封装 FileToolsManager，根据权限选择正确的 FileTools
}
```

### 6. PermissionRepositoryImpl
```kotlin
// data/repository/PermissionRepositoryImpl.kt
@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val permissionService: PermissionService
) : PermissionRepository {
    // 委托给 PermissionService
}
```

---

## DI 配置更新

```kotlin
// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
    
    @Binds
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
}
```

---

## 受影响的 UseCase

需要更新依赖为 Repository 接口：
- `EnableModsUserCase`
- `DisableModUserCase`
- `FlashModsUserCase`
- 其他使用 FileToolsManager/PermissionTools 的 UseCase
