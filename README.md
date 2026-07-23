# HematoScope — Citomorfología digital para Hematología (Android)

Aplicación **Android** para el análisis morfológico de la sangre periférica y la
médula ósea. Reúne un **atlas de descriptores** citomorfológicos, un **contador
diferencial**, herramientas de **medición** con calibración real (µm), **comparación**
de campos, gestión de **casos** y **conexión con cámaras de microscopía** —incluida la
**Hayear HY500**— vía USB (UVC), la cámara del dispositivo o una transmisión de red.

> ⚕️ Herramienta de apoyo a la decisión para personal de laboratorio formado. No
> sustituye el criterio profesional ni constituye un dispositivo diagnóstico certificado.

---

## Funcionalidades

| Módulo | Qué hace |
| --- | --- |
| **Atlas de descriptores** | Catálogo buscable de tipos celulares (serie blanca, roja, plaquetas, precursores) y descriptores morfológicos (tamaño, forma, color, inclusiones, núcleo, citoplasma) con definición y significado clínico. |
| **Recuento diferencial** | Contador manual tipo teclado para 100/200 leucocitos, con porcentajes en vivo, deshacer/reiniciar y NRBC (eritroblastos) por 100 leucocitos. |
| **Medición** | Distancia/diámetro, longitud, área (polígono con Ø equivalente), círculo, ángulo y **relación núcleo/citoplasma (N:C)** sobre la imagen, en píxeles o **µm** si hay calibración. |
| **Calibración** | Escala µm/píxel por objetivo a partir de una **platina micrométrica** (o el estándar interno del hematíe ≈ 7,5 µm). |
| **Comparación** | Dos campos lado a lado + una célula de referencia del atlas para contrastar rasgos. |
| **Casos** | Estudios por paciente: campos capturados y gradación semicuantitativa (0/1+/2+/3+) de la morfología. |
| **Captura** | Vídeo en vivo desde la cámara del dispositivo (adaptador de ocular), **USB/UVC (HY500)** o **MJPEG por red**. |

---

## Conexión con la cámara HY500 (Hayear)

La HY500 se distribuye con software **solo para PC**, pero a nivel de protocolo es
una **cámara UVC (USB Video Class)** —el mismo estándar de una webcam. HematoScope la
abre directamente en Android mediante `USB-OTG`, sin drivers propietarios, usando la
librería [`com.herohan:UVCAndroid`](https://github.com/shiyinghan/UVCAndroid).

**Requisitos**
1. Un teléfono/tablet Android con **USB Host / OTG** (la mayoría desde Android 7).
2. Un **cable/adaptador OTG** (USB-C o micro-USB a USB-A hembra).
3. La HY500 conectada a ese adaptador.

**Pasos**
1. Abra HematoScope → pestaña **Captura** → seleccione **HY500 / USB**.
2. Conecte la cámara. Android mostrará el diálogo de permiso USB → **Aceptar**.
   (El `AndroidManifest` incluye un `intent-filter` que también puede abrir la app
   automáticamente al conectar una cámara UVC.)
3. Cuando el estado indique **«En vivo»**, pulse **Capturar** y **Guardar**.

**Si la cámara no aparece o no abre**
- No todas las unidades HY500 exponen el mismo chip. Averigüe su `VID:PID`:
  el estado en pantalla lo muestra al detectarla, o use `lsusb` en un PC.
  Añada ese `vendor-id` (en **decimal**) a
  [`app/src/main/res/xml/usb_device_filter.xml`](app/src/main/res/xml/usb_device_filter.xml).
- Si el firmware **no es UVC-compatible**, use la vía de **red (MJPEG)**: ejecute en el
  PC un re-emisor MJPEG (p. ej. `ffmpeg`/`mjpg-streamer` sobre la vista de la HY500) que
  publique `http://<IP-del-PC>:<puerto>/stream`, y péguelo en **Captura → Red**.

### Alternativa sin USB: adaptador de ocular
Si prefiere no depender de la HY500, monte el teléfono sobre el ocular con un adaptador
mecánico y use **Captura → Dispositivo** (CameraX). La calibración µm/píxel funciona
igual para cualquier fuente.

---

## Arquitectura

- **Kotlin + Jetpack Compose** (Material 3), navegación con `navigation-compose`.
- **MVVM**: `ViewModel`s por pantalla; DI manual ligero vía `HematoScopeApp.repository`.
- **Room** para casos, capturas, recuentos, observaciones, calibraciones y mediciones.
- **CameraX** (cámara del dispositivo), **UVCAndroid** (USB), lector **MJPEG** propio (red).
- **Coil** para miniaturas.

```
app/src/main/java/com/hematoscope/app/
├── data/
│   ├── model/          Modelos de dominio (tipos celulares, descriptores, mediciones)
│   ├── db/             Room: entidades, DAOs, base de datos, serialización
│   └── repository/     HematoRepository (persistencia + almacenamiento de imágenes)
├── domain/
│   ├── catalog/        CellCatalog: datos de referencia citomorfológicos
│   └── measurement/    Geometría, calibración y motor de medición
├── camera/             Fuentes de imagen: dispositivo, USB/UVC (HY500), red MJPEG
└── ui/                 Tema, navegación, pantallas y ViewModels
```

El contenido clínico (rangos de tamaño, N:C, hallazgos y su significado) procede de
referencias de hematología estándar (*Rodak's Hematology*; Bain, *Blood Cells: A
Practical Guide*).

---

## Compilación

Requiere **Android Studio** (Ladybug o superior) con **Android SDK 34** y **JDK 17**.

```bash
# Compilar el APK de depuración
./gradlew assembleDebug

# Instalar en un dispositivo conectado
./gradlew installDebug
```

El APK resultante queda en `app/build/outputs/apk/debug/`.

> El entorno donde se generó este repositorio no incluye el Android SDK, por lo que el
> proyecto se entrega como **código fuente listo para compilar**. Ábralo en Android
> Studio (o ejecute `./gradlew` con el SDK instalado) para generar el APK.

### Parámetros del proyecto
- `minSdk = 24` (Android 7.0) · `targetSdk = 34` · `compileSdk = 34`
- `applicationId = com.hematoscope.app`

---

## Estado y hoja de ruta

Implementado y funcional: atlas, recuento diferencial, medición + calibración,
comparación, casos con persistencia y las tres fuentes de captura.

Ideas siguientes: segmentación asistida de núcleo/citoplasma para la N:C automática,
exportación de informe PDF del caso, y clasificación asistida por modelo en el dispositivo.
