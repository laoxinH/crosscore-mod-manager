package top.laoxin.modmanager.ui.state

import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.ui.view.modview.NavigationIndex
import java.io.File

data class ModUiState(
    val modList: List<ModBean> = emptyList(),     // 所有mod列表
    val enableModList: List<ModBean> = emptyList(),     // 开启mod列表
    val disableModList: List<ModBean> = emptyList(),     // 关闭mod列表
    val searchModList: List<ModBean> = emptyList(),     // 搜索mod列表
    val isLoading: Boolean = false,          // 是否正在加载
    val openPermissionRequestDialog: Boolean = false,  // 是否打开权限请求对话框
    val modDetail: ModBean? = null,       // 打开的mod详情
    val showModDetail: Boolean = false,   // 是否显示mod详情
    val searchBoxVisible: Boolean = false,    // 搜索框是否可见
    val loadingPath: String = "",       // 加载路径
    val selectedMenuItem: Int = 0,       // 选中的菜单项
    val showPasswordDialog: Boolean = false,   // 是否打开密码对话框
    val tipsText: String = "",       // 提示文本
    val showTips: Boolean = false,   // 是否显示提示
    val modSwitchEnable: Boolean = true,   // 是否正在切换mod
    val showUserTipsDialog: Boolean = false,   // 显示用户提示对话框
    val delEnableModsList: List<ModBean> = emptyList(),     // mod列表
    val showDisEnableModsDialog: Boolean = false,   // 是否显示禁用mod对话框
    val unzipProgress: String = "",       // 解压进度
    val modsView: NavigationIndex = NavigationIndex.ALL_MODS,   // mod视图
    val modsSelected: List<Int> = emptyList(),     // 选中的mod
    val isMultiSelect: Boolean = false,   // 是否显示多选
    val multitaskingProgress: String = "",     // 多任务进度
    // 显示删除选择MODS的弹窗
    val showDelSelectModsDialog: Boolean = false,   // 是否显示删除选择mod对话框
    val showDelModDialog: Boolean = false,
    // 显示开启失败是否关闭MODS的弹窗
    val showOpenFailedDialog: Boolean = false,   // 是否显示开启失败对话框
    // 开启失败的mods
    val openFailedMods: List<ModBean> = emptyList(),
    // 搜索框内容
    val searchContent: String = "",
    // 当前的游戏mod目录
    val currentGameModPath: String = "",
    // 当前页面的文件
    val currentFiles: List<File> = emptyList(),
    // 当前页面的mods
    val currentMods: List<ModBean> = emptyList()
) {

}
