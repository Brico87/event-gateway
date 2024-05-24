# Event-Gateway project
####
Reference Documentation: for further reference, please consider the following sections:
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.5/maven-plugin/reference/html/#build-image)
####
**Objectives:**
- [x] a client can post event through an gateway using HTTP protocol
- [x] a client can subscribe to an event stream and get event data through webhooks
- [ ] a client can unsubscribe to a specific event stream (would need a link between the client ID and the webhook)
- [x] the system administrator can add a reference link between an event, a topic to listen and the schema to use
####
**Implementation:**
- [x] when a client post an event, the JSON payload will be converted to an Avro message and sent into the Kafka cluster
- when a client subscribes to an event stream, he will indicate the event name and the webhook to call and:
  - [x] we will store that webhook into a table linking the event name to the target
  - [x] we will start a new consumer (or restart the existing consumer) with the new topic to observe
- when a event comes for a client, we need to look:
  - [x] to our reference table to link the event name to the matching schema to convert,
  - [x] to the subscription table to link the event to the webhook
  - [x] send the JSON payload to the webhook extracted from the Avro message
####
**Local infra stack:**
####
To start the local stack:
```
docker network create -d bridge event-gateway
docker-compose up -d
```
####
Once everything starts, check on http://localhost:9082/ to get the cluster state and:
- create the subject "com.demo.schema.TestPayload" into the schema registry using the management window ("Create a Subject" button on the right-hand bottom corner and paste the "test-payload.avsc" content)
- create the topic "test-topic" using the management window ("Create a topic" button)
####
To check the Redis database content, this software will do the job: https://github.com/qishibo/AnotherRedisDesktopManager.
####
To stop the local stack:
```
docker-compose down
```
####
**Test scenario:**
####
- Prepare a mock server on the platform of your choice and get a POST route ready
- On the producer app, create an event/topic mapping for the event "test" published on the "test-topic" using the schema ID 1:
```
curl --location --request POST 'http://localhost:8080/mapping/test/test-topic/1'
```
- Check on the Redis explorer that the "EventMapping:test" has been created
- On the consumer app, create a subscription for the event "test":
```
curl --location 'http://localhost:8082/subscription/test' \
--header 'Content-Type: application/json' \
--data '{
    "callbackUrl": "MOCK SERVER ROUTE"
}'
```
- Check on the Redis explorer that the "EventSubscription:test-topic" has been created
- On the producer app, send an event "test" to be dispatched:
```
curl --location 'http://localhost:8080/event/test' \
--header 'Content-Type: application/json' \
--data '{
    "id": 31564,
    "data": "blablabla"
}'
```
- Check on the mock server that the event payload is well transmitted