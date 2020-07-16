DO $$
BEGIN

    UPDATE ${myuniversity}_${mymodule}.user_summary
    SET jsonb = jsonb - 'numberOfLostItems'
    WHERE jsonb->'numberOfLostItems' IS NOT NULL;

    UPDATE ${myuniversity}_${mymodule}.user_summary
    SET jsonb = jsonb - 'outstandingFeeFineBalance'
    WHERE jsonb->'outstandingFeeFineBalance' IS NOT NULL;

EXCEPTION WHEN OTHERS THEN
END; $$;
