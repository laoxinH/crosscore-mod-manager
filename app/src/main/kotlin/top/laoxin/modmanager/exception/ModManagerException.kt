package top.laoxin.modmanager.exception

// 权限不足异常
class PermissionsException(message: String) : Exception(message)

// 未选择游戏异常
class NoSelectedGameException(message: String) : Exception(message)

// 特殊操作失败
class SpecialOperationFailedException(message: String) : Exception(message)

// 密码错误异常
class PasswordErrorException(message: String) : Exception(message)

// 流复制失败异常
class CopyStreamFailedException(message: String) : Exception(message)
