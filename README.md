# AnestesIA 💉

> **Software de Apoyo a la Decisión Clínica para Anestesiología**  
> Gestión de fármacos, temporizadores persistentes, alertas sonoras y reversión de emergencia.

---

## ⚠️ Aviso Legal

Este software es una herramienta de **soporte a la decisión clínica** (SaMD – Software as Medical Device).  
**No reemplaza el criterio del profesional médico.** Su uso queda bajo la responsabilidad exclusiva del anestesiólogo a cargo.

---

## Características Principales

| Módulo | Descripción |
|---|---|
| 🏥 Paciente | Peso en kg persistente con DataStore |
| 💊 Vademécum | CRUD completo con código de colores ASTM |
| ⏱ Temporizador | Cuenta regresiva circular por fármaco |
| 🔔 Alertas | Beep al 80 % (preventivo) y 100 % (crítico) |
| 🚨 Pánico | FAB rojo con antídotos ordenados por urgencia |
| 📦 Backup | Exportar/Importar JSON (Upsert en Room) |

---

## Stack Técnico

```
Kotlin 2.0       → Lenguaje
Jetpack Compose  → UI declarativa
Room 2.6         → Persistencia SQLite
Hilt 2.52        → Inyección de dependencias
DataStore        → Preferencias del paciente
Foreground Svc   → Alertas con pantalla bloqueada
ToneGenerator    → Beeps sin archivos de audio
kotlinx.serial   → Serialización JSON
Navigation Comp  → Navegación entre pantallas
```

---

## Arquitectura

```
app/
├── data/
│   ├── local/
│   │   ├── entity/          ← DrugEntity, ActiveTimerEntity, Mappers
│   │   ├── dao/             ← DrugDao, ActiveTimerDao
│   │   ├── AnestesiaDatabase.kt
│   │   └── DatabaseModule.kt
│   └── repository/          ← DrugRepository, TimerRepository, BackupRepository
├── domain/
│   ├── model/               ← Drug, ActiveTimer, VademecumBackup, DrugCategory
│   └── usecase/             ← (extensible)
├── presentation/
│   ├── main/                ← MainScreen + MainViewModel
│   ├── vademecum/           ← VademecumScreen + VademecumViewModel
│   ├── patient/             ← PatientViewModel + PatientModule
│   ├── theme/               ← AnestesiaTheme, AstmColors
│   └── NavGraph.kt
├── service/
│   ├── TimerForegroundService.kt
│   └── BootReceiver.kt
├── AnestesiaApplication.kt
└── MainActivity.kt
```

---

## Código de Colores ASTM

| Categoría | Color | Hex |
|---|---|---|
| Relajantes musculares | 🔴 Rojo | `#E53935` |
| Opioides | 🔵 Azul | `#1565C0` |
| Hipnóticos | 🟡 Amarillo | `#F9A825` |
| Benzodiacepinas | 🟠 Naranja | `#E65100` |

---

## Construcción Local

### Prerrequisitos
- Android Studio Ladybug (2024.2) o superior
- JDK 17
- Android SDK 35

```bash
git clone https://github.com/TU_USUARIO/AnestesiaApp.git
cd AnestesiaApp

# Debug
./gradlew assembleDebug

# Release (sin firma)
./gradlew assembleRelease
```

El APK se genera en: `app/build/outputs/apk/`

---

## GitHub Actions (CI/CD)

El workflow `.github/workflows/android.yml` se dispara automáticamente con cada push a `main` o `develop`.

**Artefactos generados:**
- `AnestesIA-debug-{número}` → APK debug listo para instalar
- `AnestesIA-release-{número}` → APK release sin firmar

**Para habilitar releases firmados**, agregue los siguientes *Secrets* en el repositorio:
```
SIGNING_KEY          ← Keystore en Base64
KEY_ALIAS            ← Alias del certificado
KEY_STORE_PASSWORD   ← Contraseña del keystore
KEY_PASSWORD         ← Contraseña de la clave
```
Luego descomente el job `sign-and-release` en el workflow.

---

## Fármacos Predeterminados

La aplicación viene precargada con:

| Fármaco | Categoría | Antídoto |
|---|---|---|
| Fentanilo | Opioide | Naloxona |
| Propofol | Hipnótico | Soporte hemodinámico |
| Succinilcolina | Relajante | Sugammadex |
| Rocuronio | Relajante | Sugammadex |
| Midazolam | Benzodiacepina | Flumazenil |
| Ketamina | Hipnótico | — |
| Morfina | Opioide | Naloxona |
| Vecuronio | Relajante | Sugammadex / Neostigmina |

---

## Permisos Android

| Permiso | Propósito |
|---|---|
| `FOREGROUND_SERVICE` | Temporizadores persistentes |
| `FOREGROUND_SERVICE_HEALTH` | Clasificación correcta en Android 14+ |
| `POST_NOTIFICATIONS` | Alertas en pantalla bloqueada |
| `RECEIVE_BOOT_COMPLETED` | Reinicio del servicio tras reinicio del dispositivo |
| `WAKE_LOCK` | Mantener CPU activa para beeps |

---

## Formato del Archivo JSON de Backup

```json
{
  "version": 1,
  "exportedAt": 1715000000000,
  "drugs": [
    {
      "id": 0,
      "name": "Fentanilo",
      "category": "OPIOIDE",
      "doseMgKg": 0.002,
      "concentrationMgMl": 0.05,
      "reinjectionTimeMinutes": 30,
      "antidote": "Naloxona",
      "notes": "Dosis: 2 mcg/kg",
      "isActive": true
    }
  ]
}
```

---

## Licencia

Uso clínico restringido. Consulte con su institución antes de desplegar en entornos de producción hospitalaria.
