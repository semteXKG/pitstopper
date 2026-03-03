# MyLaps SpeedHive API Integration

This directory contains API documentation and testing scripts for the MyLaps SpeedHive Live Timing API.

## Files

- **mylaps-speedhive-api-endpoints.txt** - Complete API reference with all endpoints, response formats, and field abbreviations
- **query-live-timing.ps1** - Interactive menu-based PowerShell script to query events, sessions, and competitors
- **query-events-apikey.ps1** - Simple script to list all live events
- **simple-query.ps1** - Basic query tool with usage examples

## Quick Start

### 1. List All Live Events
```powershell
.\query-events-apikey.ps1
```

### 2. Interactive Queries
```powershell
.\query-live-timing.ps1
```

Menu options:
1. List all live events
2. Get event details (requires Event ID)
3. Get session data (requires Event ID and Session ID)
4. Get specific competitor (requires Event ID, Session ID, Competitor ID)
5. Example: Query first available event

## API Configuration

- **Base URL**: `https://lt-api.speedhive.com/api/`
- **API Key**: `XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1`
- **Required Headers**: 
  - `ApiKey` (exact casing required)
  - `User-Agent: speedhive-android/1.68`

## Key API Endpoints

### 1. Get Live Events
```
GET /events
```
Returns array of live events in `LiveEvents` field.

### 2. Get Event Details with Sessions
```
GET /events/{eventId}?sessions=true
```
Returns event info with sessions in `ss` array.

### 3. Get Session Leaderboard
```
GET /events/{eventId}/sessions/{sessionId}/data
```
Returns leaderboard in `l` array (NOT `c` for competitors!).

### 4. Get Competitor Lap Times
```
GET /events/{eventId}/sessions/{sessionId}/competitor/{competitorId}
```
Returns lap-by-lap data in `results` array.

## Important Field Names

The API uses abbreviated field names. Key abbreviations:

### Events & Sessions
- `n` / `nam` - Name
- `s` - Status (100=scheduled, 101=practice, 102=active, 104=completed)
- `f` - Sport/Format code
- `dt` - Date/Time
- `ss` - Sessions array
- `rnNam` - Run Name (e.g., "Day 1", "Heat 1")
- `gNam` - Group Name (class/category)
- `btLpTim` - Best Lap Time

### Session Leaderboard (from `l` array)
- `id` - Competitor ID
- `no` - Number (car/bike number)
- `nam` - Name
- `btTm` - Best Time
- `ls` - Laps completed
- `pos` - Overall position
- `cl` - Class
- `pCl` - Position in class
- `gp` - Gap to competitor IN FRONT (previous position) - can be time or laps
- `df` - Total difference/gap to LEADER (P1) - can be time or laps
- `gpCl` - Gap to competitor in front (class)
- `dfCl` - Gap to class leader

**Gap Behind**: To get the gap behind a competitor, look at the next position's `gp` field.
For example, if you're P2, your gap behind = P3's `gp` value.

**Example**: P2 with gp="2 Laps", df="2 Laps" means 2 laps behind P1. 
P3 with gp="17 Laps", df="19 Laps" means 17 laps behind P2, 19 laps behind P1 total.

### Competitor Lap Data (from `results` array)
- `ls` - Lap number
- `lsTm` - Lap Time
- `pos` - Position at this lap
- `ibt` - Is Best Time (boolean - marks best lap)
- `btTm` - Best Time overall (in last/summary entry)
- `btLp` - Best Lap number (in last/summary entry)
- `nam` - Name (in last/summary entry)
- `no` - Number (in last/summary entry)

## Response Structure Notes

### Session Data
The session endpoint returns a **leaderboard** in the `l` array, not a simple competitors list:
```json
{
  "l": [
    {
      "id": 16506829,
      "no": "88",
      "nam": "MLUNGO",
      "btTm": "1:27.697",
      "ls": 19,
      "pos": 1,
      "cl": "SUPERBIKE"
    }
  ]
}
```

### Competitor Lap Times
The competitor endpoint returns lap-by-lap data where:
- Each entry in `results` array is one lap
- The lap with `ibt: true` is the best lap
- The **last entry** contains summary data with name, number, and totals

```json
{
  "results": [
    { "ls": 1, "lsTm": "1:35.485", "pos": 3, "ibt": false },
    { "ls": 2, "lsTm": "1:31.880", "pos": 3, "ibt": false },
    ...
    { 
      "ls": 19, 
      "lsTm": "1:27.697", 
      "ibt": true,        // ⭐ This is the best lap
      "btTm": "1:27.697",
      "btLp": 19,
      "nam": "MLUNGO",
      "no": "88",
      "pos": 1
    }
  ]
}
```

## Example Workflow

```powershell
# 1. List events to find an active one
$events = Invoke-RestMethod -Uri "https://lt-api.speedhive.com/api/events" -Headers @{
    "ApiKey" = "XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1"
    "User-Agent" = "speedhive-android/1.68"
}

$event = $events.LiveEvents | Where-Object { $_.s -eq 102 } | Select-Object -First 1

# 2. Get sessions for the event
$details = Invoke-RestMethod -Uri "https://lt-api.speedhive.com/api/events/$($event.id)?sessions=true" -Headers @{
    "ApiKey" = "XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1"
    "User-Agent" = "speedhive-android/1.68"
}

$session = $details.ss[0]

# 3. Get session leaderboard
$leaderboard = Invoke-RestMethod -Uri "https://lt-api.speedhive.com/api/events/$($event.id)/sessions/$($session.id)/data" -Headers @{
    "ApiKey" = "XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1"
    "User-Agent" = "speedhive-android/1.68"
}

$leader = $leaderboard.l[0]

# 4. Get leader's lap times
$laps = Invoke-RestMethod -Uri "https://lt-api.speedhive.com/api/events/$($event.id)/sessions/$($session.id)/competitor/$($leader.id)" -Headers @{
    "ApiKey" = "XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1"
    "User-Agent" = "speedhive-android/1.68"
}

$bestLap = $laps.results | Where-Object { $_.ibt -eq $true }
```

## Flutter Implementation Guide

### Recommended Dart Models

```dart
// Event model
class LiveEvent {
  final String id;
  final String name;  // from 'n' field
  final int status;   // from 's' field
  final int format;   // from 'f' field
  final String dateTime;  // from 'dt' field
  final Location location;  // from 'l' object
  final Track track;  // from 't' object
}

// Session model
class Session {
  final String id;
  final String runName;  // from 'rnNam' field
  final String groupName;  // from 'gNam' field
  final String? bestLapTime;  // from 'btLpTim' field
}

// Leaderboard entry model
class LeaderboardEntry {
  final int id;
  final String number;  // from 'no' field
  final String name;  // from 'nam' field
  final String bestTime;  // from 'btTm' field
  final int laps;  // from 'ls' field
  final int position;  // from 'pos' field
  final String classCategory;  // from 'cl' field
}

// Lap data model
class LapData {
  final int lapNumber;  // from 'ls' field
  final String lapTime;  // from 'lsTm' field
  final int position;  // from 'pos' field
  final bool isBest;  // from 'ibt' field
}

// Competitor summary model
class CompetitorSummary {
  final int id;
  final String name;  // from 'nam' field
  final String number;  // from 'no' field
  final String bestTime;  // from 'btTm' field
  final int bestLap;  // from 'btLp' field
  final int totalLaps;  // from 'ls' field
  final int position;  // from 'pos' field
  final List<LapData> laps;
}
```

### API Service Example

```dart
class SpeedHiveApiService {
  static const baseUrl = 'https://lt-api.speedhive.com/api';
  static const apiKey = 'XwUk3LPlUzq9n3LzI8butzLYm6vDFXP1';
  
  final headers = {
    'ApiKey': apiKey,
    'User-Agent': 'speedhive-android/1.68',
  };
  
  Future<List<LiveEvent>> getEvents() async {
    final response = await http.get(
      Uri.parse('$baseUrl/events'),
      headers: headers,
    );
    final data = json.decode(response.body);
    return (data['LiveEvents'] as List)
        .map((e) => LiveEvent.fromJson(e))
        .toList();
  }
  
  Future<List<LeaderboardEntry>> getSessionLeaderboard(
    String eventId, 
    String sessionId
  ) async {
    final response = await http.get(
      Uri.parse('$baseUrl/events/$eventId/sessions/$sessionId/data'),
      headers: headers,
    );
    final data = json.decode(response.body);
    // NOTE: Use 'l' field, not 'c'!
    return (data['l'] as List)
        .map((e) => LeaderboardEntry.fromJson(e))
        .toList();
  }
  
  Future<CompetitorSummary> getCompetitorLaps(
    String eventId,
    String sessionId,
    int competitorId
  ) async {
    final response = await http.get(
      Uri.parse('$baseUrl/events/$eventId/sessions/$sessionId/competitor/$competitorId'),
      headers: headers,
    );
    final data = json.decode(response.body);
    return CompetitorSummary.fromResults(data['results']);
  }
}
```

## Testing

All three main query functions have been tested and verified:

✅ **Get-EventDetails** - Correctly parses `ss` array with `rnNam`, `gNam`, `btLpTim`  
✅ **Get-SessionData** - Correctly parses `l` (leaderboard) array with `nam`, `no`, `btTm`, `ls`  
✅ **Get-CompetitorData** - Correctly parses `results` array with lap-by-lap data and `ibt` flag

## Common Pitfalls

1. **Session data**: Use `l` array (leaderboard), NOT `c` array
2. **Header casing**: Must be `ApiKey`, not `X-Api-Key` or `x-api-key`
3. **Competitor laps**: Last entry in `results` array contains summary data
4. **Best lap flag**: Use `ibt` boolean field, not comparing times
5. **Field names**: API uses abbreviations - `nam` not `name`, `no` not `number`, etc.

## Status

- ✅ APK decompiled successfully
- ✅ API endpoints extracted
- ✅ Authentication working (API key only)
- ✅ All query scripts tested
- ✅ Documentation complete
- ✅ Ready for Flutter integration

## Next Steps

1. Create Dart models based on response structures above
2. Implement HTTP client with proper headers
3. Parse JSON responses using correct field names
4. Build UI to display events, sessions, and lap times
5. Consider implementing SignalR for real-time updates (WebSocket endpoint available)

---
*Last Updated: 2026-02-10*
