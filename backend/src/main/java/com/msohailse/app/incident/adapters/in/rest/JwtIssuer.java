package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

// Tier A: issues a signed JWT on login so the frontend has a real token to carry, but
// nothing validates it yet — the actingUserId-based checks in each service remain the
// actual authorization boundary.
@ApplicationScoped
public class JwtIssuer {

	@ConfigProperty(name = "jwt.secret")
	String secret;

	String issue(User user) {
		Key key = Keys.hmacShaKeyFor(secret.getBytes());
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("userType", user.getUserType().name())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + Duration.ofHours(24).toMillis()))
				.signWith(key)
				.compact();
	}
}
