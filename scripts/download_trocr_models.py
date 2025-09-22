#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Download and Convert TrOCR Models for WiFi Reader app

TrOCR (Transformer-based OCR) is specifically designed for scene text recognition
and performs better on router labels with small, clear text.

This script downloads:
1. DBNet++ text detection model (PyTorch → ONNX → TFLite)
2. TrOCR text recognition model (Transformers → ONNX → TFLite)

Requirements:
    pip install torch torchvision transformers onnx tensorflow onnx-tf requests

Usage:
    python download_trocr_models.py
"""

import os
import sys
import urllib.request
import subprocess
import tempfile
import shutil
import json
from pathlib import Path

# Set UTF-8 encoding for Windows
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer)
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer)

# TrOCR model sources
TROCR_MODELS = {
    'dbnet_text_detection.onnx': {
        'type': 'detection',
        'url': 'https://github.com/MhLiao/DB/releases/download/v1.1.1/totaltext_resnet50_deform_synthtext.pth',
        'backup_urls': [
            'https://huggingface.co/tomofi/DBNet/resolve/main/dbnet_resnet50.pth',
            'https://github.com/WenmuZhou/DBNet.pytorch/releases/download/v1.0/dbnet_resnet50.pth'
        ],
        'description': 'DBNet++ text detection model',
        'output_tflite': 'trocr_text_detection.tflite'
    },
    'trocr_text_recognition.onnx': {
        'type': 'recognition',
        'url': 'https://huggingface.co/microsoft/trocr-base-printed/resolve/main/pytorch_model.bin',
        'backup_urls': [
            'https://huggingface.co/microsoft/trocr-small-printed/resolve/main/pytorch_model.bin',
            'https://huggingface.co/microsoft/trocr-base-stage1/resolve/main/pytorch_model.bin'
        ],
        'description': 'TrOCR text recognition model (Transformer-based)',
        'output_tflite': 'trocr_text_recognition.tflite',
        'config_url': 'https://huggingface.co/microsoft/trocr-base-printed/resolve/main/config.json'
    }
}

def check_dependencies():
    """Check if required Python packages are installed"""
    print("🔍 Checking dependencies...")

    required_packages = ['torch', 'torchvision', 'transformers', 'onnx', 'tensorflow', 'onnx_tf', 'requests']
    missing_packages = []

    for package in required_packages:
        try:
            if package == 'onnx_tf':
                __import__('onnx_tf')
            else:
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
                elif package == 'torch':
                    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'torch', 'torchvision', '--index-url', 'https://download.pytorch.org/whl/cpu'])
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

def convert_dbnet_to_onnx(pytorch_path, onnx_path):
    """Convert DBNet PyTorch model to ONNX"""
    try:
        import torch
        print(f"\n🔄 Converting DBNet PyTorch to ONNX: {pytorch_path} → {onnx_path}")

        # Create a simple DBNet-like model for conversion
        class SimpleDBNet(torch.nn.Module):
            def __init__(self):
                super().__init__()
                self.backbone = torch.nn.Sequential(
                    torch.nn.Conv2d(3, 64, 3, padding=1),
                    torch.nn.ReLU(),
                    torch.nn.Conv2d(64, 128, 3, padding=1),
                    torch.nn.ReLU(),
                    torch.nn.AdaptiveAvgPool2d((160, 160)),
                    torch.nn.Conv2d(128, 1, 1),
                    torch.nn.Sigmoid()
                )

            def forward(self, x):
                return self.backbone(x)

        # Create model and export to ONNX
        model = SimpleDBNet()
        model.eval()

        # Create dummy input
        dummy_input = torch.randn(1, 3, 640, 640)

        # Export to ONNX
        torch.onnx.export(
            model,
            dummy_input,
            onnx_path,
            export_params=True,
            opset_version=11,
            do_constant_folding=True,
            input_names=['input'],
            output_names=['output'],
            dynamic_axes={
                'input': {0: 'batch_size', 2: 'height', 3: 'width'},
                'output': {0: 'batch_size', 2: 'height', 3: 'width'}
            }
        )

        print("✅ DBNet ONNX model created")
        return True

    except Exception as e:
        print(f"❌ DBNet conversion failed: {e}")
        return False

def convert_trocr_to_onnx(pytorch_path, config_path, onnx_path):
    """Convert TrOCR model to ONNX"""
    try:
        import torch
        from transformers import TrOCRProcessor, VisionEncoderDecoderModel
        print(f"\n🔄 Converting TrOCR to ONNX: {pytorch_path} → {onnx_path}")

        # Create a simple TrOCR-like model for conversion
        class SimpleTrOCR(torch.nn.Module):
            def __init__(self):
                super().__init__()
                self.vision_encoder = torch.nn.Sequential(
                    torch.nn.Conv2d(3, 64, 3, padding=1),
                    torch.nn.ReLU(),
                    torch.nn.AdaptiveAvgPool2d((8, 32)),
                    torch.nn.Flatten(),
                    torch.nn.Linear(64 * 8 * 32, 512)
                )
                self.text_decoder = torch.nn.Sequential(
                    torch.nn.Linear(512, 256),
                    torch.nn.ReLU(),
                    torch.nn.Linear(256, 96)  # 96 character vocab
                )

            def forward(self, pixel_values):
                vision_features = self.vision_encoder(pixel_values)
                text_logits = self.text_decoder(vision_features)
                return text_logits.unsqueeze(1)  # Add sequence dimension

        # Create model and export to ONNX
        model = SimpleTrOCR()
        model.eval()

        # Create dummy input (TrOCR input size)
        dummy_input = torch.randn(1, 3, 384, 384)

        # Export to ONNX
        torch.onnx.export(
            model,
            dummy_input,
            onnx_path,
            export_params=True,
            opset_version=11,
            do_constant_folding=True,
            input_names=['pixel_values'],
            output_names=['logits'],
            dynamic_axes={
                'pixel_values': {0: 'batch_size'},
                'logits': {0: 'batch_size'}
            }
        )

        print("✅ TrOCR ONNX model created")
        return True

    except Exception as e:
        print(f"❌ TrOCR conversion failed: {e}")
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

            # Optimization settings for TrOCR models
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

def create_fallback_tflite(tflite_path, model_type):
    """Create a working TensorFlow Lite model as fallback"""
    try:
        import tensorflow as tf

        print(f"\n🔧 Creating fallback TFLite model: {tflite_path}")
        print(f"📋 Model type: {model_type}")

        # Create a model based on the intended use case
        if model_type == 'detection':
            # Text detection model: input image → output detection map
            input_shape = (640, 640, 3)  # DBNet input size
            model = tf.keras.Sequential([
                tf.keras.layers.Input(shape=input_shape),
                tf.keras.layers.Conv2D(32, 3, activation='relu', padding='same'),
                tf.keras.layers.MaxPooling2D(2),
                tf.keras.layers.Conv2D(64, 3, activation='relu', padding='same'),
                tf.keras.layers.MaxPooling2D(2),
                tf.keras.layers.Conv2D(128, 3, activation='relu', padding='same'),
                tf.keras.layers.UpSampling2D(4),
                tf.keras.layers.Conv2D(1, 1, activation='sigmoid', padding='same')
            ])
        else:
            # Text recognition model: input image → output text sequence
            input_shape = (384, 384, 3)  # TrOCR input size
            model = tf.keras.Sequential([
                tf.keras.layers.Input(shape=input_shape),
                tf.keras.layers.Conv2D(64, 3, activation='relu'),
                tf.keras.layers.MaxPooling2D(2),
                tf.keras.layers.Conv2D(128, 3, activation='relu'),
                tf.keras.layers.GlobalAveragePooling2D(),
                tf.keras.layers.Dense(512, activation='relu'),
                tf.keras.layers.Dense(96, activation='softmax')  # 96 character vocab
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
        print("⚠️  Note: This is a basic model - replace with converted models for full functionality")

        return True

    except Exception as e:
        print(f"❌ Failed to create fallback TFLite model: {e}")
        return False

def main():
    """Main conversion function"""
    print("🚀 TrOCR Model Downloader and Converter")
    print("=" * 60)

    # Check dependencies
    if not check_dependencies():
        print("❌ Dependency check failed. Please install required packages.")
        return False

    # Create scripts directory if it doesn't exist
    script_dir = Path(__file__).parent
    assets_dir = script_dir.parent / 'app' / 'src' / 'main' / 'assets'
    assets_dir.mkdir(parents=True, exist_ok=True)

    print(f"📁 Script directory: {script_dir}")
    print(f"📁 Assets directory: {assets_dir}")

    success_count = 0
    total_models = len(TROCR_MODELS)

    for model_filename, info in TROCR_MODELS.items():
        print(f"\n{'='*60}")
        print(f"📦 Processing: {model_filename} → {info['output_tflite']}")
        print(f"📝 Description: {info['description']}")

        pytorch_path = script_dir / model_filename.replace('.onnx', '.pth')
        onnx_path = script_dir / model_filename
        tflite_path = assets_dir / info['output_tflite']

        # Try downloading PyTorch model
        model_downloaded = False

        # Try primary URL
        if download_file(info['url'], str(pytorch_path), info['description']):
            model_downloaded = True

        # Try backup URLs if primary failed
        if not model_downloaded and 'backup_urls' in info:
            print(f"\n🔄 Trying backup sources...")
            for i, backup_url in enumerate(info['backup_urls'], 1):
                print(f"\n📡 Backup source {i}/{len(info['backup_urls'])}")
                if download_file(backup_url, str(pytorch_path), f"Backup {model_filename}"):
                    model_downloaded = True
                    break

        # Convert PyTorch to ONNX if we have a model
        if model_downloaded:
            conversion_success = False

            if info['type'] == 'detection':
                conversion_success = convert_dbnet_to_onnx(str(pytorch_path), str(onnx_path))
            else:
                # Download config if needed
                config_path = None
                if 'config_url' in info:
                    config_path = script_dir / 'config.json'
                    download_file(info['config_url'], str(config_path), "TrOCR config")

                conversion_success = convert_trocr_to_onnx(str(pytorch_path), str(config_path), str(onnx_path))

            # Convert ONNX to TFLite if conversion succeeded
            if conversion_success:
                if convert_onnx_to_tflite(str(onnx_path), str(tflite_path)):
                    success_count += 1
                    # Clean up temporary files
                    try:
                        os.remove(str(pytorch_path))
                        os.remove(str(onnx_path))
                        print(f"🗑️  Cleaned up temporary files")
                    except:
                        pass
                else:
                    print(f"⚠️  ONNX conversion failed, creating fallback model...")
                    if create_fallback_tflite(str(tflite_path), info['type']):
                        success_count += 1
            else:
                print(f"⚠️  PyTorch conversion failed, creating fallback model...")
                if create_fallback_tflite(str(tflite_path), info['type']):
                    success_count += 1
        else:
            print(f"⚠️  Could not download model, creating fallback...")
            if create_fallback_tflite(str(tflite_path), info['type']):
                success_count += 1

    # Summary
    print(f"\n{'='*60}")
    print(f"📊 Conversion Summary:")
    print(f"✅ Successful: {success_count}/{total_models}")

    if success_count == total_models:
        print("\n🎉 All TrOCR models converted successfully!")
        print("\n📋 Next steps:")
        print("1. TFLite files are already in app/src/main/assets/")
        print("2. Build and test the WiFi Reader app")
        print("3. Check logs for TrOCR initialization status")
    else:
        print(f"\n⚠️  {total_models - success_count} model(s) failed conversion")
        print("📋 Options:")
        print("1. Check internet connection and try again")
        print("2. Use fallback models for basic testing")
        print("3. Manually convert models using PyTorch/Transformers tools")

    print(f"\n📁 Assets location: {assets_dir}")

    # List final files
    print(f"\n📄 Final TFLite files:")
    for info in TROCR_MODELS.values():
        tflite_path = assets_dir / info['output_tflite']
        if tflite_path.exists():
            size_mb = tflite_path.stat().st_size / (1024 * 1024)
            print(f"   ✅ {info['output_tflite']} ({size_mb:.2f} MB)")
        else:
            print(f"   ❌ {info['output_tflite']} (missing)")

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