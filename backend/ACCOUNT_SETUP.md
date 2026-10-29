# Account and notification setup

The account API adds security tables using SQLModel create_all; it does not alter or delete existing account rows. Existing sessions must sign in again because tokens now carry a credential version. Password changes invalidate all previous sessions; logout revokes the current token. Deletion requires the account password.

## Recovery email

For Render free services, use the HTTPS Resend integration (SMTP ports 25, 465 and 587 are blocked on free services).

Set these in the Prototyx Render service environment, never in source control:

- RESEND_API_KEY: a sending key from your mail-provider account.
- MAIL_FROM: a sender address on a domain verified with Resend.
- PUBLIC_APP_URL: https://prototyx.onrender.com (default).

Alternatively, a hosting plan that permits SMTP can use SMTP_HOST, SMTP_USER, SMTP_PASSWORD, SMTP_PORT (587 or 465), and MAIL_FROM. TLS is required.

GET /api/auth/recovery-status reports whether credentials are configured. Without them, recovery requests explicitly return a service-unavailable message; the API never returns a recovery token. Codes are hashed in the database, expire after 30 minutes, are one-use and are rate-limited per account to one email per five minutes. Mail-provider delivery and verified-domain configuration must be tested with an account you own before release.

## Published pages

- /account/delete: external account/data deletion flow (sign-in and confirmation required).
- /account/reset: recovery-code completion flow.
- /legal/privacy: factual data-handling notice.
- /legal/terms: research service limitations.

Before a public store release, review these notices for the actual operating entity, contact details, provider agreements and retention arrangements. Add the external deletion URL and privacy URL to Play Console. The implementation does not itself complete store declarations or legal review.

## Android notifications

Notifications are local opt-in portfolio-review reminders, not push price alerts. WorkManager schedules approximately every 24 hours; Android battery policies can delay delivery. A notification channel and Android 13+ permission are supported. Notifications open the in-app inbox, hide private text on the lock screen, retain up to 50 inbox entries, and are canceled on sign-out. The explicit test-notification button checks device delivery without waiting a day. No Firebase account is required for these reminders.
