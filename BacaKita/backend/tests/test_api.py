import os
import sys

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
import main  # noqa: E402

VALID_REQ = {
    "preferences": {
        "fontSizePreference": "large",
        "lineSpacingPreference": "wide",
        "contrastPreference": "high",
        "focusModePreferred": True,
    }
}


@pytest.fixture(autouse=True)
def reset_limiter():
    main._hits.clear()


@pytest.fixture
def client():
    return TestClient(main.app)


def fake_ai(result):
    async def _call(_prefs):
        return result
    return _call


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_valid_recommendation(client, monkeypatch):
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": 22, "lineSpacingMultiplier": 1.6, "letterSpacing": 0.5,
        "theme": "high_contrast", "focusMode": True, "reason": "Titik awal yang dapat diubah.",
    }))
    r = client.post("/api/v1/recommendations", json=VALID_REQ)
    assert r.status_code == 200
    rec = r.json()["recommendation"]
    assert r.json()["success"] is True
    assert rec["fontSize"] == 22 and rec["theme"] == "high_contrast" and rec["focusMode"] is True


def test_extreme_values_are_clamped(client, monkeypatch):
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": 500, "lineSpacingMultiplier": -3, "letterSpacing": 99,
        "theme": "dark", "focusMode": False, "reason": "ok",
    }))
    rec = client.post("/api/v1/recommendations", json=VALID_REQ).json()["recommendation"]
    assert rec["fontSize"] == 40 and rec["lineSpacingMultiplier"] == 1.0 and rec["letterSpacing"] == 3.0


def test_unsupported_theme_rejected(client, monkeypatch):
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": 20, "lineSpacingMultiplier": 1.5, "letterSpacing": 0,
        "theme": "neon", "focusMode": False, "reason": "ok",
    }))
    r = client.post("/api/v1/recommendations", json=VALID_REQ)
    assert r.status_code == 502 and r.json()["success"] is False


def test_wrong_types_rejected(client, monkeypatch):
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": "besar", "lineSpacingMultiplier": 1.5, "letterSpacing": 0,
        "theme": "dark", "focusMode": False, "reason": "ok",
    }))
    assert client.post("/api/v1/recommendations", json=VALID_REQ).status_code == 502


def test_medical_claims_in_reason_are_replaced(client, monkeypatch):
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": 20, "lineSpacingMultiplier": 1.5, "letterSpacing": 0,
        "theme": "dark", "focusMode": False, "reason": "Cocok untuk penderita disleksia.",
    }))
    rec = client.post("/api/v1/recommendations", json=VALID_REQ).json()["recommendation"]
    assert "disleksia" not in rec["reason"].lower()


def test_invalid_request_is_422_with_consistent_format(client):
    bad = {"preferences": {**VALID_REQ["preferences"], "fontSizePreference": "gigantic"}}
    r = client.post("/api/v1/recommendations", json=bad)
    assert r.status_code == 422 and r.json()["error"]["code"] == "invalid_request"


def test_extra_fields_forbidden(client):
    bad = {"preferences": VALID_REQ["preferences"], "text": "isi dokumen rahasia"}
    assert client.post("/api/v1/recommendations", json=bad).status_code == 422


def test_oversized_body_rejected(client):
    r = client.post("/api/v1/recommendations", content="x" * 5000,
                    headers={"Content-Type": "application/json"})
    assert r.status_code == 413


def test_missing_api_key_gives_503(client, monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    r = client.post("/api/v1/recommendations", json=VALID_REQ)
    assert r.status_code == 503 and r.json()["error"]["code"] == "ai_not_configured"


def test_rate_limit(client, monkeypatch):
    monkeypatch.setenv("RATE_LIMIT_PER_MINUTE", "2")
    monkeypatch.setattr(main, "call_gemini", fake_ai({
        "fontSize": 20, "lineSpacingMultiplier": 1.5, "letterSpacing": 0,
        "theme": "light", "focusMode": False, "reason": "ok",
    }))
    codes = [client.post("/api/v1/recommendations", json=VALID_REQ).status_code for _ in range(3)]
    assert codes == [200, 200, 429]
