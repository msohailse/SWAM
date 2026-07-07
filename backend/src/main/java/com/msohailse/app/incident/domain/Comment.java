package com.msohailse.app.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Column(length = 2000, nullable = false)
	private String text;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@ManyToOne
	@JoinColumn(name = "incident_id", nullable = false)
	private Incident incident;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	public Comment() {
		this.createdAt = new Date();
	}

	public int getId() {
		return id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty text");
		}
		this.text = text.trim();
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public Incident getIncident() {
		return incident;
	}

	public void setIncident(Incident incident) {
		if (incident == null) {
			throw new IllegalArgumentException("Incident cannot be null");
		}
		this.incident = incident;
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		if (author == null) {
			throw new IllegalArgumentException("Author cannot be null");
		}
		this.author = author;
	}
}
