package com.dh.admin.domain.auth.exception

object AuthExceptionMessages {
    const val EMAIL_ALREADY_IN_USE = "이미 사용 중인 이메일입니다."
    const val INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다."
    const val INVALID_REFRESH_TOKEN = "유효하지 않은 Refresh Token입니다."
    const val EXPIRED_REFRESH_TOKEN = "만료된 Refresh Token입니다. 다시 로그인해주세요."
    const val USER_NOT_FOUND = "사용자를 찾을 수 없습니다."
    const val INACTIVE_ACCOUNT = "비활성화된 계정입니다."
    const val LOCKED_ACCOUNT = "계정이 잠겨있습니다. 관리자에게 문의하세요."
}
