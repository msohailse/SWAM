package com.msohailse.app.incident.domain;

// SUPER_ADMIN sees/manages every incident and is the only type that can assign or
// reassign a department; ADMIN is scoped to its own department (see User.department) and
// can optionally expire (see User.adminExpiresAt) — expiry only ever applies to ADMIN,
// never SUPER_ADMIN.
public enum UserType {
	REPORTER, ADMIN, SUPER_ADMIN
}
