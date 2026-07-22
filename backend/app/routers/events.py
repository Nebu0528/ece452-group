from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session, selectinload

from app.auth import get_current_user_id, require_roles
from app.database import get_db
from app.models.event import Event
from app.models.tag import Tag
from app.models.user import User
from app.schemas.event import EventCreate, EventOut
from app.services.occurrence import compute_next_occurrence

router = APIRouter(prefix="/events", tags=["events"])


def _to_event_out(event: Event, now: datetime) -> EventOut:
    result = EventOut.model_validate(event)
    if event.start_time is not None and event.duration is not None:
        next_occurrence = compute_next_occurrence(
            event.start_time, event.duration, event.schedule, event.frequency_end, now
        )
        if next_occurrence is not None:
            result.next_occurrence_start, result.next_occurrence_end = next_occurrence
    return result


@router.get("/", response_model=list[EventOut])
def list_events(db: Session = Depends(get_db)):
    events = db.query(Event).options(selectinload(Event.attendees)).all()
    now = datetime.utcnow()
    return [_to_event_out(e, now) for e in events]


@router.get("/{event_id}", response_model=EventOut)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(Event).options(selectinload(Event.attendees)).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    return _to_event_out(event, datetime.utcnow())


@router.post("/", response_model=EventOut)
def create_event(event: EventCreate, db: Session = Depends(get_db), user_id: int = Depends(get_current_user_id)):
    tags = db.query(Tag).filter(Tag.id.in_(event.tag_ids)).all()
    db_event = Event(
        name=event.name,
        description=event.description,
        location=event.location,
        lat=event.lat,
        lng=event.lng,
        start_time=event.start_time,
        duration=event.duration,
        schedule=event.schedule,
        frequency_end=event.frequency_end,
        user_id=user_id,
        tags=tags,
    )
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return _to_event_out(db_event, datetime.utcnow())


@router.patch("/{event_id}/review", response_model=EventOut)
def review_event(event_id: int, db: Session = Depends(get_db), reviewer_id: int = Depends(require_roles("organizer", "admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    event.reviewer_id = reviewer_id
    db.commit()
    db.refresh(event)
    return _to_event_out(event, datetime.utcnow())


@router.delete("/{event_id}")
def delete_event(event_id: int, db: Session = Depends(get_db), _: int = Depends(require_roles("organizer", "admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    db.delete(event)
    db.commit()
    return {"ok": True}


@router.post("/{event_id}/attend", response_model=EventOut)
def attend_event(event_id: int, db: Session = Depends(get_db), user_id: int = Depends(get_current_user_id)):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    user = db.query(User).get(user_id)
    if user not in event.attendees:
        event.attendees.append(user)
        db.commit()
        db.refresh(event)
    return _to_event_out(event, datetime.utcnow())


@router.delete("/{event_id}/attend", response_model=EventOut)
def unattend_event(event_id: int, db: Session = Depends(get_db), user_id: int = Depends(get_current_user_id)):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    user = db.query(User).get(user_id)
    if user in event.attendees:
        event.attendees.remove(user)
        db.commit()
        db.refresh(event)
    return _to_event_out(event, datetime.utcnow())
