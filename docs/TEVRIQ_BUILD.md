# Tevriq Quick Search — GitHub Actions APK builds

This is an independently installable fork of Quick Search v5.0 with applicationId
`org.tevriq.quicksearch` (`org.tevriq.quicksearch.debug` for debug). Kotlin
namespace stays `com.tk.quicksearch` for upstream compatibility. The F-Droid
flavor is used to avoid Google Play in-app update and review integrations.

## Debug APK

Actions -> **Tevriq Quick Search APK** -> **Run workflow** -> debug. Builds on
a public GitHub-hosted runner. The debug APK is a smoke-test build and is
**not guaranteed to have the same signing certificate across different runs**.
Do not use it for long-term data or expect upgrades without reinstalling.

## Signed release APK (persistent signing identity)

Generate a private release keystore offline. Keep a backup: losing it makes it
impossible to install future APK updates over existing releases without
uninstalling and potentially losing app data.

In repository Settings -> Secrets and variables -> Actions -> Repository secrets,
configure:

- `TEVRIQ_RELEASE_KEYSTORE_BASE64`: base64 of the complete private keystore
- `TEVRIQ_RELEASE_STORE_PASSWORD`: keystore password
- `TEVRIQ_RELEASE_KEY_ALIAS`: alias of the signing key
- `TEVRIQ_RELEASE_KEY_PASSWORD`: signing key password

Then Actions -> **Tevriq Quick Search APK** -> Run workflow -> release.
The workflow decodes the keystore only on the temporary GitHub runner and
deletes the temporary copy when finished. Never commit the keystore, passwords,
or base64 to this public repository. The Release build no longer falls back
to signing with a transient Debug key.

The first signed release and every later update must use the same key, package
ID, and a strictly higher versionCode. Existing upstream app installations
(`com.tk.quicksearch`) are unaffected; data is not migrated between packages.

## Device verification (vivo X200 Ultra / OriginOS)

Manual, on-device acceptance checks after downloading an APK:

1. Install, open and search installed apps, settings, contacts, files, and bookmarks.
2. Keep OriginOS as the home launcher; open Quick Search using a home widget,
   Quick Settings tile, overlay/edge gesture or notch gesture if supported.
3. Check permission prompts, keyboard focus, background gesture reliability,
   battery restrictions, and that search launches the selected result.
4. Verify the package ID and signing certificate before declaring release ready.

A successful GitHub Actions build does **not** establish that OriginOS gestures
or the resulting UI behavior have been verified on hardware.
