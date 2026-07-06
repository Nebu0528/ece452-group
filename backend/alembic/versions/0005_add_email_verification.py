"""add email verification fields to users

Revision ID: 0005
Revises: 0004
Create Date: 2026-07-05

"""
from alembic import op
import sqlalchemy as sa

revision = '0005'
down_revision = '0004'
branch_labels = None
depends_on = None


def upgrade():
    op.add_column('users', sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='false'))
    op.add_column('users', sa.Column('verification_code', sa.String(6), nullable=True))
    op.add_column('users', sa.Column('verification_code_expires_at', sa.DateTime(), nullable=True))


def downgrade():
    op.drop_column('users', 'verification_code_expires_at')
    op.drop_column('users', 'verification_code')
    op.drop_column('users', 'is_verified')
