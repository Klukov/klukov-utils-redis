local key = KEYS[1]
local context = ARGV[1]

local current_lock = redis.call('GET', key)
if not current_lock or current_lock ~= context then
    return false
end

redis.call('DEL', key)
return true
