package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.CreatePermissionRequest
import com.dh.admin.application.dto.UpdatePermissionRequest
import com.dh.admin.common.response.ApiResponse
import com.dh.admin.domain.role.service.PermissionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/permissions")
class PermissionController(
    private val permissionService: PermissionService
) {

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    fun create(@Valid @RequestBody request: CreatePermissionRequest): ResponseEntity<ApiResponse<*>> {
        val permission = permissionService.create(request)
        return ResponseEntity.status(201).body(ApiResponse.ok(permission, "권한 생성 성공"))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    fun findAll(): ResponseEntity<ApiResponse<*>> {
        val permissions = permissionService.findAll()
        return ResponseEntity.ok(ApiResponse.ok(permissions))
    }

    @GetMapping("/resources")
    @PreAuthorize("hasAuthority('permission:read')")
    fun findAllResources(): ResponseEntity<ApiResponse<*>> {
        val resources = permissionService.findAllResources()
        return ResponseEntity.ok(ApiResponse.ok(resources))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:update')")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdatePermissionRequest): ResponseEntity<ApiResponse<*>> {
        val permission = permissionService.update(id, request)
        return ResponseEntity.ok(ApiResponse.ok(permission, "권한 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<*>> {
        permissionService.delete(id)
        return ResponseEntity.ok(ApiResponse.ok("권한 삭제 성공"))
    }
}
