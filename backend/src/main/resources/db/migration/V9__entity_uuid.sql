ALTER TABLE users    ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE vehicles ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE spots    ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE users    ADD CONSTRAINT uq_users_uuid    UNIQUE (uuid);
ALTER TABLE vehicles ADD CONSTRAINT uq_vehicles_uuid UNIQUE (uuid);
ALTER TABLE spots    ADD CONSTRAINT uq_spots_uuid    UNIQUE (uuid);
