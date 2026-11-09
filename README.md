# EcoMarket – Tienda virtual de productos sostenibles (Android nativo)

Aplicación Android nativa (Kotlin + XML) para la comercialización de productos sostenibles y artesanales colombianos. Este repositorio corresponde a la **Actividad 2: interfaces gráficas completas** de la asignatura *Desarrollo de Aplicaciones Móviles Nativas* (Fundación Universitaria Compensar). Parte del documento técnico y los mockups entregados en la Actividad 1.

**Autor:** Edward Iván Becerra Niño · **Docente:** Flavio Alexander Navarro Carmona

## Requisitos

- Android Studio Koala o superior (o JDK 17+ y Gradle Wrapper incluido)
- Android SDK 34 · `minSdk 26` (Android 8.0) · `targetSdk 34`

## Cómo ejecutarla

```bash
./gradlew assembleDebug        # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # instala en un dispositivo o emulador conectado
```

El APK de validación también se entrega junto al proyecto (`EcoMarket-debug.apk`).

### Acceso de demostración

La app no tiene backend todavía (ver *Alcance*). En la pantalla de inicio de sesión se elige el rol (Comprador, Vendedor o Administrador); el correo se completa solo y **cualquier contraseña no vacía** es válida. El botón *Ingresar con huella* usa `BiometricPrompt`; si el dispositivo no tiene biometría registrada, entra con el rol elegido.

## Pantallas por rol

| Rol | Pantallas |
|---|---|
| Transversal | Splash, inicio de sesión con biometría, recuperación de contraseña, menú Drawer + Bottom Navigation, perfil |
| Comprador | Inicio, catálogo (búsqueda y filtro por categoría), detalle de producto, carrito, pasarela de pagos (tarjeta, PSE, efectivo), perfil |
| Vendedor | Panel de productos con CRUD completo (listar, crear, editar, eliminar), historial de pedidos recibidos con cambio de estado, perfil |
| Administrador | Dashboard con indicadores, gestión de usuarios (listar, buscar, filtrar, crear, editar, eliminar), gestión de productos (aprobar/rechazar), reporte de ventas con gráfico |

Capturas en [`docs/capturas`](docs/capturas).

## Estructura del proyecto

```
app/src/main
├── java/com/ucompensar/ecomarket
│   ├── data/            Modelos y repositorio en memoria (simula el backend REST)
│   ├── ui/              Activities: Splash, Login, RecoverPassword, Main
│   │   ├── buyer/       Fragments del comprador
│   │   ├── seller/      Fragments del vendedor
│   │   ├── admin/       Fragments del administrador + BarChartView (vista propia)
│   │   └── common/      Perfil, SimpleAdapter genérico, enlace de pedidos
│   └── util/            Formato COP, validaciones, insignias de estado
└── res
    ├── layout/          activity_*, fragment_*, item_*, view_*
    ├── drawable/        Fondos (shapes), insignias, íconos vectoriales, launcher
    ├── menu/            Menús por rol (bottom navigation) y drawer
    ├── color/           Selector de color de chips
    └── values/          colors, dimens, strings, styles, themes, ids
```

## Decisiones de diseño y recursos centralizados

- **Paleta propia** (no la predeterminada de Android Studio): verde `#2E7D32`, verde claro `#A5D6A7`, naranja `#FF7043`, definida solo en `values/colors.xml`.
- **Tema Material 3** personalizado en `values/themes.xml`; estilos reutilizables de botones, tarjetas, campos, chips y texto en `values/styles.xml`.
- Ningún layout define colores, dimensiones ni textos fijos: todo se referencia con `@color`, `@dimen`, `@string` y `@style`.
- **Drawables**: fondos con `shape` (cabecera degradada, banner, insignias de estado, avatar), `layer-list` para el ícono y vectores para toda la iconografía.
- **ViewBinding** en todas las pantallas y un `SimpleAdapter<T, VB>` genérico para las listas.
- **Navegación por rol**: `MainActivity` infla el menú del rol autenticado y reemplaza fragments; el Drawer y el Bottom Navigation quedan sincronizados.
- Accesibilidad: `contentDescription` en íconos, contraste AA y textos escalables (`sp`).

## Alcance

Esta entrega cubre el *frontend*. Los datos viven en `data/Repository.kt` (memoria) y se reinician al cerrar la app. Quedan para las siguientes actividades: API REST (Retrofit), persistencia, cámara, geolocalización, OAuth y pasarela de pagos real, tal como se planeó en el documento técnico.

## Control de versiones

El historial sigue el avance incremental: andamiaje y recursos → pantallas transversales y rol Comprador → rol Vendedor → rol Administrador → correcciones visuales y de alcance.
