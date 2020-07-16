## 1.0.5 2020-07-16
* Add migration script to remove unnecessary fields from user_summary DB table (MODPATBLK-36)

## 1.0.4 2020-07-15
* Upgrade to RMB v30.0.3 (MODPATBLK-36)

## 1.0.3
* Delete open loan from user summary when last related lost item fee is closed (MODPATBLK-21)

## 1.0.2
* Allow additional properties in user schema (MODPATBLK-30)

## 1.0.1
* Create the block when the limit is reached, not when it's exceeded (MODPATBLK-20)

## 1.0.0
* Register module as a publisher (MODPATBLK-17)
* Handle events published by mod-circulation and mod-feesfines (MODPATBLK-14, MODPATBLK-13, MODPATBLK-11, MODPATBLK-12, MODPATBLK-5, MODPATBLK-4)
* Add license statement (FOLIO-360)
* Update RMB version to 30.0.0 and Vertx to 3.9.0 (MODPATBLK-10)
* Implement Automated Patron Blocks API (MODPATBLK-7)
* Move patron block conditions and limits functionality to mod-patron-blocks (MODPATBLK-6)
* Create UserSummary entity, implement CRUD (MODPATBLK-3)
* Create initial mod-patron-blocks implementation (MODPATBLK-2) 
