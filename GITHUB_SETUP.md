# Instrucciones para Subir el Proyecto a GitHub

## 📋 Pasos para Subir el Proyecto

### 1. Crear Repositorio en GitHub
1. Ve a [GitHub.com](https://github.com) y inicia sesión
2. Haz clic en el botón "+" en la esquina superior derecha
3. Selecciona "New repository"
4. Configura el repositorio:
   - **Repository name**: `control-de-pagos-ci`
   - **Description**: `Sistema de Control de Pagos CI - Aplicación Android para gestión de clientes y reportes`
   - **Visibility**: Public (o Private si prefieres)
   - **NO marques** "Add a README file" (ya tenemos uno)
   - **NO marques** "Add .gitignore" (ya tenemos uno)
   - **NO marques** "Choose a license" (ya tenemos uno)
5. Haz clic en "Create repository"

### 2. Conectar el Repositorio Local con GitHub
Ejecuta estos comandos en la terminal (en el directorio del proyecto):

```bash
# Agregar el repositorio remoto
git remote add origin https://github.com/TU-USUARIO/control-de-pagos-ci.git

# Cambiar el nombre de la rama principal a 'main'
git branch -M main

# Subir el código
git push -u origin main
```

**Reemplaza `TU-USUARIO` con tu nombre de usuario de GitHub**

### 3. Verificar la Subida
1. Ve a tu repositorio en GitHub
2. Verifica que todos los archivos estén presentes
3. Revisa que el README.md se muestre correctamente

## 🔧 Configuración Adicional

### Configurar GitHub Actions
El proyecto ya incluye un archivo `.github/workflows/android.yml` para CI/CD automático.

### Configurar Issues y Projects
1. Ve a la pestaña "Issues" en tu repositorio
2. Habilita Issues si no están habilitados
3. Considera crear templates para issues y pull requests

### Configurar Branch Protection
1. Ve a Settings > Branches
2. Agrega una regla para la rama `main`
3. Habilita "Require pull request reviews before merging"

## 📁 Estructura del Proyecto Subido

El repositorio incluye:
- ✅ Código fuente completo de la aplicación Android
- ✅ Documentación detallada (README.md, CONTRIBUTING.md)
- ✅ Archivos de configuración (build.gradle, AndroidManifest.xml)
- ✅ Recursos (layouts, drawables, strings, etc.)
- ✅ Scripts de PowerShell para formateo de Excel
- ✅ Archivos de ejemplo (Clientes_Test.xlsx)
- ✅ Configuración de CI/CD (GitHub Actions)
- ✅ Licencia MIT
- ✅ .gitignore apropiado para Android

## 🚀 Próximos Pasos

Después de subir el proyecto:

1. **Crear un Release**: Ve a Releases y crea la primera versión
2. **Configurar GitHub Pages**: Para documentación adicional si es necesario
3. **Invitar Colaboradores**: Si planeas trabajar en equipo
4. **Configurar Notificaciones**: Para estar al tanto de issues y PRs

## 📞 Soporte

Si tienes problemas subiendo el proyecto:
1. Verifica que tengas permisos de escritura en el repositorio
2. Asegúrate de que la URL del repositorio sea correcta
3. Revisa que no haya archivos muy grandes (GitHub tiene límites)

---

**¡Tu proyecto Control de Pagos CI estará listo en GitHub!** 🎉
