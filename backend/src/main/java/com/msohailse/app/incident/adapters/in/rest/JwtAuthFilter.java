package com.msohailse.app.incident.adapters.in.rest;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import javax.crypto.SecretKey;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// Tier B: the actual enforcement gate for the tokens JwtIssuer signs. Only checks that a
// request carries a validly-signed, non-expired token — it does not derive identity from
// the token to replace the actingUserId fields the *Service classes already check; that
// remains a separate, larger change.
@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class JwtAuthFilter implements ContainerRequestFilter {

	@ConfigProperty(name = "jwt.secret")
	String secret;

	@Override
	public void filter(ContainerRequestContext ctx) {
		String method = ctx.getMethod();
		String path = ctx.getUriInfo().getPath();

		if ("GET".equals(method)) {
			return;
		}
		if ("POST".equals(method) && ("/users/login".equals(path) || "/users/register".equals(path))) {
			return;
		}

		String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}

		String token = authHeader.substring("Bearer ".length());
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		} catch (JwtException | IllegalArgumentException e) {
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		}
	}
}
