from app.database import SessionLocal
from app.models.user import User, UserRole
from app.models.tag import Tag
from app.models.event import Event
from app.auth import hash_password


def seed():
    db = SessionLocal()
    try:
        if db.query(User).first():
            print("Database already seeded, skipping.")
            return

        # Users
        admin = User(name="admin", email="admin@uwaterloo.ca", password=hash_password("admin123"), role=UserRole.ADMIN)
        organizer = User(name="organizer", email="organizer@uwaterloo.ca", password=hash_password("organizer123"), role=UserRole.ORGANIZER)
        student = User(name="student", email="student@uwaterloo.ca", password=hash_password("student123"), role=UserRole.BASIC)
        db.add_all([admin, organizer, student])
        db.flush()

        # Tags
        engineering = Tag(name="Engineering", description="Engineering related events")
        social = Tag(name="Social", description="Social and networking events")
        academic = Tag(name="Academic", description="Academic and research events")
        sports = Tag(name="Sports", description="Sports and recreation events")
        db.add_all([engineering, social, academic, sports])
        db.flush()

        # Events
        db.add_all([
            Event(
                name="UW Hackathon 2026",
                description="24-hour hackathon open to all UWaterloo students.",
                location="E7 Building",
                lat=43.4729,
                lng=-80.5393,
                user_id=organizer.id,
                reviewer_id=admin.id,
                tags=[engineering, social],
            ),
            Event(
                name="Engineering Research Fair",
                description="Showcase of undergraduate and graduate research projects.",
                location="DC Atrium",
                lat=43.4723,
                lng=-80.5449,
                user_id=organizer.id,
                reviewer_id=admin.id,
                tags=[engineering, academic],
            ),
            Event(
                name="Intramural Soccer Signup",
                description="Sign up for the fall intramural soccer league.",
                location="CIF Field",
                lat=43.4752,
                lng=-80.5516,
                user_id=student.id,
                tags=[sports, social],
            ),
        ])
        db.commit()
        print("Seeded: 3 users, 4 tags, 3 events.")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
