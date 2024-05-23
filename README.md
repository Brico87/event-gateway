# Event-Gateway project
####
Reference Documentation: for further reference, please consider the following sections:
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.5/maven-plugin/reference/html/#build-image)
####
Objectives:
- a client can post event through an gateway using HTTP protocol
- a client can subscribe to an event stream and get event data through webhooks
- a client can unsubscribe to a specific event stream
- the system administrator can add a reference link between an event, a topic to listen and the schema to use
####
Implementation:
- when a client post an event, the JSON payload will be converted to an Avro message and sent into the Kafka cluster
- when a client subscribes to an event stream, he will indicate the event name and the webhook to call and:
  - we will store that webhook into a table linking the event name to the target
  - we will start a new consumer (or restart the consumer) with the topic to observe
- when a event comes for a client, we need to look:
  - to our reference table to link the event name to the matching schema to convert,
  - to the subscription table to link the event to the webhook
  - send the JSON payload to the webhook extracted from the Avro message
####
Local infra stack:
####
To start the local stack:
```
docker network create -d bridge event-gateway
docker-compose up -d
```
####
Once everything starts, check on http://localhost:9082/ to get the cluster state and:
- create the subject "com.demo.schema.TestPayload" into the schema registry using the management window ("Create a Subject" button on the right-hand bottom corner and paste the "test-schema.avsc" content)
- create the topic "test-topic" using the management window ("Create a topic" button)
####
To stop the local stack:
```
docker-compose down
```
####

