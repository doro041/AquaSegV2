# Lobster Instance Segmentation App (Kotlin and YOLOv8-Seg + MobileSAM)


[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A real-time mobile application for instance segmentation of egg-bearing lobsters using a lightweight YOLO11-seg,. Built natively in Kotlin for Android, with efficient offline on-device inference powered by TensorFlow Lite.

## 📱 Overview

This Android application provides real-time instance segmentation of lobsters directly from your device's camera feed. Tailored to support offshore workers and researchers, it utilizes a **custom-trained YOLOv8-Seg model** specifically designed for detecting and segmenting **egg-bearing lobsters**. Optimized for **offline use**, the app ensures reliable functionality even in environments without network connectivity.

*(Note: While initial research explored point-prompt-based segmentation pipeline YOLO12+MobileSAM, the current application utilizes a different approach based on a custom dataset and YOLO11seg-based segmentation pipeline, due to it being a single model,and running faster on a mobile device. Future iteration of the project, will be able to integrate the tested pipeline in a more efficient manner.)*
## ✨ Key Features

* **🦞 Egg-Bearing Lobster Detection:** Identifies and segments egg-bearing lobsters in real-time.
* **📸 Real-time Inference:** Provides immediate segmentation results directly from the camera feed.
* **💾 Save with Overlayed Masks:** Allows users to save captured images with the generated segmentation masks overlaid.
* **⏱️ Performance Monitoring:** Displays on-screen metrics for:
    * **Inference Time:** The duration taken by the model to perform segmentation.
    * **Post-processing Time:** The time required to process the model's output (e.g., generating masks).
    * **Pre-processing Time:** The time taken to prepare the input image for the model.
* **🛰️ Offline Operation:** Functions entirely offline, making it suitable for remote locations without internet access.
* **ℹ️ Introduction & About Pages:** Includes dedicated screens for app introduction and information about the technology and its usage.

## 🛠️ Technologies Used

- **Kotlin:** The primary programming language for the Android application.
- **TensorFlow Lite:** Google's lightweight machine learning framework for on-device inference.
- **YOLO11-Seg:** A state-of-the-art object detection and instance segmentation model, optimized for mobile deployment.



## ⚙️ Setup and Installation (Example - Adjust as needed)

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/](https://github.com/)doro041/AquaSegV2.git
    ```

2.  **Open in Android Studio:** Open the project in Android Studio.

3.  **Build and Run:** Connect an Android device or emulator and run the application.
   




## 🧑‍💻 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for any bugs, feature requests, or improvements.

## 📚 Research Context

This app is part of a research dissertation focused on the detection and segmentation of egg-bearing lobsters using machine learning techniques. The project explores various segmentation pipelines, with the current app using a YOLO11-Seg model for real-time mobile deployment. Future iterations may integrate a previously tested point-prompt-based pipeline (YOLO12+MobileSAM) for more advanced segmentation capabilities, or create a few-shot instance segmentation pipeline.

