package com.msohailse.app.analyzer.domain;

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

	@Column(nullable = false)
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

	public void setText(String text) {
		this.text = text;
	}

	public void setIncident(Incident incident) {
		this.incident = incident;
	}

	public void setAuthor(User author) {
		this.author = author;
	}
}
