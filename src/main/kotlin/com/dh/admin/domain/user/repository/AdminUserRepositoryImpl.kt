package com.dh.admin.domain.user.repository

import com.dh.admin.application.dto.RoleSummary
import com.dh.admin.application.dto.UserListItemResponse
import com.dh.admin.domain.role.entity.QRole
import com.dh.admin.domain.user.entity.QAdminUser
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class AdminUserRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : AdminUserQueryRepository {

    private val adminUser = QAdminUser.adminUser
    private val role = QRole.role

    override fun findUserList(
        keyword: String?,
        roleCode: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<UserListItemResponse> {
        val conditions = buildConditions(keyword, roleCode, isActive)

        val total = queryFactory
            .select(adminUser.id.countDistinct())
            .from(adminUser)
            .leftJoin(adminUser.roles, role)
            .where(*conditions.toTypedArray())
            .fetchOne() ?: 0L

        if (total == 0L) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val userIds = queryFactory
            .select(adminUser.id)
            .from(adminUser)
            .leftJoin(adminUser.roles, role)
            .where(*conditions.toTypedArray())
            .groupBy(adminUser.id)
            .orderBy(adminUser.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        if (userIds.isEmpty()) {
            return PageImpl(emptyList(), pageable, total)
        }

        val tuples = queryFactory
            .select(
                adminUser.id,
                adminUser.email,
                adminUser.name,
                adminUser.isActive,
                role.id,
                role.code,
                role.name
            )
            .from(adminUser)
            .leftJoin(adminUser.roles, role)
            .where(adminUser.id.`in`(userIds))
            .orderBy(adminUser.id.desc(), role.code.asc())
            .fetch()

        val aggregateMap = linkedMapOf<Long, UserAggregate>()

        tuples.forEach { tuple ->
            val userId = tuple.get(adminUser.id) ?: return@forEach

            val aggregate = aggregateMap.getOrPut(userId) {
                UserAggregate(
                    id = userId,
                    email = tuple.get(adminUser.email) ?: "",
                    name = tuple.get(adminUser.name) ?: "",
                    isActive = tuple.get(adminUser.isActive) ?: false
                )
            }

            val roleId = tuple.get(role.id)
            if (roleId != null) {
                val roleCodeValue = tuple.get(role.code) ?: ""
                val roleNameValue = tuple.get(role.name) ?: ""
                aggregate.rolesById.putIfAbsent(
                    roleId,
                    RoleSummary(id = roleId, code = roleCodeValue, name = roleNameValue)
                )
            }
        }

        val contentById = aggregateMap.mapValues { (_, aggregate) ->
            UserListItemResponse(
                id = aggregate.id,
                email = aggregate.email,
                name = aggregate.name,
                isActive = aggregate.isActive,
                roles = aggregate.rolesById.values.toList()
            )
        }

        val orderedContent = userIds.mapNotNull { contentById[it] }
        return PageImpl(orderedContent, pageable, total)
    }

    private fun buildConditions(
        keyword: String?,
        roleCode: String?,
        isActive: Boolean?
    ): List<BooleanExpression> {
        val conditions = mutableListOf<BooleanExpression>()

        if (!keyword.isNullOrBlank()) {
            conditions += adminUser.email.containsIgnoreCase(keyword)
                .or(adminUser.name.containsIgnoreCase(keyword))
        }

        if (!roleCode.isNullOrBlank()) {
            conditions += role.code.eq(roleCode)
        }

        if (isActive != null) {
            conditions += adminUser.isActive.eq(isActive)
        }

        return conditions
    }

    private data class UserAggregate(
        val id: Long,
        val email: String,
        val name: String,
        val isActive: Boolean,
        val rolesById: LinkedHashMap<Long, RoleSummary> = linkedMapOf()
    )
}
