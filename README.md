# ⚔️ Titanes de Guerra

Juego de estrategia para Android inspirado en *Dawn of Titans*: conquista territorios,
comanda ejércitos por carriles y despliega titanes colosales para destruir el castillo enemigo.

## 🎮 Cómo se juega

- **Campaña**: 10 territorios para conquistar, cada uno más difícil que el anterior.
- **Batalla**: toca una carta de tropa y luego un carril para desplegarla.
  El maná se regenera con el tiempo. Destruye el castillo enemigo antes de que caiga el tuyo.
- **Tropas**: Espadachín ⚔️, Arquero 🏹, Caballería 🐎 y el poderoso Titán 🗿.
- **Mejoras**: gana oro en las batallas y sube de nivel a tus tropas en la pantalla *Ejército*.
- El progreso se guarda automáticamente en el dispositivo.

## 📲 Descargar el APK

Cada vez que se hace *push*, GitHub Actions compila el APK automáticamente:

1. Ve a la pestaña **Releases** del repositorio y descarga `app-debug.apk` de la release
   **“Titanes de Guerra — APK más reciente”** (o a **Actions → Compilar APK → Artifacts**).
2. Ábrelo en tu Android y acepta instalar desde orígenes desconocidos.
3. ¡A conquistar!

## 🛠️ Compilar en local

Requiere Android SDK (API 35) y JDK 17:

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```

## 🧱 Tecnología

- App Android nativa (Java) con un `WebView` a pantalla completa.
- Juego escrito en HTML5 + Canvas + JavaScript puro (sin dependencias), en `app/src/main/assets/`.
- Guardado con `localStorage`.
