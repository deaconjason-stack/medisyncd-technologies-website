# MediSyncD HospitalOS Android v4.2.1

v4.2.1 is the complete standalone Android build of the current HospitalOS application.

Working Android features:
- 103-module HospitalOS interface preserved.
- Domonique 2.0 routes every existing AI module through either:
  1. an optional secure HTTPS gateway (recommended for organization deployment), or
  2. a user-entered OpenAI API key encrypted at rest with Android Keystore for a private standalone install.
- OpenAI direct mode uses the Responses API and model gpt-5.6-sol.
- React runtime is bundled locally for offline app startup.
- HospitalOS local data persists on-device.
- Full localStorage backup export and restore.
- Android Calendar appointment handoff (user confirms each event in their calendar app).
- Android Slack sharing (opens Slack when installed, otherwise Android share chooser).
- File uploads/downloads.
- Camera/microphone runtime permission handling.
- Device PIN/biometric gate enabled by default when the device has a secure lock.
- Screenshot/screen-recording protection enabled by default.
- App backups disabled and cleartext network traffic blocked.
- Android 16 / API 36 target.

Important deployment boundary:
This build is a working standalone product/demo and private-use application. It is not, by itself, a certified EHR, a HIPAA compliance certification, or authorization for real-patient production deployment. Facility deployment still requires organization-managed identity/RBAC, policy/BAA review, centralized records/audit retention, clinical validation, and applicable regulatory/certification work. The secure gateway path is included for that future deployment model.
