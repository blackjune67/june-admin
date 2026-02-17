package com.dh.admin.interfaces.api

import com.dh.admin.application.dto.AssignRolesRequest
import com.dh.admin.common.response.ApiResponse
import com.dh.admin.domain.role.service.RoleService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserRoleController(
    private val roleService: RoleService
) {

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:read')")
    fun getUserRoles(@PathVariable id: Long): ResponseEntity<ApiResponse<*>> {
        val roles = roleService.getUserRoles(id)
        return ResponseEntity.ok(ApiResponse.ok(roles))
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:update')")
    fun assignUserRoles(
        @PathVariable id: Long,
        @RequestBody request: AssignRolesRequest
    ): ResponseEntity<ApiResponse<*>> {
        roleService.assignUserRoles(id, request)
        return ResponseEntity.ok(ApiResponse.ok("역할 할당 성공"))
    }
}
