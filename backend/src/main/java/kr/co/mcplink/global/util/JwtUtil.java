package kr.co.mcplink.global.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import kr.co.mcplink.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private SecretKey secretKey;
	@Value("${jwt.expiration-ms}")
	private long expirationMs;

	public String generateToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId())
			.claim("name", user.getName())
			.claim("role", user.getRole())
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey)
			.compact();
	}
	// public String generateRefreshToken(User user) { // 리프레시 토큰 생성 로직 (필요시)
	//     Date now = new Date();
	//     Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
	//
	//     return Jwts.builder()
	//             .subject(user.getEmail())
	//             .issuedAt(now)
	//             .expiration(expiryDate)
	//             .signWith(secretKey)
	//             .compact();
	// }

	public Claims getClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public String getEmailFromToken(String token) {
		return getClaims(token).getSubject();
	}

	public Long getUserIdFromToken(String token) {
		return getClaims(token).get("userId", Long.class);
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
			return true;
		} catch (SignatureException ex) {
			log.error("Invalid JWT signature");
		} catch (MalformedJwtException ex) {
			log.error("Invalid JWT token");
		} catch (ExpiredJwtException ex) {
			log.error("Expired JWT token");
		} catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT token");
		} catch (IllegalArgumentException ex) {
			log.error("JWT claims string is empty.");
		}
		return false;
	}

}
