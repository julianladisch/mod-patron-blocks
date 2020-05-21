INSERT INTO patron_block_conditions (id, jsonb) VALUES
('3d7c52dc-c732-4223-8bf8-e5917801386f', '{
  "id": "3d7c52dc-c732-4223-8bf8-e5917801386f",
  "name": "Maximum number of items charged out",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Integer",
  "message": ""
}'),
('72b67965-5b73-4840-bc0b-be8f3f6e047e', '{
  "id": "72b67965-5b73-4840-bc0b-be8f3f6e047e",
  "name": "Maximum number of lost items",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Integer",
  "message": ""
}'),
('584fbd4f-6a34-4730-a6ca-73a6a6a9d845', '{
  "id": "584fbd4f-6a34-4730-a6ca-73a6a6a9d845",
  "name": "Maximum number of overdue items",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Integer",
  "message": ""
}'),
('e5b45031-a202-4abb-917b-e1df9346fe2c', '{
  "id": "e5b45031-a202-4abb-917b-e1df9346fe2c",
  "name": "Maximum number of overdue recalls",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Integer",
  "message": ""
}'),
('cf7a0d5f-a327-4ca1-aa9e-dc55ec006b8a', '{
  "id": "cf7a0d5f-a327-4ca1-aa9e-dc55ec006b8a",
  "name": "Maximum outstanding fee/fine balance",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Double",
  "message": ""
}'),
('08530ac4-07f2-48e6-9dda-a97bc2bf7053', '{
  "id": "08530ac4-07f2-48e6-9dda-a97bc2bf7053",
  "name": "Recall overdue by maximum number of days",
  "blockBorrowing": false,
  "blockRenewals": false,
  "blockRequests": false,
  "valueType": "Integer",
  "message": ""
}') ON CONFLICT DO NOTHING;
