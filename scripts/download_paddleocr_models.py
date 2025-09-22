#!/usr/bin/env python3
"""
Download and setup PaddleOCR models for cross-platform OCR.
"""

import os
import sys
import requests
import tarfile
import shutil
from pathlib import Path

def download_file(url, filepath):
    """Download file with progress bar."""
    print(f"Downloading {filepath.name}...")

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

def extract_tar(tar_path, extract_dir):
    """Extract tar file."""
    print(f"Extracting {tar_path.name}...")
    with tarfile.open(tar_path, 'r') as tar:
        tar.extractall(extract_dir)
    print(f"  Extracted to: {extract_dir}")

def find_model_files(extract_dir):
    """Find PaddlePaddle model files."""
    model_files = []
    for root, dirs, files in os.walk(extract_dir):
        for file in files:
            if file in ['inference.pdmodel', 'inference.pdiparams']:
                model_files.append(Path(root) / file)
    return model_files

def convert_to_onnx(model_dir, output_path):
    """Convert PaddlePaddle model to ONNX (requires paddle2onnx)."""
    try:
        import paddle2onnx

        model_file = model_dir / 'inference.pdmodel'
        params_file = model_dir / 'inference.pdiparams'

        if not (model_file.exists() and params_file.exists()):
            print(f"  Model files not found in {model_dir}")
            return False

        print(f"  Converting {model_dir.name} to ONNX...")

        # Convert to ONNX
        onnx_model = paddle2onnx.command.c_paddle_to_onnx(
            model_file=str(model_file),
            params_file=str(params_file),
            opset_version=11,
            enable_onnx_checker=True
        )

        with open(output_path, 'wb') as f:
            f.write(onnx_model)

        print(f"  Converted to: {output_path}")
        return True

    except ImportError:
        print("  paddle2onnx not installed. Install with: pip install paddle2onnx")
        return False
    except Exception as e:
        print(f"  Conversion failed: {e}")
        return False

def convert_to_tflite(onnx_path, tflite_path):
    """Convert ONNX model to TensorFlow Lite (requires onnx-tensorflow)."""
    try:
        import onnx
        from onnx_tf.backend import prepare
        import tensorflow as tf

        print(f"  Converting {onnx_path.name} to TensorFlow Lite...")

        # Load ONNX model
        onnx_model = onnx.load(str(onnx_path))

        # Convert to TensorFlow
        tf_rep = prepare(onnx_model)

        # Convert to TensorFlow Lite
        converter = tf.lite.TFLiteConverter.from_saved_model(tf_rep.export_graph())
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        tflite_model = converter.convert()

        # Save TensorFlow Lite model
        with open(tflite_path, 'wb') as f:
            f.write(tflite_model)

        print(f"  Converted to: {tflite_path}")
        return True

    except ImportError as e:
        print(f"  Missing dependencies: {e}")
        print("  Install with: pip install onnx onnx-tensorflow tensorflow")
        return False
    except Exception as e:
        print(f"  TFLite conversion failed: {e}")
        return False

def create_mock_tflite(filepath, model_type):
    """Create a mock TensorFlow Lite model for testing."""
    # TensorFlow Lite file header
    tflite_header = b'TFL3'  # Magic number
    tflite_header += b'\x00' * 16  # Version and other headers

    # Add some dummy model data
    model_data = f"Mock {model_type} model".encode('utf-8')
    tflite_header += model_data
    tflite_header += b'\x00' * (1024 - len(tflite_header))  # Pad to 1KB

    with open(filepath, 'wb') as f:
        f.write(tflite_header)

    print(f"  Created mock model: {filepath}")

def main():
    print("PaddleOCR Model Setup")
    print("=" * 30)

    # Setup directories
    script_dir = Path(__file__).parent
    assets_dir = script_dir.parent / 'app' / 'src' / 'main' / 'assets'
    temp_dir = script_dir / 'temp_models'

    # Create directories
    assets_dir.mkdir(parents=True, exist_ok=True)
    temp_dir.mkdir(parents=True, exist_ok=True)

    print(f"Assets directory: {assets_dir}")
    print(f"Temp directory: {temp_dir}")

    # Model URLs and info
    models = [
        {
            'name': 'Detection',
            'url': 'https://paddleocr.bj.bcebos.com/PP-OCRv3/english/en_PP-OCRv3_det_infer.tar',
            'tar_file': temp_dir / 'det_model.tar',
            'extract_dir': temp_dir / 'det_extracted',
            'onnx_file': temp_dir / 'detection.onnx',
            'tflite_file': assets_dir / 'paddleocr_detection.tflite'
        },
        {
            'name': 'Recognition',
            'url': 'https://paddleocr.bj.bcebos.com/PP-OCRv3/english/en_PP-OCRv3_rec_infer.tar',
            'tar_file': temp_dir / 'rec_model.tar',
            'extract_dir': temp_dir / 'rec_extracted',
            'onnx_file': temp_dir / 'recognition.onnx',
            'tflite_file': assets_dir / 'paddleocr_recognition.tflite'
        }
    ]

    success_count = 0

    for model in models:
        print(f"\n--- Processing {model['name']} Model ---")

        try:
            # Download model
            if not model['tar_file'].exists():
                download_file(model['url'], model['tar_file'])
            else:
                print(f"  Using cached: {model['tar_file']}")

            # Extract model
            if model['tar_file'].exists():
                extract_tar(model['tar_file'], model['extract_dir'])

                # Find model directory
                model_dirs = [d for d in model['extract_dir'].iterdir() if d.is_dir()]
                if model_dirs:
                    model_dir = model_dirs[0]  # Take first directory

                    # Try to convert to ONNX then TFLite
                    if convert_to_onnx(model_dir, model['onnx_file']):
                        if convert_to_tflite(model['onnx_file'], model['tflite_file']):
                            success_count += 1
                            print(f"  ✓ {model['name']} model ready")
                            continue

                    # If conversion failed, create reference file
                    print(f"  Conversion failed, creating reference...")
                    with open(model['tflite_file'], 'w') as f:
                        f.write(f"# {model['name']} Model Reference\n")
                        f.write(f"# Source: {model['url']}\n")
                        f.write(f"# Extracted to: {model_dir}\n")
                        f.write("# Needs conversion: paddle2onnx -> onnx-tf -> tflite\n")
                else:
                    print(f"  No model directory found in extraction")

        except Exception as e:
            print(f"  Error processing {model['name']}: {e}")

    # If no real models were converted, create mock models for testing
    if success_count == 0:
        print(f"\n--- Creating Mock Models for Testing ---")
        for model in models:
            if not model['tflite_file'].exists() or model['tflite_file'].stat().st_size < 100:
                create_mock_tflite(model['tflite_file'], model['name'])

        print(f"\nMock models created. The PaddleOCREngine will run in enhanced mock mode.")
        print(f"For real models, install: pip install paddle2onnx onnx-tensorflow tensorflow")
    else:
        print(f"\n✓ Successfully processed {success_count} models")

    # Cleanup
    if temp_dir.exists():
        shutil.rmtree(temp_dir)
        print(f"Cleaned up: {temp_dir}")

    print(f"\nSetup complete! Models are in: {assets_dir}")

if __name__ == '__main__':
    main()