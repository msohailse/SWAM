package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.application.service.UserService;
import com.msohailse.app.incident.domain.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

	@Inject
	UserService userService;

	public record RegisterRequest(String firstName, String lastName, String email, String password) {}
	public record LoginRequest(String email, String password) {}

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

	@POST
	@Path("/login")
	public User login(LoginRequest request) {
		return userService.login(request.email(), request.password());
	}
}
