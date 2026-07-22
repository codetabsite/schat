# SChat

Minimalist, Firebase tabanlı anlık mesajlaşma uygulaması.  



---

## Özellikler

- İlk girişte kullanıcı adı seçimi
- Kullanıcı adı ile arkadaş ekleme (+)
- Metin mesajı
- Fotoğraf & GIF gönderme
- Firebase Realtime Database (anlık)
- Firebase Storage (medya)
- Firebase Anonymous Auth
- Minimalist siyah tema, sarı-yeşil accent
- GitHub Actions → Debug + imzalı Release APK

---

## Kurulum

### 1. Firebase Projesi Oluştur

1. [Firebase Console](https://console.firebase.google.com) → Yeni proje
2. Android uygulaması ekle → package: `com.tdev.schat`
3. `google-services.json` dosyasını indir → `app/` klasörüne koy
4. **Authentication** → Anonymous'u etkinleştir
5. **Realtime Database** → Oluştur → `firebase-database-rules.json` içindeki kuralları yapıştır
6. **Storage** → Oluştur → `firebase-storage-rules.txt` içindeki kuralları yapıştır

### 2. Yerel Build

```bash
# Projeyi klonla
git clone https://github.com/KULLANICI/SChat.git
cd SChat

# google-services.json'ı app/ klasörüne koy

# Debug APK
./gradlew assembleDebug

# APK çıkış yeri:
# app/build/outputs/apk/debug/app-debug.apk
```

### 3. Release Keystore Oluştur (bir kez)

```bash
chmod +x generate-keystore.sh
./generate-keystore.sh
```

Script çıktısından 4 değeri kopyala, GitHub Secrets'a ekle.

---

## GitHub Actions (Otomatik Build)

### Gerekli Secrets

| Secret | Açıklama |
|--------|----------|
| `GOOGLE_SERVICES_JSON` | `google-services.json` dosyasının **tüm içeriği** |
| `KEYSTORE_BASE64` | `generate-keystore.sh` çıktısındaki base64 |
| `KEYSTORE_PASSWORD` | Keystore şifresi |
| `KEY_ALIAS` | `schat-key` |
| `KEY_PASSWORD` | Key şifresi |

### Secrets Nasıl Eklenir?

GitHub repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

### Otomatik Tetikleme

- `main`/`master`'a her push → debug + release APK build
- `v*` tag push (ör. `git tag v1.0.0 && git push --tags`) → GitHub Release oluşturur, APK'ları ekler

---

## Proje Yapısı

```
app/src/main/java/com/tdev/schat/
├── MainActivity.kt              # Entry point, navigation
├── data/
│   ├── model/Models.kt          # User, Message, Chat, MediaType
│   └── repository/ChatRepository.kt  # Firebase işlemleri
├── viewmodel/
│   └── MainViewModel.kt         # UI state yönetimi
└── ui/
    ├── theme/Theme.kt           # Renkler, tipografi
    └── screens/
        ├── SetupScreen.kt       # İlk açılış - kullanıcı adı
        ├── HomeScreen.kt        # Sohbet listesi + arkadaş ekleme
        └── ChatScreen.kt        # Mesajlaşma ekranı
```

---

## Teknolojiler

- Kotlin + Jetpack Compose
- Firebase Realtime Database
- Firebase Storage
- Firebase Authentication (Anonymous)
- Coil (GIF desteği dahil)
- Navigation Compose
- Coroutines / Flow

---

## Lisans

MIT
