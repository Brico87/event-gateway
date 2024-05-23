# Event-Gateway project
####
Objectives:
- a client can post event through an gateway using HTTP protocol
- a client can subscribe to event stream and get event data through webhooks
- a client can unsubscrube to a specific event stream
####
Local infra stack:
####
To start the local stack:
```
docker network create -d bridge event-gateway
docker-compose up -d
```
####
Once everything starts, check on http://localhost:9082/ to get the cluster state.
####
To stop the local stack:
```
docker-compose down
```
####

