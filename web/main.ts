const rawSrc = 'assets/raw_image.jpg';
const edgesSrc = 'assets/edge_image.jpg';

if (!document.getElementById('frame') || !document.getElementById('fps') || !document.getElementById('res') || !document.getElementById('toggleBtn')) {
  console.error('Missing DOM elements (frame/fps/res/toggleBtn)');
} else {
  const frame = document.getElementById('frame') as HTMLImageElement;
  const fpsEl = document.getElementById('fps') as HTMLElement;
  const resEl = document.getElementById('res') as HTMLElement;
  const btn = document.getElementById('toggleBtn') as HTMLButtonElement;

  let mode: 'raw' | 'edges' = 'raw';
  let simulatedFps = 30;

  // sync initial button label
  btn.textContent = 'Mode: Raw';

  // create a small disconnected badge in the viewer (hidden by default)
  const statusBadge = document.createElement('div');
  statusBadge.id = 'ws-status';
  statusBadge.textContent = 'Disconnected';
  statusBadge.style.display = 'none';
  statusBadge.setAttribute('aria-hidden', 'true');
  // place at bottom-right inside the viewer
  statusBadge.style.position = 'absolute';
  statusBadge.style.right = '12px';
  statusBadge.style.bottom = '12px';
  statusBadge.style.padding = '6px 8px';
  statusBadge.style.background = 'rgba(160,30,30,0.9)';
  statusBadge.style.color = '#fff';
  statusBadge.style.fontSize = '12px';
  statusBadge.style.borderRadius = '6px';
  statusBadge.style.zIndex = '30';
  (frame.parentElement || document.body).appendChild(statusBadge);

  const updateFrame = () => {
    frame.src = mode === 'raw' ? rawSrc : edgesSrc;
  };

  const toggleMode = () => {
    mode = mode === 'raw' ? 'edges' : 'raw';
    btn.textContent = mode === 'raw' ? 'Mode: Raw' : 'Mode: Edges';
    updateFrame();
  };

  btn.addEventListener('click', toggleMode);

  // Auto mode checkbox controls whether incoming WS/sim updates can change the displayed mode
  let autoModeEl = document.getElementById('autoMode') as HTMLInputElement | null;
  if (!autoModeEl) {
    // If the checkbox is missing from the served HTML, create it dynamically so the user always has control
    const controlsContainer = document.querySelector('.controls') as HTMLElement | null || null;
    const label = document.createElement('label');
    label.className = 'auto-mode';
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.id = 'autoMode';
    input.checked = false;
    label.appendChild(input);
    label.appendChild(document.createTextNode(' Auto mode'));
    if (controlsContainer) {
      controlsContainer.appendChild(label);
      console.info('Auto mode checkbox created dynamically');
    } else {
      // If no controls container, append into overlay
      const overlay = document.querySelector('.overlay');
      if (overlay) overlay.appendChild(label);
    }
    autoModeEl = input;
  } else {
    autoModeEl.checked = false; // default off so user controls toggling manually
  }

  // simulate FPS updates every second
  const tick = () => {
    simulatedFps = 28 + Math.floor(Math.random() * 6);
    fpsEl.textContent = simulatedFps.toString();
  };

  // static resolution per requirement
  resEl.textContent = '1920x1080';

  updateFrame();
  setInterval(tick, 1000);

  // --- WebSocket mock connection ---
  // Try to open a real WebSocket to ws://localhost:8080. If unavailable, fall back to an in-page simulator.
  let ws: WebSocket | null = null;
  let simInterval: number | null = null;

  const handleFrameData = (payload: { fps: number; resolution: string; mode: string }) => {
    // Update UI from incoming payload
    if (typeof payload.fps === 'number') fpsEl.textContent = payload.fps.toString();
    if (typeof payload.resolution === 'string') resEl.textContent = payload.resolution;
    // Only change the displayed mode if Auto mode is enabled; otherwise, update FPS/res only
    const incomingMode = (payload.mode === 'edges' || payload.mode === 'raw') ? payload.mode as 'raw' | 'edges' : null;
    const autoOn = autoModeEl ? autoModeEl.checked : true; // default to true if no checkbox present
    if (incomingMode && autoOn) {
      mode = incomingMode;
      // sync button text and image
      btn.textContent = mode === 'raw' ? 'Mode: Raw' : 'Mode: Edges';
      updateFrame();
    }
  };

  function startSimulator() {
    // send a simulated message every second
    simInterval = window.setInterval(() => {
      const sample = {
        fps: 28 + Math.floor(Math.random() * 6),
        resolution: '1920x1080',
        mode: Math.random() > 0.5 ? 'edges' : 'raw'
      };
      console.debug('Simulator emits', sample.mode);
      handleFrameData(sample);
    }, 1000);
    // indicate disconnected (simulator runs while disconnected)
    statusBadge.style.display = 'block';
  }

  function stopSimulator() {
    if (simInterval !== null) {
      clearInterval(simInterval);
      simInterval = null;
    }
    statusBadge.style.display = 'none';
  }

  function connectWS() {
    const hostsToTry = [] as string[];
    if (location && location.hostname) hostsToTry.push(location.hostname);
    // Add localhost variants for cases where the WS mock is bound to loopback
    hostsToTry.push('127.0.0.1', 'localhost');

    let tried = 0;

    const tryNextHost = () => {
      if (tried >= hostsToTry.length) {
        console.warn('All WS hosts failed; starting simulator');
        startSimulator();
        statusBadge.style.display = 'block';
        return;
      }

      const host = hostsToTry[tried++];
      const wsUrl = `ws://${host}:8080`;
      console.info('Attempting WebSocket connection to', wsUrl);

      // create socket for this host
      try {
        ws = new WebSocket(wsUrl);
      } catch (err) {
        console.warn('Failed to construct WebSocket for', wsUrl, err);
        // try next host
        tryNextHost();
        return;
      }

      const connTimeout = window.setTimeout(() => {
        if (!ws || ws.readyState !== WebSocket.OPEN) {
          try { ws && ws.close(); } catch (_) {}
          ws = null;
          console.warn('Connection attempt timed out for', wsUrl);
          tryNextHost();
        }
      }, 1000);

      ws.onopen = () => {
        clearTimeout(connTimeout);
        statusBadge.style.display = 'none';
        if (simInterval !== null) stopSimulator();
        console.info('WebSocket connected to', wsUrl);
      };

      ws.onmessage = (ev: MessageEvent) => {
        try {
          const data = JSON.parse(ev.data);
          handleFrameData(data);
        } catch (e) {
          console.warn('Invalid WS payload', ev.data);
        }
      };

      ws.onclose = () => {
        console.warn('WebSocket closed for', wsUrl);
        // try other hosts once; if none, simulator will start
        tryNextHost();
      };

      ws.onerror = (ev) => {
        console.error('WebSocket error for', wsUrl, ev);
        try { ws && ws.close(); } catch (_) {}
        ws = null;
        tryNextHost();
      };
    };

    // start trying hosts
    tryNextHost();
  }

  // attempt connection; if no server is listening, the error path will run and simulator will start
  connectWS();
}

export {};
