# Navigation 3 迁移实施计划

## 概述
将当前 HorizontalPager + 手动状态管理架构迁移到 Navigation 3，实现：
- 类型安全导航
- 开发者控制的返回栈
- 预见式返回动画
- 自适应布局支持

---

## Navigation 3 核心概念

| 概念 | 说明 |
|------|------|
| **NavKey** | 可序列化的目的地标识符，可携带参数 |
| **NavEntry** | 每个目的地的 Composable 容器 |
| **backStack** | `SnapshotStateList<NavKey>`，开发者完全控制 |
| **NavDisplay** | 观察 backStack 并渲染 UI，处理返回手势 |
| **entryProvider** | NavKey → NavEntry 的映射函数 |

---

## 依赖配置

```toml
# libs.versions.toml
[versions]
navigation3 = "1.0.0-alpha01"

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-compose = { module = "androidx.navigation3:navigation3-compose", version.ref = "navigation3" }
```

---

## 实施步骤

### Phase 1: 定义 NavKey

```kotlin
// ui/navigation/NavKeys.kt
import kotlinx.serialization.Serializable

sealed interface AppNavKey {
    @Serializable
    data object Console : AppNavKey
    
    @Serializable
    data object ModList : AppNavKey
    
    @Serializable
    data class ModDetail(val modId: Int) : AppNavKey
    
    @Serializable
    data object Settings : AppNavKey
    
    @Serializable
    data object About : AppNavKey
    
    @Serializable
    data object License : AppNavKey
}
```

---

### Phase 2: 创建 EntryProvider

```kotlin
// ui/navigation/AppEntryProvider.kt
@Composable
fun appEntryProvider(
    consoleViewModel: ConsoleViewModel,
    modListViewModel: ModListViewModel,
    // ... 其他 ViewModel
): (AppNavKey) -> NavEntry<AppNavKey> = { key ->
    when (key) {
        is AppNavKey.Console -> NavEntry(key) {
            ConsolePage(viewModel = consoleViewModel)
        }
        is AppNavKey.ModList -> NavEntry(key) {
            ModListPage(viewModel = modListViewModel)
        }
        is AppNavKey.ModDetail -> NavEntry(key) {
            ModDetailPage(modId = key.modId)
        }
        is AppNavKey.Settings -> NavEntry(key) {
            SettingPage()
        }
        // ...
    }
}
```

---

### Phase 3: 设置 NavDisplay

```kotlin
// ui/view/ModManagerApp.kt
@Composable
fun ModManagerApp() {
    // 开发者控制的返回栈
    val backStack = remember { 
        mutableStateListOf<AppNavKey>(AppNavKey.Console) 
    }
    
    // 获取 ViewModel
    val consoleViewModel: ConsoleViewModel = hiltViewModel()
    // ...
    
    Scaffold(
        topBar = { /* TopBar 基于 backStack.lastOrNull() */ },
        bottomBar = { NavigationBar(backStack) }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            entryProvider = appEntryProvider(consoleViewModel, ...),
            modifier = Modifier.padding(innerPadding)
        )
    }
}
```

---

### Phase 4: 导航操作

```kotlin
// 导航到新目的地
fun navigateTo(destination: AppNavKey) {
    backStack.add(destination)
}

// 返回
fun goBack(): Boolean {
    if (backStack.size > 1) {
        backStack.removeLast()
        return true
    }
    return false
}

// 跳转到指定目的地（清除中间栈）
fun navigateAndClear(destination: AppNavKey) {
    backStack.clear()
    backStack.add(destination)
}

// 底部导航切换（保持主页面栈）
fun switchBottomTab(tab: AppNavKey) {
    // 清除到第一个匹配项或添加新的
    val index = backStack.indexOfFirst { it::class == tab::class }
    if (index >= 0) {
        while (backStack.size > index + 1) {
            backStack.removeLast()
        }
    } else {
        backStack.add(tab)
    }
}
```

---

### Phase 5: 预见式返回动画

Navigation 3 内置支持预见式返回：

```xml
<!-- AndroidManifest.xml -->
<application
    android:enableOnBackInvokedCallback="true"
    ...>
```

NavDisplay 自动处理返回手势和预见式动画。

---

## 迁移顺序

1. **Week 1**
   - [ ] 添加依赖
   - [ ] 定义 NavKey sealed class
   - [ ] 创建 EntryProvider
   - [ ] 替换 ModManagerApp 主结构

2. **Week 2**
   - [ ] 迁移 Console 页面
   - [ ] 迁移 ModList 页面
   - [ ] 迁移 Settings 页面

3. **Week 3**
   - [ ] 实现子页面导航 (ModDetail, About, License)
   - [ ] 实现底部导航逻辑
   - [ ] 处理横屏 NavigationRail

4. **Week 4**
   - [ ] 测试预见式返回动画
   - [ ] 优化过渡动画
   - [ ] 处理边缘情况

---

## 注意事项

1. **ViewModel 作用域**
   - 使用 `hiltViewModel()` 时注意作用域
   - 考虑使用 `hiltViewModel(viewModelStoreOwner)` 共享 ViewModel

2. **状态保存**
   - NavKey 必须 @Serializable
   - 使用 rememberSaveable 保存复杂状态

3. **向后兼容**
   - Navigation 3 需要 Compose 1.7+
   - 确保 minSdk 兼容
