import enum
from datetime import datetime
from sqlalchemy import Boolean, Column, DateTime, Integer, String, Enum
from sqlalchemy.orm import relationship
from app.database import Base


class UserRole(str, enum.Enum):
    BASIC = "basic"
    ORGANIZER = "organizer"
    ADMIN = "admin"


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, nullable=False)
    password = Column(String, nullable=False)
    role = Column(Enum(UserRole, values_callable=lambda obj: [e.value for e in obj]), default=UserRole.BASIC, nullable=False)

    is_verified = Column(Boolean, default=False, nullable=False)
    verification_code = Column(String(6), nullable=True)
    verification_code_expires_at = Column(DateTime, nullable=True)

    created_events = relationship("Event", foreign_keys="Event.user_id", back_populates="creator")
    reviewed_events = relationship("Event", foreign_keys="Event.reviewer_id", back_populates="reviewer")
