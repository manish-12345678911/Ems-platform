-- ====================================================================
-- EMS Platform — 1-Click Supabase Cloud Database Schema & Seed
-- Run this in Supabase SQL Editor (https://supabase.com/dashboard)
-- ====================================================================

-- 1. Enable PostGIS for Spatial Proximity Queries
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Crew Accounts Table (Global Multi-Device Auth)
CREATE TABLE IF NOT EXISTS public.crew_accounts (
    unit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_sign VARCHAR(20) UNIQUE NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    type VARCHAR(10) DEFAULT 'ALS',
    label VARCHAR(50) DEFAULT 'Tactical Unit',
    lat DOUBLE PRECISION DEFAULT 26.9150,
    lon DOUBLE PRECISION DEFAULT 75.8100,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Live Ambulance Units Table (Real-Time GPS Telemetry)
CREATE TABLE IF NOT EXISTS public.ambulance_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_sign VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(10) NOT NULL DEFAULT 'ALS',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    lat DOUBLE PRECISION NOT NULL DEFAULT 26.9150,
    lon DOUBLE PRECISION NOT NULL DEFAULT 75.8100,
    geom GEOMETRY(Point, 4326),
    logged_in BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Trigger to auto-update PostGIS geom from lat/lon
CREATE OR REPLACE FUNCTION update_ambulance_geom()
RETURNS TRIGGER AS $$
BEGIN
    NEW.geom := ST_SetSRID(ST_MakePoint(NEW.lon, NEW.lat), 4326);
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ambulance_geom ON public.ambulance_units;
CREATE TRIGGER trg_ambulance_geom
BEFORE INSERT OR UPDATE ON public.ambulance_units
FOR EACH ROW EXECUTE FUNCTION update_ambulance_geom();

-- 4. Hospitals Network Table
CREATE TABLE IF NOT EXISTS public.hospitals (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    area VARCHAR(60),
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    geom GEOMETRY(Point, 4326),
    beds INT DEFAULT 200,
    icu INT DEFAULT 15,
    trauma_level VARCHAR(50) DEFAULT 'Level 1 Trauma'
);

-- 5. Seed Core Jaipur Hospitals
INSERT INTO public.hospitals (id, name, area, lat, lon, geom, beds, icu, trauma_level)
VALUES
    ('hosp-sms', 'SMS Hospital & Apex Trauma Center', 'JLN Marg', 26.8988, 75.8164, ST_SetSRID(ST_MakePoint(75.8164, 26.8988), 4326), 3500, 28, 'Level 1 Trauma'),
    ('hosp-fortis', 'Fortis Escorts Hospital', 'Malviya Nagar', 26.8524, 75.8054, ST_SetSRID(ST_MakePoint(75.8054, 26.8524), 4326), 250, 14, 'Level 1 Cardiac/Trauma'),
    ('hosp-ehcc', 'Eternal Heart Care Centre (EHCC)', 'Jawahar Circle', 26.8623, 75.7584, ST_SetSRID(ST_MakePoint(75.7584, 26.8623), 4326), 220, 12, 'Level 1 Cardiac/Trauma'),
    ('hosp-narayana', 'Narayana Multispeciality Hospital', 'Pratap Nagar', 26.7865, 75.8245, ST_SetSRID(ST_MakePoint(75.8245, 26.7865), 4326), 330, 18, 'Level 1 Trauma'),
    ('hosp-manipal', 'Manipal Hospital', 'Vidhyadhar Nagar', 26.9734, 75.7766, ST_SetSRID(ST_MakePoint(75.7766, 26.9734), 4326), 280, 16, 'Level 2 Trauma')
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Default Fleet Units (AMB-01 through AMB-14)
INSERT INTO public.crew_accounts (unit_id, call_sign, username, password, type, label, lat, lon)
VALUES
    ('aaaaaaaa-1111-1111-1111-111111111111', 'AMB-01', 'amb-01', 'crew123', 'ALS', 'Mobile ICU', 26.9150, 75.8100),
    ('bbbbbbbb-2222-2222-2222-222222222222', 'AMB-02', 'amb-02', 'crew123', 'BLS', 'Basic Tactical', 26.9239, 75.8267),
    ('cccccccc-3333-3333-3333-333333333333', 'AMB-03', 'amb-03', 'crew123', 'ALS', 'Trauma Unit', 26.8988, 75.8164),
    ('dddddddd-4444-4444-4444-444444444444', 'AMB-04', 'amb-04', 'crew123', 'BLS', 'Basic Tactical', 26.9073, 75.7925),
    ('55555555-0005-0005-0005-000000000005', 'AMB-05', 'amb-05', 'crew123', 'ALS', 'Paramedic ALS', 26.8524, 75.8054),
    ('66666666-0006-0006-0006-000000000006', 'AMB-06', 'amb-06', 'crew123', 'BLS', 'Basic Tactical', 26.8512, 75.7892),
    ('77777777-0007-0007-0007-000000000007', 'AMB-07', 'amb-07', 'crew123', 'ALS', 'Paramedic ALS', 26.8623, 75.7584),
    ('88888888-0008-0008-0008-000000000008', 'AMB-08', 'amb-08', 'crew123', 'BLS', 'Basic Tactical', 26.9077, 75.7397),
    ('99999999-0009-0009-0009-000000000009', 'AMB-09', 'amb-09', 'crew123', 'ALS', 'Paramedic ALS', 26.8973, 75.8260),
    ('aaaaaaaa-0010-0010-0010-000000000010', 'AMB-10', 'amb-10', 'crew123', 'BLS', 'Basic Tactical', 26.9452, 75.7337),
    ('bbbbbbbb-0011-0011-0011-000000000011', 'AMB-11', 'amb-11', 'crew123', 'ALS', 'Paramedic ALS', 26.9734, 75.7766),
    ('cccccccc-0012-0012-0012-000000000012', 'AMB-12', 'amb-12', 'crew123', 'BLS', 'Basic Tactical', 26.9050, 75.7780),
    ('dddddddd-0013-0013-0013-000000000013', 'AMB-13', 'amb-13', 'crew123', 'ALS', 'Paramedic ALS', 26.8285, 75.8522),
    ('eeeeeeee-0014-0014-0014-000000000014', 'AMB-14', 'amb-14', 'crew123', 'ALS', 'Paramedic ALS', 26.7788, 75.8277)
ON CONFLICT (call_sign) DO NOTHING;

INSERT INTO public.ambulance_units (id, call_sign, type, status, lat, lon)
SELECT unit_id, call_sign, type, 'AVAILABLE', lat, lon
FROM public.crew_accounts
ON CONFLICT (call_sign) DO NOTHING;

-- 7. Enable Realtime Replication & RLS Access
ALTER TABLE public.ambulance_units ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.crew_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hospitals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public Read/Write for Fleet" ON public.ambulance_units FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Public Read/Write for Accounts" ON public.crew_accounts FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Public Read for Hospitals" ON public.hospitals FOR SELECT USING (true);

-- Enable Supabase Realtime broadcast
ALTER PUBLICATION supabase_realtime ADD TABLE public.ambulance_units;
