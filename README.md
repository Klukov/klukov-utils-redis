# klukov-utils-redis
Simple distributed Java utils based on Redis

### Java utils project:
https://github.com/Klukov/klukov-utils  
  
  
## SETUP
All tests run on test containers. However, if you need to test it manually use below commands.  
  
### Start redis:
```docker run --name redis-limiter -p 6379:6379 -d redis:7.4```  
  
  
### Test redis commands
```
docker exec -it redis-limiter sh
# redis-cli
```
  
  
## Simple lock
Simple lock is based on simple redis command:  
```SET <prefix>:<key> LOCKED NX EX <lock-duration-seconds>```  
which returns "OK" when lock is acquired, or (nil) when key already exists.  
Spring redis template translates this to true or false respectively.

## Context lock
The Context lock provides a mechanism for managing distributed locks with contextual awareness.
In cases where an application goes down while holding a lock, we want the 'winning' process to be able to reacquire the lock. 
Using a simple lock mechanism, the lock cannot be acquired again until it expires, which might lead to conflicts or loosing data.
To solve this problem, a Lua script was created to check the context value before adding a key, as in a simple lock. 
If the lock exists and has the same context, the ```acquire``` method returns ```true``` and refreshes the expiration time. 
However, if the context is different, the expiration time is not refreshed.

## User action limiter
User action limiter is focused on data consistency. The design is made to keep data consistency.  
The limit can never be exceeded.  

It is based on two redis keys. 
Let's define ```FTD = floored time duration```, which is used as time window.  
The first key contains count of finished transaction in specific period of time. The first key structure:
``` <prefix>:finished:<FTD>:<userId> ```  
The second key contains json with pending transactions. The second key structure:
``` <prefix>:pending:<FTD>:<userId> ```  
Value's json format: ``` {event: <new_event_id>, expiration: <new_event_expiration_timestamp>} ```  

Each call ```isActionAllowed``` verifies sum of finished transaction count and pending transaction count.

When you call method ```isActionAllowed``` then you must call 
```actionProcessed``` method to finalize transaction and make it durable.  
Each transaction has some expiration. Limit can only exceed when transaction will finish after transaction expiration.

