package com.dh.admin.domain.menu.entity

import com.dh.admin.common.entity.BaseEntity
import com.dh.admin.domain.role.entity.Permission
import jakarta.persistence.*

@Entity
@Table(name = "menus")
class Menu(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, unique = true, length = 50)
    val code: String,

    @Column(length = 255)
    var path: String? = null,

    @Column(length = 50)
    var icon: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Menu? = null,

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    val children: MutableList<Menu> = mutableListOf(),

    @Column(nullable = false)
    var sortOrder: Int = 0,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    var requiredPermission: Permission? = null
) : BaseEntity()
