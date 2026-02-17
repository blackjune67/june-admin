package com.dh.admin.domain.menu.repository

import com.dh.admin.domain.menu.entity.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MenuRepository : JpaRepository<Menu, Long> {
    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.requiredPermission WHERE m.parent IS NULL ORDER BY m.sortOrder")
    fun findAllRootMenus(): List<Menu>

    @Query("SELECT m FROM Menu m LEFT JOIN FETCH m.requiredPermission LEFT JOIN FETCH m.children ORDER BY m.sortOrder")
    fun findAllWithChildren(): List<Menu>

    fun findByCode(code: String): Menu?
    fun existsByCode(code: String): Boolean
}
