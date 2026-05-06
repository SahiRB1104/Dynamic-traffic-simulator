import React, { useEffect, useMemo, useRef, useState } from "react";

function formatCoordinate(value) {
  return Number.parseFloat(value).toFixed(3);
}

export default function SearchBox({
  label,
  value,
  selected,
  onChange,
  onSelect,
  onSetCurrentLocation,
  onSetOnMap
}) {
  const [results, setResults] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const blurTimer = useRef(null);

  const query = value.trim();
  const selectedLabel = selected?.label?.trim() ?? "";
  const isSelectedQuery = Boolean(selectedLabel) && selectedLabel === query;

  useEffect(() => {
    if (blurTimer.current) {
      clearTimeout(blurTimer.current);
      blurTimer.current = null;
    }

    if (!query) {
      setResults([]);
      setLoading(false);
      setError("");
      setIsOpen(false);
      return undefined;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(
          `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&countrycodes=in`,
          {
            signal: controller.signal,
            headers: {
              Accept: "application/json"
            }
          }
        );

        if (!response.ok) {
          throw new Error("Location search failed");
        }

        const data = await response.json();

        if (controller.signal.aborted) {
          return;
        }

        const nextResults = Array.isArray(data)
          ? data.map((item) => ({
              label: item.display_name,
              lat: Number.parseFloat(item.lat),
              lon: Number.parseFloat(item.lon),
              raw: item
            }))
          : [];

        setResults(nextResults);
        setIsOpen(nextResults.length > 0 && !isSelectedQuery);
      } catch (fetchError) {
        if (!controller.signal.aborted) {
          setResults([]);
          setError("Unable to load address suggestions");
          setIsOpen(false);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }, 300);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query, isSelectedQuery]);

  const coordinateLabel = useMemo(() => {
    if (!selected) {
      return "Start typing an address, then pick a result";
    }

    return `📍 ${formatCoordinate(selected.lat)}, ${formatCoordinate(selected.lon)}`;
  }, [selected]);

  const chooseResult = (result) => {
    onChange(result.label);
    onSelect({
      label: result.label,
      lat: result.lat,
      lon: result.lon
    });
    setIsOpen(false);
    setResults([]);
    setError("");
  };

  return (
    <div className="relative">
      <div className="relative">
        <input
          value={value}
          onChange={(event) => {
            onChange(event.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(results.length > 0 && !isSelectedQuery)}
          onBlur={() => {
            blurTimer.current = setTimeout(() => setIsOpen(false), 120);
          }}
          placeholder={label}
          className="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 pr-20 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-cyan-400"
        />

        <div className="pointer-events-none absolute inset-y-0 right-3 flex items-center gap-2 text-xs text-slate-400">
          {loading && <span>Searching</span>}
          {!loading && <span>OpenStreetMap</span>}
        </div>
      </div>

      <div className="mt-2 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={onSetCurrentLocation}
          className="rounded-full border border-cyan-300/30 bg-cyan-400/10 px-3 py-1 text-xs font-semibold text-cyan-200 transition hover:bg-cyan-400/20"
        >
          Use current location
        </button>
        <button
          type="button"
          onClick={onSetOnMap}
          className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold text-slate-200 transition hover:bg-white/10"
        >
          Set on map
        </button>
      </div>

      <p className="mt-2 text-xs text-slate-400">{coordinateLabel}</p>
      {error && <p className="mt-1 text-xs text-rose-300">{error}</p>}

      {isOpen && results.length > 0 && (
        <div className="absolute z-30 mt-2 max-h-80 w-full overflow-hidden rounded-2xl border border-white/10 bg-slate-950 shadow-2xl shadow-slate-950/50">
          {results.map((result) => (
            <button
              key={`${result.label}-${result.lat}-${result.lon}`}
              type="button"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => chooseResult(result)}
              className="block w-full border-b border-white/5 px-4 py-3 text-left transition last:border-b-0 hover:bg-white/5"
            >
              <div className="text-sm font-medium text-white">{result.label}</div>
              <div className="mt-1 text-xs text-slate-400">
                {formatCoordinate(result.lat)}, {formatCoordinate(result.lon)}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
