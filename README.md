# Flow-Apps

Seis apps Android nativas (Kotlin + Jetpack Compose + Material 3), cada una independiente y con su propio Gradle.

| App | Estado |
|---|---|
| CountDown | Lista para compilar (widget de pantalla de inicio, notificaciones, fotos, favoritos) |
| MusicFlow | Lista para compilar (Media3, segundo plano, notificación multimedia, letras, Liquid Glass) |
| EventFlow, WordFlow, GalleryX, TaskFlow | Pendientes (no incluidas todavía) |

El código aún no se ha compilado en un entorno real. El primer build en GitHub Actions lo comprobará.

## Descargar el APK
1. En GitHub abre la pestaña **Actions**.
2. Elige **Build CountDown** o **Build MusicFlow** y toca **Run workflow** (o espera al push).
3. Cuando termine con ✅, abre la ejecución y baja a **Artifacts**.
4. Descarga `CountDown-apk` o `MusicFlow-apk` (zip con el APK dentro) y `...-source` (zip del código de esa app).

## Instalar
1. Descomprime el zip descargado y abre el `.apk`.
2. Si Android lo pide, permite "Instalar apps desconocidas" para tu navegador o gestor de archivos.
3. Es un APK de depuración firmado con la clave debug, solo para uso personal.

## Iconos PNG propios
Cada app tiene un `PNG-ICONOS.md` con la ruta exacta donde poner tus imágenes.

## Notas
- Cada workflow usa `gradle assembleDebug` con Gradle 8.7 (no hace falta `gradlew`).
- MusicFlow no descarga letras: se pegan o se importan (.lrc sincronizado o .txt) y se guardan en el teléfono.
- El visualizador de MusicFlow es una animación decorativa, no reacciona al audio real.
