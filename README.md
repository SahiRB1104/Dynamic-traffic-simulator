# Dynamic Traffic Routing Simulator

Production-style full-stack traffic simulator with:

- Java 21 + Spring Boot backend
- React + Tailwind frontend
- Dijkstra and A* route computation
- Live traffic updates over Server-Sent Events (SSE)

## Project Structure

```
|- backend/
|  |- src/main/java/com/traffic/simulator/
|  |  |- controller/
|  |  |- model/
|  |  |- service/
|  |  \- SimulatorApplication.java
|  |- src/main/resources/
|  |  \- application.properties
|  |- src/test/java/com/traffic/simulator/
|  |  |- service/
|  |  \- SimulatorApplicationTests.java
|  |- pom.xml
|  \- mvnw.cmd
\- frontend/
	|- src/
	|- package.json
	\- vite.config.js
```

## Requirements

- Java 21
- Maven Wrapper (included)
- Node.js 18+

If Maven reports Java 17 while Java 21 is installed, set `JAVA_HOME` to your JDK 21 path before running Maven commands.

## Run Backend

From `backend` folder:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Backend base URL: `http://localhost:8080`

## Run Frontend

From `frontend` folder:

```powershell
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

`vite.config.js` proxies `/api` requests to the backend.

## API Endpoints

- `GET /api/route?source=A&destination=D&algorithm=DIJKSTRA`
- `GET /api/route?source=A&destination=D&algorithm=ASTAR`
- `GET /api/nodes`
- `GET /api/traffic`
- `GET /api/traffic/stream` (SSE stream)
- `GET /api/health`

Sample expected baseline result for `A -> D`:

```json
{
	"distance": 7,
	"path": ["A", "C", "B", "D"],
	"algorithm": "DIJKSTRA"
}
```

## Tests

Run backend tests:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
.\mvnw.cmd test
```

Current coverage includes:

- Route correctness for Dijkstra and A*
- Traffic state emission model
- Spring context startup
