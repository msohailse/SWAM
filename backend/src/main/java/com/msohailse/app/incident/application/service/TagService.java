package com.msohailse.app.incident.application.service;

import com.msohailse.app.incident.application.port.out.TagRepositoryPort;
import com.msohailse.app.incident.domain.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class TagService {

	@Inject
	TagRepositoryPort tagRepository;

	@Transactional
	public Tag create(String tagTitle, String tagDescription) {
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
	public Tag update(int id, String tagTitle, String tagDescription) {
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
	public void delete(int id) {
		tagRepository.delete(id);
	}
}
