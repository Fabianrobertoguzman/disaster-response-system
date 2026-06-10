-- Reference seed data for the H2 (MySQL-mode) TEST database: the same rows as
-- src/main/resources/db/seed.sql (8 partner agencies, 6 responders, 3
-- resources, no user accounts), but with FIXED uuid literals instead of the
-- MySQL UUID() function - deterministic for tests and dialect-independent.
-- Any change to the production seed MUST be mirrored here.

INSERT INTO partner_agencies (name, available_units) VALUES
  ('Fire and Emergency Service', 12),
  ('Hospital System', 40),
  ('Electricity Utility', 6),
  ('Transport Authority', 9),
  ('Police Service', 20),
  ('Waste Management Service', 7),
  ('Water Utility', 8),
  ('Education Department', 5);

INSERT INTO responders (uuid, name) VALUES
  ('00000000-0000-4000-8000-000000000001', 'Alpha'),
  ('00000000-0000-4000-8000-000000000002', 'Bravo'),
  ('00000000-0000-4000-8000-000000000003', 'Charlie'),
  ('00000000-0000-4000-8000-000000000004', 'Delta'),
  ('00000000-0000-4000-8000-000000000005', 'Echo'),
  ('00000000-0000-4000-8000-000000000006', 'Foxtrot');

INSERT INTO resources (uuid, resource_type) VALUES
  ('00000000-0000-4000-8000-000000000011', 'Ambulance'),
  ('00000000-0000-4000-8000-000000000012', 'Fire Appliance'),
  ('00000000-0000-4000-8000-000000000013', 'Rescue Boat');
