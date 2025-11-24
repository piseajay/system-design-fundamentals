# HAProxy Load Balancing Demo (Java + Spring Boot)

This example shows HTTP layer (L7) load balancing with **HAProxy** distributing traffic across two Java Spring Boot instances running the same application on different ports. Use it to learn fundamentals: why load balancing matters, how algorithms work, and how health checks enable resilience.

## 1. Why Load Balancing?
Load balancing spreads requests across multiple backend instances to:
- Increase capacity (horizontal scalability)
- Improve reliability (one instance can fail, others serve traffic)
- Reduce latency (avoid overloading a single node)
- Enable rolling upgrades (take instances out of rotation safely)

## 2. Layer 4 vs Layer 7
- Layer 4 (TCP) balances raw connections (faster, protocol‑agnostic).
- Layer 7 (HTTP) understands requests: can route by headers, paths, cookies.
This demo uses L7 (HTTP) features: health checks and HTTP response header injection.

## 3. HAProxy Core Concepts
| Concept | In This Demo |
|---------|--------------|
| `frontend` | Accepts inbound traffic on port 8080 |
| `backend`  | Pool of servers (`app1`, `app2`) |
| `server`   | Each Spring Boot container with its host:port |
| Health check | `GET /actuator/health` endpoint |
| Algorithm | `balance roundrobin` rotates requests evenly |
| Header injection | Adds `X-Served-By` with chosen server name |

## 4. Load Balancing Algorithms (HAProxy supports many)
- `roundrobin`: Cycles through servers evenly (used here).
- `leastconn`: Chooses server with fewest active connections (good for long requests).
- `source`: Hash of client IP gives sticky routing (basic session affinity without cookies).
- `random`, `first`, `uri`, `hdr(<name>)`, etc. for more advanced routing.
Switch algorithms by changing the `balance` line.

## 5. Health Checks
HAProxy calls `/actuator/health`; if it fails, server is marked DOWN and removed from rotation until healthy again. This prevents sending user traffic to a broken instance.

## 6. Application Behavior
Each request returns JSON containing:
- `instanceId`: Set via environment (`app1` / `app2`).
- `requestNumber`: Incrementing counter local to that instance.
- `hostname`: Container hostname (useful when scaling beyond two).

Sample response:
```json
{
  "instanceId": "app1",
  "requestNumber": 7,
  "hostname": "app1"
}
```
Repeated `curl` calls to HAProxy on port 8080 will alternate instance IDs.

## 7. Repository Overview
```
load-balancer/
  docker-compose.yml      # Orchestrates two app instances + HAProxy
  haproxy/haproxy.cfg     # HAProxy configuration
  java-app/               # Single Spring Boot source reused by both instances
    pom.xml
    Dockerfile            # Multi-stage build
    src/main/java/...     # Application & controller
    src/main/resources/application.properties
```

## 8. How It Works End-to-End
1. Browser/CLI sends HTTP request to `localhost:8080`.
2. HAProxy (frontend) receives it and selects backend server using round-robin.
3. HAProxy forwards request to either `app1:8081` or `app2:8082`.
4. Server responds with JSON; HAProxy adds `X-Served-By` header.
5. Client receives response; successive requests show alternating instance IDs.

## 9. Running the Demo
Prerequisites: Docker + Docker Compose installed (Mac: Docker Desktop).

Build & start:
```bash
cd load-balancer
docker compose up --build -d
```

Watch logs:
```bash
docker compose logs -f haproxy
docker compose logs -f app1
```

Test round-robin:
```bash
curl -s localhost:8080 | jq .
curl -s localhost:8080 | jq .
```
(If `jq` not installed, just `curl -s localhost:8080`.)

Check health of each instance directly:
```bash
curl -s localhost:8080/actuator/health            # through HAProxy (picked server)
curl -s $(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' app1):8081/actuator/health
```

Stop one instance and observe failover:
```bash
docker stop app1
for i in {1..5}; do curl -s localhost:8080 | jq .; done
```
All responses should now report `instanceId` = `app2`.

Bring it back:
```bash
docker start app1
```

## 10. Changing the Algorithm
Edit `haproxy/haproxy.cfg`:
```
backend app-backend
    balance leastconn
```
Then reload:
```bash
docker compose restart haproxy
```

## 11. Scaling Further
Add more identical instances (e.g., `app3`) or use Docker Compose scaling (same image, different ports). For a dynamic environment (Kubernetes, ECS), service discovery would auto-register instances instead of static entries.

## 12. Session Affinity (Sticky Sessions)
If your app stores user session in memory, you want the same user to hit the same backend. HAProxy options:
- `balance source` (hash client IP)
- Cookie-based persistence: add `cookie` directives and per-server `cookie` values.
Prefer stateless design or external session store (Redis) to avoid stickiness constraints.

## 13. Observability Enhancements (Ideas)
- Enable HAProxy stats page.
- Configure Prometheus + Grafana for metrics.
- Add distributed tracing (OpenTelemetry) between instances.

## 14. Failover vs Load Balancing
Load balancing spreads traffic constantly; failover kicks in when instance fails. HAProxy marries both: health checks exclude failed nodes while continuing distribution across healthy nodes.

## 15. Security Considerations
- Terminate TLS at HAProxy (bind with `ssl crt /path`).
- Rate limiting / ACLs.
- Hide internal headers; sanitize incoming ones (avoid spoofed `X-Forwarded-For`).

## 16. Next Steps for Learning
- Try `leastconn` vs `roundrobin` under artificial load.
- Add third instance and watch rotation.
- Introduce an artificial delay in one instance; observe behavior.
- Implement circuit breaking (at app layer) combined with HAProxy health checks.

## 17. Cleanup
```bash
docker compose down --remove-orphans --volumes
```

---
Happy scaling! Tweak and experiment to internalize how a load balancer improves resilience and performance.
