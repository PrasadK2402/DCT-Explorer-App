# DCT Explorer App

An Android application that performs **image compression using Discrete Cosine Transform (DCT)**.

## 📌 Overview

This project demonstrates how images can be compressed by converting them into frequency components using DCT, reducing file size while maintaining visual quality.

## 🚀 Features

* Select image from device
* Apply DCT-based compression
* Compare original vs compressed image
* Save compressed output

## 🛠️ Tech Stack

* Java
* Android SDK
* XML

## 🧠 How It Works

* Image is divided into small blocks (8×8)
* DCT is applied to each block
* Less important (high-frequency) data is reduced
* Image is reconstructed using inverse DCT

## ▶️ How to Run

```bash
git clone https://github.com/PrasadK2402/DCT-Explorer-App.git
```

* Open in Android Studio
* Sync Gradle
* Run on emulator or device

## 📂 Project Structure

* `app/` → main application code
* `gradle/` → build system files
* `build.gradle` → project configuration

## 📄 License

Open-source project for learning purposes.
