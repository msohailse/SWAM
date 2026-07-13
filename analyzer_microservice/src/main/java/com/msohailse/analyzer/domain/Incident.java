package com.msohailse.analyzer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Analyzer-service's own read-mostly view of the "incidents" table it shares with
// api-service. Deliberately minimal — each service owns its own model of the shared
// schema rather than sharing Java classes, which is the normal (and simpler) way to keep
// two independently-deployable services from being coupled at the code level.
@Entity
@Table(name = "incidents")
public class Incident {

	@Id
	private int id;

	@Column(nullable = false)
	private String title;

	private String description;

	@Column(nullable = false)
	private boolean isClosed;

	public Incident() {
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isClosed() {
		return isClosed;
	}
}
