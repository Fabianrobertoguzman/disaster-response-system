-- Reference seed data for DRS-Enhanced.
-- User accounts are NOT seeded here: passwords must be hashed at runtime, so the
-- application bootstrap creates a default administrator on first start instead.

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
  (UUID(), 'Alpha'),
  (UUID(), 'Bravo'),
  (UUID(), 'Charlie'),
  (UUID(), 'Delta'),
  (UUID(), 'Echo'),
  (UUID(), 'Foxtrot');

INSERT INTO resources (uuid, resource_type) VALUES
  (UUID(), 'Ambulance'),
  (UUID(), 'Fire Appliance'),
  (UUID(), 'Rescue Boat');
