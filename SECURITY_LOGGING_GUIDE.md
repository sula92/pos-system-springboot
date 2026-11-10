# Simple Login + Log File Structure

This project now includes:

- Basic login using Spring Security
- Request-aware JSON logs in `logs/pos-system.log`
- A structure that is easy to ship to ELK later

## 1) Login

Configure credentials with environment variables:

- `POS_APP_USER` (default: `admin`)
- `POS_APP_PASSWORD` (default: `admin123`)

Protected endpoints require authentication (for example: `/customer`).

Public endpoints:

- `/`
- `/error`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## 2) Log file structure

Log configuration is in `src/main/resources/logback-spring.xml`.

Main log file:

- `logs/pos-system.log`

Archive pattern:

- `logs/archive/pos-system.YYYY-MM-DD.i.log.gz`

Each log line is JSON-like (single line) with fields:

- `timestamp`
- `level`
- `service`
- `thread`
- `logger`
- `requestId`
- `user`
- `method`
- `path`
- `remoteIp`
- `message`

Example line:

```json
{"timestamp":"2026-10-24T10:00:00.123+05:30","level":"INFO","service":"pos-system","thread":"http-nio-8080-exec-1","logger":"com.idet.pos.controller.CustomerController","requestId":"8b6e4f91-ec90-4e68-a4bf-4f5274cc4c5f","user":"admin","method":"GET","path":"/pos-system/customer","remoteIp":"127.0.0.1","message":"Fetching all customers"}
```

## 3) ELK next step

You can ingest `logs/pos-system.log` with Filebeat and parse each line as JSON.

Suggested Filebeat input type:

- `type: filestream`
- `paths: ["logs/pos-system.log"]`
- `json.keys_under_root: true`
- `json.add_error_key: true`

