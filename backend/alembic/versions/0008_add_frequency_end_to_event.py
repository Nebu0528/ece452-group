"""add frequency_end to event, backfill schedule for legacy rows

Revision ID: 0008
Revises: 0007
Create Date: 2026-07-22

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import table, column, select, update


revision = '0008'
down_revision = '0007'
branch_labels = None
depends_on = None

events_table = table(
    'events',
    column('id', sa.Integer),
    column('start_time', sa.DateTime),
    column('schedule', sa.String),
)


def upgrade():
    op.add_column('events', sa.Column('frequency_end', sa.Date(), nullable=True))

    bind = op.get_bind()
    legacy_rows = bind.execute(
        select(events_table.c.id, events_table.c.start_time)
        .where(events_table.c.schedule.is_(None))
    ).fetchall()

    for row in legacy_rows:
        if row.start_time is not None:
            # Standard cron day-of-week: 0/7 = Sunday, 1 = Monday, ..., 6 = Saturday.
            # Python's weekday() is Monday=0, so convert with (weekday() + 1) % 7.
            cron_dow = (row.start_time.weekday() + 1) % 7
            cron = f"{row.start_time.minute} {row.start_time.hour} * * {cron_dow}"
        else:
            cron = "0 0 * * 1"
        bind.execute(
            update(events_table).where(events_table.c.id == row.id).values(schedule=cron)
        )


def downgrade():
    op.drop_column('events', 'frequency_end')
