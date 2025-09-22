#!/usr/bin/env python3
"""
Download working TensorFlow Lite OCR models for real text detection and recognition.
This replaces the problematic PaddleOCR conversion script with direct downloads.
"""

import os
import sys
import requests
import shutil
from pathlib import Path

def download_file(url, filepath):
    """Download file with progress bar."""
    print(f"Downloading {filepath.name}...")

    try:
        response = requests.get(url, stream=True)
        response.raise_for_status()

        total_size = int(response.headers.get('content-length', 0))
        downloaded = 0

        with open(filepath, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"\r  Progress: {percent:.1f}%", end='', flush=True)

        print(f"\n  Downloaded: {filepath} ({downloaded} bytes)")
        return True
    except Exception as e:
        print(f"\n  Error downloading {url}: {e}")
        return False

def main():
    print("Real TensorFlow Lite OCR Model Setup")
    print("=" * 40)

    # Setup directories
    script_dir = Path(__file__).parent
    assets_dir = script_dir.parent / 'app' / 'src' / 'main' / 'assets'

    # Create directories
    assets_dir.mkdir(parents=True, exist_ok=True)

    print(f"Assets directory: {assets_dir}")

    # Working TensorFlow Lite OCR models
    models = [
        {
            'name': 'CRAFT Text Detection',
            'description': 'CRAFT text detection model trained on SynthText, IC13, IC17',
            'url': 'https://github.com/tulasiram58827/craft_tflite/raw/main/models/craft_float16.tflite',
            'file': assets_dir / 'craft_text_detection.tflite'
        },
        {
            'name': 'KerasOCR Text Recognition',
            'description': 'Keras OCR float16 quantized text recognition model',
            'url': 'https://github.com/tulasiram58827/ocr_tflite/raw/main/models/keras_ocr_float16.tflite',
            'file': assets_dir / 'keras_text_recognition.tflite'
        },
        {
            'name': 'Deep Text Recognition (Alternative)',
            'description': 'CNN-based text recognition model (alternative to Keras OCR)',
            'url': 'https://github.com/tulasiram58827/ocr_tflite/raw/main/models/cnn_float16.tflite',
            'file': assets_dir / 'cnn_text_recognition.tflite'
        }
    ]

    success_count = 0

    for model in models:
        print(f"\n--- Downloading {model['name']} ---")
        print(f"Description: {model['description']}")

        if model['file'].exists():
            print(f"  File already exists: {model['file']}")
            existing_size = model['file'].stat().st_size
            if existing_size > 10000:  # At least 10KB for a real model
                print(f"  Using existing model ({existing_size} bytes)")
                success_count += 1
                continue
            else:
                print(f"  Existing file too small ({existing_size} bytes), re-downloading...")

        if download_file(model['url'], model['file']):
            file_size = model['file'].stat().st_size
            if file_size > 10000:  # Validate size
                success_count += 1
                print(f"  OK {model['name']} ready ({file_size} bytes)")
            else:
                print(f"  ERROR Downloaded file too small ({file_size} bytes)")
                model['file'].unlink(missing_ok=True)
        else:
            print(f"  ERROR Failed to download {model['name']}")

    print(f"\n--- Summary ---")
    if success_count >= 2:  # Need at least detection + recognition
        print(f"OK Successfully downloaded {success_count}/{len(models)} models")
        print(f"OK Ready for real OCR processing!")
        print(f"\nModels in: {assets_dir}")

        # List final model files
        print("\nFinal model files:")
        for model_file in assets_dir.glob("*.tflite"):
            size = model_file.stat().st_size
            print(f"  {model_file.name}: {size:,} bytes")

    else:
        print(f"ERROR Only {success_count}/{len(models)} models downloaded successfully")
        print("Need at least text detection + text recognition models")

    print(f"\nSetup complete!")

if __name__ == '__main__':
    main()