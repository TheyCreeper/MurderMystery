BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "inventory_item" (
	"id"	INTEGER,
	"uuid"	TEXT NOT NULL,
	"slot"	INTEGER NOT NULL,
	"material"	TEXT NOT NULL,
	"amount"	INTEGER NOT NULL DEFAULT 1,
	"item_data"	BLOB NOT NULL,
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("uuid") REFERENCES "player"("uuid")
);
CREATE TABLE IF NOT EXISTS "player" (
	"uuid"	TEXT UNIQUE,
	"realname"	TEXT,
	PRIMARY KEY("uuid")
);
CREATE INDEX IF NOT EXISTS "idx_items_owner_type" ON "inventory_item" (
	"uuid"
);
COMMIT;
