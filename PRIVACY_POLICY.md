# Privacy Policy for Phantom

**Effective Date:** September 25, 2026  
**Last Updated:** September 25, 2026  

Phantom ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains our practices regarding data collection, use, and protection when you use the Phantom Android application.

---

## 1. Overview & Privacy-First Architecture
Phantom is engineered from the ground up as a **client-side, privacy-respecting application**. 
- We do **not** require user accounts or registration.
- We do **not** collect, store, sell, or share personal identifiable information (PII) such as your name, email address, phone number, location, or device identifiers.
- We do **not** run third-party advertising tracking SDKs or user profiling trackers.

---

## 2. Information Handled by the Application

### A. Local Device Data (Stored Strictly on Your Device)
All user-generated preferences and activities remain entirely on your local device within the app's sandboxed storage:
- **Watch History & Search History:** Stored exclusively in a local SQLite/Room database on your device. You can clear or disable history at any time in the app settings.
- **Subscriptions & Bookmarks:** Channel subscriptions and saved playlists are kept strictly on your device and are never synchronized to our servers or Google accounts.
- **App Settings:** Playback preferences (playback speed, audio-only mode, sleep timer) are stored locally in Android SharedPreferences.

### B. Network Communications & Third-Party APIs
To provide video search, streaming, and metadata, the application communicates directly from your device with external service endpoints:
- **YouTube / InnerTube:** The app sends user search queries and video playback requests directly to YouTube public endpoints to retrieve content. Your interaction with these services is governed by [Google's Privacy Policy](https://policies.google.com/privacy) and [YouTube Terms of Service](https://www.youtube.com/t/terms).
- **SponsorBlock API:** The app communicates with the open-source SponsorBlock API (`sponsor.ajay.app`) solely to query timestamp segments (such as sponsor intervals or intros) for videos you watch. No personal user identification is transmitted.
- **GitHub Updates:** The app queries a public JSON file hosted on GitHub to check for official application updates.

---

## 3. App Permissions & Why They Are Needed

Phantom only requests permissions strictly required to provide its core features:
- **INTERNET (`android.permission.INTERNET`):** Required to fetch video streams, thumbnails, search results, and update info.
- **FOREGROUND SERVICE (`android.permission.FOREGROUND_SERVICE`):** Required to maintain smooth, uninterrupted background audio playback and control playback from the notification bar when the screen is turned off.
- **POST NOTIFICATIONS (`android.permission.POST_NOTIFICATIONS`):** Used to display media playback controls and update download notifications on Android 13+.
- **REQUEST INSTALL PACKAGES (`android.permission.REQUEST_INSTALL_PACKAGES`):** Only requested when you choose to download and apply an official in-app software update.

---

## 4. Children’s Privacy
Phantom does not knowingly collect or solicit any personal information from children under the age of 13. If you believe any minor has provided data to us, please contact us and we will ensure no such data is stored.

---

## 5. Security of Your Information
Because Phantom does not operate user data servers, your data is inherently protected by Android's application sandbox security model. All communication with streaming endpoints is encrypted via industry-standard TLS/HTTPS protocols.

---

## 6. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated "Last Updated" date. We encourage you to review this Privacy Policy periodically.

---

## 7. Contact Us
If you have any questions, feedback, or concerns regarding this Privacy Policy or Phantom, please reach out to us:
- **Developer:** Fiki-io
- **Email:** totoyu379@gmail.com
- **Project Repository:** [https://github.com/Fiki-io/Phantom](https://github.com/Fiki-io/Phantom)
