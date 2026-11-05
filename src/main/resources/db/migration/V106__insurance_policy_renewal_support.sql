-- 1. endorsement_sequence: the entity has always mapped "endorsement_sequence" but V1 created
--    "endorsement_secuence" (typo) on fresh databases. Databases that predate Flyway already have the
--    right column. Normalise both cases without touching existing data.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema()
                     AND table_name = 'insurance_policies' AND column_name = 'endorsement_sequence') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema()
                     AND table_name = 'insurance_policies' AND column_name = 'endorsement_secuence') THEN
            ALTER TABLE insurance_policies RENAME COLUMN endorsement_secuence TO endorsement_sequence;
        ELSE
            ALTER TABLE insurance_policies ADD COLUMN endorsement_sequence VARCHAR(20);
        END IF;
    END IF;
END $$;

-- 2. Link a renewed policy to the policy it was copied from (nullable: existing rows stay NULL).
ALTER TABLE insurance_policies
    ADD COLUMN IF NOT EXISTS renewed_from_policy_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_insurance_policies_renewed_from') THEN
        ALTER TABLE insurance_policies
            ADD CONSTRAINT fk_insurance_policies_renewed_from
            FOREIGN KEY (renewed_from_policy_id) REFERENCES insurance_policies (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_insurance_policies_renewed_from
    ON insurance_policies (renewed_from_policy_id);

-- 3. A policy number may repeat across renewals: uniqueness becomes (tenant, number, term).
--    Drop whichever unique constraint / index currently enforces (tenant, number) or (number); its name
--    differs between databases created by Flyway and databases created earlier by Hibernate.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE rel.relname = 'insurance_policies'
          AND ns.nspname = current_schema()
          AND con.contype = 'u'
          AND (SELECT array_agg(att.attname::text ORDER BY att.attname)
               FROM unnest(con.conkey) AS k(attnum)
               JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = k.attnum)
              IN (ARRAY['policy_number', 'tenant_id'], ARRAY['policy_number'])
    LOOP
        EXECUTE format('ALTER TABLE insurance_policies DROP CONSTRAINT %I', c.conname);
    END LOOP;

    FOR c IN
        SELECT idx.relname AS index_name
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class rel ON rel.oid = i.indrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE rel.relname = 'insurance_policies'
          AND ns.nspname = current_schema()
          AND i.indisunique
          AND NOT i.indisprimary
          AND NOT EXISTS (SELECT 1 FROM pg_constraint x WHERE x.conindid = i.indexrelid)
          AND 0 <> ALL (string_to_array(i.indkey::text, ' ')::int[])
          AND (SELECT array_agg(att.attname::text ORDER BY att.attname)
               FROM unnest(string_to_array(i.indkey::text, ' ')::int[]) AS k(attnum)
               JOIN pg_attribute att ON att.attrelid = i.indrelid AND att.attnum = k.attnum)
              IN (ARRAY['policy_number', 'tenant_id'], ARRAY['policy_number'])
    LOOP
        EXECUTE format('DROP INDEX %I', c.index_name);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_insurance_policies_tenant_number_term
    ON insurance_policies (tenant_id, policy_number, COALESCE(term_number, ''));
