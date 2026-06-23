"""add start_time, duration, schedule to event

Revision ID: 0004
Revises: 985a12372678
Create Date: 2026-06-23

"""
from alembic import op
import sqlalchemy as sa


revision = '0004'
down_revision = '985a12372678'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('events', sa.Column('start_time', sa.DateTime(), nullable=True))
    op.add_column('events', sa.Column('duration', sa.Integer(), nullable=True))
    op.add_column('events', sa.Column('schedule', sa.String(), nullable=True))


def downgrade():
    op.drop_column('events', 'schedule')
    op.drop_column('events', 'duration')
    op.drop_column('events', 'start_time')
