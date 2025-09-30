# Control de Pagos CI

Una aplicación Android para la gestión y control de pagos de clientes con funcionalidades de checklist, reportes y administración.

## 📱 Características

- **Gestión de Clientes**: Administración completa de información de clientes
- **Sistema de Checklist**: Formularios dinámicos con preguntas personalizables
- **Reportes PDF**: Generación automática de reportes en formato PDF
- **Estados de Pago**: Control de estados PAGADO/PENDIENTE
- **Modo Administrador**: Funcionalidades avanzadas para administradores
- **Importación/Exportación**: Soporte para archivos Excel
- **Notificaciones**: Sistema de notificaciones integrado
- **Tutorial Interactivo**: Guía de uso para nuevos usuarios

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: Kotlin
- **UI Framework**: Android Views con ViewBinding
- **Base de Datos**: SharedPreferences + JSON
- **PDF Generation**: iText7
- **Excel Support**: Apache POI
- **Arquitectura**: MVVM con LiveData
- **Navegación**: Bottom Navigation + Drawer Navigation

## 📋 Requisitos del Sistema

- **Android**: API 26+ (Android 8.0+)
- **RAM**: Mínimo 2GB recomendado
- **Almacenamiento**: 50MB para la aplicación + espacio para datos
- **Permisos**: Almacenamiento para exportar reportes

## 🚀 Instalación

### Opción 1: Instalación Directa
1. Descarga el archivo APK más reciente desde [Releases](../../releases)
2. Habilita "Fuentes desconocidas" en tu dispositivo Android
3. Instala el APK

### Opción 2: Compilación desde Código Fuente
1. Clona el repositorio:
   ```bash
   git clone https://github.com/tu-usuario/control-de-pagos-ci.git
   cd control-de-pagos-ci
   ```

2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias de Gradle
4. Compila e instala en tu dispositivo:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📖 Guía de Uso

### Primer Uso
1. Al abrir la aplicación por primera vez, se mostrará un tutorial interactivo
2. Completa el tutorial para familiarizarte con las funcionalidades
3. Configura los datos iniciales en el modo administrador

### Gestión de Clientes
- **Agregar Cliente**: Toca el botón "+" en la pantalla principal
- **Editar Cliente**: Mantén presionado un cliente en la lista
- **Eliminar Cliente**: Usa el menú contextual del cliente

### Sistema de Checklist
- Las preguntas se configuran en el modo administrador
- Cada cliente puede tener un checklist personalizado
- Los estados se guardan automáticamente

### Generación de Reportes
- Toca el botón "Generar Reporte" en la pantalla principal
- El reporte se guarda en la carpeta de descargas
- Los reportes incluyen toda la información de clientes y estados

### Configuración
- Accede a la configuración desde el menú "Más"
- Cambia entre estados PAGADO/PENDIENTE
- Importa/exporta datos desde Excel

## 🏗️ Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/checklist/app/
│   │   ├── MainActivity.kt              # Actividad principal
│   │   ├── QuestionsActivity.kt         # Gestión de clientes
│   │   ├── EjecutivosActivity.kt        # Gestión de ejecutivos
│   │   ├── ReportsActivity.kt           # Visualización de reportes
│   │   ├── ConfigActivity.kt            # Configuración
│   │   ├── NotificationsActivity.kt     # Sistema de notificaciones
│   │   ├── TutorialActivity.kt          # Tutorial interactivo
│   │   ├── managers/                    # Gestores de datos
│   │   └── utils/                       # Utilidades
│   ├── res/                            # Recursos (layouts, drawables, etc.)
│   └── AndroidManifest.xml             # Configuración de la aplicación
├── build.gradle.kts                    # Configuración del módulo
└── proguard-rules.pro                  # Reglas de ProGuard
```

## 🔧 Configuración de Desarrollo

### Dependencias Principales
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    // ... más dependencias
}
```

### Configuración de Compilación
- **Compile SDK**: 35
- **Target SDK**: 35
- **Min SDK**: 26
- **Java Version**: 1.8

## 📊 Funcionalidades por Versión

### v1.0 - Versión Base
- Gestión básica de clientes
- Sistema de checklist
- Generación de reportes PDF

### v1.1 - Mejoras de UI
- Interfaz mejorada
- Navegación optimizada
- Mejor rendimiento

### v1.2 - Funcionalidades Avanzadas
- Sistema de notificaciones
- Importación/exportación Excel
- Modo administrador mejorado

## 🐛 Solución de Problemas

### La aplicación se cierra inesperadamente
- Verifica que tengas Android 8.0 o superior
- Asegúrate de tener suficiente espacio de almacenamiento
- Reinstala la aplicación

### Los reportes no se generan
- Verifica los permisos de almacenamiento
- Asegúrate de tener espacio suficiente en el dispositivo
- Revisa que haya datos de clientes para reportar

### Problemas de importación Excel
- Verifica que el archivo Excel tenga el formato correcto
- Asegúrate de que el archivo no esté corrupto
- Revisa los permisos de lectura de archivos

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

## 📞 Soporte

Para soporte técnico o reportar bugs, por favor:
- Abre un issue en GitHub
- Incluye información detallada del problema
- Adjunta logs si es posible

## 🔄 Historial de Versiones

- **v1.9** - Sistema de notificaciones, optimizaciones de rendimiento
- **v1.8** - Corrección de estados de clientes, mejoras en reportes
- **v1.7** - Sesión persistente, mejoras en UI
- **v1.6** - Configuración de administrador, importación Excel
- **v1.5** - Categorías coloreadas, menú mejorado
- **v1.4** - Formularios automáticos, diagnósticos completos
- **v1.3** - Sistema de preguntas, categorías mejoradas
- **v1.2** - Categorías, reportes mejorados
- **v1.1** - Menú flotante, UI actualizada
- **v1.0** - Versión inicial

---

**Desarrollado con ❤️ para la gestión eficiente de pagos de clientes**
