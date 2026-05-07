import React, { useMemo, useEffect, useState } from "react";
import { MapContainer, Marker, Popup, Polyline, TileLayer, useMapEvents } from "react-leaflet";
import TrafficHeatmap from "./TrafficHeatmap";

function congestionColor(level) {
  if (level === "HIGH") return "#ef4444";
  if (level === "MEDIUM") return "#f59e0b";
  return "#22c55e";
}

function estimatedMinutes(distanceKm, congestionWeight) {
  const baseSpeedKmh = 45;
  return Math.max(1, Math.round(((distanceKm * congestionWeight) / baseSpeedKmh) * 60));
}

export default function TrafficMap({
  cities,
  route,
  congestionEdges,
  sourceCity,
  destinationCity,
  sourcePoint,
  destinationPoint,
  activeEndpoint,
  onEndpointChange,
  onEndpointFocus,
  onEdgeClick,
  heatmapEnabled,
  onHeatmapToggle
}) {
  const cityIndex = useMemo(() => new Map(cities.map((city) => [city.name, city])), [cities]);

  const sourceFallback = cityIndex.get(sourceCity) ?? cityIndex.get("Mumbai") ?? cities[0];
  const destinationFallback = cityIndex.get(destinationCity) ?? cityIndex.get("Pune") ?? cities[1] ?? cities[0];

  function MapClickSetter() {
    useMapEvents({
      click(event) {
        if (!activeEndpoint) return;
        onEndpointChange?.(activeEndpoint, event.latlng.lat, event.latlng.lng);
      }
    });

    return null;
  }

  const renderEndpointMarker = (endpoint, point, fallbackCity, label) => {
    const position = point ? [point.lat, point.lon] : [fallbackCity.lat, fallbackCity.lon];

    return (
      <Marker
        key={endpoint}
        position={position}
        draggable
        eventHandlers={{
          dragend: (event) => {
            const next = event.target.getLatLng();
            onEndpointFocus?.(endpoint);
            onEndpointChange?.(endpoint, next.lat, next.lng);
          }
        }}
      >
        <Popup>
          <div className="space-y-1 text-sm">
            <p className="font-semibold capitalize">{label} endpoint</p>
            <p>
              {position[0].toFixed(4)}, {position[1].toFixed(4)}
            </p>
            <p className="text-xs text-slate-500">Drag the pin or click the map after choosing this endpoint.</p>
          </div>
        </Popup>
      </Marker>
    );
  };

  const routeSegments = useMemo(() => {
    if (!route?.path?.length) return [];

    return route.path
      .map((node, index) => {
        if (index === route.path.length - 1) return null;
        const from = cityIndex.get(node.name);
        const to = cityIndex.get(route.path[index + 1].name);
        if (!from || !to) return null;

        const matchedEdge = congestionEdges.find(
          (edge) => edge.fromNode?.name === from.name && edge.toNode?.name === to.name
        );

        return {
          key: `${from.name}->${to.name}`,
          positions: [
            [from.lat, from.lon],
            [to.lat, to.lon]
          ],
          edge: matchedEdge ?? {
            fromNode: from,
            toNode: to,
            distanceKm: route.totalDistance / Math.max(route.path.length - 1, 1),
            congestionWeight: 1,
            level: "LOW"
          }
        };
      })
      .filter(Boolean);
  }, [route, cityIndex, congestionEdges]);

  const center = [20.5937, 78.9629];
  const [osrmRoute, setOsrmRoute] = useState(null);

  useEffect(() => {
    // Fetch a road-following route from OSRM for the main source->destination pair
    // and render it as a dark base polyline beneath the colored segment polylines.
    async function fetchOsrmRoute() {
      setOsrmRoute(null);
      if (!route?.path?.length) return;

      // Always prioritize pinned endpoints so the rendered path matches user-selected GPS points.
      const start = sourcePoint ?? sourceFallback;
      const end = destinationPoint ?? destinationFallback;

      if (!start || !end) return;

      const startLonLat = `${start.lon},${start.lat}`;
      const endLonLat = `${end.lon},${end.lat}`;

      const url = `https://router.project-osrm.org/route/v1/driving/${startLonLat};${endLonLat}?overview=full&geometries=geojson`;

      try {
        const resp = await fetch(url);
        if (!resp.ok) return;
        const data = await resp.json();
        const coords = data.routes?.[0]?.geometry?.coordinates;
        if (!coords) return;

        // OSRM returns [lon, lat] pairs — convert to [lat, lon]
        const positions = coords.map((c) => [c[1], c[0]]);
        setOsrmRoute(positions);
      } catch (e) {
        // ignore errors silently (fallback to straight segment polylines)
        setOsrmRoute(null);
      }
    }

    fetchOsrmRoute();
  }, [route, sourcePoint, destinationPoint, sourceFallback, destinationFallback]);

  return (
    <div className="relative w-full h-full">
      <MapContainer center={center} zoom={5} scrollWheelZoom className="h-full min-h-[78vh] w-full">
        <MapClickSetter />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <TrafficHeatmap isEnabled={heatmapEnabled} onToggle={onHeatmapToggle} />

      {renderEndpointMarker("source", sourcePoint, sourceFallback, "source")}
      {renderEndpointMarker("destination", destinationPoint, destinationFallback, "destination")}

      {cities.map((city) => (
        <Marker key={city.name} position={[city.lat, city.lon]}>
          <Popup>
            <div className="space-y-1 text-sm">
              <p className="font-semibold">{city.name}</p>
              <p>
                {city.lat.toFixed(4)}, {city.lon.toFixed(4)}
              </p>
            </div>
          </Popup>
        </Marker>
      ))}

      {/* Draw a single road-following OSRM route (dark base) if available */}
      {!heatmapEnabled && osrmRoute && (
        <Polyline
          key="osrm-route"
          positions={osrmRoute}
          pathOptions={{ color: "#0f172a", weight: 6, opacity: 0.95 }}
        />
      )}

      {!heatmapEnabled && !osrmRoute && routeSegments.map((segment) => {
        const color = congestionColor(segment.edge.level);
        const minutes = estimatedMinutes(segment.edge.distanceKm, segment.edge.congestionWeight);

        return (
          <Polyline
            key={segment.key}
            positions={segment.positions}
            pathOptions={{ color, weight: 6, opacity: 0.9 }}
            eventHandlers={{ click: () => onEdgeClick?.(segment.edge) }}
          >
            <Popup>
              <div className="space-y-1 text-sm">
                <p className="font-semibold">
                  {segment.edge.fromNode?.name} → {segment.edge.toNode?.name}
                </p>
                <p>Distance: {segment.edge.distanceKm.toFixed(1)} km</p>
                <p>Estimated time: {minutes} min</p>
                <p>Congestion: {segment.edge.level}</p>
              </div>
            </Popup>
          </Polyline>
        );
      })}
    </MapContainer>

      {/* Heatmap Toggle Button */}
      <button
        onClick={onHeatmapToggle}
        className={`absolute left-4 top-4 z-[1000] flex items-center gap-2 rounded-lg px-4 py-2 font-semibold text-sm transition ${
          heatmapEnabled
            ? "bg-red-500/80 hover:bg-red-600/80 text-white"
            : "bg-white/80 hover:bg-white text-slate-900"
        }`}
      >
        <span className="text-lg">🌡️</span>
        <span>{heatmapEnabled ? "Heatmap ON" : "Heatmap OFF"}</span>
      </button>
    </div>
  );
}
