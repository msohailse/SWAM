package com.msohailse.app.incident.adapters.in.rest;

import com.msohailse.app.incident.application.service.DepartmentService;
import com.msohailse.app.incident.domain.Department;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/departments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DepartmentResource {

	@Inject
	DepartmentService departmentService;

	public record CreateDepartmentRequest(String name, String description) {}

	@POST
	public Department create(CreateDepartmentRequest request) {
		return departmentService.create(request.name(), request.description());
	}

	@GET
	public List<Department> findAll() {
		return departmentService.findAll();
	}

	@GET
	@Path("/{id}")
	public Department findById(@PathParam("id") int id) {
		return departmentService.findById(id);
	}
}
