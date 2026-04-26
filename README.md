# TV WebApp - Google TV / Android TV uchun WebView APK

## Ishlatish

1. `MainActivity.java` faylida `TARGET_URL` ni o'zingizning sayt manzili bilan almashtiring:
   ```java
   private static final String TARGET_URL = "https://your-website.com";
   ```

2. Android Studio da loyihani oching

3. APK build qiling: **Build > Build Bundle(s) / APK(s) > Build APK(s)**

## Xususiyatlar
- ✅ TV yonganda avtomatik ishga tushadi (BOOT_COMPLETED)
- ✅ To'liq ekran rejimi
- ✅ TV pult tugmalari ishlaydi (D-pad, Back)
- ✅ Internet xatosi sahifasi
- ✅ Google TV / Android TV launcher da ko'rinadi
- ✅ JavaScript, localStorage qo'llab-quvvatlanadi

## TV ga o'rnatish
- ADB orqali: `adb install app-debug.apk`
- USB orqali: TV Settings > Device Preferences > Security > Unknown Sources yoqing
