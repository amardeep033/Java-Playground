0. pre-requisites:
- this project is for sde 2 switch, something which i can demo and put it in resume
- we will host this project within budget of 1500
- we will only build main service, curl for source and ngrok/webhook.site for destination in demo
- we will deploy it as docker image
- Note: we won't be forcing technology, will only include if it make sense, and keep it simple but code should be of good quality and app should be robust(less feature but stable and working)
- we are covering all three communication : rest(sync), event(async), callback
- we are also covering all important stuffs like rate limiter, circuit breaker, retry policy, cache policy, outbox, dlq, obervability, idempotency, jwt, api key, cqrs

1. svix like webhook platflorm:
- user registration, app registration, api key management and settings like retry, rate limits
- register/manage webhook, events, signing secret, test
- test event and monitor its journey/status
- replay event and manually retry failed dlq
- analytics dashboard/metrics, consumer health, registered app health
- rbac of screen, jwt, api key, nginx

2. we are maintaining two services(two dbs):
- event service - which has two thread pool - rcvr(n) and outbox poller and publisher(1) -- idempotent on our side
- delivery service - which has one thread pool - rcvr and sender (Bounded in-process work queue)  -- AMQP already supports concurrent consumers and we didn't want in mem queue full and for delayed retry wanted to make use of rabbitmq queue itself instead of self managing -- idempotent on dest side (we work on atleast one delivery mode)
- the split was done because sending can be hot path and we wanted them to be independently deployable for scale
- both services communicates via rabbitmq - contains three queues: main(we only listen to), retry(rabbitmq moves to main) and dlq(user manually move main)

3. future:
- delivery service can be converted into rust
- convert poller part to cdc abd thus rabbitmq will change to kafka for debezium native support
- in delivery service, introduce two pools for rcvr and worker separate
- intoduce rag
- introduce clickhouse
- integrate workflow service like temporal for user to define state machine
- maybe introduce grpc too for sync call between services

4. Notes:
- in code side, make use of solid principal and lld pattern wherever suitable
- we will be making use of redis for attempt counter and 
- we will be making use of otel for monitoring and metrics dashboard, also have projection(cqrs) for same wherever applicable
- we will be making use of postgres db 

5. state machine:
- status of a job: img/app.png
- status in rabbitmq: img/rabbitmq.png
- circuit breaker: at customer endpoint level, whereas dlq at event level

----------------------------------------

no 
i want three things : 
1. track one complete flow of app with smallest details like if then this path else this path and so on 
2. then list down the table belonging in both the service and APIs list
3. list down the phases of building it, using ai, like implement x then integrate y and now add z type.. so that i can compare using git in vs and see how and what is changing