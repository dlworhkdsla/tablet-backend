package com.tablet.backend.domain.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
    @Id
    @Column("user_id")
    val userId: String,

    @Column("password")
    val password: String,

    @Column("pass_expire_date")
    val passExpireDate: String,

    @Column("delete_flag")
    val deleteFlag: String = "N",

    @Column("creator_id")
    val creatorId: String,

    @Column("create_dttm")
    val createDttm: String,

    @Column("modifier_id")
    val modifierId: String? = null,

    @Column("modify_dttm")
    val modifyDttm: String? = null,
)
