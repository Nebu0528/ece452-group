from datetime import datetime


def test_create_event_with_weekly_schedule_round_trips(client):
    payload = {
        "name": "Weekly Standup",
        "description": "Team sync",
        "location": "E7 2001",
        "lat": 43.47,
        "lng": -80.54,
        "start_time": "2026-08-03T09:00:00",
        "duration": 30,
        "schedule": "0 9 * * 1",
        "frequency_end": "2026-12-31",
        "tag_ids": [],
    }
    response = client.post("/events/", json=payload)
    assert response.status_code == 200
    body = response.json()
    assert body["schedule"] == "0 9 * * 1"
    assert body["frequency_end"] == "2026-12-31"


def test_create_event_rejects_frequency_end_without_schedule(client):
    payload = {
        "name": "Bad Event",
        "start_time": "2026-08-03T09:00:00",
        "duration": 30,
        "frequency_end": "2026-12-31",
        "tag_ids": [],
    }
    response = client.post("/events/", json=payload)
    assert response.status_code == 422


def test_create_event_rejects_invalid_cron(client):
    payload = {
        "name": "Bad Cron",
        "start_time": "2026-08-03T09:00:00",
        "duration": 30,
        "schedule": "not a cron string",
        "tag_ids": [],
    }
    response = client.post("/events/", json=payload)
    assert response.status_code == 422


def test_list_events_computes_next_occurrence_for_future_one_time_event(client):
    payload = {
        "name": "Future One-Time Event",
        "start_time": "2100-01-01T10:00:00",
        "duration": 60,
        "tag_ids": [],
    }
    client.post("/events/", json=payload)
    response = client.get("/events/")
    assert response.status_code == 200
    event = next(e for e in response.json() if e["name"] == "Future One-Time Event")
    assert event["next_occurrence_start"] == "2100-01-01T10:00:00"
    assert event["next_occurrence_end"] == "2100-01-01T11:00:00"


def test_list_events_next_occurrence_is_null_for_past_one_time_event(client):
    payload = {
        "name": "Past One-Time Event",
        "start_time": "2000-01-01T10:00:00",
        "duration": 60,
        "tag_ids": [],
    }
    client.post("/events/", json=payload)
    response = client.get("/events/")
    event = next(e for e in response.json() if e["name"] == "Past One-Time Event")
    assert event["next_occurrence_start"] is None
    assert event["next_occurrence_end"] is None


def test_list_events_recurring_series_anchored_in_past_still_has_future_occurrence(client):
    payload = {
        "name": "Long Running Weekly Series",
        "start_time": "2000-01-03T09:00:00",  # a Monday
        "duration": 30,
        "schedule": "0 9 * * 1",
        "tag_ids": [],
    }
    client.post("/events/", json=payload)
    response = client.get("/events/")
    event = next(e for e in response.json() if e["name"] == "Long Running Weekly Series")
    assert event["next_occurrence_start"] is not None
    assert datetime.fromisoformat(event["next_occurrence_start"]) > datetime(2020, 1, 1)
