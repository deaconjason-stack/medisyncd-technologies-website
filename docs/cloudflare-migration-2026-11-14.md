# Cloudflare migration runbook — MediSyncD Technologies
Prepared 2026-11-14. **Staging only: this document does not establish that DNS or hosting was changed.**

## Goal
Move the public site away from IONOS website hosting to Cloudflare Pages while retaining ownership of **medisyncdtechnologies.com** and preserving **Google Workspace mail**.

## Repository and static site
- Repository: \`deaconjason-stack/medisyncd-technologies-website\`
- Static publication directory: \`site\`
- Contents: \`index.html\`, \`privacy.html\`, \`404.html\`, \`favicon.svg\`, \`robots.txt\`, \`sitemap.xml\`, and Cloudflare Pages \`_headers\`.
- No backend, tracking scripts, forms, payment flow, or private patient/student information.
- The header and favicon now embed an optimized version of the current company-owned MediSyncD gold shield logo; confirm final cropping/presentation during visual review.
- Prior August 1 Next.js design summary is preserved; this static deployment is the urgent interim public website, not a completed Next.js implementation.

## Cloudflare Pages — publish before editing DNS
1. In Cloudflare, go to **Workers & Pages → Create application → Pages → Connect to Git**.
2. Select the GitHub repository \`deaconjason-stack/medisyncd-technologies-website\`.
3. Production branch: \`main\` **after this migration PR is merged**.
4. Framework preset: **None**; no build command.
5. Build output directory: **site** (relative to repository root). Keep repository root directory at its default.
6. Deploy, then check the resulting **pages.dev** address on mobile and desktop, and test the links, favicon, privacy notice, 404 behavior and HTTPS.
7. Only after the staging site passes review, add both custom domains \`medisyncdtechnologies.com\` and \`www.medisyncdtechnologies.com\` to Pages. Use Cloudflare's exact DNS instructions.

## Verified DNS snapshot while IONOS is authoritative (November 14, 2026)
- IONOS registrar (RDAP), registration due May 5, 2027.
- Existing nameservers: \`ns1100.ui-dns.org\`, \`ns1056.ui-dns.biz\`, \`ns1060.ui-dns.de\`, \`ns1066.ui-dns.com\`.
- Old website records: apex A \`217.160.0.34\`; apex AAAA \`2001:8d8:100f:f000::200\`; www A \`212.227.172.253\`; www AAAA \`2001:8d8:105:1:0:1:0:7\`.
- Google Workspace MX: \`aspmx.l.google.com\` priority 1; \`alt1.aspmx.l.google.com\` and \`alt2.aspmx.l.google.com\` priority 5; \`alt3.aspmx.l.google.com\` and \`alt4.aspmx.l.google.com\` priority 10.
- SPF at apex: \`v=spf1 include:_spf.google.com ~all\`.
- Google site verification TXT was observed; copy its exact value from IONOS rather than relying on this runbook.
- One DNS check found no \`_dmarc\` TXT record. **This is not a complete DNS zone inventory.** Check Google DKIM selectors, CAA, SRV, verification TXT, subdomains, and any mail or other integrations before nameserver change.

## DNS cutover — approval gate
1. Add this domain to a Cloudflare DNS zone and verify every imported record. **Cloudflare scanning does not guarantee completeness**.
2. Restore all existing Google Workspace MX, SPF, DKIM, DMARC (if configured), verification and other needed records; do not turn on Cloudflare Email Routing, which conflicts with Google MX.
3. Check IONOS DNSSEC/DS; if enabled, disable before nameserver change and follow Cloudflare's propagation instructions.
4. Change authoritative nameservers at IONOS **only** to the pair assigned to the exact new Cloudflare zone, and wait for the zone to be Active.
5. Verify apex + www redirect, certificate, mobile and desktop page behavior, broken links, 404, and two-way Google Workspace email to/from an external account.
6. Keep old IONOS hosting until the replacement passes and rollback options have been documented.

## Registrar transfer — a separate later step
1. Registrar transfer requires Cloudflare zone to be Active, user payment details, and permission to transfer.
2. Confirm no 60-day transfer lock (including one triggered by a recent registrant contact change).
3. In IONOS, unlock the domain and obtain the Auth/EPP code **only when ready to use it**. Never place that code in GitHub/chat.
4. Enter code and pay through Cloudflare's transfer workflow; verify registrar emails and completion.
5. Only after transfer and site + email verification, cancel **IONOS web hosting**, not the domain registration. Retain current backups and accounting records.

## Launch acceptance criteria
- Public pages load over HTTPS on apex and www.
- Website content reflects current products and clearly marks concepts as in development.
- Responsive layout, keyboard focus, privacy page, no unintentional forms or third-party trackers.
- Google Workspace mail delivery and outbound SPF/DKIM pass; services stay operational.
- Domain remains registered and under authorized owner control; Cloudflare becomes registrar only when transfer is confirmed.

Official references:
- https://developers.cloudflare.com/pages/get-started/git-integration/
- https://developers.cloudflare.com/dns/manage-dns-records/how-to/set-up-google-workspace/
- https://developers.cloudflare.com/registrar/get-started/transfer-domain-to-cloudflare/
