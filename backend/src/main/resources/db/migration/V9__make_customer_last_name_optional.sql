-- Make last_name optional: in a kennel context, clients are often known only by first name or nickname
ALTER TABLE customer ALTER COLUMN last_name DROP NOT NULL;
