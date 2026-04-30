import React from "react";

const nodePositions = {
  A: { x: 70, y: 90 },
  B: { x: 240, y: 70 },
  C: { x: 210, y: 210 },
  D: { x: 370, y: 140 }
};

function edgeKey(source, destination) {
  return `${source}->${destination}`;
}

export default function GraphView({ routePath, trafficEdges }) {
  const highlighted = new Set();
  for (let i = 0; i < routePath.length - 1; i += 1) {
    highlighted.add(edgeKey(routePath[i], routePath[i + 1]));
  }

  const edgeMap = new Map();
  for (const edge of trafficEdges) {
    edgeMap.set(edgeKey(edge.source, edge.destination), edge);
  }

  return (
    <svg viewBox="0 0 430 280" className="w-full h-auto">
      {Array.from(edgeMap.values()).map((edge) => {
        const from = nodePositions[edge.source];
        const to = nodePositions[edge.destination];
        if (!from || !to) {
          return null;
        }

        const highlightedEdge = highlighted.has(edgeKey(edge.source, edge.destination));
        const color =
          edge.congestion === "HIGH"
            ? "#dc2626"
            : edge.congestion === "MEDIUM"
              ? "#f59e0b"
              : "#0f766e";

        return (
          <g key={edgeKey(edge.source, edge.destination)}>
            <line
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
              stroke={highlightedEdge ? "#111827" : color}
              strokeWidth={highlightedEdge ? 6 : 4}
              strokeDasharray={highlightedEdge ? "10 8" : "0"}
              className={highlightedEdge ? "animate-pulseEdge" : ""}
            />
            <text
              x={(from.x + to.x) / 2}
              y={(from.y + to.y) / 2 - 8}
              fontSize="12"
              textAnchor="middle"
              fill="#1f2937"
            >
              {edge.weight}
            </text>
          </g>
        );
      })}

      {Object.entries(nodePositions).map(([node, pos]) => (
        <g key={node}>
          <circle cx={pos.x} cy={pos.y} r="22" fill="#ffffff" stroke="#0f766e" strokeWidth="4" />
          <text x={pos.x} y={pos.y + 4} textAnchor="middle" fontSize="15" fill="#0f172a" fontWeight="700">
            {node}
          </text>
        </g>
      ))}
    </svg>
  );
}
