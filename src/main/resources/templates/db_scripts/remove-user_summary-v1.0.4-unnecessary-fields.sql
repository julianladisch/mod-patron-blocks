UPDATE user_summary
SET jsonb = jsonb - 'numberOfLostItems'
WHERE jsonb->'numberOfLostItems' IS NOT NULL;

UPDATE user_summary
SET jsonb = jsonb - 'outstandingFeeFineBalance'
WHERE jsonb->'outstandingFeeFineBalance' IS NOT NULL;
