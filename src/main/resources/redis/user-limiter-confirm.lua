-- Keys and arguments
local finished_key = KEYS[1]
local pending_key = KEYS[2]
local event_id = ARGV[1]
local key_ttl = ARGV[2]

-- Step 1: Increment finished transactions
redis.call('INCR', finished_key)
redis.call('EXPIRE', finished_key, key_ttl)

-- Step 2: Get pending transactions
local pending_transactions_json = redis.call('GET', pending_key)
if not pending_transactions_json then
    return false
end

-- Step 3: Parse the pending transactions list
local pending_transactions = cjson.decode(pending_transactions_json)

-- Step 4: Remove the event from the pending list
local found = false
for i, transaction in ipairs(pending_transactions) do
    if transaction.event == event_id then
        table.remove(pending_transactions, i)
        found = true
        break
    end
end

-- Step 5: If not found, return expired transaction
if not found then
    return false
end

-- Step 6: Update the pending list
if #pending_transactions == 0 then
    redis.call('DEL', pending_key)
else
    local new_pending_json = cjson.encode(pending_transactions)
    redis.call('SET', pending_key, new_pending_json)
    redis.call('EXPIRE', pending_key, key_ttl)
end

-- Return confirmation
return true
