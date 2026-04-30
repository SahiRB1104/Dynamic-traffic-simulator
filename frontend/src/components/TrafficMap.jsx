import React, { useMemo } from "react";
import { MapContainer, Marker, Popup, Polyline, TileLayer } from "react-leaflet";

function congestionColor(level) {
  if (level === "HIGH") return "#ef4444";
  if (level === "MEDIUM") return "#f59e0b";
  return "#22c55e";
}

function estimatedMinutes(distanceKm, congestionWeight) {
  const baseSpeedKmh = 45;
  return Math.max(1, Math.round(((distanceKm * congestionWeight) / baseSpeedKmh) * 60));
}

export default function TrafficMap({ cities, route, congestionEdges, onEdgeClick }) {
  const cityIndex = useMemo(() => new Map(cities.map((city) => [city.name, city])), [cities]);

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

  return (
    <MapContainer center={center} zoom={5} scrollWheelZoom className="h-full min-h-[78vh] w-full">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

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

      {routeSegments.map((segment) => {
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
  );
}
