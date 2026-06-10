from pydantic import BaseModel
from app.schemas.tag import TagOut


class EventCreate(BaseModel):
    name: str
    description: str | None = None
    location: str | None = None
    tag_ids: list[int] = []


class EventOut(BaseModel):
    id: int
    name: str
    description: str | None
    location: str | None
    user_id: int
    reviewer_id: int | None
    tags: list[TagOut] = []

    model_config = {"from_attributes": True}
