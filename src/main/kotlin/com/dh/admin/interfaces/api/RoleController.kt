package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.*
import com.dh.admin.common.response.ApiResponse
import com.dh.admin.domain.role.service.RoleService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/roles")
class RoleController(
    private val roleService: RoleService
) {

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    fun create(@Valid @RequestBody request: CreateRoleRequest): ResponseEntity<ApiResponse<*>> {
        val role = roleService.create(request)
        return ResponseEntity.status(201).body(ApiResponse.ok(role, "역할 생성 성공"))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    fun findAll(): ResponseEntity<ApiResponse<*>> {
        val roles = roleService.findAll()
        return ResponseEntity.ok(ApiResponse.ok(roles))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    fun findById(@PathVariable id: Long): ResponseEntity<ApiResponse<*>> {
        val role = roleService.findById(id)
        return ResponseEntity.ok(ApiResponse.ok(role))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateRoleRequest): ResponseEntity<ApiResponse<*>> {
        val role = roleService.update(id, request)
        return ResponseEntity.ok(ApiResponse.ok(role, "역할 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<*>> {
        roleService.delete(id)
        return ResponseEntity.ok(ApiResponse.ok("역할 삭제 성공"))
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update')")
    fun assignPermissions(
        @PathVariable id: Long,
        @RequestBody request: AssignPermissionsRequest
    ): ResponseEntity<ApiResponse<*>> {
        val role = roleService.assignPermissions(id, request)
        return ResponseEntity.ok(ApiResponse.ok(role, "권한 할당 성공"))
    }
}
