from datetime import datetime
from app.database import SessionLocal
from app.models.user import User, UserRole
from app.models.tag import Tag
from app.models.event import Event
from app.auth import hash_password


def weekly_schedule(start_time: datetime) -> str:
    # Standard cron day-of-week: 0/7 = Sunday, 1 = Monday, ..., 6 = Saturday.
    # Python's weekday() is Monday=0, so convert with (weekday() + 1) % 7.
    cron_dow = (start_time.weekday() + 1) % 7
    return f"{start_time.minute} {start_time.hour} * * {cron_dow}"


def daily_schedule(start_time: datetime) -> str:
    return f"{start_time.minute} {start_time.hour} * * *"


def seed():
    db = SessionLocal()
    try:
        if db.query(User).first():
            print("Database already seeded, skipping.")
            return

        # Users
        admin = User(name="admin", email="admin@uwaterloo.ca", password=hash_password("admin123"), role=UserRole.ADMIN, is_verified=True)
        organizer = User(name="organizer", email="organizer@uwaterloo.ca", password=hash_password("organizer123"), role=UserRole.ORGANIZER, is_verified=True)
        student = User(name="student", email="student@uwaterloo.ca", password=hash_password("student123"), role=UserRole.BASIC, is_verified=True)
        db.add_all([admin, organizer, student])
        db.flush()

        # Tags
        engineering = Tag(name="Engineering", description="Engineering related events")
        social = Tag(name="Social", description="Social and networking events")
        academic = Tag(name="Academic", description="Academic and research events")
        sports = Tag(name="Sports", description="Sports and recreation events")
        arts = Tag(name="Arts", description="Arts, culture, and performance events")
        volunteer = Tag(name="Volunteer", description="Community service and volunteering")

        # Accessibility tags
        wheelchair_accessible = Tag(name="Wheelchair Accessible", description="Venue has step-free access")
        sign_language = Tag(name="Sign Language Interpretation", description="ASL interpretation provided")
        quiet_space = Tag(name="Quiet Space", description="A low-stimulation space is available nearby")

        db.add_all([
            engineering, social, academic, sports, arts, volunteer,
            wheelchair_accessible, sign_language, quiet_space,
        ])
        db.flush()

        # Event start times — every seeded event repeats weekly, forever, from its own start time.
        wathacks_start = datetime(2026, 9, 19, 18, 0)
        research_fair_start = datetime(2026, 10, 5, 10, 0)
        velocity_demo_start = datetime(2026, 11, 12, 14, 0)
        basketball_start = datetime(2026, 10, 22, 19, 0)
        cs_symposium_start = datetime(2026, 11, 3, 9, 0)
        wie_start = datetime(2026, 10, 8, 17, 30)
        math_bbq_start = datetime(2026, 9, 8, 12, 0)
        coop_info_start = datetime(2026, 9, 15, 16, 0)
        physics_colloquium_start = datetime(2026, 10, 29, 15, 30)
        ece_symposium_start = datetime(2026, 4, 8, 9, 0)
        drama_club_start = datetime(2026, 11, 20, 19, 30)
        sustainability_fair_start = datetime(2026, 10, 15, 11, 0)
        open_swim_start = datetime(2026, 10, 1, 8, 0)
        same_day_1_start = datetime(2026, 9, 19, 9, 0)
        same_day_2_start = datetime(2026, 9, 19, 11, 0)
        same_day_3_start = datetime(2026, 9, 19, 13, 0)
        overlap_1_start = datetime(2026, 9, 19, 15, 0)
        overlap_2_start = datetime(2026, 9, 19, 15, 30)
        overlap_3_start = datetime(2026, 9, 19, 16, 0)
        soccer_signup_start = datetime(2026, 9, 5, 10, 0)
        weef_start = datetime(2026, 10, 6, 17, 0)
        food_bank_start = datetime(2026, 10, 18, 10, 0)
        solar_car_start = datetime(2026, 9, 25, 13, 0)
        gala_start = datetime(2026, 3, 27, 18, 0)
        daily_event_1_start = datetime(2026, 8, 3, 7, 0)
        daily_event_2_start = datetime(2026, 8, 3, 12, 0)
        daily_event_3_start = datetime(2026, 8, 3, 16, 30)
        daily_event_4_start = datetime(2026, 8, 3, 19, 0)
        daily_event_5_start = datetime(2026, 8, 3, 21, 0)

        # Events — duration is in minutes
        db.add_all([
            # Approved events
            Event(
                name="WatHacks 2026",
                description="24-hour hackathon open to all UWaterloo students. Build something amazing with teams of up to 4.",
                location="E7 Building",
                lat=43.4729, lng=-80.5393,
                start_time=wathacks_start,
                duration=1440,
                schedule=weekly_schedule(wathacks_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, social, wheelchair_accessible, quiet_space],
                attendees=[student, admin],
            ),
            Event(
                name="Engineering Research Fair",
                description="Showcase of undergraduate and graduate research projects across all engineering disciplines.",
                location="DC Atrium",
                lat=43.4723, lng=-80.5449,
                start_time=research_fair_start,
                duration=240,
                schedule=weekly_schedule(research_fair_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, academic],
            ),
            Event(
                name="Velocity Demo Day",
                description="Waterloo's top student startups pitch to investors and the public. Networking reception follows.",
                location="Velocity Garage, 250 Laurelwood Dr",
                lat=43.5091, lng=-80.5647,
                start_time=velocity_demo_start,
                duration=180,
                schedule=weekly_schedule(velocity_demo_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, social],
            ),
            Event(
                name="UW Warriors Basketball vs. Guelph Gryphons",
                description="Cheer on the Warriors in this cross-town rivalry game. Student entrance is free with WatCard.",
                location="Physical Activities Complex (PAC)",
                lat=43.4754, lng=-80.5503,
                start_time=basketball_start,
                duration=120,
                schedule=weekly_schedule(basketball_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[sports, social],
                attendees=[student],
            ),
            Event(
                name="CS Research Symposium",
                description="Annual showcase of Cheriton School of CS graduate research. Poster session and keynote included.",
                location="DC 1302",
                lat=43.4723, lng=-80.5449,
                start_time=cs_symposium_start,
                duration=360,
                schedule=weekly_schedule(cs_symposium_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[academic, engineering, sign_language],
                attendees=[student, organizer, admin],
            ),
            Event(
                name="Women in Engineering Networking Night",
                description="Connecting WiE students with industry professionals and alumni. Light refreshments provided.",
                location="E5 Building, 3rd Floor",
                lat=43.4727, lng=-80.5416,
                start_time=wie_start,
                duration=150,
                schedule=weekly_schedule(wie_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, social],
            ),
            Event(
                name="Math Faculty Welcome Week BBQ",
                description="Kick off the semester with free food and games in the Math courtyard. All Math and CS students welcome.",
                location="MC Courtyard",
                lat=43.4725, lng=-80.5436,
                start_time=math_bbq_start,
                duration=180,
                schedule=weekly_schedule(math_bbq_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[social, academic],
            ),
            Event(
                name="UW Co-op Info Session",
                description="Learn how to navigate WaterlooWorks, optimize your resume, and land your first co-op. Hosted by CECA.",
                location="TC 2218",
                lat=43.4730, lng=-80.5510,
                start_time=coop_info_start,
                duration=90,
                schedule=weekly_schedule(coop_info_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[academic],
            ),
            Event(
                name="Physics Colloquium: Quantum Entanglement",
                description="Prof. Christine Muschik presents recent advances in quantum networking at the IQC.",
                location="PHY 313",
                lat=43.4710, lng=-80.5440,
                start_time=physics_colloquium_start,
                duration=90,
                schedule=weekly_schedule(physics_colloquium_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[academic],
            ),
            Event(
                name="ECE Design Symposium",
                description="Fourth-year ECE capstone teams present their final projects to industry judges and the public.",
                location="E5, 5th Floor",
                lat=43.4727, lng=-80.5416,
                start_time=ece_symposium_start,
                duration=480,
                schedule=weekly_schedule(ece_symposium_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, academic],
            ),
            Event(
                name="UW Drama Club: Into the Woods",
                description="The UW Drama Club presents Sondheim's beloved musical. Performances run Thursday through Saturday.",
                location="Theatre of the Arts, Hagey Hall",
                lat=43.4735, lng=-80.5505,
                start_time=drama_club_start,
                duration=150,
                schedule=weekly_schedule(drama_club_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[arts, social],
            ),
            Event(
                name="Sustainability Fair",
                description="Explore sustainability initiatives on campus. Local vendors, zero-waste workshops, and EV displays.",
                location="EV3 Foyer",
                lat=43.4700, lng=-80.5454,
                start_time=sustainability_fair_start,
                duration=300,
                schedule=weekly_schedule(sustainability_fair_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[social, volunteer],
            ),
            Event(
                name="CIF Open Swim",
                description="Drop-in recreational swim at the Columbia Ice Field pool. Lanes available for lap swimming too.",
                location="Columbia Ice Field (CIF) Pool",
                lat=43.4752, lng=-80.5516,
                start_time=open_swim_start,
                duration=120,
                schedule=weekly_schedule(open_swim_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[sports],
            ),
            # Same-day events (2026-09-19, alongside WatHacks 2026) — for testing the planner day view
            Event(
                name="same_day_test_1",
                description="Kickoff event for WatHacks 2026. Meet the sponsors and hear the challenge tracks announced.",
                location="E7 Building, Room 1004",
                lat=43.4729, lng=-80.5393,
                start_time=same_day_1_start,
                duration=60,
                schedule=weekly_schedule(same_day_1_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering, social],
            ),
            Event(
                name="same_day_test_2",
                description="Back-to-back technical workshops hosted by hackathon sponsors. Drop in for any session.",
                location="E7 Building, Room 1001",
                lat=43.4729, lng=-80.5393,
                start_time=same_day_2_start,
                duration=90,
                schedule=weekly_schedule(same_day_2_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering],
            ),
            Event(
                name="same_day_test_3",
                description="Grab lunch and find teammates before the hacking begins. Icebreaker games included.",
                location="E7 Building Atrium",
                lat=43.4729, lng=-80.5393,
                start_time=same_day_3_start,
                duration=60,
                schedule=weekly_schedule(same_day_3_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[social],
            ),
            # Overlapping events (2026-09-19) — for testing the schedule's side-by-side column layout
            Event(
                name="overlapping_event_1",
                description="Overlaps with overlapping_event_2 and overlapping_event_3 for testing the day schedule.",
                location="E7 Building, Room 2001",
                lat=43.4729, lng=-80.5393,
                start_time=overlap_1_start,
                duration=90,
                schedule=weekly_schedule(overlap_1_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering],
            ),
            Event(
                name="overlapping_event_2",
                description="Overlaps with overlapping_event_1 and overlapping_event_3 for testing the day schedule.",
                location="E7 Building, Room 2002",
                lat=43.4729, lng=-80.5393,
                start_time=overlap_2_start,
                duration=60,
                schedule=weekly_schedule(overlap_2_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering],
            ),
            Event(
                name="overlapping_event_3",
                description="Overlaps with overlapping_event_1 and overlapping_event_2 for testing the day schedule.",
                location="E7 Building, Room 2003",
                lat=43.4729, lng=-80.5393,
                start_time=overlap_3_start,
                duration=30,
                schedule=weekly_schedule(overlap_3_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering],
            ),
            # Daily recurring events — for testing the "Happening This Week" / campus map daily overlap
            Event(
                name="daily_event_1",
                description="Daily-recurring test event, early morning slot.",
                location="SLC Great Hall",
                lat=43.4745, lng=-80.5525,
                start_time=daily_event_1_start,
                duration=45,
                schedule=daily_schedule(daily_event_1_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[social],
            ),
            Event(
                name="daily_event_2",
                description="Daily-recurring test event, midday slot.",
                location="DC Atrium",
                lat=43.4723, lng=-80.5449,
                start_time=daily_event_2_start,
                duration=45,
                schedule=daily_schedule(daily_event_2_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[academic],
            ),
            Event(
                name="daily_event_3",
                description="Daily-recurring test event, late afternoon slot.",
                location="PAC Fieldhouse",
                lat=43.4754, lng=-80.5503,
                start_time=daily_event_3_start,
                duration=45,
                schedule=daily_schedule(daily_event_3_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[sports],
            ),
            Event(
                name="daily_event_4",
                description="Daily-recurring test event, evening slot.",
                location="E7 Building Atrium",
                lat=43.4729, lng=-80.5393,
                start_time=daily_event_4_start,
                duration=45,
                schedule=daily_schedule(daily_event_4_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[engineering],
            ),
            Event(
                name="daily_event_5",
                description="Daily-recurring test event, night slot.",
                location="CIF Field",
                lat=43.4752, lng=-80.5516,
                start_time=daily_event_5_start,
                duration=45,
                schedule=daily_schedule(daily_event_5_start),
                frequency_end=None,
                user_id=organizer.id, reviewer_id=admin.id,
                tags=[social],
            ),
            # Pending events
            Event(
                name="Intramural Soccer Signup",
                description="Register your team for the fall intramural soccer league. Games run Tuesday and Thursday evenings.",
                location="CIF Field",
                lat=43.4752, lng=-80.5516,
                start_time=soccer_signup_start,
                duration=60,
                schedule=weekly_schedule(soccer_signup_start),
                frequency_end=None,
                user_id=student.id,
                tags=[sports, social],
            ),
            Event(
                name="WEEF Grants Info Session",
                description="Learn how to apply for Waterloo Engineering Endowment Foundation grants to fund your project.",
                location="CPH 3607",
                lat=43.4718, lng=-80.5408,
                start_time=weef_start,
                duration=60,
                schedule=weekly_schedule(weef_start),
                frequency_end=None,
                user_id=student.id,
                tags=[engineering, academic],
            ),
            Event(
                name="UW Food Bank Volunteer Day",
                description="Help sort and distribute food hampers at the UW Food Bank. Shifts are 2 hours. All volunteers welcome.",
                location="SLC Great Hall",
                lat=43.4745, lng=-80.5525,
                start_time=food_bank_start,
                duration=120,
                schedule=weekly_schedule(food_bank_start),
                frequency_end=None,
                user_id=student.id,
                tags=[volunteer, social],
            ),
            Event(
                name="Midnight Sun Solar Car Showcase",
                description="See Midnight Sun's latest solar car up close. Team members will be on hand to answer questions.",
                location="Engineering Quad, E7 Parking Lot",
                lat=43.4731, lng=-80.5396,
                start_time=solar_car_start,
                duration=180,
                schedule=weekly_schedule(solar_car_start),
                frequency_end=None,
                user_id=student.id,
                tags=[engineering],
            ),
            Event(
                name="Student Leadership Awards Gala",
                description="Annual ceremony recognizing outstanding student leaders across all faculties. Formal attire encouraged.",
                location="SLC Multipurpose Room",
                lat=43.4745, lng=-80.5525,
                start_time=gala_start,
                duration=180,
                schedule=weekly_schedule(gala_start),
                frequency_end=None,
                user_id=student.id,
                tags=[social],
            ),
        ])
        db.commit()
        print("Seeded: 3 users, 6 tags, 29 events (24 approved, 5 pending); 5 daily-recurring, rest weekly-recurring.")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
