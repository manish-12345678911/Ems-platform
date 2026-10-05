-- Seed hospitals and redeployment zones (Jaipur Sector)
SET search_path TO hospital, public;

INSERT INTO hospital.hospital (id, name, location)
VALUES
    ('00000000-0000-0000-0000-000000000010', 'SMS Medical College & Hospital (Apex Trauma)', ST_SetSRID(ST_MakePoint(75.8164, 26.8988), 4326)::geography),
    ('00000000-0000-0000-0000-000000000020', 'Fortis Escorts Hospital Jaipur (Cardiac & Neuro)', ST_SetSRID(ST_MakePoint(75.8050, 26.8520), 4326)::geography),
    ('00000000-0000-0000-0000-000000000030', 'Manipal Hospital Jaipur (Vidhyadhar Nagar)', ST_SetSRID(ST_MakePoint(75.7600, 26.9600), 4326)::geography),
    ('00000000-0000-0000-0000-000000000040', 'Narayana Multispeciality Hospital (Pratap Nagar)', ST_SetSRID(ST_MakePoint(75.8522, 26.8285), 4326)::geography)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, location = EXCLUDED.location;

INSERT INTO hospital.hospital_capability (hospital_id, need)
VALUES
    ('00000000-0000-0000-0000-000000000010', 'TRAUMA'),
    ('00000000-0000-0000-0000-000000000010', 'CARDIAC'),
    ('00000000-0000-0000-0000-000000000010', 'STROKE'),
    ('00000000-0000-0000-0000-000000000010', 'BURN'),
    ('00000000-0000-0000-0000-000000000020', 'CARDIAC'),
    ('00000000-0000-0000-0000-000000000020', 'TRAUMA'),
    ('00000000-0000-0000-0000-000000000030', 'GENERAL'),
    ('00000000-0000-0000-0000-000000000030', 'TRAUMA'),
    ('00000000-0000-0000-0000-000000000030', 'PEDIATRIC'),
    ('00000000-0000-0000-0000-000000000040', 'CARDIAC'),
    ('00000000-0000-0000-0000-000000000040', 'STROKE')
ON CONFLICT (hospital_id, need) DO NOTHING;

SET search_path TO redeploy, public;

DELETE FROM redeploy.zone;
INSERT INTO redeploy.zone (centroid, demand_per_hour)
VALUES
    (ST_SetSRID(ST_MakePoint(75.8164, 26.8988), 4326)::geography, 8.5),
    (ST_SetSRID(ST_MakePoint(75.7584, 26.8623), 4326)::geography, 6.2),
    (ST_SetSRID(ST_MakePoint(75.8054, 26.8524), 4326)::geography, 5.4),
    (ST_SetSRID(ST_MakePoint(75.7766, 26.9734), 4326)::geography, 4.8);
