from typing import Optional
from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import or_
from sqlalchemy.orm import Session, selectinload
from app.database import get_db
from app.models.event import Event, EventStatus
from app.models.tag import Tag
from app.models.user import User, UserRole
from app.schemas.event import EventCreate, EventOut
from app.auth import get_current_user_id, require_roles, decode_token

router = APIRouter(prefix="/events", tags=["events"])

_optional_bearer = OAuth2PasswordBearer(tokenUrl="/users/login", auto_error=False)


def _get_optional_user(token: Optional[str] = Depends(_optional_bearer), db: Session = Depends(get_db)) -> Optional[User]:
    if not token:
        return None
    try:
        payload = decode_token(token)
        user_id = int(payload["sub"])
        return db.query(User).get(user_id)
    except Exception:
        return None


@router.get("/", response_model=list[EventOut])
def list_events(db: Session = Depends(get_db), current_user: Optional[User] = Depends(_get_optional_user)):
    query = db.query(Event).options(selectinload(Event.attendees))
    if current_user and current_user.role == UserRole.ADMIN:
        pass  # admin sees all
    elif current_user and current_user.role == UserRole.ORGANIZER:
        # organizer sees approved + their own events
        query = query.filter(or_(Event.status == EventStatus.APPROVED, Event.user_id == current_user.id))
    else:
        query = query.filter(Event.status == EventStatus.APPROVED)
    return query.all()


@router.get("/{event_id}", response_model=EventOut)
def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(Event).options(selectinload(Event.attendees)).filter(Event.id == event_id).first()
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
def review_event(event_id: int, db: Session = Depends(get_db), reviewer_id: int = Depends(require_roles("admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    event.reviewer_id = reviewer_id
    event.status = EventStatus.APPROVED
    db.commit()
    db.refresh(event)
    return event


@router.patch("/{event_id}/reject", response_model=EventOut)
def reject_event(event_id: int, db: Session = Depends(get_db), reviewer_id: int = Depends(require_roles("admin"))):
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    event.status = EventStatus.REJECTED
    event.reviewer_id = reviewer_id
    db.commit()
    db.refresh(event)
    return event


@router.delete("/{event_id}")
def delete_event(event_id: int, db: Session = Depends(get_db), user_id: int = Depends(get_current_user_id)):
    user = db.query(User).get(user_id)
    if user.role not in (UserRole.ORGANIZER, UserRole.ADMIN):
        raise HTTPException(status_code=403, detail="Insufficient permissions")
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if user.role == UserRole.ORGANIZER and event.user_id != user_id:
        raise HTTPException(status_code=403, detail="Cannot delete another organizer's event")
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
    return event


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
    return event
