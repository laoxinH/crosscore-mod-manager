# ModBean 空安全重构设计文档

## 概述

对 `ModBean` 数据类进行空安全重构，提升代码质量和类型安全性。

---

## 新增枚举类型

### `ModForm` - Mod形式枚举

```kotlin
/**
 * Mod形式枚举，限定只能为以下三种形式
 */
enum class ModForm {
    /** 传统形式 - 被动扫描 */
    TRADITIONAL,
    
    /** 新形式 - 包含mod配置文件的主动式mod包 */
    ACTIVE,
    
    /** 打包式 - 包含配置文件的需要打包unity资源包的形式 */
    PACKAGED
}
```

---

## 字段设计

### 可空字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `version` | `String?` | 版本信息可选 |
| `virtualPaths` | `String?` | 虚拟路径可选 |
| `icon` | `String?` | 图标路径可选 |
| `images` | `List<String>?` | 预览图列表可选 |
| `readmePath` | `String?` | readme路径可选 |
| `fileReadmePath` | `String?` | 文件readme路径可选 |
| `author` | `String?` | 作者信息可选 |
| `modConfig` | `String?` | **[新增]** Mod配置JSON（非传统形式使用） |

### 非空字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | `Int` | `0` | 主键 |
| `name` | `String` | `""` | Mod名称 |
| `description` | `String` | `""` | Mod描述 |
| `date` | `Long` | `0L` | 创建/修改时间 |
| `path` | `String` | `""` | Mod文件路径 |
| `modFiles` | `List<String>` | `emptyList()` | Mod包含的文件列表 |
| `gameFilesPath` | `List<String>` | `emptyList()` | **[新增]** 需要替换的游戏文件路径 |
| `modForm` | `ModForm` | `TRADITIONAL` | **[新增]** Mod形式（枚举） |
| `isEncrypted` | `Boolean` | `false` | 是否加密 |
| `password` | `String` | `""` | 密码 |
| `gamePackageName` | `String` | `""` | 关联的游戏包名 |
| `gameModPath` | `String` | `""` | 游戏Mod目录路径 |
| `modType` | `String` | `""` | Mod类型 |
| `isEnable` | `Boolean` | `false` | 是否已启用 |
| `isZipFile` | `Boolean` | `true` | 是否为压缩包格式 |

---

## 新增字段说明

### `modForm: ModForm`

标记当前Mod属于哪种形式，只允许三种值：
- `TRADITIONAL` - 传统形式，被动扫描
- `ACTIVE` - 新形式，包含mod配置文件的主动式mod包
- `PACKAGED` - 打包式，需要打包unity资源包

### `modConfig: String?`

存储非传统形式Mod的JSON配置内容。
- 传统形式：为 `null`
- 新形式/打包式：包含mod包内的JSON配置

### `gameFilesPath: List<String>`

存储需要替换的游戏文件完整路径列表。

---

## 数据库迁移 (5→6)

```sql
-- 更新 NULL 值为默认值
UPDATE mods SET name = '' WHERE name IS NULL;
UPDATE mods SET description = '' WHERE description IS NULL;
UPDATE mods SET path = '' WHERE path IS NULL;
UPDATE mods SET modFiles = '[]' WHERE modFiles IS NULL;
UPDATE mods SET password = '' WHERE password IS NULL;
UPDATE mods SET gamePackageName = '' WHERE gamePackageName IS NULL;
UPDATE mods SET gameModPath = '' WHERE gameModPath IS NULL;
UPDATE mods SET modType = '' WHERE modType IS NULL;

-- 添加新字段
ALTER TABLE mods ADD COLUMN gameFilesPath TEXT NOT NULL DEFAULT '[]';
ALTER TABLE mods ADD COLUMN modForm TEXT NOT NULL DEFAULT 'TRADITIONAL';
ALTER TABLE mods ADD COLUMN modConfig TEXT;
```
