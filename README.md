# MediSyncD Telehealth — Cloudflare Pages Demo
**Fictional demonstration only. Not a clinical service.** Maintained for MediSyncD Technologies, LLC.

This separate Git branch contains a Cloudflare Pages–ready Vite/React SPA adapted from the MediSyncD Telehealth prototype.
Seven interactive views: overview, fictional provider discovery, sample appointments, visual-only virtual visit room, mock clinician workspace, practice management, and safety launch checklist. Browser memory only; refresh resets data.

**No real medical care, PHI, clinician logins, actual bookings, video sessions, payment, EHR, or backend. Do not enter personal health information.**

## Local development
Node.js 22+: `npm install`, then `npm run dev`. Run `npm run build` to produce `dist/`.

## Deploy free demo to Cloudflare Pages
1. Open https://dash.cloudflare.com → **Workers & Pages** → **Create application** → **Pages** → **Import existing Git repository**.
2. Select `deaconjason-stack/medisyncd-technologies-website` and production branch **`telehealth-cloudflare-demo`**. Do not change the existing website's main branch.
3. Set project name **medisyncd-telehealth-demo** (if available); framework **Vite**; build command `npm run build`; output directory `dist`; root `/`; environment variable `NODE_VERSION=22` if necessary.
4. Deploy and open the assigned `*.pages.dev` URL. Verify direct reloads on all routes, mobile menus, sample bookings and prominent demo warnings.
5. Publish only after you verify content does not accept actual clinical data.

## Safety
This is only a demonstration: it has no login or server-side security, and it is not suitable for live patients. A real clinical service requires independently reviewed compliance, professional licensing, clinical oversight, appropriate BAAs, access controls, tenancy, security assessment, audit logs, incident response, informed consent, and operational safeguards.
