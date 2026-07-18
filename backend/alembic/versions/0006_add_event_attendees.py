"""add event attendees

Revision ID: 0006
Revises: 0005
Create Date: 2026-07-18

"""
from alembic import op
import sqlalchemy as sa

revision = '0006'
down_revision = '0005'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        'event_attendees',
        sa.Column('event_id', sa.Integer(), sa.ForeignKey('events.id', ondelete='CASCADE'), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('users.id', ondelete='CASCADE'), primary_key=True),
    )


def downgrade():
    op.drop_table('event_attendees')
