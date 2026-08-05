INSERT INTO "test_table_ktorm" ("id", "name", "birthday", "active", "weight", "height") VALUES
(-1, 'name1', '1984-01-04 06:40:00', 't', 56.5, 168),
(-2, 'name2', '1985-01-14 06:40:00', 't', 53, 161),
(-3, 'name3', '1995-01-14 06:40:00', 'f', 80, 180),
(-4, 'name4', '1995-03-09 06:40:00', 't', 48.12, 163),
(-5, 'name5', '1995-03-09 06:40:00', 't', NULL, NULL),
(-6, 'name6', '2015-01-30 15:03:12', 'f', 48.12, 163),
(-7, 'name7', '2010-08-23 21:21:21', 't', 18, 111),
(-8, 'name8', '2014-06-10 00:50:00', 't', 10, 80),
(-9, 'name9', '1884-01-04 06:40:00', 't', 66, 178),
(-10, 'name10', '1884-03-29 06:40:00', 'f', 66, 178),
(-11, 'name11', '2015-01-30 17:20:00', 't', NULL, NULL);

INSERT INTO "test_built_in_ktorm" ("id", "name", "built_in") VALUES
(901, 'custom_del_a', 'f'),
(902, 'built_in_row', 't'),
(903, 'custom_del_b', 'f');


-- org / region / creator matrix, so a filtered query has both hits and misses to prove itself on.
INSERT INTO "test_scoped_order" ("id", "title", "org_id", "region_code", "create_user_id") VALUES
('so-1', 'org-a emea, by alice',  'org-a', 'emea', 'alice'),
('so-2', 'org-a apac, by bob',    'org-a', 'apac', 'bob'),
('so-3', 'org-b emea, by bob',    'org-b', 'emea', 'bob'),
('so-4', 'org-c emea, by alice',  'org-c', 'emea', 'alice'),
('so-5', 'no org, by alice',      NULL,    'emea', 'alice');
