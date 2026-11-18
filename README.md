# EcoMarket – Tienda virtual de productos sostenibles (Android nativo)

Aplicación Android nativa (Kotlin + XML) para comprar y vender productos sostenibles y artesanales colombianos. Este repositorio corresponde al **proyecto final (Actividad de Transferencia)** de la asignatura *Desarrollo de Aplicaciones Móviles Nativas* de la Fundación Universitaria Compensar. Continúa el documento técnico (Actividad 1) y las interfaces de la Actividad 2, y les da funcionamiento completo.

**Autor:** Edward Iván Becerra Niño · **Docente:** Flavio Alexander Navarro Carmona

## Funcionalidades

| Componente | Implementación |
|---|---|
| Autenticación | Registro (comprador o vendedor), inicio y cierre de sesión, recuperación y cambio de contraseña con **Firebase Authentication** |
| Biometría | Ingreso con huella: `BiometricPrompt` + llave AES del **Android Keystore** que solo se desbloquea con la huella y cifra las credenciales |
| CRUD de usuarios | El administrador lista, busca, crea, edita (rol y estado) y elimina usuarios en **Cloud Firestore**, con doble confirmación al cambiar roles y **bitácora de auditoría** |
| CRUD de productos | El vendedor crea, edita y elimina productos con foto tomada con la **cámara** o elegida de la **galería**, guardada en **Firebase Storage**; el administrador los aprueba o rechaza |
| Carrito y órdenes | Carrito persistido en Firestore, órdenes con historial y estados: pendiente de pago → pagado → enviado → entregado |
| Geolocalización | `FusedLocationProviderClient` (Google Play Services) obtiene la ubicación y `Geocoder` la convierte en la dirección de entrega |
| Pasarela de pagos | **ePayco** Checkout dentro de un WebView; la transacción se confirma consultando la referencia (`ref_payco`) en los servidores de ePayco |
| Reportes | Dashboard y reporte de ventas calculados en tiempo real a partir de las órdenes pagadas |

## Arquitectura

```
app/src/main/java/com/ucompensar/ecomarket
├── EcoMarketApp.kt          Inicializa Firebase (nube o emulador local)
├── data/
│   ├── model/               Modelos de Firestore (UserProfile, Product, CartItem, Order…)
│   ├── repository/          AuthRepository, UserRepository, ProductRepository,
│   │                        CartRepository, OrderRepository, ImageStorage
│   ├── payment/             EpaycoGateway (checkout y validación) y PaymentResult
│   └── Session.kt           Perfil del usuario autenticado
├── security/                BiometricAuth y BiometricCredentialStore (Keystore)
├── location/                LocationHelper (FusedLocationProvider + Geocoder)
├── ui/
│   ├── auth/                Login, registro y recuperación de contraseña
│   ├── buyer/               Inicio, catálogo, detalle, carrito, checkout, pago, pedidos
│   ├── seller/              Productos (CRUD con fotos) y pedidos recibidos
│   ├── admin/               Dashboard, usuarios, moderación y reporte de ventas
│   └── common/              Perfil, adaptador genérico y tarjetas de pedido
└── util/                    Formato COP, insignias, manejo de errores y corrutinas
```

- Capas: **UI** (Activities y Fragments con ViewBinding) → **repositorios** (lógica de datos) → **Firebase SDK**.
- Las pantallas escuchan Firestore con `Flow` (`dataObjects()`) solo mientras están visibles, así reflejan los cambios al instante.
- Las operaciones de red usan corrutinas; los errores de Firebase se traducen a mensajes en español.

### Modelo de datos (Cloud Firestore)

| Colección | Documento | Contenido |
|---|---|---|
| `users` | `{uid}` | nombre, correo, teléfono, rol (`BUYER`, `SELLER`, `ADMIN`), estado, negocio, fecha y versión de la autorización de datos |
| `audit_logs` | `{id}` | acción, autor, usuario afectado, rol anterior y nuevo, fecha |
| `products` | `{id}` | nombre, descripción, precio, stock, categoría, vendedor, URL de la imagen, aprobación |
| `carts/{uid}/items` | `{productId}` | cantidad y copia del precio |
| `orders` | `{id}` | comprador, líneas, totales con IVA, dirección y coordenadas, estado, referencia de ePayco |

Las reglas de seguridad están en [`firebase/firestore.rules`](firebase/firestore.rules) y [`firebase/storage.rules`](firebase/storage.rules): cada usuario solo lee y modifica lo suyo, el vendedor solo gestiona sus productos y el administrador gestiona todo.

### Seguridad en la asignación de roles

| Criterio | Dónde se aplica |
|---|---|
| El rol de administrador nunca se autoasigna (el registro solo permite comprador o vendedor) | Reglas de Firestore |
| Solo un administrador activo asigna o cambia roles | Reglas de Firestore |
| Nadie cambia su propio rol ni se suspende o elimina a sí mismo | Reglas de Firestore e interfaz |
| Siempre queda al menos un administrador activo | `UserRepository` |
| Una cuenta suspendida pierde los privilegios de su rol | Reglas de Firestore y login |
| Todo cambio de rol se confirma; conceder administrador pide una segunda confirmación | `UserFormFragment` |
| Cada acción administrativa queda en `audit_logs`, que nadie puede editar ni borrar | Reglas de Firestore |

### Normatividad aplicada en la app

- **Ley 1581 de 2012 y Decreto 1377 de 2013**: aviso de privacidad consultable en el registro, autorización expresa y registro de su fecha y versión en el perfil (`privacyAcceptedAt`, `privacyPolicyVersion`).
- **Ley 1480 de 2011**: precio total con IVA discriminado antes de pagar y aviso del derecho de retracto (art. 47) y de reversión del pago (art. 51).
- **Ley 527 de 1999**: cada orden conserva la referencia y el estado de la transacción de ePayco.

## Cómo ejecutarla

Requisitos: Android Studio (Koala o superior) o JDK 17, Android SDK 34, `minSdk 26`.

### Opción A: proyecto de Firebase en la nube (dispositivo físico)

1. Cree un proyecto en [console.firebase.google.com](https://console.firebase.google.com) y registre una app Android con el paquete `com.ucompensar.ecomarket`.
2. Descargue `google-services.json` y cópielo en `app/`.
3. Active **Authentication → Correo/contraseña**, **Firestore Database** y **Storage**. Storage puede pedir activar el plan Blaze, que tiene una capa gratuita.
4. Publique las reglas: `firebase deploy --only firestore:rules,storage` (o péguelas en la consola).
5. Cree la primera cuenta desde la app y, en la consola de Firestore, cambie su `role` a `ADMIN`.

### Opción B: Firebase Emulator Suite (sin cuenta en la nube)

Sin `google-services.json`, la app se conecta sola a los emuladores locales:

```bash
npm install -g firebase-tools
firebase emulators:start --project demo-ecomarket   # Auth 9099, Firestore 8080, Storage 9199
python3 tools/seed_emulator.py                       # usuarios y productos de demostración
./gradlew installDebug
```

Para un **teléfono físico** en la misma red Wi-Fi que el computador:

```bash
python3 tools/seed_emulator.py --device-host 192.168.1.50   # IP del computador
./gradlew installDebug -Pecomarket.emulatorHost=192.168.1.50
```

Cuentas de demostración (contraseña `EcoMarket2026`):

| Rol | Correo |
|---|---|
| Administrador | admin@ecomarket.co |
| Vendedor | camila@ecomarket.co · andres@ecomarket.co |
| Comprador | maria@correo.com · jorge@correo.com |

### Pasarela de pagos ePayco

1. Cree una cuenta en [epayco.com](https://epayco.com) y active el modo de pruebas.
2. Copie la **llave pública** (Integraciones → Llaves API) en `gradle.properties`:
   ```properties
   EPAYCO_PUBLIC_KEY=su_llave_publica
   EPAYCO_TEST=true
   ```
3. Pague con la tarjeta de prueba de ePayco: Visa `4575 6231 8229 0326`, vencimiento `12/27`, CVV `123`.

La llave incluida por defecto es la de demostración del repositorio oficial de ejemplos de ePayco: abre el checkout, pero ePayco rechaza la transacción porque no pertenece a un comercio activo.

### APK

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk (firma de depuración)
```

## Capturas

En [`docs/capturas`](docs/capturas) hay capturas de cada pantalla por rol.

## Control de versiones

El historial refleja el avance por actividades: recursos e interfaces (Actividad 2), y luego configuración de Firebase, capa de datos, autenticación, comprador, vendedor y administrador (Transferencia).
