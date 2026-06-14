from pydantic import BaseModel
from app.schemas.tag import TagOut


class EventCreate(BaseModel):
    name: str
    description: str | None = None
    location: str | None = None
    lat: float | None = None
    lng: float | None = None
    tag_ids: list[int] = []


class EventOut(BaseModel):
    id: int
    name: str
    description: str | None
    location: str | None
    lat: float | None
    lng: float | None
    user_id: int
    reviewer_id: int | None
    tags: list[TagOut] = []

    model_config = {"from_attributes": True}
