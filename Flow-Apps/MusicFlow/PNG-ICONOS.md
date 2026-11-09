# Cómo poner tus PNG en MusicFlow

Icono del launcher:
1. Borra `app/src/main/res/drawable/ic_launcher_foreground.xml`.
2. Sube tu PNG (cuadrado, 432x432 px recomendado, con margen) como
   `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`.
3. El color de fondo se cambia en `app/src/main/res/values/colors.xml` (`ic_launcher_bg`).

Logotipo dentro de la app: `app/src/main/res/drawable-nodpi/logo.png` y úsalo con `painterResource(R.drawable.logo)`.
