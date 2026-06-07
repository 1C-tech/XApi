from __future__ import annotations

import os

import httpx

from .models import PostItem


class XPostClient:
    def __init__(self, http_client: httpx.Client | None = None) -> None:
        self.base_url = os.getenv("XAPI_BASE_URL", "http://localhost:8080")
        self.http = http_client or httpx.Client(timeout=15.0)

    def get_user_posts(self, user_id: str, count: int = 5) -> list[PostItem]:
        response = self.http.get(
            f"{self.base_url}/api/x/user-tweets",
            params={"userId": user_id, "count": count},
        )
        response.raise_for_status()
        body = response.json()
        return [
            PostItem(
                id=str(item.get("id", "")),
                author_name=item.get("authorName"),
                author_screen_name=item.get("authorScreenName"),
                text=item.get("fullText") or "",
                created_at=item.get("createdAt"),
            )
            for item in body.get("tweets", [])
            if item.get("fullText")
        ]
