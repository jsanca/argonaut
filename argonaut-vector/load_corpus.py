#!/usr/bin/env python3

import argparse
import json
from pathlib import Path
from urllib import request, error


def post_json(url: str, payload: dict) -> tuple[int, str]:
    data = json.dumps(payload).encode("utf-8")

    req = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with request.urlopen(req, timeout=30) as response:
            return response.status, response.read().decode("utf-8")
    except error.HTTPError as exc:
        return exc.code, exc.read().decode("utf-8")
    except error.URLError as exc:
        raise RuntimeError(f"Could not reach {url}: {exc}") from exc


def main():
    parser = argparse.ArgumentParser(
        description="Load Argonaut Vector synthetic corpus through the REST API."
    )
    parser.add_argument(
        "--corpus",
        required=True,
        help="Directory containing Markdown corpus files.",
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8086",
        help="Argonaut Vector base URL.",
    )
    args = parser.parse_args()

    corpus_dir = Path(args.corpus)
    endpoint = f"{args.base_url.rstrip('/')}/api/vector/store"

    documents = sorted(corpus_dir.glob("*.md"))

    if not documents:
        raise SystemExit(f"No Markdown files found in {corpus_dir}")

    success = 0
    failed = 0

    for document in documents:
        document_id = document.stem
        content = document.read_text(encoding="utf-8")

        status, body = post_json(
            endpoint,
            {
                "id": document_id,
                "content": content,
            },
        )

        if 200 <= status < 300:
            success += 1
            print(f"[OK]   {document_id}")
        else:
            failed += 1
            print(f"[FAIL] {document_id} HTTP {status}: {body}")

    print()
    print(f"Stored: {success}")
    print(f"Failed: {failed}")
    print(f"Total:  {len(documents)}")

    if failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()