# SupremeBass - Data Safety Mapping

**App:** SupremeBass - Bass Booster & Sound Enhancer
**Developer:** Supreme Corp
**Date:** July 4, 2026

---

## Data Collection & Sharing Table

| Data Type | Collected | Shared | Purpose | Required | Third Party |
|-----------|-----------|--------|---------|----------|-------------|
| Advertising ID | Yes | Yes | Ad personalization | No | Google AdMob |
| App interactions | Yes | Yes | Ad performance measurement | No | Google AdMob |
| Device info | Yes | Yes | Ad serving compatibility | No | Google AdMob |
| Audio data | No | No | - | No | - |
| Contacts | No | No | - | No | - |
| Location | No | No | - | No | - |
| Microphone | No | No | - | No | - |
| Music library | No | No | - | No | - |
| Personal files | No | No | - | No | - |
| Camera | No | No | - | No | - |
| SMS | No | No | - | No | - |
| Phone | No | No | - | No | - |
| Health | No | No | - | No | - |
| Financial info | No | No | - | No | - |
| Purchase history | No | No | - | No | - |
| User content | No | No | - | No | - |
| Browsing history | No | No | - | No | - |
| Search history | No | No | - | No | - |
| Identifiers | No | No | - | No | - |
| Diagnostics | No | No | - | No | - |
| Other data | No | No | - | No | - |

---

## Data Use Declarations

### Collected Data

| Data Type | Purpose | Is this data required for the app to function? |
|-----------|---------|-----------------------------------------------|
| Advertising ID | To serve personalized ads and measure ad performance | No - App functions without it; ads will be generic |
| App interactions | To improve app experience and ad optimization | No - App functions without it |
| Device info | To ensure compatibility and serve appropriate ads | No - App functions without it |

### Shared Data

| Data Type | Shared With | Purpose |
|-----------|-------------|---------|
| Advertising ID | Google AdMob | Ad personalization and targeting |
| App interactions | Google AdMob | Ad performance measurement |
| Device info | Google AdMob | Ad serving and compatibility |

---

## Data Safety Section Requirements

### Is data collected or shared?
- **Yes** — Advertising ID, app interactions, and device info are collected and shared with Google AdMob.

### What data is collected?
- Advertising ID
- App interactions
- Device info

### What data is NOT collected?
- Audio data
- Contacts
- Location
- Microphone
- Music library
- Personal files
- Camera
- SMS
- Phone
- Health
- Financial info
- Purchase history
- User content
- Browsing history
- Search history
- Identifiers (beyond Advertising ID)
- Diagnostics

### Is data encrypted in transit?
- Yes — All data transmitted to Google AdMob is encrypted using HTTPS/TLS.

### Can users request data deletion?
- Yes — Users can clear App data through device settings, which removes all locally stored data.

### Is the app committed to following the Families Policy?
- No — The app is not directed at children under 13.

---

## Notes

1. All audio processing is local-only. No audio data is ever transmitted from the device.
2. The app does not request microphone permissions.
3. The app does not request location permissions.
4. The app does not request contact permissions.
5. All AI processing occurs locally on the device.
6. Data shared with Google AdMob is subject to Google's privacy policy and data handling practices.
7. The Advertising ID is a resettable identifier and is not linked to persistent device identifiers by this app.
