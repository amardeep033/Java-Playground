-- KEYS[1] is one API path's shared GET counter; there are no ARGV arguments.
-- Increment and first-request expiry execute without other clients' commands interleaving.
-- The 60-second window starts on the first request; INCR preserves the existing TTL.
-- All attempts increment, including blocked ones; they do not extend the window.
-- Return the count; the controller allows 1..5 and maps larger values to HTTP 429.
local count = redis.call('INCR', KEYS[1])
if count == 1 then redis.call('EXPIRE', KEYS[1], 60) end
return count
