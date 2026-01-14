package top.laoxin.modmanager.domain.model

/** 通用结果包装类 用于封装操作结果，支持成功和失败两种状态 */
sealed class Result<out T> {
    /**
     * 成功状态
     * @param data 返回的数据
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * 错误状态
     * @param error 错误信息
     */
    data class Error(val error: AppError) : Result<Nothing>()

    /** 判断是否成功 */
    val isSuccess: Boolean
        get() = this is Success

    /** 判断是否失败 */
    val isError: Boolean
        get() = this is Error

    /** 获取成功数据，失败时返回 null */
    fun getOrNull(): T? = (this as? Success)?.data

    /** 获取错误信息，成功时返回 null */
    fun errorOrNull(): AppError? = (this as? Error)?.error

    /** 获取成功数据，失败时返回默认值 */
    fun getOrDefault(default: @UnsafeVariance T): T = getOrNull() ?: default

    /** 获取成功数据，失败时抛出异常 */
    fun getOrThrow(): T =
            when (this) {
                is Success -> data
                is Error -> throw error.toException()
            }

    /** 转换成功数据 */
    inline fun <R> map(transform: (T) -> R): Result<R> =
            when (this) {
                is Success -> Success(transform(data))
                is Error -> this
            }

    /** 转换成功数据（支持返回 Result） */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> =
            when (this) {
                is Success -> transform(data)
                is Error -> this
            }

    /** 处理成功情况 */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /** 处理错误情况 */
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }

    companion object {
        /** 从可能抛出异常的代码块创建 Result */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(AppError.Unknown(e))
            }
        }
    }
}
