# Lugat · İngilizce Kelime

Kotlin + Jetpack Compose ile yazılmış, çevrimdışı çalışan İngilizce kelime öğrenme uygulaması.

## İçerik

- **14 deste, 759 kelime.** Her kelimede Türkçe karşılık, kelime türü, örnek cümle ve cümlenin çevirisi var.
  - **Okul:** İlkokul (A1), Ortaokul/LGS (A2), Lise (B1)
  - **Sınavlar:** YKS-YDT (B2), YDS/YÖKDİL (C1)
  - **Üniversite ve bölümler:** Hazırlık, Tıp, Mühendislik, Bilgisayar, Hukuk, İşletme-Ekonomi, Psikoloji-Eğitim
  - **Ekstra:** Phrasal Verbs, Seyahat İngilizcesi
- **51 "günün kelimesi" kaydı.** Kelime kökenleri ve Türkçe konuşanların sık karıştırdığı yalancı eşler.

## Özellikler

- **6 çalışma modu:**
  - Kartlar: 3D dönen kart; sağa kaydırınca "biliyorum", sola kaydırınca "tekrar".
  - Test (4 seçenek)
  - Yazma
  - Dinle ve Yaz
  - Eşleştirme oyunu (süre tutulur)
  - Boşluk doldurma
- **Aralıklı tekrar:** Leitner sistemi, 5 kutu. Tekrar aralıkları 1, 3, 7, 14 ve 30 gün.
- **Takip:** Günlük hedef halkası, seri (🔥), 7 günlük grafik, zor kelimeler ve favoriler.
- **Günün kelimesi bildirimi:** WorkManager ile kullanıcının seçtiği saatte gönderilir.
- **Telaffuz:** Cihazın metin okuma motoruyla (ABD/İngiliz aksanı, hız ayarlı).
- **Görünüm:** Açık ve koyu tema.

## Mimari

```
domain/     Saf Kotlin: modeller, Srs (Leitner), AnswerChecker, Streak, Quiz, Blank, Sessions, WordOfDay
data/       ContentSource (assets/content.json), Room (ilerleme + günlük etkinlik), DataStore (ayarlar), LearningRepository
tts/        Speaker (TextToSpeech)
notify/     Günün kelimesi bildirimi (WorkManager)
ui/         Compose ekranları: onboarding, home, decks, study (6 mod), stats, settings, wotd
```

## İçeriği düzenleme

Kelimeler `scripts/content/*.py` dosyalarındadır. Her satırın biçimi şöyle:

`ingilizce | tür | türkçe | örnek cümle | cümlenin çevirisi`

Düzenledikten sonra aşağıdaki komutu çalıştırın. Komut içeriği doğrular ve `app/src/main/assets/content.json` dosyasını üretir. GitHub Actions bu adımı her derlemede kendisi de yapar.

```bash
python scripts/content/build_content.py
```

## APK alma (GitHub)

1. Bu klasörün içeriğini yeni bir GitHub reposuna yükleyin.
2. **Actions** sekmesinde "Lugat APK" iş akışı otomatik çalışır.
3. İş bitince **Artifacts → lugat-apk** dosyasını indirin. İçindeki `app-debug.apk` dosyasını telefona kurun.

Play Store için imzalı `.aab` dosyası üretmek isterseniz repo ayarlarında şu dört secret'ı tanımlayın: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
