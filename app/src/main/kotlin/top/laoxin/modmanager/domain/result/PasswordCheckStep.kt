package top.laoxin.modmanager.domain.result

import top.laoxin.modmanager.domain.bean.ModBean

/**
 * Represents a specific error that can occur during the password check process.
 * These are business facts, not UI strings.
 */
sealed interface PasswordCheckError {
    data object InvalidPassword : PasswordCheckError
    data class DecompressionFailed(val reason: String) : PasswordCheckError
    data object DecompressionResultEmpty : PasswordCheckError
    data class Unexpected(val throwable: Throwable) : PasswordCheckError
}

/**
 * Represents each step of the password check and mod processing operation.
 * It can emit progress, a final success, or a specific, typed error.
 */
sealed interface PasswordCheckStep {
    data object Validating : PasswordCheckStep
    data class Decompressing(val progress: Int) : PasswordCheckStep
    data object ProcessingInfo : PasswordCheckStep
    data class Success(val updatedMod: ModBean) : PasswordCheckStep
    data class Error(val error: PasswordCheckError) : PasswordCheckStep
}
