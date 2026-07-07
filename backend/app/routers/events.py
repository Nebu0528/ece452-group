from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.event import Event
from app.models.tag import Tag
from app.schemas.event import EventCreate, EventOut
from app.auth import get_current_user_id, require_roles

router = APIRouter(prefix="/events", tags=["events"])


@router.get("/", response_model=list[EventOut])
def list_events(db: Session = Depends(get_db)):
    return db.query(Event).all()


@router.get("/{event_id}", response_model=EventOut)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    return event


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
        user_id=user_id,
        tags=tags,
    )
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return db_event


@router.patch("/{event_id}/review", response_model=EventOut)
def review_event(event_id: int, db: Session = Depends(get_db), reviewer_id: int = Depends(require_roles("organizer", "admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    event.reviewer_id = reviewer_id
    db.commit()
    db.refresh(event)
    return event


@router.delete("/{event_id}")
def delete_event(event_id: int, db: Session = Depends(get_db), _: int = Depends(require_roles("organizer", "admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    db.delete(event)
    db.commit()
    return {"ok": True}
