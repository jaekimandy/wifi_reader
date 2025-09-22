#!/usr/bin/env python3
"""
Download Tesseract OCR data files for WiFi Reader app
- Tesseract language data for multiple languages
- Optimized for text recognition on router labels
"""

import os
import urllib.request
import hashlib
import sys
from pathlib import Path

# Tesseract language data files - using reliable GitHub release URLs
MODELS = {
    "eng.traineddata": {
        "url": "https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata",
        "fallback_url": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata",
        "size_mb": 15.0,
        "description": "English language data (primary)"
    },
    "spa.traineddata": {
        "url": "https://github.com/tesseract-ocr/tessdata/raw/main/spa.traineddata",
        "fallback_url": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/spa.traineddata",
        "size_mb": 12.0,
        "description": "Spanish language data"
    },
    "fra.traineddata": {
        "url": "https://github.com/tesseract-ocr/tessdata/raw/main/fra.traineddata",
        "fallback_url": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/fra.traineddata",
        "size_mb": 11.0,
        "description": "French language data"
    },
    "deu.traineddata": {
        "url": "https://github.com/tesseract-ocr/tessdata/raw/main/deu.traineddata",
        "fallback_url": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/deu.traineddata",
        "size_mb": 12.0,
        "description": "German language data"
    }
}

def download_file(url, filepath, description="file"):
    """Download a file with progress indicator"""
    try:
        print(f"Downloading {description}...")
        print(f"   URL: {url}")
        print(f"   Saving to: {filepath}")

        def progress_hook(block_num, block_size, total_size):
            if total_size > 0:
                percent = min(100, (block_num * block_size * 100) // total_size)
                bar_length = 30
                filled_length = (percent * bar_length) // 100
                bar = '#' * filled_length + '-' * (bar_length - filled_length)
                print(f"\r   Progress: [{bar}] {percent}%", end='', flush=True)

        urllib.request.urlretrieve(url, filepath, progress_hook)
        print("\n   [OK] Download completed!")
        return True

    except Exception as e:
        print(f"\n   [ERROR] Download failed: {e}")
        return False

def verify_file_size(filepath, expected_size_mb, tolerance=0.5):
    """Verify downloaded file size is reasonable"""
    if not os.path.exists(filepath):
        return False

    actual_size_mb = os.path.getsize(filepath) / (1024 * 1024)
    if abs(actual_size_mb - expected_size_mb) > tolerance:
        print(f"   [WARNING] File size {actual_size_mb:.1f}MB differs from expected {expected_size_mb}MB")
        return False

    print(f"   [OK] File size verified: {actual_size_mb:.1f}MB")
    return True

def main():
    print("Tesseract OCR Language Data Downloader")
    print("=" * 50)

    # Create assets directory
    assets_dir = Path("app/src/main/assets")
    assets_dir.mkdir(parents=True, exist_ok=True)
    print(f"Assets directory: {assets_dir.absolute()}")

    # Download each model
    for filename, model_info in MODELS.items():
        print(f"\n{model_info['description']}")
        print("-" * 40)

        filepath = assets_dir / filename

        # Skip if file already exists and is correct size
        if filepath.exists() and verify_file_size(filepath, model_info['size_mb']):
            print(f"   [OK] {filename} already exists and verified")
            continue

        # Try primary URL first
        success = download_file(model_info['url'], filepath, model_info['description'])

        # Try fallback URL if primary fails
        if not success and 'fallback_url' in model_info:
            print(f"   [RETRY] Trying fallback URL...")
            success = download_file(model_info['fallback_url'], filepath, model_info['description'])

        if success:
            if verify_file_size(filepath, model_info['size_mb']):
                print(f"   [OK] {filename} downloaded and verified successfully!")
            else:
                print(f"   [WARNING] {filename} downloaded but size verification failed")
        else:
            print(f"   [ERROR] Failed to download {filename}")
            return False

    print("\n" + "=" * 50)
    print("All OCR models downloaded successfully!")
    print("\nDownloaded models:")
    for filename in MODELS.keys():
        filepath = assets_dir / filename
        if filepath.exists():
            size_mb = os.path.getsize(filepath) / (1024 * 1024)
            print(f"   [OK] {filename} ({size_mb:.1f}MB)")
        else:
            print(f"   [MISSING] {filename}")

    print(f"\nModels location: {assets_dir.absolute()}")
    print("\nYou can now build the app with real OCR capabilities!")
    return True

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)