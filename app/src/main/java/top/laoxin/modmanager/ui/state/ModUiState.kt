package top.laoxin.modmanager.ui.state

import androidx.annotation.StringRes
import net.lingala.zip4j.util.UnzipUtil
import top.laoxin.modmanager.bean.ModBean

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
    val tipsText: String= "",       // 提示文本
    val showTips: Boolean = false,   // 是否显示提示
    val modSwitchEnable: Boolean = true,   // 是否正在切换mod
    val showUserTipsDialog: Boolean = false,   // 显示用户提示对话框
    val delEnableModsList : List<ModBean> = emptyList(),     // mod列表
    val showDisEnableModsDialog : Boolean = false,   // 是否显示禁用mod对话框
    val unzipProgress: String = "",       // 解压进度
    )
