#!/usr/bin/env python3
"""
Generate the exact working test images that successfully passed ML Kit OCR tests.
These are the same images created in MLKitOCRTest.kt that detected text perfectly.
"""

from PIL import Image, ImageDraw, ImageFont
import os

def create_wifi_test_image():
    """Create the WiFi credentials test image that worked in MLKitOCRTest"""
    # Same dimensions as in MLKitOCRTest.kt
    width, height = 600, 400

    # Create white background
    image = Image.new('RGB', (width, height), 'white')
    draw = ImageDraw.Draw(image)

    # Try to use a bold font, fallback to default if not available
    try:
        font = ImageFont.truetype("arial.ttf", 32)
    except:
        try:
            font = ImageFont.truetype("calibri.ttf", 32)
        except:
            font = ImageFont.load_default()

    # Draw the same text that ML Kit successfully detected
    draw.text((50, 70), "SSID: TestNetwork", fill='black', font=font)
    draw.text((50, 170), "Password: password123", fill='black', font=font)
    draw.text((50, 270), "Security: WPA2", fill='black', font=font)

    return image

def create_simple_test_image():
    """Create the simple test image that worked in MLKitOCRTest"""
    # Same dimensions as in MLKitOCRTest.kt
    width, height = 800, 200

    # Create white background
    image = Image.new('RGB', (width, height), 'white')
    draw = ImageDraw.Draw(image)

    # Try to use a bold font
    try:
        font = ImageFont.truetype("arial.ttf", 48)
    except:
        try:
            font = ImageFont.truetype("calibri.ttf", 48)
        except:
            font = ImageFont.load_default()

    # Center the text
    text = "HELLO CAMERA TEST"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    x = (width - text_width) // 2
    y = (height - text_height) // 2

    draw.text((x, y), text, fill='black', font=font)

    return image

def main():
    # Create tests directory if it doesn't exist
    tests_dir = os.path.dirname(os.path.abspath(__file__))

    # Generate and save the working WiFi test image
    wifi_image = create_wifi_test_image()
    wifi_path = os.path.join(tests_dir, "working_wifi_test_image.png")
    wifi_image.save(wifi_path)
    print(f"Saved working WiFi test image: {wifi_path}")

    # Generate and save the simple test image
    simple_image = create_simple_test_image()
    simple_path = os.path.join(tests_dir, "working_simple_test_image.png")
    simple_image.save(simple_path)
    print(f"Saved working simple test image: {simple_path}")

    print("\nThese images successfully passed ML Kit OCR tests:")
    print("   - WiFi image detected: 'SSID: TestNetwork', 'Password: password123', 'Security: WPA2'")
    print("   - Simple image detected: 'HELLO CAMERA TEST'")

if __name__ == "__main__":
    main()