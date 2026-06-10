from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.tag import Tag
from app.schemas.tag import TagCreate, TagOut

router = APIRouter(prefix="/tags", tags=["tags"])


@router.get("/", response_model=list[TagOut])
def list_tags(db: Session = Depends(get_db)):
    return db.query(Tag).all()


@router.post("/", response_model=TagOut)
def create_tag(tag: TagCreate, db: Session = Depends(get_db)):
    db_tag = Tag(**tag.model_dump())
    db.add(db_tag)
    db.commit()
    db.refresh(db_tag)
    return db_tag


@router.delete("/{tag_id}")
def delete_tag(tag_id: int, db: Session = Depends(get_db)):
    tag = db.query(Tag).filter(Tag.id == tag_id).first()
    if not tag:
        raise HTTPException(status_code=404, detail="Tag not found")
    db.delete(tag)
    db.commit()
    return {"ok": True}
