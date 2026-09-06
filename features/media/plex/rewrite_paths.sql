-- Plex : reecriture des chemins Windows -> chemins conteneur Linux
--
-- A EXECUTER UNIQUEMENT AVEC `Plex SQLite.exe`, jamais avec un sqlite3 standard :
-- la base embarque des tables virtuelles fts4/spellfix1 dont les modules n'existent
-- que dans le SQLite custom de Plex, et des triggers les alimentent.
--
-- A EXECUTER SUR LA COPIE DE TRAVAIL, jamais sur la base de l'install native.
--
-- Mapping (cf. .docs/plex-docker.md §2.4) :
--     D:\  -> /data/d   (ro)      G:\  -> /data/g   (ro)
--     H:\  -> /data/h   (rw, DVR) I:\  -> /data/i   (ro)
--
-- Deux formes de chemins coexistent dans la base :
--   * brute        `D:\0-Movies\film.mkv`               -> separateurs a convertir
--   * URL-encodee  `file:///D:/0-Movies/film%20a.mkv`   -> deja en `/`, garder les %20
--
-- SUBSTR(x, 3)  retire `D:`            et laisse `\reste`
-- SUBSTR(x, 11) retire `file:///D:`    et laisse `/reste`

BEGIN;

-- ---------------------------------------------------------------- D:  (Film)
UPDATE media_parts        SET file      = '/data/d' || REPLACE(SUBSTR(file, 3), '\', '/')      WHERE file      LIKE 'D:\%';
UPDATE section_locations  SET root_path = '/data/d' || REPLACE(SUBSTR(root_path, 3), '\', '/') WHERE root_path LIKE 'D:\%';
UPDATE media_streams      SET url       = 'file:///data/d' || SUBSTR(url, 11)                  WHERE url       LIKE 'file:///D:/%';
UPDATE metadata_items     SET guid      = 'file:///data/d' || SUBSTR(guid, 11)                 WHERE guid      LIKE 'file:///D:/%';

-- ------------------------------------------------------------- G:  (Series)
UPDATE media_parts        SET file      = '/data/g' || REPLACE(SUBSTR(file, 3), '\', '/')      WHERE file      LIKE 'G:\%';
UPDATE section_locations  SET root_path = '/data/g' || REPLACE(SUBSTR(root_path, 3), '\', '/') WHERE root_path LIKE 'G:\%';
UPDATE media_streams      SET url       = 'file:///data/g' || SUBSTR(url, 11)                  WHERE url       LIKE 'file:///G:/%';
UPDATE metadata_items     SET guid      = 'file:///data/g' || SUBSTR(guid, 11)                 WHERE guid      LIKE 'file:///G:/%';

-- ----------------------------------------------------------- H:  (Series-3)
UPDATE media_parts        SET file      = '/data/h' || REPLACE(SUBSTR(file, 3), '\', '/')      WHERE file      LIKE 'H:\%';
UPDATE section_locations  SET root_path = '/data/h' || REPLACE(SUBSTR(root_path, 3), '\', '/') WHERE root_path LIKE 'H:\%';
UPDATE media_streams      SET url       = 'file:///data/h' || SUBSTR(url, 11)                  WHERE url       LIKE 'file:///H:/%';
UPDATE metadata_items     SET guid      = 'file:///data/h' || SUBSTR(guid, 11)                 WHERE guid      LIKE 'file:///H:/%';

-- ----------------------------------------------------------- I:  (Series-2)
UPDATE media_parts        SET file      = '/data/i' || REPLACE(SUBSTR(file, 3), '\', '/')      WHERE file      LIKE 'I:\%';
UPDATE section_locations  SET root_path = '/data/i' || REPLACE(SUBSTR(root_path, 3), '\', '/') WHERE root_path LIKE 'I:\%';
UPDATE media_streams      SET url       = 'file:///data/i' || SUBSTR(url, 11)                  WHERE url       LIKE 'file:///I:/%';
UPDATE metadata_items     SET guid      = 'file:///data/i' || SUBSTR(guid, 11)                 WHERE guid      LIKE 'file:///I:/%';

COMMIT;

-- Doit repondre `ok`. Toute autre reponse = on repart de la copie d'origine.
PRAGMA integrity_check;
