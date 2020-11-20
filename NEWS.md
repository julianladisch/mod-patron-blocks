## 1.1.4 2020-11-20
* Fix skipping loans and fees/fines during synchronization (MODPATBLK-69)

## 1.1.3 2020-11-17
* Fix generation of FEE_FINE_BALANCE_CHANGED events by synchronization (MODPATBLK-69)

## 1.1.2 2020-11-13
* Update mod-pubsub-client version to 1.3.3 (MODPATBLK-67)

## 1.1.1 2020-11-12
* Fix missing `feefineTypeId` in events created by synchronization (MODPATBLK-62)
* Upgrade to RMB 31.1.5 and Vert.x 3.9.4 (MODPATBLK-63)
* Fix incorrect synchronization of FEE_FINE_BALANCE_CHANGED events (MODPATBLK-65) 
* Fix synchronization job status update upon finish (MODPATBLK-66)

## 1.1.0 2020-10-14
* Automated patron blocks data synchronization (MODPATBLK-41)
* Fix memory leak (MODPATBLK-43)
* Properly enforce fee/fine limit for automated patron blocks (MODPATBLK-48) 
* Upgrade to RMB 31.0.2 and JDK 11 (MODPATBLK-50)
* Correctly determine loan's overdue status (MODPATBLK-18, MODPATBLK-52)
* Fix 3 implemented automated patron blocks - block when limits are exceeded (MODPATBLK-34)
* Remove Maximum outstanding fee/fine balance automated patron block when condition removed by library (MODPATBLK-27)
* Add diagnostic GET endpoint for UserSummary objects (MODPATBLK-35)
* Add migration script to remove unnecessary fields from user_summary DB table (MODPATBLK-36)
* Delete open loan from user summary when last related lost item fee is closed (MODPATBLK-21)
* Allow additional properties in user schema (MODPATBLK-30)
* Create patron block when the limit is reached, not when it's exceeded (MODPATBLK-20)

## 1.0.0
* Register module as a publisher (MODPATBLK-17)
* Handle events published by mod-circulation and mod-feesfines (MODPATBLK-14, MODPATBLK-13, MODPATBLK-11, MODPATBLK-12, MODPATBLK-5, MODPATBLK-4)
* Add license statement (FOLIO-360)
* Update RMB version to 30.0.0 and Vertx to 3.9.0 (MODPATBLK-10)
* Implement Automated Patron Blocks API (MODPATBLK-7)
* Move patron block conditions and limits functionality to mod-patron-blocks (MODPATBLK-6)
* Create UserSummary entity, implement CRUD (MODPATBLK-3)
* Create initial mod-patron-blocks implementation (MODPATBLK-2) 
