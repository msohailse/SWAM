package com.msohailse.app.incident.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.msohailse.app.incident.domain.Tag;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TagPostgresRepositoryTest {

	@Inject
	TagPostgresRepository tagRepo;

	@Test
	@TestTransaction
	void saveAssignsAnId() {
		Tag tag = new Tag();
		tag.setTagTitle("Fire");

		tagRepo.save(tag);

		assertThat(tag.getId()).isGreaterThan(0);
	}

	@Test
	@TestTransaction
	void findByIdReturnsSavedTag() {
		Tag tag = new Tag();
		tag.setTagTitle("Theft");
		tag.setTagDescription("Incidents related to theft");
		tagRepo.save(tag);

		Tag retrieved = tagRepo.findById(tag.getId());

		assertThat(retrieved.getTagTitle()).isEqualTo("Theft");
		assertThat(retrieved.getTagDescription()).isEqualTo("Incidents related to theft");
	}

	@Test
	@TestTransaction
	void findByIdReturnsNullForNonExistentId() {
		assertThat(tagRepo.findById(99999)).isNull();
	}

	@Test
	@TestTransaction
	void findAllReturnsAllSavedTags() {
		Tag fire = new Tag();
		fire.setTagTitle("Fire");
		Tag flood = new Tag();
		flood.setTagTitle("Flood");
		tagRepo.save(fire);
		tagRepo.save(flood);

		List<Tag> tags = tagRepo.findAll();

		assertThat(tags).extracting(Tag::getTagTitle).contains("Fire", "Flood");
	}

	@Test
	@TestTransaction
	void updateChangesTitleAndDescription() {
		Tag tag = new Tag();
		tag.setTagTitle("Vandalism");
		tagRepo.save(tag);

		tag.setTagTitle("Vandalism (updated)");
		tag.setTagDescription("Now with a description");
		tagRepo.update(tag);

		Tag retrieved = tagRepo.findById(tag.getId());
		assertThat(retrieved.getTagTitle()).isEqualTo("Vandalism (updated)");
		assertThat(retrieved.getTagDescription()).isEqualTo("Now with a description");
	}

	@Test
	@TestTransaction
	void deleteRemovesTag() {
		Tag tag = new Tag();
		tag.setTagTitle("Temporary");
		tagRepo.save(tag);
		int id = tag.getId();

		tagRepo.delete(id);

		assertThat(tagRepo.findById(id)).isNull();
	}
}
