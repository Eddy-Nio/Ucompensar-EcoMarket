package com.ucompensar.ecomarket

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

/**
 * Punto de entrada de la aplicación.
 *
 * Inicializa Firebase. Si el proyecto se compiló sin `google-services.json` (o con
 * `-Pecomarket.useEmulator=true`), conecta Auth, Firestore y Storage al Firebase
 * Emulator Suite local para poder probar la app sin un proyecto en la nube.
 */
class EcoMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    /** Crea la instancia por defecto de Firebase y, si aplica, la redirige a los emuladores. */
    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            // Sin google-services.json: proyecto "demo-" que solo existe en el emulador local
            FirebaseApp.initializeApp(this, demoOptions())
        }
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            connectToEmulators(FirebaseApp.getInstance())
        }
    }

    companion object {
        private const val TAG = "EcoMarketApp"
        private const val AUTH_PORT = 9099
        private const val FIRESTORE_PORT = 8080
        private const val STORAGE_PORT = 9199

        /** Opciones de un proyecto de demostración; el prefijo "demo-" impide que se use la nube. */
        fun demoOptions(): FirebaseOptions = FirebaseOptions.Builder()
            .setProjectId("demo-ecomarket")
            .setApplicationId("1:000000000000:android:0000000000000000")
            // Valor de relleno con el formato que exige el SDK (A + 38 caracteres); no es una
            // llave de Google y el emulador no la valida.
            .setApiKey("ADemoKeyLocalEmulatorOnly00000000000000")
            .setStorageBucket("demo-ecomarket.appspot.com")
            .build()

        /**
         * Conecta los tres servicios de una instancia de Firebase al Emulator Suite.
         * También se usa con la instancia secundaria con la que el administrador crea cuentas.
         */
        fun connectToEmulators(app: FirebaseApp) {
            val host = BuildConfig.FIREBASE_EMULATOR_HOST
            runCatching {
                Firebase.auth(app).useEmulator(host, AUTH_PORT)
                Firebase.firestore(app).useEmulator(host, FIRESTORE_PORT)
                Firebase.storage(app).useEmulator(host, STORAGE_PORT)
            }.onFailure { Log.w(TAG, "Firebase ya estaba inicializado; se conserva su configuración", it) }
        }
    }
}
