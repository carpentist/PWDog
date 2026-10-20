# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 0.2.x | Yes |
| < 0.2 | No |

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Use GitHub's private vulnerability reporting:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability**.
3. Include the affected version, Windows version, reproduction steps, and impact.

If private reporting is unavailable, open a minimal public issue asking for a private contact channel without disclosing technical details.

Please do not include real passwords, recovery keys, or pairing tokens in reports. Use a throwaway vault whenever possible.

## Scope

Security-relevant areas include:

- Argon2id parameters and key derivation
- DEK wrapping, recovery slots, and verifier checks
- SQLCipher database handling and key lifetime
- Windows Hello / DPAPI quick unlock
- The loopback autofill API and browser extension
- Installer and uninstall data cleanup
- Clipboard handling
- Any path traversal or data-directory assumptions

## Security model in brief

- Each local account has a random 256-bit DEK. The DEK is wrapped by a password-derived KEK and by a recovery key.
- Password databases are SQLCipher-encrypted. `accounts.db` contains plaintext metadata: account names, avatars, KDF parameters, and salts.
- Quick unlock is protected with DPAPI and bound to the current Windows user. The master password remains a fallback.
- The browser API listens only on `127.0.0.1:27831` and requires a random Bearer token. It refuses to return secrets while locked and never auto-submits forms.
- Copied passwords are cleared after 15 seconds if the clipboard still contains that value.

## Known limitations

PWDog has not been independently audited. The JVM cannot guarantee full memory zeroization, and a process memory dump may expose live keys. Program-internal account isolation is not the same as Windows user isolation: a Windows user can read account metadata and attempt to guess another local account's master password.

Uninstalling version 0.2.0 or later removes the entire `%LOCALAPPDATA%\PWDogData` directory for the current Windows user. Back up before uninstalling.
