local key = KEYS[1]
local context = ARGV[1]
local expiration_seconds = ARGV[2]

local current_lock = redis.call('GET', key)
if not current_lock or current_lock == context then
    redis.call('SET', key, context, 'EX', expiration_seconds)
    return true
end

return false
