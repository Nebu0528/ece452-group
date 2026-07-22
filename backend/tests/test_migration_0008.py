import os
import sqlite3

from alembic import command
from alembic.config import Config

BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def _upgrade(db_path, revision, monkeypatch):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{db_path}")
    cfg = Config(os.path.join(BACKEND_DIR, "alembic.ini"))
    command.upgrade(cfg, revision)


def test_frequency_end_backfills_legacy_null_schedule_rows(monkeypatch, tmp_path):
    db_path = str(tmp_path / "test_migration.db")

    _upgrade(db_path, "0007", monkeypatch)

    conn = sqlite3.connect(db_path)
    conn.execute(
        "INSERT INTO users (id, name, email, password) VALUES (1, 'Legacy User', 'legacy@test.com', 'hash')"
    )
    conn.execute(
        "INSERT INTO events (id, name, start_time, duration, schedule, user_id) "
        "VALUES (1, 'Legacy Event', '2026-01-05 09:00:00', 30, NULL, 1)"
    )
    conn.commit()
    conn.close()

    _upgrade(db_path, "0008", monkeypatch)

    conn = sqlite3.connect(db_path)
    row = conn.execute("SELECT schedule, frequency_end FROM events WHERE id = 1").fetchone()
    conn.close()

    assert row[0] is not None
    parts = row[0].split()
    assert len(parts) == 5
    assert parts[4] == "1"  # 2026-01-05 is a Monday -> cron day-of-week 1
    assert row[1] is None
