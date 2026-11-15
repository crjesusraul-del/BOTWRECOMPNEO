# BOTW Recomp Neo (Motorola Edge 40 Neo Edition)

Lanzador de recompilación estática y optimizador de hardware calibrado para **Motorola Edge 40 Neo** (**MediaTek Dimensity 7030**, GPU **ARM Mali-G610 MC3**, **8 GB / 12 GB RAM** y pantalla **pOLED de 144Hz**), inspirado en `mamcjr/BOTWRecompAndroid`.

> **Aviso legal:** Este repositorio no incluye archivos del juego, ROMs (`.nsp`/`.xci`), claves (`prod.keys`) ni librerías de terceros propietarias. El usuario debe vincular su copia extraída legalmente mediante el selector SAF de Android.

## Cómo obtener el APK desde GitHub automáticamente

1. Sube este proyecto a tu repositorio en **GitHub** (en la rama `main` o `master`).
2. Entra a la pestaña **Actions** en GitHub y selecciona el workflow **Build Android APK** (se ejecuta automáticamente con cada `push` o manualmente con **Run workflow**).
3. Al finalizar la compilación, baja hasta la sección **Artifacts** y descarga **`BOTW-Recomp-Neo-APK`**, que contiene el archivo `BOTW-Recomp-Neo-debug.apk` firmado y listo para instalar en tu Motorola Edge 40 Neo.
4. *(Opcional)* Si creas un tag como `v1.0.0`, el workflow creará automáticamente un **GitHub Release** con el APK adjunto.
5. *(Opcional para Release firmado)* Si deseas también el APK `release`, agrega en *Settings → Secrets and variables → Actions* de tu repositorio: `UPLOAD_KEYSTORE_BASE64`, `STORE_PASSWORD` y `KEY_PASSWORD`.

## Compilación local

```bash
# En Linux / macOS:
base64 -d debug.keystore.base64 > debug.keystore
cp -n .env.example .env || true
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

El APK generado queda en `app/build/outputs/apk/debug/app-debug.apk`.
