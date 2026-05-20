package com.tablet.backend.service

import com.tablet.backend.config.JwtProperties
import com.tablet.backend.domain.user.User
import com.tablet.backend.dto.auth.LoginRequest
import com.tablet.backend.dto.auth.LoginResponse
import com.tablet.backend.dto.auth.RegisterRequest
import com.tablet.backend.dto.auth.RegisterResponse
import com.tablet.backend.exception.DuplicateResourceException
import com.tablet.backend.exception.UnauthorizedException
import com.tablet.backend.repository.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtProperties: JwtProperties,
) {
    private val passwordEncoder = BCryptPasswordEncoder()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val dttmFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8))
    }

    @Transactional
    suspend fun register(request: RegisterRequest): RegisterResponse {
        if (userRepository.existsByUserId(request.userId)) {
            throw DuplicateResourceException("이미 사용 중인 ID입니다: ${request.userId}")
        }

        val now = LocalDateTime.now()
        val expireDate = LocalDate.now().plusDays(90).format(dateFmt)

        val user = User(
            userId = request.userId,
            password = passwordEncoder.encode(request.password),
            passExpireDate = expireDate,
            deleteFlag = "N",
            creatorId = request.userId,
            createDttm = now.format(dttmFmt),
        )

        val saved = userRepository.save(user)
        return RegisterResponse.from(saved)
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUserId(request.userId)
            ?: throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")

        if (user.deleteFlag == "Y") {
            throw UnauthorizedException("비활성화된 계정입니다.")
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        val expireMillis = jwtProperties.accessTokenExpireMinutes * 60 * 1000
        val now = Date()
        val expiry = Date(now.time + expireMillis)

        val token = Jwts.builder()
            .subject(user.userId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()

        return LoginResponse(
            userId = user.userId,
            accessToken = token,
            expiresIn = expireMillis / 1000,
        )
    }
}
