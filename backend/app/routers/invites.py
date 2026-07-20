import uuid
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session

from app.auth import require_roles
from app.database import get_db
from app.email import send_invite_email
from app.models.invite import Invite
from app.models.user import User, UserRole

router = APIRouter(prefix="/invites", tags=["invites"])

INVITE_EXPIRY_HOURS = 48


class InviteCreate(BaseModel):
    email: EmailStr
    role: UserRole


@router.post("/", status_code=201)
def create_invite(
    body: InviteCreate,
    db: Session = Depends(get_db),
    _: int = Depends(require_roles("admin")),
):
    if body.role == UserRole.BASIC:
        raise HTTPException(status_code=400, detail="Invites are only for organizer or admin roles")

    # Block if email is already registered
    if db.query(User).filter(User.email == body.email).first():
        raise HTTPException(status_code=400, detail="A user with this email is already registered")

    # Invalidate any existing unused invite for this email so only the latest one works
    db.query(Invite).filter(
        Invite.email == body.email,
        Invite.used == False
    ).update({"used": True})

    token = str(uuid.uuid4())
    expires = datetime.utcnow() + timedelta(hours=INVITE_EXPIRY_HOURS)

    invite = Invite(email=body.email, role=body.role.value, token=token, expires_at=expires)
    db.add(invite)
    db.commit()

    import threading
    threading.Thread(
        target=_send_invite_background,
        args=(body.email, body.role.value, token),
        daemon=True,
    ).start()

    return {"message": f"Invite sent to {body.email}"}


def _send_invite_background(email: str, role: str, token: str):
    try:
        send_invite_email(email, role, token)
    except Exception as e:
        print(f"[WARN] Failed to send invite email to {email}: {e}")
        print(f"[DEV] Invite token for {email} ({role}): {token}")
