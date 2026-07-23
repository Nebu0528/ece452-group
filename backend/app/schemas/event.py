from datetime import date, datetime

from pydantic import BaseModel, model_validator

from app.schemas.tag import TagOut
from app.services.occurrence import is_valid_schedule


class EventCreate(BaseModel):
    name: str
    description: str | None = None
    location: str | None = None
    lat: float | None = None
    lng: float | None = None
    start_time: datetime
    duration: int
    schedule: str | None = None
    frequency_end: date | None = None
    tag_ids: list[int] = []

    @model_validator(mode="after")
    def validate_recurrence(self):
        if self.schedule is not None and not is_valid_schedule(self.schedule):
            raise ValueError(f"Invalid cron schedule: {self.schedule!r}")
        if self.frequency_end is not None:
            if self.schedule is None:
                raise ValueError("frequency_end requires schedule to be set")
            if self.frequency_end < self.start_time.date():
                raise ValueError("frequency_end must be on or after start_time's date")
        return self


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
    frequency_end: date | None
    next_occurrence_start: datetime | None = None
    next_occurrence_end: datetime | None = None
    user_id: int
    reviewer_id: int | None
    tags: list[TagOut] = []
    attendee_ids: list[int] = []

    model_config = {"from_attributes": True}
