# Royal Caribs Ship & Offshore Proxy

A two‑component proxy system that funnels all HTTP traffic from a ship through a single persistent TCP connection to an offshore server—minimizing TCP connection counts while preserving request/response fidelity.

## Prerequisites

- Docker & Docker Compose  
- Git  
- (Optional) Java 8 & Maven, if you want to rebuild the JARs locally

## Getting Started

### Clone the repository

git clone https://github.com/<your‑username>/ship-offshore-proxy.git
cd ship-offshore-proxy

Pre-built Images

docker pull jojiseb/offshore-proxy:latest
docker pull jojiseb/ship-proxy:latest

docker compose pull
docker compose up

Verify

curl -v -x http://localhost:8080 http://httpbin.org/get

Limitations

HTTPS (CONNECT) is not supported in this version
