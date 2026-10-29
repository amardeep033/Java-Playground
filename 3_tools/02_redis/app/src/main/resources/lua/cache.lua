-- Actions: get, put, and delete; storage: product hash plus ID recency list.
-- KEYS[1] = product hash, KEYS[2] = recency list (most recent first).
-- ARGV[1] = action, ARGV[2] = product ID, ARGV[3] = JSON value for put only.
-- One script invocation updates cache state without other commands interleaving.
-- Capacity is three cached products, not three Redis keys; no product TTL is assigned.

local action, id = ARGV[1], ARGV[2]

-- Delete the product field and its recency entry; the caller already deleted the database row.
if action == 'delete' then
    redis.call('HDEL', KEYS[1], id)
    redis.call('LREM', KEYS[2], 0, id) -- Count 0 removes all occurrences of this ID, not all list elements.
    return nil
end


-- Read an existing product or store its JSON; a missing get returns nil (Java null).
local value
if action == 'get' then
    value = redis.call('HGET', KEYS[1], id)
    if not value then return nil end
else -- The Java caller supplies "put" for the remaining action.
    value = ARGV[3]
    redis.call('HSET', KEYS[1], id, value)
end


-- Every cache hit or write moves that ID to the head (most recently used).
-- A fourth product displaces the tail; eviction affects only Redis, not PostgreSQL.
redis.call('LREM', KEYS[2], 0, id)
redis.call('LPUSH', KEYS[2], id)
if redis.call('LLEN', KEYS[2]) > 3 then
    local oldest = redis.call('RPOP', KEYS[2])
    redis.call('HDEL', KEYS[1], oldest)
end
return value
