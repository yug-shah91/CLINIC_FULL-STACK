import { useEffect, useState } from 'react';

// Runs an async function when the component mounts (and whenever `deps` change)
// and tracks { data, loading, error }. Call reload() to fetch again.
//
//   const { data, loading, error, reload } = useFetch(() => appointmentApi.mine(), []);
export default function useFetch(fetcher, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let ignore = false; // stops an old, slow request from overwriting a newer one
    setState((s) => ({ ...s, loading: true, error: null })); // keep old data visible while loading

    fetcher()
      .then((data) => !ignore && setState({ data, loading: false, error: null }))
      .catch((error) => !ignore && setState({ data: null, loading: false, error }));

    return () => {
      ignore = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { ...state, reload: () => setTick((t) => t + 1) };
}
