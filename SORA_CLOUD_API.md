# Sora Cloud Network API Specification

Sora AI Studio connects to local network Sora Cloud servers over Wi-Fi via mDNS / HTTP REST API.

---

## Service Discovery
- **Protocol**: mDNS / DNS-SD
- **Service Type**: `_soracloud._tcp.local.`
- **Default Port**: `8080` (HTTP) or `8443` (HTTPS)

---

## REST Endpoints

### 1. `GET /api/v1/health`
Returns server hardware status and available GPU compute.

#### Response
```json
{
  "server_id": "box-01",
  "server_name": "Sora Cloud Box Pro",
  "status": "ONLINE",
  "gpu": "NVIDIA RTX 4090",
  "total_ram_gb": 64.0,
  "available_ram_gb": 52.4,
  "active_users": 3,
  "supported_modes": ["FAST", "BALANCED", "CINEMA", "4K_UPSCALE"]
}
```

### 2. `POST /api/v1/jobs/submit`
Submits a video rendering request to the server queue.

#### Request Body
```json
{
  "model_name": "Wan 2.1 Video 1.3B",
  "prompt": "Cinematic shot of solar shuttle orbiting gas giant",
  "mode": "CINEMA",
  "resolution": "1080p",
  "fps": 30,
  "duration_sec": 5
}
```

### 3. `GET /api/v1/jobs/{job_id}/stream`
Server-Sent Events (SSE) stream returning real-time rendering progress updates.
