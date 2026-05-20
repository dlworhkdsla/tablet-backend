package com.tablet.backend.dto.user

import com.tablet.backend.domain.user.User

data class UserResponse(
    val userId: String,
    val passExpireDate: String,
    val deleteFlag: String,
    val creatorId: String,
    val createDttm: String,
    val modifierId: String?,
    val modifyDttm: String?,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            userId = user.userId,
            passExpireDate = user.passExpireDate,
            deleteFlag = user.deleteFlag,
            creatorId = user.creatorId,
            createDttm = user.createDttm,
            modifierId = user.modifierId,
            modifyDttm = user.modifyDttm,
        )
    }
}
