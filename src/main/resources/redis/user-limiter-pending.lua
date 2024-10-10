-- Keys and arguments
local finished_key = KEYS[1]
local pending_key = KEYS[2]
local limit = tonumber(ARGV[1])
local new_event_id = ARGV[2]
local new_event_expiration = tonumber(ARGV[3])
local current_timestamp = tonumber(ARGV[4])
local key_ttl = tonumber(ARGV[5])

-- Helper function to clean up expired transactions
local function cleanup_expired(pending_list, current_time)
    local valid_transactions = {}
    for _, transaction in ipairs(pending_list) do
        if transaction.expiration > current_time then
            table.insert(valid_transactions, transaction)
        end
    end
    return valid_transactions
end

-- Step 1: Get finished transactions count
local finished_transactions = tonumber(redis.call('GET', finished_key) or '0')

-- Step 2: Check if finished transactions exceed or meet the limit
if finished_transactions >= limit then
    return false
end

-- Step 3: Get pending transactions list
local pending_transactions_json = redis.call('GET', pending_key)
local pending_transactions = {}
if pending_transactions_json then
    pending_transactions = cjson.decode(pending_transactions_json)
end

-- Step 4: Clean up expired transactions
local cleaned_pending = cleanup_expired(pending_transactions, current_timestamp)

-- Step 5: Check if the current number of finished and pending transactions exceeds the limit
if (finished_transactions + #cleaned_pending) >= limit then
    return false
end

-- Step 6: Add new pending transaction if not duplicate
for _, transaction in ipairs(cleaned_pending) do
    if transaction.event == new_event_id then
        return false
    end
end
table.insert(cleaned_pending, {event = new_event_id, expiration = new_event_expiration})

-- Step 7: Update the pending transactions list
local new_pending_json = cjson.encode(cleaned_pending)
redis.call('SET', pending_key, new_pending_json)
redis.call('EXPIRE', pending_key, key_ttl)

-- Return success
return true
