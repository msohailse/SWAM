package com.msohailse.app.incident.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.msohailse.app.incident.domain.Severity;

@Entity
@Table(name="incidents")
public class Incident {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;

	@Column(length=200, nullable=false)
	private String title;

	@Column(length=2000, nullable=true)
	private String description;

	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private Severity severity;

	@Column(nullable=false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date reportedAt;

	@Column(nullable=false)
	private boolean isClosed;

	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private User reportedBy;

	@ManyToOne
	@JoinColumn(name="tag_id", nullable=false)
	private Tag tag;

	public Incident() {
		this.reportedAt = new Date();
		this.isClosed = false;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		checkIfEmpty(title, "title");
		String normalized = trimAllSpaces(title);
		checkIfEmpty(normalized, "title");
		this.title = normalized;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description == null) {
			this.description = null;
			return;
		}
		this.description = trimAllSpaces(description);
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		if (severity == null) {
			throw new IllegalArgumentException("Empty severity");
		}
		this.severity = severity;
	}

	public Date getReportedAt() {
		return reportedAt;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setIsClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	public User getReportedBy() {
		return reportedBy;
	}

	public void setReportedBy(User reportedBy) {
		if (reportedBy == null) {
			throw new IllegalArgumentException("Reporter cannot be null");
		}
		this.reportedBy = reportedBy;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		if (tag == null) {
			throw new IllegalArgumentException("Tag cannot be null");
		}
		this.tag = tag;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(severity).append("] ").append(title)
		  .append(" Description: ").append(description)
		  .append(" Category: ").append(tag == null ? "null" : tag.getTagTitle())
		  .append(" at ").append(reportedAt);
		return sb.toString();
	}

	
	// helper methods

	private static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

	private void checkIfEmpty(String value, String fieldName) {
		if (isNullOrEmpty(value)) {
			throw new IllegalArgumentException("Empty " + fieldName);
		}
	}

	// we start from empty string builder and
	// if we found white space in original value we add one and mark lastWasSpace true
	// if we found another whitespace we don't append and keep the lastWasSpace true
	// we keep ignoring the white space if lastWasSpace = true until we get actual char
	private String trimAllSpaces(String value) {
		StringBuilder sb = new StringBuilder();
		boolean lastWasSpace = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (Character.isWhitespace(c)) {
				if (!lastWasSpace && sb.length() > 0)
					sb.append(' ');
				lastWasSpace = true;
			} else {
				sb.append(c);
				lastWasSpace = false;
			}
		}
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ')
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

}
