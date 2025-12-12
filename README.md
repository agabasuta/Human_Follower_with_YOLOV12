# Human Follower with YOLOv12

Aplikasi ini merupakan implementasi sistem *human follower* berbasis **YOLOv12** yang dijalankan pada perangkat **Android**. Sistem ini dirancang untuk mendeteksi dan melacak posisi manusia secara real-time melalui kamera ponsel, kemudian menampilkan arah perpindahan target pada antarmuka aplikasi. Proyek ini dibuat menggunakan **Android Studio** dan ditujukan sebagai demonstrasi bagaimana model deteksi modern dapat digunakan dalam aplikasi mobile untuk pemantauan, pelacakan gerakan, ataupun analisis spasial berbasis video.

---

## ✨ Fitur Utama

- **Deteksi Real-time:** Menggunakan YOLOv12 untuk mendeteksi manusia dengan akurasi tinggi.  
- **Tracking Sederhana:** Aplikasi menghitung pergerakan target dan menunjukkan arah geraknya.  
- **Antarmuka Mobile:** Dirancang khusus untuk Android menggunakan Android Studio.  
- **Optimasi Model:** Model telah dikonversi/dioptimalkan agar dapat berjalan pada perangkat mobile.  

---

## 📱 Arsitektur Aplikasi

### 1. Camera Input  
Mengambil frame kamera secara real-time menggunakan **CameraX** atau API kamera Android.

### 2. YOLOv12 Inference  
Model YOLOv12 dijalankan di perangkat (ONNX / TensorFlow Lite / NCNN — sesuai implementasi).

### 3. Bounding Box Processing  
Sistem mengekstrak lokasi **bounding box** manusia dari hasil inferensi.

### 4. Direction Estimation  
Aplikasi memprediksi **arah gerakan target** (kiri, kanan, maju, menjauh) berdasarkan perubahan posisi objek di frame.

### 5. UI Rendering  
Menampilkan video kamera, hasil deteksi, dan indikator arah langsung pada layar pengguna.

---

## 🛠️ Tools & Teknologi

- **Android Studio**  
- **YOLOv12 Model**  
- **ONNX Runtime and TensorFlow Lite**    
- **Kotlin**

