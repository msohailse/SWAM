package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.application.port.out.UserRepositoryPort;
import com.msohailse.app.incident.domain.Tag;
import com.msohailse.app.incident.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class TagService {

	@Inject
	TagRepositoryPort tagRepository;

	@Inject
	UserRepositoryPort userRepository;

	// Tag management here (create/update/delete) is admin-only. This is separate from the
	// find-or-create-by-title that happens inline in IncidentService.create() when a
	// reporter types a new tag while filing an incident — that stays open to any reporter.
	private void requireActiveAdmin(int actingUserId) {
		User actingUser = userRepository.findById(actingUserId);
		if (actingUser == null || !actingUser.isActiveAdmin()) {
			throw new IllegalArgumentException("Only an admin can manage tags");
		}
	}

	@Transactional
	public Tag create(int actingUserId, String tagTitle, String tagDescription) {
		requireActiveAdmin(actingUserId);
		Tag existing = tagRepository.findByTitle(tagTitle);
		if (existing != null) {
			return existing;
		}
		Tag tag = new Tag();
		tag.setTagTitle(tagTitle);
		tag.setTagDescription(tagDescription);
		tagRepository.save(tag);
		return tag;
	}

	public Tag findById(int id) {
		return tagRepository.findById(id);
	}

	public List<Tag> findAll() {
		return tagRepository.findAll();
	}

	@Transactional
	public Tag update(int actingUserId, int id, String tagTitle, String tagDescription) {
		requireActiveAdmin(actingUserId);
		Tag tag = tagRepository.findById(id);
		if (tag == null) {
			throw new IllegalArgumentException("Tag not found: " + id);
		}
		tag.setTagTitle(tagTitle);
		tag.setTagDescription(tagDescription);
		tagRepository.update(tag);
		return tag;
	}

	@Transactional
	public void delete(int actingUserId, int id) {
		requireActiveAdmin(actingUserId);
		tagRepository.delete(id);
	}
}
