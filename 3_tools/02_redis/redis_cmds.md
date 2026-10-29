# Redis Commands: Spring Data Redis And Lua

Operation mappings for the project's `StringRedisTemplate`; Java and Lua return types can differ.
`key` maps to `KEYS[1]`, a second key to `KEYS[2]`; dynamic values normally come from `ARGV`.
Names such as `value`, `field`, `member`, `score`, and `id` are illustrative variables.
Spring can issue equivalent commands internally, including millisecond `PX` instead of `EX`.

**Argument meaning depends on the command:** `LREM key 0 value` removes all occurrences of
that value, not the entire list. `HDEL key 0` removes the field named `0`, not all fields.
Use `DEL key` to remove the entire hash/list/key.

## Strings / counters

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForValue().get(key)` | `redis.call('GET', KEYS[1])` | missing key returns nil in Lua, null in Java |
| `redis.opsForValue().set(key, value)` | `redis.call('SET', KEYS[1], value)` | overwrite value, clear old TTL unless specified |
| `redis.opsForValue().set(key, value, Duration.ofSeconds(60))` | `redis.call('SET', KEYS[1], value, 'EX', 60)` | overwrite value, EX 60 sets 60-second TTL, one command |
| `redis.opsForValue().setIfAbsent(key, value)` | `redis.call('SET', KEYS[1], value, 'NX')` | NX only if absent, atomic check + write, no expiry assigned |
| `redis.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(60))` | `redis.call('SET', KEYS[1], value, 'NX', 'EX', 60)` | NX only if absent, atomic check + write, EX 60 sets 60-second TTL in same command |
| `redis.opsForValue().setIfPresent(key, value, Duration.ofSeconds(60))` | `redis.call('SET', KEYS[1], value, 'XX', 'EX', 60)` | XX only if present, EX 60 replaces TTL, atomic conditional write |
| `redis.opsForValue().increment(key)` | `redis.call('INCR', KEYS[1])` | atomic +1, missing starts at 0, existing TTL preserved |
| `redis.opsForValue().increment(key, 5L)` | `redis.call('INCRBY', KEYS[1], 5)` | atomic +5, integer counter, existing TTL preserved |
| `redis.opsForValue().increment(key, 1.5)` | `redis.call('INCRBYFLOAT', KEYS[1], 1.5)` | atomic +1.5, decimal increment, existing TTL preserved |
| `redis.opsForValue().decrement(key)` | `redis.call('DECR', KEYS[1])` | atomic -1, missing starts at 0, existing TTL preserved |
| `redis.opsForValue().decrement(key, 5L)` | `redis.call('DECRBY', KEYS[1], 5)` | atomic -5, missing starts at 0, existing TTL preserved |
| `redis.opsForValue().append(key, value)` | `redis.call('APPEND', KEYS[1], value)` | append string, create if missing, return byte length |
| `redis.opsForValue().size(key)` | `redis.call('STRLEN', KEYS[1])` | byte length, missing returns 0 |
| `redis.opsForValue().multiGet(List.of(key1, key2))` | `redis.call('MGET', KEYS[1], KEYS[2])` | multiple keys, input order preserved, nil for missing values |
| `redis.opsForValue().multiSet(Map.of(key1, v1, key2, v2))` | `redis.call('MSET', KEYS[1], v1, KEYS[2], v2)` | atomic multi-key write, replaces values, clears affected TTLs |

## Keys / expiry

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.hasKey(key)` | `redis.call('EXISTS', KEYS[1])` | key existence, Redis integer reply, Java Boolean |
| `redis.delete(key)` | `redis.call('DEL', KEYS[1])` | delete whole key, return deleted-key count |
| `redis.delete(List.of(key1, key2))` | `redis.call('DEL', KEYS[1], KEYS[2])` | delete both whole keys, return deleted-key count |
| `redis.unlink(key)` | `redis.call('UNLINK', KEYS[1])` | remove key immediately, reclaim memory asynchronously |
| `redis.type(key)` | `redis.call('TYPE', KEYS[1])` | value type, missing returns none |
| `redis.expire(key, Duration.ofSeconds(60))` | `redis.call('EXPIRE', KEYS[1], 60)` | seconds, replace existing TTL, nonpositive duration deletes key |
| `redis.expire(key, Duration.ofMillis(1500))` | `redis.call('PEXPIRE', KEYS[1], 1500)` | milliseconds, 1500 = 1.5 seconds |
| `redis.getExpire(key)` | `redis.call('TTL', KEYS[1])` | seconds, -1 no expiry, -2 missing key |
| `redis.getExpire(key, TimeUnit.MILLISECONDS)` | `redis.call('PTTL', KEYS[1])` | milliseconds, -1 no expiry, -2 missing key |
| `redis.persist(key)` | `redis.call('PERSIST', KEYS[1])` | remove expiry, keep value |
| `redis.rename(key1, key2)` | `redis.call('RENAME', KEYS[1], KEYS[2])` | overwrite destination, source must exist, source TTL follows value |
| `redis.renameIfAbsent(key1, key2)` | `redis.call('RENAMENX', KEYS[1], KEYS[2])` | rename only if destination absent, source must exist |

## Hashes

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForHash().get(key, field)` | `redis.call('HGET', KEYS[1], field)` | read one field, missing field returns nil |
| `redis.opsForHash().put(key, field, value)` | `redis.call('HSET', KEYS[1], field, value)` | create or replace fields, keep other fields |
| `redis.opsForHash().putAll(key, Map.of(f1, v1, f2, v2))` | `redis.call('HSET', KEYS[1], f1, v1, f2, v2)` | set multiple fields, keep unmentioned fields, field values are not JSON paths |
| `redis.opsForHash().putIfAbsent(key, field, value)` | `redis.call('HSETNX', KEYS[1], field, value)` | write only if field absent, no overwrite |
| `redis.opsForHash().hasKey(key, field)` | `redis.call('HEXISTS', KEYS[1], field)` | field existence, Redis integer reply, Java Boolean |
| `redis.opsForHash().entries(key)` | `redis.call('HGETALL', KEYS[1])` | all field/value pairs, Java Map, potentially large reply |
| `redis.opsForHash().keys(key)` | `redis.call('HKEYS', KEYS[1])` | all field names, no guaranteed order |
| `redis.opsForHash().values(key)` | `redis.call('HVALS', KEYS[1])` | all field values, no guaranteed order |
| `redis.opsForHash().size(key)` | `redis.call('HLEN', KEYS[1])` | field count, missing key returns 0 |
| `redis.opsForHash().delete(key, field)` | `redis.call('HDEL', KEYS[1], field)` | delete named fields, ignore missing fields, last field removes hash key |
| `redis.opsForHash().increment(key, field, 1L)` | `redis.call('HINCRBY', KEYS[1], field, 1)` | atomic field +1, missing field starts at 0 |
| `redis.opsForHash().increment(key, field, 1.5)` | `redis.call('HINCRBYFLOAT', KEYS[1], field, 1.5)` | atomic field +1.5, missing field starts at 0 |

## Lists

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForList().leftPush(key, value)` | `redis.call('LPUSH', KEYS[1], value)` | insert at head/left, return new list length |
| `redis.opsForList().rightPush(key, value)` | `redis.call('RPUSH', KEYS[1], value)` | insert at tail/right, return new list length |
| `redis.opsForList().leftPop(key)` | `redis.call('LPOP', KEYS[1])` | remove and return head, empty returns nil |
| `redis.opsForList().rightPop(key)` | `redis.call('RPOP', KEYS[1])` | remove and return tail, empty returns nil |
| `redis.opsForList().range(key, 0, -1)` | `redis.call('LRANGE', KEYS[1], 0, -1)` | 0 first element, -1 last element, inclusive end, 0..-1 reads all |
| `redis.opsForList().size(key)` | `redis.call('LLEN', KEYS[1])` | element count, missing key returns 0 |
| `redis.opsForList().index(key, 0)` | `redis.call('LINDEX', KEYS[1], 0)` | 0 first element, negative indexes from tail, out-of-range returns nil |
| `redis.opsForList().set(key, 0, value)` | `redis.call('LSET', KEYS[1], 0, value)` | replace element at index 0, out-of-range is an error |
| `redis.opsForList().remove(key, 0, value)` | `redis.call('LREM', KEYS[1], 0, value)` | count 0 removes all matches of value, other values remain |
| `redis.opsForList().remove(key, 2, value)` | `redis.call('LREM', KEYS[1], 2, value)` | positive count, remove up to 2 matches from head |
| `redis.opsForList().remove(key, -2, value)` | `redis.call('LREM', KEYS[1], -2, value)` | negative count, remove up to 2 matches from tail |
| `redis.opsForList().trim(key, 0, 9)` | `redis.call('LTRIM', KEYS[1], 0, 9)` | keep indexes 0..9, inclusive end, remove everything outside range |

## Sets

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForSet().add(key, member)` | `redis.call('SADD', KEYS[1], member)` | unique members, duplicates ignored, return newly added count |
| `redis.opsForSet().remove(key, member)` | `redis.call('SREM', KEYS[1], member)` | remove named members, ignore missing members |
| `redis.opsForSet().isMember(key, member)` | `redis.call('SISMEMBER', KEYS[1], member)` | membership test, Redis integer reply, Java Boolean |
| `redis.opsForSet().members(key)` | `redis.call('SMEMBERS', KEYS[1])` | all members, unordered, potentially large reply |
| `redis.opsForSet().size(key)` | `redis.call('SCARD', KEYS[1])` | unique-member count, missing key returns 0 |
| `redis.opsForSet().pop(key)` | `redis.call('SPOP', KEYS[1])` | remove and return random member, empty returns nil |
| `redis.opsForSet().intersect(key1, key2)` | `redis.call('SINTER', KEYS[1], KEYS[2])` | members in both sets, read only |
| `redis.opsForSet().union(key1, key2)` | `redis.call('SUNION', KEYS[1], KEYS[2])` | members in either set, deduplicated, read only |
| `redis.opsForSet().difference(key1, key2)` | `redis.call('SDIFF', KEYS[1], KEYS[2])` | members in first set but not second, order matters |

## Sorted sets

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForZSet().add(key, member, score)` | `redis.call('ZADD', KEYS[1], score, member)` | unique member, replace existing score, no duplicate member |
| `redis.opsForZSet().incrementScore(key, member, 5)` | `redis.call('ZINCRBY', KEYS[1], 5, member)` | atomic score +5, create missing member with score 5 |
| `redis.opsForZSet().score(key, member)` | `redis.call('ZSCORE', KEYS[1], member)` | member score, missing member returns nil |
| `redis.opsForZSet().rank(key, member)` | `redis.call('ZRANK', KEYS[1], member)` | ascending rank, zero based, missing member returns nil |
| `redis.opsForZSet().reverseRank(key, member)` | `redis.call('ZREVRANK', KEYS[1], member)` | descending rank, zero based, missing member returns nil |
| `redis.opsForZSet().range(key, 0, 9)` | `redis.call('ZRANGE', KEYS[1], 0, 9)` | rank indexes 0..9, inclusive end, ascending score by default |
| `redis.opsForZSet().reverseRange(key, 0, 9)` | `redis.call('ZRANGE', KEYS[1], 0, 9, 'REV')` | descending score, rank indexes 0..9, inclusive end |
| `redis.opsForZSet().rangeWithScores(key, 0, 9)` | `redis.call('ZRANGE', KEYS[1], 0, 9, 'WITHSCORES')` | ascending score, include scores, Java typed tuples |
| `redis.opsForZSet().rangeByScore(key, 0, 100)` | `redis.call('ZRANGE', KEYS[1], 0, 100, 'BYSCORE')` | score bounds 0..100 inclusive, not rank indexes |
| `redis.opsForZSet().count(key, 0, 100)` | `redis.call('ZCOUNT', KEYS[1], 0, 100)` | score bounds 0..100 inclusive, count matching members |
| `redis.opsForZSet().size(key)` | `redis.call('ZCARD', KEYS[1])` | member count, missing key returns 0 |
| `redis.opsForZSet().remove(key, member)` | `redis.call('ZREM', KEYS[1], member)` | remove named members, ignore missing members |

## Streams / messaging

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForStream().add(key, Map.of("name", "Keyboard"))` | `redis.call('XADD', KEYS[1], '*', 'name', 'Keyboard')` | * generates entry ID, create stream if missing |
| `redis.opsForStream().range(key, Range.unbounded())` | `redis.call('XRANGE', KEYS[1], '-', '+')` | - minimum ID, + maximum ID, both bounds inclusive |
| `redis.opsForStream().size(key)` | `redis.call('XLEN', KEYS[1])` | retained entry count, missing key returns 0 |
| `redis.opsForStream().acknowledge(key, "workers", id)` | `redis.call('XACK', KEYS[1], 'workers', id)` | remove group pending state, keep retained entry, return acknowledged count |
| `redis.opsForStream().delete(key, id)` | `redis.call('XDEL', KEYS[1], id)` | delete retained entry, not consumer acknowledgement |
| `redis.opsForStream().trim(key, 100)` | `redis.call('XTRIM', KEYS[1], 'MAXLEN', 100)` | MAXLEN 100 keeps at most 100 retained entries, oldest removed first |
| `redis.convertAndSend(channel, message)` | `redis.call('PUBLISH', channel, message)` | live subscribers only, no offline replay, receiver count is not processing success |

## HyperLogLog (approximate distinct counts)

| Spring Data Redis API | Redis command in Lua | Notes |
| --- | --- | --- |
| `redis.opsForHyperLogLog().add(key, value)` | `redis.call('PFADD', KEYS[1], value)` | add to approximate distinct counter, reply signals sketch change |
| `redis.opsForHyperLogLog().size(key)` | `redis.call('PFCOUNT', KEYS[1])` | estimate distinct count, not exact membership |

## Scanning And Execution Notes

| Topic | Notes |
| --- | --- |
| Key discovery | `redis.scan(ScanOptions...)`, iterate cursor, close cursor |
| Redis SCAN | cursor starts at 0, continue until returned cursor is 0, COUNT is a hint, duplicates possible |
| Consistency | SCAN is incremental, not a consistent snapshot, empty batch need not mean finished |
| Large keyspaces | avoid routine `KEYS *`, prefer SCAN |
| Missing collections | empty hash/list/set/sorted-set containers generally lose their key, not stored as empty containers |
| JSON vs hash | SET stores a string, HGET/HSET operate on native hash fields, no JSON-property parsing |
| Script arguments | declare accessed keys through KEYS, ordinary values through ARGV, Lua indexes start at 1 |
| Blocking operations | use client-side blocking/subscription APIs, Lua scripts cannot wait for list/stream arrivals |
| Cluster | keys in these multi-key operations must share a hash slot, hash tags can control placement |
| Atomicity | one command atomic, separate commands can interleave, scripts do not provide rollback |
| Serialization | configure serializers explicitly, generic Java types do not select JSON |
