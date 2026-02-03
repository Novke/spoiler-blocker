"""
Download ML model for Spoiler Shield app.

Usage:
    python download_model.py

Or manually download:
    - model.onnx: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
    - vocab.txt:  https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/raw/main/vocab.txt

Place both files in: app/src/main/assets/
"""

import os
import urllib.request
from pathlib import Path

ASSETS_DIR = Path("app/src/main/assets")

FILES = {
    "model.onnx": "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx",
    "vocab.txt": "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/raw/main/vocab.txt"
}

def download_file(url: str, dest: Path):
    print(f"Downloading {dest.name}...")
    urllib.request.urlretrieve(url, dest)
    size = dest.stat().st_size / (1024 * 1024)
    print(f"  Done: {size:.1f} MB")

def main():
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    for filename, url in FILES.items():
        dest = ASSETS_DIR / filename
        if dest.exists():
            print(f"{filename} already exists, skipping")
        else:
            download_file(url, dest)

    print("\nAll files downloaded! Run: ./gradlew assembleDebug")

if __name__ == "__main__":
    main()
