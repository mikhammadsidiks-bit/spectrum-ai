# SPECTRUM AI 1.1

Android OSINT utility for public technical intelligence.

## Modules
- Domain: RDAP registration data, DNS A/AAAA/MX/NS/TXT, certificate transparency via crt.sh, public urlscan.io results.
- IP: public IP metadata via ipwho.is, IP RDAP allocation data, reverse DNS via Google DNS.
- URL: extracts the hostname and runs domain technical checks.

All lookups use public technical registries and public endpoints. No private databases, account access, personal-data broker scraping, or login bypass. Results can be incomplete or unavailable; verify important findings with the linked source.

## Build
GitHub Actions runs `gradle assembleDebug` on pushes to main and uploads the APK artifact.
