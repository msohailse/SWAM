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
		Date defaultExpiration = new Date(now.getTime() + Duration.ofHours(24).toMillis());
		Date adminExpiresAt = user.getAdminExpiresAt();

		// A time-boxed admin grant shouldn't outlive itself just because the token has a
		// longer default lifetime — cap the token's own expiration at the grant's expiry.
		Date expiration = (adminExpiresAt != null && adminExpiresAt.before(defaultExpiration))
				? adminExpiresAt
				: defaultExpiration;

		var builder = Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("userType", user.getUserType().name())
				.issuedAt(now)
				.expiration(expiration);
		if (adminExpiresAt != null) {
			builder.claim("adminExpiresAt", adminExpiresAt);
		}
		return builder.signWith(key).compact();
	}
}
