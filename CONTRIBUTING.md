# Guía de Contribución

¡Gracias por tu interés en contribuir a Control de Pagos CI! Este documento te guiará a través del proceso de contribución.

## 🚀 Cómo Contribuir

### 1. Fork y Clone
1. Fork este repositorio
2. Clona tu fork localmente:
   ```bash
   git clone https://github.com/tu-usuario/control-de-pagos-ci.git
   cd control-de-pagos-ci
   ```

### 2. Configurar el Entorno de Desarrollo
1. Instala Android Studio
2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias de Gradle
4. Configura un dispositivo Android o emulador

### 3. Crear una Rama
```bash
git checkout -b feature/nombre-de-tu-feature
```

### 4. Hacer Cambios
- Sigue las convenciones de código existentes
- Añade comentarios en español
- Prueba tus cambios antes de hacer commit

### 5. Commit y Push
```bash
git add .
git commit -m "feat: descripción de tu cambio"
git push origin feature/nombre-de-tu-feature
```

### 6. Crear Pull Request
1. Ve a GitHub y crea un Pull Request
2. Describe claramente los cambios realizados
3. Menciona cualquier issue relacionado

## 📝 Convenciones de Código

### Kotlin
- Usa camelCase para variables y funciones
- Usa PascalCase para clases
- Comentarios en español
- Indentación de 4 espacios

### Commits
Usa el formato conventional commits:
- `feat:` nueva funcionalidad
- `fix:` corrección de bug
- `docs:` cambios en documentación
- `style:` cambios de formato
- `refactor:` refactorización de código
- `test:` añadir o modificar tests
- `chore:` cambios en build, dependencias, etc.

### Ejemplos:
```bash
git commit -m "feat: agregar sistema de notificaciones"
git commit -m "fix: corregir crash en MainActivity"
git commit -m "docs: actualizar README con nuevas funcionalidades"
```

## 🐛 Reportar Bugs

### Antes de Reportar
1. Verifica que el bug no haya sido reportado ya
2. Prueba con la última versión
3. Revisa la documentación

### Información Necesaria
- **Descripción**: Descripción clara del problema
- **Pasos para reproducir**: Lista de pasos detallados
- **Comportamiento esperado**: Qué debería pasar
- **Comportamiento actual**: Qué está pasando
- **Screenshots**: Si aplica
- **Información del dispositivo**: Modelo, versión de Android
- **Logs**: Logs de error si están disponibles

## ✨ Sugerir Funcionalidades

### Antes de Sugerir
1. Verifica que la funcionalidad no exista
2. Considera si es apropiada para el proyecto
3. Piensa en la implementación

### Información Necesaria
- **Descripción**: Descripción clara de la funcionalidad
- **Justificación**: Por qué sería útil
- **Casos de uso**: Ejemplos de cómo se usaría
- **Alternativas**: Otras opciones consideradas

## 🧪 Testing

### Antes de Enviar PR
- [ ] El código compila sin errores
- [ ] No hay warnings de linting
- [ ] Las funcionalidades nuevas están probadas
- [ ] No se rompieron funcionalidades existentes
- [ ] El código sigue las convenciones del proyecto

### Tipos de Testing
- **Unit Tests**: Para lógica de negocio
- **Integration Tests**: Para flujos completos
- **UI Tests**: Para interacciones de usuario

## 📋 Checklist para Pull Requests

### Código
- [ ] Código limpio y bien documentado
- [ ] Sin código comentado innecesario
- [ ] Variables y funciones con nombres descriptivos
- [ ] Manejo apropiado de errores

### Funcionalidad
- [ ] Nueva funcionalidad funciona correctamente
- [ ] No se rompieron funcionalidades existentes
- [ ] UI es consistente con el resto de la app
- [ ] Performance no se ve afectada negativamente

### Documentación
- [ ] README actualizado si es necesario
- [ ] Comentarios en código en español
- [ ] Changelog actualizado

## 🏷️ Etiquetas de Issues

- `bug`: Algo no funciona
- `enhancement`: Nueva funcionalidad o mejora
- `documentation`: Mejoras en documentación
- `question`: Pregunta o duda
- `help wanted`: Se necesita ayuda
- `good first issue`: Bueno para principiantes

## 📞 Contacto

Si tienes preguntas sobre cómo contribuir:
- Abre un issue con la etiqueta `question`
- Menciona a los maintainers
- Revisa la documentación existente

## 🙏 Reconocimientos

¡Gracias a todos los contribuidores que hacen posible este proyecto!

---

**¡Gracias por contribuir a Control de Pagos CI!** 🎉
