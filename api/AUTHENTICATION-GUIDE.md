# MyLaps SpeedHive API - Authentication & Usage Guide

## Authentication Requirements

The MyLaps SpeedHive API requires **TWO forms of authentication**:

1. **API Key** - Hardcoded in the app (static)
2. **Bearer Token** - OAuth 2.0 token from Azure B2C (dynamic, requires user login)

### API Keys (Extracted from APK)

```
Event Results API:     XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
Live Timing API:       XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
Practice API:          SpeedhiveAndroidApp-f3deaaed-2dbb-41be-a469-bb33be4de434
Push Notifications:    e89edbec-0174-5071-b87b-32d311f82c9b
Users & Products API:  SpeedhiveAndroidApp-L7GQi3n6a45kvuGYPeLxRtsv0p3X77olRHmQMp9h
```

### OAuth 2.0 Configuration (Azure B2C)

```
Tenant:           mylapsb2cprd.onmicrosoft.com
Policy:           B2C_1A_SIGNUP_SIGNIN
Client ID:        fd727b52-5fc9-453e-9193-bec38aeacfc6
Scope:            https://mylapsb2cprd.onmicrosoft.com/speedhive-api/read openid offline_access
Redirect URI:     msauth://com.mylaps.speedhive/FH12WcUsBxcdd3B%2Fua1JLvw4EV8%3D
Token Endpoint:   https://mylapsb2cprd.b2clogin.com/mylapsb2cprd.onmicrosoft.com/B2C_1A_SIGNUP_SIGNIN/oauth2/v2.0/token
```

## Required Headers for API Calls

```http
ApiKey: XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
Authorization: Bearer {access_token}
User-Agent: speedhive-android/1.68
Accept: application/json
```

## Getting a Bearer Token

### Option 1: Using the Mobile App (Recommended)
1. Install MyLaps SpeedHive app on an Android device
2. Log in with your credentials
3. Use a tool like HTTP Toolkit or Charles Proxy to intercept the traffic
4. Extract the Bearer token from the Authorization header

### Option 2: OAuth Flow (Requires Credentials)
You need valid MyLaps SpeedHive account credentials:
- Username/Email
- Password

The app uses Resource Owner Password Credentials (ROPC) flow or a custom B2C flow.

### Option 3: Create an Account
1. Visit https://speedhive.mylaps.com
2. Create a free MyLaps SpeedHive account
3. Use those credentials with the authentication script

## API Endpoints for Live Timing

### Query a Specific Car in an Event

**Step 1: Get list of events**
```http
GET https://lt-api.speedhive.com/api/events
Headers:
  ApiKey: XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
  Authorization: Bearer {token}
  User-Agent: speedhive-android/1.68
```

**Step 2: Get event details with sessions**
```http
GET https://lt-api.speedhive.com/api/events/{eventId}?sessions=true
Headers:
  ApiKey: XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
  Authorization: Bearer {token}
  User-Agent: speedhive-android/1.68
```

**Step 3: Get session data (all competitors)**
```http
GET https://lt-api.speedhive.com/api/events/{eventId}/sessions/{sessionId}/data
Headers:
  ApiKey: XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
  Authorization: Bearer {token}
  User-Agent: speedhive-android/1.68
```

**Step 4: Get specific competitor/car data**
```http
GET https://lt-api.speedhive.com/api/events/{eventId}/sessions/{sessionId}/competitor/{competitorId}
Headers:
  ApiKey: XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1
  Authorization: Bearer {token}
  User-Agent: speedhive-android/1.68
```

## Alternative: SignalR Real-Time Connection

For live timing updates, the app uses SignalR WebSocket connection:

```
Hub URL: https://notifications.speedhive.com/
```

This provides real-time push updates for:
- Lap times
- Position changes
- Session status
- Competitor updates

## Next Steps

To use these APIs, you need to:

1. **Get valid credentials** - Create a MyLaps SpeedHive account
2. **Obtain a Bearer token** - Use the authentication flow
3. **Make API calls** - Include both ApiKey and Authorization headers

## Limitations

- API keys extracted from the app may have been rotated/expired since APK build
- OAuth tokens expire (typically 1 hour) and need to be refreshed
- Some endpoints may have rate limiting
- Terms of Service may prohibit reverse engineering / unofficial API access

## Legal Notice

This information is provided for educational purposes only. Always review and comply with MyLaps' Terms of Service when accessing their APIs.
