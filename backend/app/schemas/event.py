from datetime import datetime
from pydantic import BaseModel
from app.schemas.tag import TagOut


class EventCreate(BaseModel):
    name: str
    description: str | None = None
    location: str | None = None
    lat: float | None = None
    lng: float | None = None
    start_time: datetime
    duration: int
    schedule: str | None = None
    tag_ids: list[int] = []


class EventOut(BaseModel):
    id: int
    name: str
    description: str | None
    location: str | None
    lat: float | None
    lng: float | None
    start_time: datetime | None
    duration: int | None
    schedule: str | None
    user_id: int
    reviewer_id: int | None
    status: str
    organizer_name: str | None = None
    tags: list[TagOut] = []
    attendee_ids: list[int] = []

    model_config = {"from_attributes": True}
