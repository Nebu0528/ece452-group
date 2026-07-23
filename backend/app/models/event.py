import enum
from sqlalchemy import Column, Integer, String, ForeignKey, Table, Float, DateTime, Enum as SAEnum
from sqlalchemy.orm import relationship
from app.database import Base


class EventStatus(str, enum.Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"

event_tags = Table(
    "event_tags",
    Base.metadata,
    Column("event_id", Integer, ForeignKey("events.id"), primary_key=True),
    Column("tag_id", Integer, ForeignKey("tags.id"), primary_key=True),
)

event_attendees = Table(
    "event_attendees",
    Base.metadata,
    Column("event_id", Integer, ForeignKey("events.id", ondelete="CASCADE"), primary_key=True),
    Column("user_id", Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
)


class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    description = Column(String)
    location = Column(String)
    lat = Column(Float, nullable=True)
    lng = Column(Float, nullable=True)
    start_time = Column(DateTime, nullable=True)
    duration = Column(Integer, nullable=True)
    schedule = Column(String, nullable=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    reviewer_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    status = Column(SAEnum(EventStatus, values_callable=lambda x: [e.value for e in x]), default=EventStatus.PENDING, nullable=False, server_default="pending")

    creator = relationship("User", foreign_keys=[user_id], back_populates="created_events")
    reviewer = relationship("User", foreign_keys=[reviewer_id], back_populates="reviewed_events")
    tags = relationship("Tag", secondary=event_tags)
    attendees = relationship("User", secondary=event_attendees, back_populates="attending_events")

    @property
    def attendee_ids(self) -> list[int]:
        return [u.id for u in self.attendees]

    @property
    def organizer_name(self) -> str | None:
        return self.creator.name if self.creator else None
