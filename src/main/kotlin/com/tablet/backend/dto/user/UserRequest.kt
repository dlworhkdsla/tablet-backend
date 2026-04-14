package com.tablet.backend.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank(message = "사용자명은 필수입니다.")
    @field:Size(min = 2, max = 50, message = "사용자명은 2~50자 사이여야 합니다.")
    val username: String,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    val password: String,
)

data class UpdateUserRequest(
    @field:Size(min = 2, max = 50, message = "사용자명은 2~50자 사이여야 합니다.")
    val username: String?,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String?,
)
