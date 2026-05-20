package com.tablet.backend.dto.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "로그인 ID는 필수입니다.")
    @field:Size(max = 50, message = "로그인 ID는 50자 이하여야 합니다.")
    val userId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    val password: String,
)

data class LoginRequest(
    @field:NotBlank(message = "로그인 ID는 필수입니다.")
    val userId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String,
)
