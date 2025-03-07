package top.laoxin.modmanager.domain.usercase.app

import top.laoxin.modmanager.constant.PathType
import top.laoxin.modmanager.tools.PermissionTools
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckPermissionUserCase @Inject constructor(
    private val permissionTools: PermissionTools,
) {

    operator fun invoke(path: String): Boolean {
        return permissionTools.checkPermission(path) != PathType.NULL
    }
}