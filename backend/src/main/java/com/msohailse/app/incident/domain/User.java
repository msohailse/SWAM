package com.msohailse.app.incident.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name="users")
public class User {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

    @Column(length=50, nullable=false, unique=false)
	private String firstName;

    @Column(length=50, nullable=false, unique=false)
	private String lastName;


    @Column(nullable=false, unique=true, length=255)
    private String email;

    @Column(nullable=false, unique=false, length=255)
	private String password;

	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private UserType userType;

	@ManyToOne
	@JoinColumn(name="department_id", nullable=true)
	private Department department;

	// Only ever meaningful for ADMIN — null means the grant doesn't expire. SUPER_ADMIN
	// and REPORTER should always leave this null.
	@Column(nullable=true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date adminExpiresAt;

	public User() {
		this.userType = UserType.REPORTER;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		validateNameString(firstName);
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		validateNameString(lastName);
		this.lastName = lastName;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		validateEmailString(email);
		this.email = email;
	}

	@JsonIgnore
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		validatePasswordString(password);
		this.password = password;
	}

	public UserType getUserType() {
		return this.userType;
	}

	public void setUserType(UserType userType) {
		if (userType == null) {
			throw new IllegalArgumentException("Empty user type");
		}
		this.userType = userType;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	public Date getAdminExpiresAt() {
		return adminExpiresAt;
	}

	public void setAdminExpiresAt(Date adminExpiresAt) {
		this.adminExpiresAt = adminExpiresAt;
	}

	// Domain helpers, not part of the JSON contract — @JsonIgnore because "active" is only
	// true as of the moment this is called; a value serialized once at login time would go
	// stale as soon as an expiry passes, which is exactly the bug this whole feature exists
	// to avoid. Clients must recompute liveness themselves from adminExpiresAt (see
	// AuthService.isAdmin() on the frontend), not trust a cached boolean.
	@JsonIgnore
	public boolean isSuperAdmin() {
		return userType == UserType.SUPER_ADMIN;
	}

	// SUPER_ADMIN is always active. ADMIN is active unless its expiry has passed.
	// REPORTER is never an active admin.
	@JsonIgnore
	public boolean isActiveAdmin() {
		if (userType == UserType.SUPER_ADMIN) {
			return true;
		}
		if (userType != UserType.ADMIN) {
			return false;
		}
		return adminExpiresAt == null || adminExpiresAt.after(new Date());
	}

	// helper methods can be moved to a separate utility class if needed in future
	/**
	 * DRY (don't repeat principle) we will use same function to validate both first
	 * and last name since they have same validation rules. Helper method to
	 * validate name strings. Throws IllegalArgumentException if validation fails.
	 */
	private void validateNameString(String name) {
		if (name == null || name.trim().isEmpty()) {
			// name is empty
			throw new IllegalArgumentException("Empty name");
		}
		int i = 0;
		// since the name is not empty now we can look further
		while (i < name.length()) {
			if (Character.isWhitespace(name.charAt(i))) {
				throw new IllegalArgumentException("White space found");
			}
			i++;
		}
	}

	// helper method to validate email string
	private void validateEmailString(String email) {
		// email validation logic can be implemented here
		if (email == null || email.trim().isEmpty()) {
			// email is empty
			throw new IllegalArgumentException("Empty email");
		}
		// further email validation can be added here (e.g. regex check for valid email
		// format)
		email = email.trim();
		if (!email.contains("@") || !email.contains(".")) {
			throw new IllegalArgumentException("Invalid email format");
		}
	}

	// helper method to validate password
	// password has to be
	private void validatePasswordString(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Empty password");
		}
		if (password.length() < 8) {
			throw new IllegalArgumentException("Password too short");
		}
		boolean hasDigit = false;
		boolean hasUpper = false;
		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			if (Character.isDigit(c))
				hasDigit = true;
			if (Character.isUpperCase(c))
				hasUpper = true;
		}
		if (!hasDigit) {
			throw new IllegalArgumentException("Password must contain a digit");
		}
		if (!hasUpper) {
			throw new IllegalArgumentException("Password must contain an uppercase letter");
		}
	}

}
