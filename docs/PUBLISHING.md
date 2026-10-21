# Publishing checklist

## Repository metadata

Suggested description:

```text
Local-first Windows password manager built with Kotlin and Compose. AES-256-GCM, Argon2id, SQLCipher, browser autofill.
```

Suggested topics:

```text
password-manager windows kotlin compose-desktop local-first sqlcipher argon2id browser-extension
```

Social preview image: [`docs/images/social-preview.png`](images/social-preview.png). Upload it under **Settings → General → Social preview**.

Enable **Settings → Code security → Private vulnerability reporting** so [SECURITY.md](../SECURITY.md) works as written.

## Version bump

A release needs all of these updated together:

1. `desktopApp/build.gradle.kts` → `packageVersion`
2. `desktopApp/build.gradle.kts` → the `--app-version` argument in `packageMsiWithCleanup`
3. `CHANGELOG.md`
4. `docs/RELEASE_NOTES_v<version>.md`
5. `README.md` download filename and screenshots if the UI changed
6. `extension/manifest.json` and `extension/manifest.firefox.json` version

The Release workflow fails if the tag version and `packageVersion` do not match.

## Build locally

```powershell
.\gradlew.bat :shared:jvmTest :desktopApp:test
.\gradlew.bat :desktopApp:packageMsiWithCleanup :desktopApp:packagePortableZip
```

Outputs:

- `desktopApp\build\compose\binaries\main\msi\PWDog-<version>.msi`
- `desktopApp\build\compose\binaries\main\portable\PWDog-<version>-portable.zip`

## Release assets

- `PWDog-<version>.msi`
- `PWDog-<version>-portable.zip`
- `PWDog-Extension-<version>.zip` (Chromium)
- `PWDog-Firefox-Extension-<version>.xpi` (signed Firefox, from the Sign Firefox Extension workflow)
- `SHA256SUMS.txt`
- `SHA256SUMS-firefox.txt` (signed XPI)

The Release workflow builds the app packages and Chromium extension on `windows-latest`. The Firefox XPI is signed separately by `sign-firefox.yml` and attached to the release.

## Tag and publish

```powershell
git tag v0.3.0
git push origin v0.3.0
```

The workflow creates the GitHub Release using `docs/RELEASE_NOTES_v0.3.0.md` and attaches the assets.

## Before announcing

- Verify the MSI installs per-machine with administrator rights and can target a non-system drive.
- Verify the portable ZIP extracts and runs from `E:\PWDog`.
- Verify upgrade from the previous version preserves `%LOCALAPPDATA%\PWDogData`.
- Verify a true uninstall removes `%LOCALAPPDATA%\PWDogData` for the current Windows user.
- Check that Chromium and Firefox extensions both load, pair, and fill.
- Confirm `SHA256SUMS.txt` matches the uploaded files.
- Confirm no real vault data, tokens, or personal screenshots were committed.

## Not yet automated

- Code signing
- Auto-update
- Chrome Web Store / Edge Add-ons publishing
