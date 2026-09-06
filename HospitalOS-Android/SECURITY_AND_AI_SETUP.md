# MediSyncD HospitalOS Android v4.1

This build targets Android 16 / API 36 and preserves the HospitalOS v4.0 application while improving the Android container.

Added: safer WebViewAssetLoader delivery, locally bundled React runtime, blocked cleartext HTTP, Safe Browsing, persistent DOM storage, file picker support, permission-gated camera/mic access, Android audit-log export, configurable screenshot protection, disabled app backups, and a secure HTTPS Domonique 2.0 gateway configuration that keeps provider API keys out of the APK.

The included Cloudflare Worker can use Anthropic or OpenAI and can optionally require APP_TOKEN.

Production note: this is not a certification of HIPAA compliance, EHR certification, clinical validation, or production readiness. Real-patient deployment still needs organization-specific identity/RBAC, risk analysis, vendor/BAA review, audit/retention design, clinical validation, and applicable regulatory work.
