package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.application.service.UserService;
import com.msohailse.app.incident.domain.User;
import com.msohailse.app.incident.domain.UserType;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

	@Inject
	UserService userService;

	@Inject
	JwtIssuer jwtIssuer;

	public record RegisterRequest(String firstName, String lastName, String email, String password) {}
	public record LoginRequest(String email, String password) {}
	public record CreateUserRequest(int actingUserId, String firstName, String lastName, String email,
			String password, UserType userType, Integer departmentId, Integer adminExpiresInDays) {}

	@GET
	public List<User> findAll() {
		return userService.findAll();
	}

	@GET
	@Path("/{id}")
	public User findById(@PathParam("id") int id) {
		return userService.findById(id);
	}

	@POST
	@Path("/register")
	public User register(RegisterRequest request) {
		return userService.register(request.firstName(), request.lastName(), request.email(), request.password());
	}

	// Super-admin-only — creates an ADMIN (with a required department, optional
	// time-boxed expiry) or another SUPER_ADMIN. Public /register above never creates
	// anything but a REPORTER.
	@POST
	@Path("/admin-create")
	public User createUser(CreateUserRequest request) {
		return userService.createUser(request.actingUserId(), request.firstName(), request.lastName(),
				request.email(), request.password(), request.userType(), request.departmentId(),
				request.adminExpiresInDays());
	}

	// Token rides on the Authorization response header rather than the body, so the
	// response shape (and every existing caller/test expecting a plain User) is unchanged.
	@POST
	@Path("/login")
	public Response login(LoginRequest request) {
		User user = userService.login(request.email(), request.password());
		String token = jwtIssuer.issue(user);
		return Response.ok(user).header("Authorization", "Bearer " + token).build();
	}
}
