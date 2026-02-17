package com.dh.admin.domain.menu.service

import com.dh.admin.application.dto.CreateMenuRequest
import com.dh.admin.application.dto.MenuResponse
import com.dh.admin.application.dto.PermissionResponse
import com.dh.admin.application.dto.UpdateMenuRequest
import com.dh.admin.common.exception.DuplicateException
import com.dh.admin.common.exception.ResourceNotFoundException
import com.dh.admin.domain.menu.entity.Menu
import com.dh.admin.domain.menu.repository.MenuRepository
import com.dh.admin.domain.role.repository.PermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MenuService(
    private val menuRepository: MenuRepository,
    private val permissionRepository: PermissionRepository
) {

    @Transactional
    fun create(request: CreateMenuRequest): MenuResponse {
        if (menuRepository.existsByCode(request.code)) {
            throw DuplicateException("이미 존재하는 메뉴 코드입니다: ${request.code}")
        }

        val parent = request.parentId?.let {
            menuRepository.findById(it)
                .orElseThrow { ResourceNotFoundException("상위 메뉴를 찾을 수 없습니다: $it") }
        }

        val permission = request.permissionId?.let {
            permissionRepository.findById(it)
                .orElseThrow { ResourceNotFoundException("권한을 찾을 수 없습니다: $it") }
        }

        val menu = menuRepository.save(
            Menu(
                name = request.name,
                code = request.code,
                path = request.path,
                icon = request.icon,
                parent = parent,
                sortOrder = request.sortOrder,
                requiredPermission = permission
            )
        )
        return menu.toResponse()
    }

    fun findAllTree(): List<MenuResponse> {
        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus.map { it.toResponseWithChildren() }
    }

    fun findAccessibleMenus(permissionAuthorities: Set<String>): List<MenuResponse> {
        val rootMenus = menuRepository.findAllRootMenus()
        return rootMenus
            .filter { it.isActive }
            .mapNotNull { filterAccessibleMenu(it, permissionAuthorities) }
    }

    @Transactional
    fun update(id: Long, request: UpdateMenuRequest): MenuResponse {
        val menu = menuRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("메뉴를 찾을 수 없습니다: $id") }

        menu.name = request.name
        request.path?.let { menu.path = it }
        request.icon?.let { menu.icon = it }
        request.sortOrder?.let { menu.sortOrder = it }
        request.isActive?.let { menu.isActive = it }

        if (request.parentId != null) {
            val parent = menuRepository.findById(request.parentId)
                .orElseThrow { ResourceNotFoundException("상위 메뉴를 찾을 수 없습니다: ${request.parentId}") }
            menu.parent = parent
        }

        if (request.permissionId != null) {
            val permission = permissionRepository.findById(request.permissionId)
                .orElseThrow { ResourceNotFoundException("권한을 찾을 수 없습니다: ${request.permissionId}") }
            menu.requiredPermission = permission
        }

        return menuRepository.save(menu).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val menu = menuRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("메뉴를 찾을 수 없습니다: $id") }
        menuRepository.delete(menu)
    }

    private fun filterAccessibleMenu(menu: Menu, authorities: Set<String>): MenuResponse? {
        val requiredAuthority = menu.requiredPermission?.authority
        if (requiredAuthority != null && requiredAuthority !in authorities) {
            return null
        }

        val accessibleChildren = menu.children
            .filter { it.isActive }
            .mapNotNull { filterAccessibleMenu(it, authorities) }

        return MenuResponse(
            id = menu.id,
            name = menu.name,
            code = menu.code,
            path = menu.path,
            icon = menu.icon,
            parentId = menu.parent?.id,
            sortOrder = menu.sortOrder,
            isActive = menu.isActive,
            requiredPermission = menu.requiredPermission?.let {
                PermissionResponse(it.id, it.resource, it.action, it.name, it.description, it.authority)
            },
            children = accessibleChildren
        )
    }

    private fun Menu.toResponse() = MenuResponse(
        id = this.id,
        name = this.name,
        code = this.code,
        path = this.path,
        icon = this.icon,
        parentId = this.parent?.id,
        sortOrder = this.sortOrder,
        isActive = this.isActive,
        requiredPermission = this.requiredPermission?.let {
            PermissionResponse(it.id, it.resource, it.action, it.name, it.description, it.authority)
        }
    )

    private fun Menu.toResponseWithChildren(): MenuResponse = MenuResponse(
        id = this.id,
        name = this.name,
        code = this.code,
        path = this.path,
        icon = this.icon,
        parentId = this.parent?.id,
        sortOrder = this.sortOrder,
        isActive = this.isActive,
        requiredPermission = this.requiredPermission?.let {
            PermissionResponse(it.id, it.resource, it.action, it.name, it.description, it.authority)
        },
        children = this.children.map { it.toResponseWithChildren() }
    )
}
