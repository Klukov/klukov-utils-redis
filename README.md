# klukov-utils-redis

Simple distributed Java utils based on Redis

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

