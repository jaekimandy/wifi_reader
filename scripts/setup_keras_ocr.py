#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Setup Keras OCR Models for WiFi Reader App

This script sets up the Keras OCR model as an alternative to the problematic
CRAFT+CRNN EasyOCR implementation that has tensor size mismatches.

Keras OCR is an end-to-end OCR model that does both text detection AND
recognition in a single model, which should be much simpler than the
two-stage CRAFT+CRNN pipeline.

Usage:
    python setup_keras_ocr.py
"""

import os
import sys
import shutil
from pathlib import Path

# Set UTF-8 encoding for Windows
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer)
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer)

def main():
    """Setup Keras OCR model for the WiFi Reader app"""
    print("🚀 Setting up Keras OCR Model")
    print("=" * 40)

    # Get script directory
    script_dir = Path(__file__).parent
    print(f"📁 Script directory: {script_dir}")

    # Define source and destination paths
    keras_model_source = script_dir / "keras_ocr_float16.tflite"
    assets_dir = script_dir.parent / "app" / "src" / "main" / "assets"
    keras_model_dest = assets_dir / "keras_ocr_float16.tflite"

    print(f"📁 Assets directory: {assets_dir}")

    # Check if source model exists
    if not keras_model_source.exists():
        print(f"❌ Source model not found: {keras_model_source}")
        print("💡 Run download_ocr_tflite_direct.py first to download Keras OCR models")
        return False

    # Check source model size
    source_size = keras_model_source.stat().st_size
    source_size_mb = source_size / (1024 * 1024)
    print(f"📊 Source model size: {source_size_mb:.1f} MB")

    # Verify it's a valid TFLite file
    with open(keras_model_source, 'rb') as f:
        header = f.read(8)
        if b'TFL3' not in header:
            print(f"❌ Invalid TensorFlow Lite file: {keras_model_source}")
            print(f"   Header: {header.hex()}")
            return False

    print("✅ Valid TensorFlow Lite model confirmed")

    # Create assets directory if it doesn't exist
    assets_dir.mkdir(parents=True, exist_ok=True)

    # Copy model to assets directory
    try:
        print(f"📋 Copying model to assets directory...")
        shutil.copy2(keras_model_source, keras_model_dest)

        # Verify the copy
        if keras_model_dest.exists():
            dest_size = keras_model_dest.stat().st_size
            if dest_size == source_size:
                print(f"✅ Model copied successfully: {keras_model_dest}")
                print(f"📊 Destination size: {dest_size / (1024 * 1024):.1f} MB")
            else:
                print(f"❌ Copy size mismatch: {source_size} vs {dest_size}")
                return False
        else:
            print(f"❌ Copy failed: {keras_model_dest} not found")
            return False

    except Exception as e:
        print(f"❌ Failed to copy model: {e}")
        return False

    # Summary
    print(f"\n{'='*40}")
    print("📊 Setup Summary:")
    print(f"✅ Model: keras_ocr_float16.tflite ({source_size_mb:.1f} MB)")
    print(f"✅ Location: {keras_model_dest}")
    print(f"✅ Type: End-to-end OCR (detection + recognition)")

    print("\n📋 Next steps:")
    print("1. Update WiFiDetectionPipeline to use KerasOCREngine")
    print("2. Build and test the app")
    print("3. Run JUnit tests to verify functionality")

    print(f"\n💡 Advantages of Keras OCR over CRAFT+CRNN:")
    print("• Single model (no tensor size mismatches)")
    print("• End-to-end processing (simpler pipeline)")
    print("• Pre-validated on TensorFlow Hub")
    print("• No complex two-stage detection/recognition")

    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n⏹️  Setup interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1)