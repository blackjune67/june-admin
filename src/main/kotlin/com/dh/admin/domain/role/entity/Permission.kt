package com.dh.admin.domain.role.entity

import com.dh.admin.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["resource", "action"])]
)
class Permission(
    @Column(nullable = false, length = 50)
    val resource: String,

    @Column(nullable = false, length = 50)
    val action: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 255)
    var description: String? = null
) : BaseEntity() {

    val authority: String
        get() = "$resource:$action"
}
