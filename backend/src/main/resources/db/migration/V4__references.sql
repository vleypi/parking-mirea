-- V4: справочник типов мест для КР3.
-- active: отключённый тип не предлагается в формах, но остаётся у существующих мест.
-- sort_order: порядок в выпадающих списках.
ALTER TABLE spot_types
    ADD COLUMN active     BOOLEAN  NOT NULL DEFAULT TRUE,
    ADD COLUMN sort_order SMALLINT NOT NULL DEFAULT 0;

UPDATE spot_types SET sort_order = id * 10;
