package com.msohailse.app.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name="tags")
public class Tag {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	

	public Tag() {}

	public int getId() {
		return id;
	}

	@Column(length=100, nullable=false, unique=false)
	private String tagTitle;

	public String getTagTitle() {
		return tagTitle;
	}

	public void setTagTitle(String tagTitle) {
		checkIfEmpty(tagTitle, "title");
		String normalized = trimAllSpaces(tagTitle);
		checkIfEmpty(normalized, "title");
		this.tagTitle = normalized;
	}

	@Column(length=500, nullable=true, unique=false)
	private String tagDescription;
	

	public String getTagDescription() {
		return tagDescription;
	}

	public void setTagDescription(String tagDescription) {
		if (tagDescription == null) {
			this.tagDescription = null;
			return;
		}
		this.tagDescription = trimAllSpaces(tagDescription);
	}
	
	
	// helper methods
	// check if empty or null
	static boolean isNullOrEmpty(String sb) {
	    return sb == null || sb.length() == 0 || sb.isEmpty();
	}
			
	private String getDefaultValueIfEmpty(String fieldName) {
		if (isNullOrEmpty(fieldName)) {
			fieldName = "Required field";
		}
		return fieldName;
	}
			
	private void checkIfEmpty(String value, String fieldName) {
		fieldName = getDefaultValueIfEmpty(fieldName);
		if (isNullOrEmpty(value)) {
			throw new IllegalArgumentException("Empty " + fieldName);
		}
	}
	
	
	// trim all white spaces
	// we start from empty string builder and 
	// if we found white space in original value we add one and mark lastWasSpace true
	// if we found another whitespace we don't append and keep the lastWasSpace true
	// we keep ignoring the white space if lastWasSpace = true until we get actual
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
