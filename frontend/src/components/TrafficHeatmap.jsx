import React, { useEffect, useRef, useState } from "react";
import { useMap, CircleMarker, Popup } from "react-leaflet";
import axios from "axios";

export default function TrafficHeatmap({ isEnabled }) {
  const map = useMap();
  const heatmarkersRef = useRef([]);
  const legendRef = useRef(null);
  const [heatmapData, setHeatmapData] = useState([]);
  const [error, setError] = useState(null);

  // Fetch heatmap data every 30 seconds
  useEffect(() => {
    const fetchHeatmapData = async () => {
      try {
        const response = await axios.get("/api/heatmap");
        if (response.data && Array.isArray(response.data)) {
          setHeatmapData(response.data);
          setError(null);
        }
      } catch (err) {
        console.error("Error fetching heatmap data:", err);
        setError(err.message);
      }
    };

    // Fetch immediately
    fetchHeatmapData();

    // Set up interval for 30 seconds
    const interval = setInterval(fetchHeatmapData, 30000);

    return () => clearInterval(interval);
  }, []);

  // Function to get color based on intensity
  const getHeatmapColor = (intensity) => {
    if (intensity < 0.25) return "#00ff00"; // Green
    if (intensity < 0.5) return "#7fff00"; // Chartreuse
    if (intensity < 0.75) return "#ffff00"; // Yellow
    if (intensity < 0.85) return "#ff8800"; // Orange
    return "#ff0000"; // Red
  };

  // Function to get opacity based on intensity
  const getOpacity = (intensity) => {
    return Math.max(0.3, Math.min(0.9, 0.3 + intensity * 0.6));
  };

  // Update heatmap markers when data or enabled state changes
  useEffect(() => {
    if (!map) return;

    // Remove existing markers
    heatmarkersRef.current.forEach((marker) => {
      if (map.hasLayer(marker)) {
        map.removeLayer(marker);
      }
    });
    heatmarkersRef.current = [];

    // Remove legend
    if (legendRef.current) {
      legendRef.current.remove();
      legendRef.current = null;
    }

    if (isEnabled && heatmapData.length > 0) {
      try {
        // Create circle markers for each heatmap point
        heatmapData.forEach((point) => {
          const color = getHeatmapColor(point.intensity);
          const opacity = getOpacity(point.intensity);
          
          const circle = L.circleMarker([point.lat, point.lon], {
            radius: 8 + point.intensity * 12, // Size based on intensity
            fillColor: color,
            color: color,
            weight: 0,
            opacity: 0,
            fillOpacity: opacity
          })
            .bindPopup(
              `<div style="font-size: 12px;">
                <strong>Traffic Intensity</strong><br/>
                Lat: ${point.lat.toFixed(4)}<br/>
                Lon: ${point.lon.toFixed(4)}<br/>
                Intensity: ${(point.intensity * 100).toFixed(1)}%
              </div>`
            )
            .addTo(map);

          heatmarkersRef.current.push(circle);
        });

        // Add legend
        addLegend();
        setError(null);
      } catch (err) {
        console.error("Error creating heatmap markers:", err);
        setError(err.message);
      }
    }

    return () => {
      // Cleanup markers on unmount
      heatmarkersRef.current.forEach((marker) => {
        if (map && map.hasLayer(marker)) {
          map.removeLayer(marker);
        }
      });
      heatmarkersRef.current = [];
    };
  }, [map, isEnabled, heatmapData]);

  const addLegend = () => {
    if (!map) return;

    // Remove existing legend if present
    if (legendRef.current) {
      legendRef.current.remove();
    }

    const legend = document.createElement("div");
    legend.className = "leaflet-bottom leaflet-right";
    legend.style.zIndex = "1000";
    legend.style.padding = "10px";

    legend.innerHTML = `
      <div style="
        background: white;
        padding: 12px;
        border-radius: 5px;
        box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
        font-family: Arial, sans-serif;
        font-size: 12px;
      ">
        <div style="font-weight: bold; margin-bottom: 8px;">Traffic Intensity</div>
        <div style="display: flex; align-items: center; margin-bottom: 4px;">
          <div style="width: 20px; height: 20px; background: #00ff00; border-radius: 50%; margin-right: 8px;"></div>
          <span>Low (0-25%)</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 4px;">
          <div style="width: 20px; height: 20px; background: #ffff00; border-radius: 50%; margin-right: 8px;"></div>
          <span>Medium (25-75%)</span>
        </div>
        <div style="display: flex; align-items: center; margin-bottom: 4px;">
          <div style="width: 20px; height: 20px; background: #ff8800; border-radius: 50%; margin-right: 8px;"></div>
          <span>High (75-85%)</span>
        </div>
        <div style="display: flex; align-items: center;">
          <div style="width: 20px; height: 20px; background: #ff0000; border-radius: 50%; margin-right: 8px;"></div>
          <span>Critical (85%+)</span>
        </div>
      </div>
    `;

    const legendContainer = document.createElement("div");
    legendContainer.style.position = "absolute";
    legendContainer.style.bottom = "20px";
    legendContainer.style.right = "20px";
    legendContainer.appendChild(legend);

    map._container.appendChild(legendContainer);
    legendRef.current = legendContainer;
  };

  if (error) {
    console.warn("Heatmap component error:", error);
  }

  return null; // This component doesn't render anything visible
}



