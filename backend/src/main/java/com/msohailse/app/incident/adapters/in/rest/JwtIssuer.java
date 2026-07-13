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

	// Two independent lifetimes on the same token: `exp` is always the 24h session length
	// (when the user has to log in again, regardless of role). `adminExpiresAt` is a
	// separate claim carrying the time-boxed admin grant — whoever enforces the token later
	// checks that claim on top of `exp` to decide whether admin actions are still allowed,
	// without forcing a fresh login just because the grant lapsed mid-session.
	String issue(User user) {
		Key key = Keys.hmacShaKeyFor(secret.getBytes());
		Date now = new Date();
		Date sessionExpiration = new Date(now.getTime() + Duration.ofHours(24).toMillis());
		Date adminExpiresAt = user.getAdminExpiresAt();

		var builder = Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("userType", user.getUserType().name())
				.issuedAt(now)
				.expiration(sessionExpiration);
		if (adminExpiresAt != null) {
			builder.claim("adminExpiresAt", adminExpiresAt);
		}
		return builder.signWith(key).compact();
	}
}
