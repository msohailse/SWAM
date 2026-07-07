Project Title: Scalable Event Driven Incident Management System
This proposal outlines the design and implementation of a scalable Incident Reporting System focused on high throughput event processing via messaging queue and intelligent data consistency. The architecture uses modern patterns to manage complex interactions and infrastructure orchestration effectively.


1. System Domain and Functional Logic

The application facilitates the reporting of incidents submitted by users through a categorized model. The domain is built upon three primary entities: Incident, User, and Tag. Each incident record captures essential metadata including title, description, severity, and a resolution timestamp. Relationships between entities are managed through a many to many relational mapping to support flexible classification.

2. Advanced Architectural Patterns
To address the challenges of software intensive systems, the project adopts the following architectural styles:

Event Driven Architecture EDA:

The system decouples the ingestion of reports from the processing logic using a messaging backbone.

Hexagonal Architecture:

Core business rules are isolated within a domain layer, interacting with external services through abstract ports and concrete adapters.

Command Query Responsibility Segregation CQRS:

Write operations for new reports are separated from read operations for analytical querying to enhance performance.

3. Intelligent Event Processing and Deduplication
The system includes a specialized Analyzer Service designed to maintain data integrity during high traffic periods:

REST API Ingestion:

Users submit reports through a standardized RESTful interface. Upon submission, the system returns a status code indicating the report is being processed.

Asynchronous Messaging:

Reports are published to Apache Kafka topics to prevent system bottlenecks during traffic spikes.

Analyzer Service: 

This component consumes reports from the queue and performs contextual analysis. It evaluates the content to identify if an incident is a duplicate of an existing case.

Case Number Assignment:

If the report is unique, a new Case Number is generated. If a duplicate is identified, the report is linked to the existing Case Number.

State Retrieval:

Users can retrieve the final Case Number and status of their report via subsequent GET requests to the REST API.

4. Technology Stack
Backend:

 Developed in Java using Jakarta Enterprise Architecture principles.

Persistence:

PostgreSQL serves as the primary database. JPA Hibernate is used for object relational mapping.

Relational Transaction Management:

Transactions are implemented strictly to ensure data consistency during concurrent operations. Specific focus is placed on the ACID properties of the Incident to Tag mapping, ensuring that the creation of an incident and its associated tags occurs within a single unit of work to prevent partial data states.

Frontend:

A single page application is built using the Angular framework to provide a reactive user experience.

Orchestration:

The system is fully containerized using Docker and managed through Kubernetes.

5.  Infrastructure and Orchestration
The deployment strategy focuses on cloud native principles and performance engineering:

Containerization:

All components including the Kafka brokers and Analyzer services are containerized via Docker.

Kubernetes Orchestration:

The system is managed through Kubernetes manifests defining deployments and services and stateful sets.

Scaling and Measurement:

I will implement Horizontal Pod Autoscaling HPA to scale the Analyzer services based on the message backlog in Kafka. The project report will include performance measurements of scaling latency and system throughput under synthetic workloads.

And For Technical Report I was told to do below things
objective and purpose of the project work
 software requirements analysis (functional and non functional requirements)
use case analysis (with UML Use Case Diagram)
domain modeling (with UML Class Diagram)
preliminary/conceptual design
detailed design
application architecture
database design (ER-diagram)
frontend mockup design
description of the main components of the software implementation
 frontend preview of the UI
future developments and conclusions

Thank you for your time and considerations


and Proff response is below

The work you propose is very interesting and the hexagonal architecture is "rare" in development projects;
it can be considered an excellent operative scenario for the SWAM exam (typically, a project of this "size" is not required).
The selected design patterns, frameworks and technologies are state of the art for event-driven architectures.

I believe you have experience in design and development, judging by how you describe the project in your document.
This will be crucial for tackling this "non-trivial" project.

Remember that you don't have to fully implement your design; a prototype with limited functionality may suffice.
But if you are motivated you can easily proceed as you planned.

I will try to give you a clarification about the SWAM exam.

As a premise: there is only one exam - do not consider if you find into Moodle more sub-modules or channels - the main teacher of SWAM is Professor Vicario;
the other teachers (like me) are assistants.

To pass the SWAM course, you must complete a “project work” (like the one you proposed).

You have to write a technical report, including (if you deem it necessary):
- objective and purpose of the project work
- software requirements analysis (functional and non functional requirements)
- use case analysis (with UML Use Case Diagram)
- domain modeling (with UML Class Diagram)
- preliminary/conceptual design
- detailed design
- application architecture
- database design (ER-diagram)
- frontend mockup design
- description of the main components of the software implementation
- frontend preview of the UI
- future developments and conclusions

You have also to prepare a presentation, with a summary of the work done (an exposure time of approximately 20 minutes is recommended).