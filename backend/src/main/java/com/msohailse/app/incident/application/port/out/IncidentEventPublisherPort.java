package com.msohailse.app.incident.application.port.out;

public interface IncidentEventPublisherPort {
	void publishIncidentCreated(int incidentId, String title, String description, String tagTitle);
}
