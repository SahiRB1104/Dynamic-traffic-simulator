import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import TrafficMap from "./components/TrafficMap";
import { indiaCities } from "./data/indiaCities";

const algorithms = [
  { label: "Dijkstra", value: "DIJKSTRA" },
  { label: "A*", value: "ASTAR" }
];

const initialForm = {
  source: "Mumbai",
  destination: "Pune",
  algorithm: "DIJKSTRA"
};

const initialPoints = {
  source: null,
  destination: null
};

function estimateTime(distanceKm, congestionWeight) {
  const baseSpeedKmh = 45;
  return Math.max(1, Math.round(((distanceKm * congestionWeight) / baseSpeedKmh) * 60));
}

export default function App() {
  const [form, setForm] = useState(initialForm);
  const [points, setPoints] = useState(initialPoints);
  const [activeEndpoint, setActiveEndpoint] = useState("source");
  const [route, setRoute] = useState(null);
  const [congestionEdges, setCongestionEdges] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [status, setStatus] = useState("Connecting live traffic stream...");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let stream;

    const connect = () => {
      stream = new EventSource("http://localhost:8080/api/stream/traffic");

      stream.addEventListener("traffic-init", (event) => {
        const payload = JSON.parse(event.data);
        setCongestionEdges(payload);
        setLastUpdated(new Date());
        setStatus("Live traffic connected");
      });

      stream.addEventListener("traffic-update", (event) => {
        const payload = JSON.parse(event.data);
        setCongestionEdges(payload);
        setLastUpdated(new Date());
        setStatus("Traffic refreshed");
      });

      stream.onerror = () => {
        setStatus("Traffic stream disconnected. Reconnecting...");
      };
    };

    connect();
    return () => stream?.close();
  }, []);

  const sortedCities = useMemo(() => [...indiaCities].sort((a, b) => a.name.localeCompare(b.name)), []);
  const cityByName = useMemo(() => new Map(indiaCities.map((city) => [city.name, city])), []);

  const nearestCityForPoint = (latitude, longitude) => {
    let nearest = sortedCities[0];
    let nearestDistance = Number.POSITIVE_INFINITY;

    sortedCities.forEach((city) => {
      const latDiff = latitude - city.lat;
      const lonDiff = longitude - city.lon;
      const distance = Math.hypot(latDiff, lonDiff);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = city;
      }
    });

    return nearest;
  };

  const updateCityEndpoint = (endpoint, cityName) => {
    setForm((current) => ({ ...current, [endpoint]: cityName }));
    setPoints((current) => ({
      ...current,
      [endpoint]: null
    }));
  };

  const setEndpointFromCoordinates = (endpoint, latitude, longitude) => {
    const nearestCity = nearestCityForPoint(latitude, longitude);
    setForm((current) => ({ ...current, [endpoint]: nearestCity.name }));
    setPoints((current) => ({
      ...current,
      [endpoint]: { lat: latitude, lon: longitude }
    }));
  };

  const requestCurrentLocation = (endpoint) => {
    if (!navigator.geolocation) {
      setError("Geolocation is not supported in this browser");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setError("");
        setActiveEndpoint(endpoint);
        setEndpointFromCoordinates(endpoint, position.coords.latitude, position.coords.longitude);
        setStatus(`${endpoint === "source" ? "Source" : "Destination"} set from current location`);
      },
      (geoError) => {
        setError(geoError.message || "Unable to get current location");
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    );
  };

  const findRoute = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const sourcePoint = points.source;
      const destinationPoint = points.destination;

      const response = await axios.post("http://localhost:8080/api/route", {
        source: form.source,
        sourceLatitude: sourcePoint?.lat,
        sourceLongitude: sourcePoint?.lon,
        destination: form.destination,
        destinationLatitude: destinationPoint?.lat,
        destinationLongitude: destinationPoint?.lon,
        algorithm: form.algorithm
      });
      setRoute(response.data);
    } catch (err) {
      setError(err.response?.data?.error ?? err.response?.data?.message ?? "Route lookup failed");
      setRoute(null);
    } finally {
      setLoading(false);
    }
  };

  const routeTime = route ? estimateTime(route.totalDistance, 1) : null;

  return (
    <div className="relative min-h-screen overflow-hidden text-slate-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(56,189,248,0.18),_transparent_35%),radial-gradient(circle_at_80%_20%,_rgba(251,191,36,0.15),_transparent_30%)]" />

      <div className="relative flex min-h-screen flex-col lg:flex-row">
        <aside className="z-10 w-full border-b border-white/10 bg-slate-950/85 p-5 backdrop-blur-xl lg:min-h-screen lg:w-[360px] lg:border-b-0 lg:border-r lg:p-6">
          <div className="mb-6">
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-cyan-300">Dynamic Traffic Routing</p>
            <h1 className="mt-2 text-3xl font-black tracking-tight text-white">India Route Planner</h1>
            <p className="mt-3 text-sm leading-6 text-slate-300">
              Find the fastest route, inspect congestion, and watch live traffic updates from the backend.
            </p>
          </div>

          <form onSubmit={findRoute} className="space-y-4 rounded-3xl border border-white/10 bg-white/5 p-4 shadow-2xl shadow-slate-950/40">
            <div className="grid grid-cols-2 gap-2 rounded-2xl border border-white/10 bg-slate-950/40 p-2 text-xs font-semibold uppercase tracking-[0.2em] text-slate-300">
              <button
                type="button"
                onClick={() => setActiveEndpoint("source")}
                className={`rounded-xl px-3 py-2 transition ${activeEndpoint === "source" ? "bg-cyan-400 text-slate-950" : "bg-white/0 hover:bg-white/5"}`}
              >
                Edit source
              </button>
              <button
                type="button"
                onClick={() => setActiveEndpoint("destination")}
                className={`rounded-xl px-3 py-2 transition ${activeEndpoint === "destination" ? "bg-cyan-400 text-slate-950" : "bg-white/0 hover:bg-white/5"}`}
              >
                Edit destination
              </button>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-200">Source city</label>
              <select
                value={form.source}
                onChange={(e) => updateCityEndpoint("source", e.target.value)}
                className="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-sm text-white outline-none ring-0 transition focus:border-cyan-400"
              >
                {sortedCities.map((city) => (
                  <option key={city.name} value={city.name}>
                    {city.name}
                  </option>
                ))}
              </select>
              <div className="mt-2 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => requestCurrentLocation("source")}
                  className="rounded-full border border-cyan-300/30 bg-cyan-400/10 px-3 py-1 text-xs font-semibold text-cyan-200 transition hover:bg-cyan-400/20"
                >
                  Use current location
                </button>
                <button
                  type="button"
                  onClick={() => setActiveEndpoint("source")}
                  className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold text-slate-200 transition hover:bg-white/10"
                >
                  Set on map
                </button>
              </div>
              <p className="mt-2 text-xs text-slate-400">
                {points.source
                  ? `Pinned at ${points.source.lat.toFixed(4)}, ${points.source.lon.toFixed(4)}`
                  : `Using city default: ${cityByName.get(form.source)?.name ?? form.source}`}
              </p>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-200">Destination city</label>
              <select
                value={form.destination}
                onChange={(e) => updateCityEndpoint("destination", e.target.value)}
                className="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-400"
              >
                {sortedCities.map((city) => (
                  <option key={city.name} value={city.name}>
                    {city.name}
                  </option>
                ))}
              </select>
              <div className="mt-2 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => requestCurrentLocation("destination")}
                  className="rounded-full border border-cyan-300/30 bg-cyan-400/10 px-3 py-1 text-xs font-semibold text-cyan-200 transition hover:bg-cyan-400/20"
                >
                  Use current location
                </button>
                <button
                  type="button"
                  onClick={() => setActiveEndpoint("destination")}
                  className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold text-slate-200 transition hover:bg-white/10"
                >
                  Set on map
                </button>
              </div>
              <p className="mt-2 text-xs text-slate-400">
                {points.destination
                  ? `Pinned at ${points.destination.lat.toFixed(4)}, ${points.destination.lon.toFixed(4)}`
                  : `Using city default: ${cityByName.get(form.destination)?.name ?? form.destination}`}
              </p>
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-200">Algorithm</label>
              <select
                value={form.algorithm}
                onChange={(e) => setForm({ ...form, algorithm: e.target.value })}
                className="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-400"
              >
                {algorithms.map((algorithm) => (
                  <option key={algorithm.value} value={algorithm.value}>
                    {algorithm.label}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-2xl bg-cyan-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? "Finding route..." : "Find Best Route"}
            </button>
          </form>

          {error && <div className="mt-4 rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}

          {route && (
            <div className="mt-4 rounded-3xl border border-emerald-400/20 bg-emerald-400/10 p-4 text-sm text-emerald-100">
              <p className="text-xs uppercase tracking-[0.25em] text-emerald-200">Route found</p>
              <p className="mt-2 text-lg font-bold text-white">{route.path.map((node) => node.name).join(" → ")}</p>
              <div className="mt-3 space-y-1 text-slate-200">
                <p><span className="font-semibold text-white">Distance:</span> {route.totalDistance.toFixed(1)} km</p>
                <p><span className="font-semibold text-white">Estimated time:</span> {routeTime} min</p>
                <p><span className="font-semibold text-white">Algorithm:</span> {form.algorithm}</p>
              </div>
            </div>
          )}

          <div className="mt-4 rounded-3xl border border-white/10 bg-white/5 p-4 text-sm text-slate-300">
            <p className="font-semibold text-white">Live Status</p>
            <p className="mt-1">{status}</p>
            <p className="mt-2 text-xs text-slate-400">Backend: http://localhost:8080</p>
          </div>
        </aside>

        <section className="relative flex-1 p-4 lg:p-6">
          <div className="absolute right-4 top-4 z-20 rounded-full border border-white/10 bg-slate-950/85 px-4 py-2 text-xs text-slate-200 shadow-lg backdrop-blur-md lg:right-6 lg:top-6">
            Last updated: {lastUpdated ? lastUpdated.toLocaleString() : "waiting for traffic stream..."}
          </div>

          <div className="h-[calc(100vh-2rem)] overflow-hidden rounded-[2rem] border border-white/10 bg-slate-900/50 p-2 shadow-2xl shadow-slate-950/40 lg:h-[calc(100vh-3rem)]">
            <TrafficMap
              cities={indiaCities}
              route={route}
              congestionEdges={congestionEdges}
              sourceCity={form.source}
              destinationCity={form.destination}
              sourcePoint={points.source}
              destinationPoint={points.destination}
              activeEndpoint={activeEndpoint}
              onEndpointChange={setEndpointFromCoordinates}
              onEndpointFocus={setActiveEndpoint}
            />
          </div>
        </section>
      </div>
    </div>
  );
}
