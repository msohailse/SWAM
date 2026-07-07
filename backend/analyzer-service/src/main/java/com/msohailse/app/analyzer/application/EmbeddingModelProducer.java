package com.msohailse.app.analyzer.application;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

// The raw langchain4j jar isn't a Quarkus/CDI extension, so it doesn't register itself
// as an injectable bean — this producer is what makes @Inject EmbeddingModel work.
// Model loading is somewhat expensive, so @ApplicationScoped keeps it to one instance.
public class EmbeddingModelProducer {

	@Produces
	@ApplicationScoped
	public EmbeddingModel embeddingModel() {
		return new AllMiniLmL6V2EmbeddingModel();
	}
}
