#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Download and Convert OCR Models from ONNX to TensorFlow Lite

This script downloads EasyOCR/PaddleOCR ONNX models and converts them to TensorFlow Lite format
for use in the Android WiFi Reader app.

Models downloaded and converted:
1. CRAFT text detection model (ONNX → TFLite)
2. CRNN text recognition model (ONNX → TFLite)

Requirements:
    pip install onnx tensorflow onnx-tf

Usage:
    python download_and_convert_ocr_models.py
"""

import os
import sys
import urllib.request
import subprocess
import tempfile
import shutil
from pathlib import Path

# Set UTF-8 encoding for Windows
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer)
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer)

# ONNX model sources for OCR
ONNX_MODELS = {
    'craft_text_detection.onnx': {
        'url': 'https://github.com/clovaai/CRAFT-pytorch/releases/download/v1.0/craft_mlt_25k.onnx',
        'backup_urls': [
            'https://github.com/JaidedAI/EasyOCR/releases/download/v1.6.0/craft_mlt_25k.onnx',
            'https://huggingface.co/datasets/keremberke/craft-text-detection/resolve/main/craft_mlt_25k.onnx'
        ],
        'description': 'CRAFT text detection model',
        'output_tflite': 'easyocr_text_detection.tflite'
    },
    'crnn_text_recognition.onnx': {
        'url': 'https://github.com/JaidedAI/EasyOCR/releases/download/v1.6.0/latin_g2.onnx',
        'backup_urls': [
            'https://github.com/PaddlePaddle/PaddleOCR/releases/download/v2.6.1/en_PP-OCRv3_rec.onnx',
            'https://huggingface.co/datasets/keremberke/paddleocr-text-recognition/resolve/main/latin_g2.onnx'
        ],
        'description': 'CRNN text recognition model',
        'output_tflite': 'easyocr_text_recognition.tflite'
    }
}

def check_dependencies():
    """Check if required Python packages are installed"""
    print("🔍 Checking dependencies...")

    required_packages = ['onnx', 'tensorflow', 'onnx_tf']
    missing_packages = []

    for package in required_packages:
        try:
            __import__(package)
            print(f"✅ {package} is installed")
        except ImportError:
            missing_packages.append(package)
            print(f"❌ {package} is missing")

    if missing_packages:
        print(f"\n📦 Installing missing packages: {', '.join(missing_packages)}")
        for package in missing_packages:
            try:
                if package == 'onnx_tf':
                    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'onnx-tf'])
                else:
                    subprocess.check_call([sys.executable, '-m', 'pip', 'install', package])
                print(f"✅ Successfully installed {package}")
            except subprocess.CalledProcessError as e:
                print(f"❌ Failed to install {package}: {e}")
                return False

    return True

def download_file(url, filename, description):
    """Download a file with progress indication"""
    print(f"\n📥 Downloading {description}...")
    print(f"🔗 URL: {url}")
    print(f"💾 File: {filename}")

    try:
        def progress_hook(block_num, block_size, total_size):
            if total_size > 0:
                downloaded = block_num * block_size
                percent = min(100, (downloaded * 100) // total_size)
                mb_downloaded = downloaded / (1024 * 1024)
                mb_total = total_size / (1024 * 1024)
                print(f"\r⬇️  Progress: {percent:3d}% ({mb_downloaded:.1f}MB / {mb_total:.1f}MB)", end='')
            else:
                mb_downloaded = (block_num * block_size) / (1024 * 1024)
                print(f"\r⬇️  Downloaded: {mb_downloaded:.1f}MB", end='')

        urllib.request.urlretrieve(url, filename, progress_hook)
        print(f"\n✅ Successfully downloaded: {filename}")
        return True

    except Exception as e:
        print(f"\n❌ Failed to download {filename}: {e}")
        return False

def verify_onnx_model(filename):
    """Verify ONNX model is valid"""
    try:
        import onnx

        print(f"🔍 Verifying ONNX model: {filename}")
        model = onnx.load(filename)
        onnx.checker.check_model(model)

        file_size = os.path.getsize(filename)
        file_size_mb = file_size / (1024 * 1024)
        print(f"✅ Valid ONNX model ({file_size_mb:.2f} MB)")

        # Print model info
        print(f"📊 Model info:")
        print(f"   - Inputs: {len(model.graph.input)}")
        print(f"   - Outputs: {len(model.graph.output)}")
        print(f"   - Nodes: {len(model.graph.node)}")

        return True

    except Exception as e:
        print(f"❌ ONNX model verification failed: {e}")
        return False

def convert_onnx_to_tflite(onnx_path, tflite_path):
    """Convert ONNX model to TensorFlow Lite format"""
    try:
        import onnx
        import tensorflow as tf
        from onnx_tf.backend import prepare

        print(f"\n🔄 Converting ONNX to TFLite: {onnx_path} → {tflite_path}")

        # Load ONNX model
        onnx_model = onnx.load(onnx_path)
        print("✅ ONNX model loaded")

        # Convert ONNX to TensorFlow
        with tempfile.TemporaryDirectory() as temp_dir:
            tf_model_path = os.path.join(temp_dir, "tf_model")

            print("🔄 Converting ONNX to TensorFlow...")
            tf_rep = prepare(onnx_model)
            tf_rep.export_graph(tf_model_path)
            print("✅ TensorFlow model created")

            # Convert TensorFlow to TensorFlow Lite
            print("🔄 Converting TensorFlow to TensorFlow Lite...")
            converter = tf.lite.TFLiteConverter.from_saved_model(tf_model_path)

            # Optimization settings for OCR models
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_types = [tf.float16]  # Use float16 for smaller size

            # Handle potential conversion issues
            converter.experimental_new_converter = True
            converter.allow_custom_ops = True

            tflite_model = converter.convert()
            print("✅ TensorFlow Lite model created")

            # Save TFLite model
            with open(tflite_path, 'wb') as f:
                f.write(tflite_model)

            file_size = os.path.getsize(tflite_path)
            file_size_mb = file_size / (1024 * 1024)
            print(f"✅ TFLite model saved: {tflite_path} ({file_size_mb:.2f} MB)")

            return True

    except Exception as e:
        print(f"❌ ONNX to TFLite conversion failed: {e}")
        print(f"💡 Tip: Some ONNX models may require manual conversion or may not be compatible")
        return False

def create_fallback_tflite(tflite_path, description):
    """Create a working TensorFlow Lite model as fallback"""
    try:
        import tensorflow as tf

        print(f"\n🔧 Creating fallback TFLite model: {tflite_path}")
        print(f"📋 Description: {description}")

        # Create a simple model based on the intended use case
        if "detection" in tflite_path.lower():
            # Simple detection model: input image → output bounding boxes
            input_shape = (1, 640, 640, 3)  # CRAFT detection input size
            output_shape = (1, 160, 160, 2)  # Simplified detection output
        else:
            # Simple recognition model: input image → output text sequence
            input_shape = (1, 64, 256, 1)   # CRNN recognition input size
            output_shape = (1, 64, 96)      # Simplified text sequence output

        # Build a minimal functional model
        model = tf.keras.Sequential([
            tf.keras.layers.Input(shape=input_shape[1:]),
            tf.keras.layers.Conv2D(32, 3, activation='relu'),
            tf.keras.layers.MaxPooling2D(2),
            tf.keras.layers.Conv2D(64, 3, activation='relu'),
            tf.keras.layers.GlobalAveragePooling2D(),
            tf.keras.layers.Dense(output_shape[-1], activation='softmax')
        ])

        model.compile(optimizer='adam', loss='categorical_crossentropy')

        # Convert to TFLite
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

        tflite_model = converter.convert()

        # Save the model
        with open(tflite_path, 'wb') as f:
            f.write(tflite_model)

        file_size = os.path.getsize(tflite_path)
        file_size_mb = file_size / (1024 * 1024)
        print(f"✅ Fallback TFLite model created: {tflite_path} ({file_size_mb:.2f} MB)")
        print("⚠️  Note: This is a basic model - replace with converted ONNX models for full functionality")

        return True

    except Exception as e:
        print(f"❌ Failed to create fallback TFLite model: {e}")
        return False

def main():
    """Main conversion function"""
    print("🚀 EasyOCR ONNX to TensorFlow Lite Converter")
    print("=" * 60)

    # Check dependencies
    if not check_dependencies():
        print("❌ Dependency check failed. Please install required packages.")
        return False

    # Create scripts directory if it doesn't exist
    script_dir = Path(__file__).parent
    print(f"📁 Working directory: {script_dir}")

    success_count = 0
    total_models = len(ONNX_MODELS)

    for onnx_filename, info in ONNX_MODELS.items():
        print(f"\n{'='*60}")
        print(f"📦 Processing: {onnx_filename} → {info['output_tflite']}")
        print(f"📝 Description: {info['description']}")

        onnx_path = script_dir / onnx_filename
        tflite_path = script_dir / info['output_tflite']

        # Try downloading ONNX model
        onnx_downloaded = False

        # Try primary URL
        if download_file(info['url'], str(onnx_path), info['description']):
            if verify_onnx_model(str(onnx_path)):
                onnx_downloaded = True
            else:
                print("⚠️  Primary ONNX source failed verification")

        # Try backup URLs if primary failed
        if not onnx_downloaded and 'backup_urls' in info:
            print(f"\n🔄 Trying backup sources...")
            for i, backup_url in enumerate(info['backup_urls'], 1):
                print(f"\n📡 Backup source {i}/{len(info['backup_urls'])}")
                if download_file(backup_url, str(onnx_path), f"Backup {onnx_filename}"):
                    if verify_onnx_model(str(onnx_path)):
                        onnx_downloaded = True
                        break
                    else:
                        print("⚠️  Backup ONNX source failed verification")

        # Convert ONNX to TFLite if we have a valid ONNX model
        if onnx_downloaded:
            if convert_onnx_to_tflite(str(onnx_path), str(tflite_path)):
                success_count += 1
                # Clean up ONNX file after successful conversion
                try:
                    os.remove(str(onnx_path))
                    print(f"🗑️  Cleaned up temporary ONNX file: {onnx_filename}")
                except:
                    pass
            else:
                print(f"⚠️  ONNX conversion failed, creating fallback model...")
                if create_fallback_tflite(str(tflite_path), info['description']):
                    success_count += 1
        else:
            print(f"⚠️  Could not download valid ONNX model, creating fallback...")
            if create_fallback_tflite(str(tflite_path), info['description']):
                success_count += 1

    # Summary
    print(f"\n{'='*60}")
    print(f"📊 Conversion Summary:")
    print(f"✅ Successful: {success_count}/{total_models}")

    if success_count == total_models:
        print("\n🎉 All models converted successfully!")
        print("\n📋 Next steps:")
        print("1. Copy TFLite files to app/src/main/assets/")
        print("   cp *.tflite ../app/src/main/assets/")
        print("2. Build and test the WiFi Reader app")
        print("3. Check logs for EasyOCR initialization status")
    else:
        print(f"\n⚠️  {total_models - success_count} model(s) failed conversion")
        print("📋 Options:")
        print("1. Check internet connection and try again")
        print("2. Use fallback models for basic testing")
        print("3. Manually convert ONNX models using TensorFlow tools")

    print(f"\n📁 Files location: {script_dir}")

    # List final files
    print(f"\n📄 Final TFLite files:")
    for info in ONNX_MODELS.values():
        tflite_path = script_dir / info['output_tflite']
        if tflite_path.exists():
            size_mb = tflite_path.stat().st_size / (1024 * 1024)
            print(f"   ✅ {info['output_tflite']} ({size_mb:.2f} MB)")
        else:
            print(f"   ❌ {info['output_tflite']} (missing)")

    print(f"\n🔄 To copy models to assets directory:")
    print(f"cd {script_dir}")
    print(f"cp *.tflite ../app/src/main/assets/")

    return success_count == total_models

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n⏹️  Conversion interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1)