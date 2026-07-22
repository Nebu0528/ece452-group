"""add event status column

Revision ID: 0008
Revises: 0007
Create Date: 2026-07-22

"""
from alembic import op
import sqlalchemy as sa

revision = '0008'
down_revision = '0007'
branch_labels = None
depends_on = None


def upgrade():
    op.execute("CREATE TYPE eventstatus AS ENUM ('pending', 'approved', 'rejected')")
    op.add_column('events', sa.Column(
        'status',
        sa.Enum('pending', 'approved', 'rejected', name='eventstatus', create_type=False),
        nullable=False,
        server_default='pending'
    ))
    # Backfill: events with a reviewer_id are approved
    op.execute("UPDATE events SET status = 'approved' WHERE reviewer_id IS NOT NULL")


def downgrade():
    op.drop_column('events', 'status')
    op.execute("DROP TYPE IF EXISTS eventstatus")
