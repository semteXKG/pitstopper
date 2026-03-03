# Copilot Instructions for PitStopper Project

## Standard Release Process

**When user asks to "create a version" or "create a release", follow these exact steps:**

### 1. Update Version Numbers
Edit `app/build.gradle.kts`:
- Increment `versionCode` (e.g., 3→4)  
- Increment `versionName` (e.g., "1.0.3"→"1.0.4")

### 2. Build Release APK
```powershell
cd C:\Users\local-semtex\StudioProjects\pitstopper
.\gradlew assembleRelease
```

### 3. Verify APK Signing
```powershell
C:\Users\local-semtex\AppData\Local\Android\Sdk\build-tools\36.1.0\apksigner.bat verify -verbose app\build\outputs\apk\release\app-release.apk
```

### 4. Copy to Releases Directory
```powershell
Copy-Item "app\build\outputs\apk\release\app-release.apk" "releases\pitstopper-X.X.X.apk"
```

### 5. Update VERSIONS.md
Add new version section at the top with:
- Version number and date
- Major features list with 🚀 emoji
- Technical improvements with 🔧 emoji  
- Files section referencing the new APK

### 6. Git Commit and Tag ⚠️ CRITICAL
```powershell
git add app/build.gradle.kts releases/VERSIONS.md releases/pitstopper-X.X.X.apk
git commit -m "Release vX.X.X: [brief description]

- [Major feature 1]
- [Major feature 2] 
- Update build version to X.X.X (versionCode Y)
- Add comprehensive version documentation"

git tag -a "vX.X.X" -m "Release vX.X.X: [brief description]

Major additions:
- [Feature 1]
- [Feature 2]
- [Other improvements]"
```

### 7. Verification Steps
- ✅ APK is properly signed (v2 scheme)
- ✅ Tag was created: `git tag -l`
- ✅ Files exist in releases directory
- ✅ Version numbers updated correctly

## Important Notes
- **NEVER skip the git tag step** - user specifically mentioned this was missed before
- Always use descriptive commit messages with bullet points
- Include both major features and technical improvements in changelog
- Verify APK signature before considering release complete
- Release APK should be larger if new libraries were added (normal)

## Project Context
- **Location**: `C:\Users\local-semtex\StudioProjects\pitstopper`
- **Keystore**: `release.keystore` (configured in build.gradle.kts)
- **Build Tools**: Located in `C:\Users\local-semtex\AppData\Local\Android\Sdk\build-tools\36.1.0\`
- **Target SDK**: 36
- **Min SDK**: 24

## Last Release
- **Version**: 1.0.4 (versionCode 4)
- **Date**: February 14, 2026
- **Major Features**: MQTT Server, SpeedHive UI improvements
- **APK Size**: 7.8MB