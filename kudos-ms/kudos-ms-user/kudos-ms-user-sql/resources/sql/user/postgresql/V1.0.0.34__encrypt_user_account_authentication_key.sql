-- AES-GCM ciphertext includes a marker, version, random IV and authentication tag.
-- Existing plaintext TOTP secrets remain readable and are encrypted on their next write.
alter table "user_account" alter column "authentication_key" type varchar(512);
