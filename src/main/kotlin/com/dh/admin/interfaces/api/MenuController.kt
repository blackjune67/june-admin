package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.CreateMenuRequest
import com.dh.admin.application.dto.UpdateMenuRequest
import com.dh.admin.common.response.ApiResponse
import com.dh.admin.domain.menu.service.MenuService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/menus")
class MenuController(
    private val menuService: MenuService
) {

    @PostMapping
    @PreAuthorize("hasAuthority('menu:create')")
    fun create(@Valid @RequestBody request: CreateMenuRequest): ResponseEntity<ApiResponse<*>> {
        val menu = menuService.create(request)
        return ResponseEntity.status(201).body(ApiResponse.ok(menu, "메뉴 생성 성공"))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('menu:read')")
    fun findAllTree(): ResponseEntity<ApiResponse<*>> {
        val menus = menuService.findAllTree()
        return ResponseEntity.ok(ApiResponse.ok(menus))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:update')")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateMenuRequest): ResponseEntity<ApiResponse<*>> {
        val menu = menuService.update(id, request)
        return ResponseEntity.ok(ApiResponse.ok(menu, "메뉴 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:delete')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<*>> {
        menuService.delete(id)
        return ResponseEntity.ok(ApiResponse.ok("메뉴 삭제 성공"))
    }
}
