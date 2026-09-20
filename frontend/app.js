
//   - the ESP32 device  -> WRITES readings   (POST /api/sensors)
//   - the user          -> READS  readings   (GET  /api/sensors[/{deviceId}])

const API_BASE = "http://localhost:8082/api/sensors";
const STEP_MS = 650;     
const LINGER_MS = 1200;  
const MAX_LOG = 15;

const $ = (id) => document.getElementById(id);
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const esc = (s) =>
  String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));


function setOnline(ok) {
  const pill = $("svcPill");
  pill.className = "pill " + (ok ? "ok" : "bad");
  pill.textContent = ok ? "service online" : "service offline";
}


function pretty(text) {
  try {
    const s = JSON.stringify(JSON.parse(text), null, 2);
    return s.length > 1500 ? s.slice(0, 1500) + "\n… (truncated)" : s;
  } catch {
    return text;
  }
}

function addLog(e) {
  const log = $("callLog");
  log.querySelector(".empty")?.remove();
  log.querySelectorAll("details[open]").forEach((d) => d.removeAttribute("open"));

  const ok = typeof e.status === "number" && e.status < 400;
  const d = document.createElement("details");
  d.open = true;
  d.innerHTML = `
    <summary>
      <span class="t">${new Date().toLocaleTimeString([], { hour12: false })}</span>
      <span class="who ${e.actor === "ESP32" ? "sensor" : "user"}">${esc(e.actor)}</span>
      <span class="req"><b>${esc(e.method)}</b> ${esc(e.url.replace("http://localhost:8082", ""))}</span>
      <span class="st ${ok ? "ok" : "bad"}">${esc(e.status)}</span>
      <span class="ms">${esc(e.ms)} ms</span>
    </summary>
    <div class="bodies">
      ${e.reqBody ? `<div><h4>Request body</h4><pre>${esc(e.reqBody)}</pre></div>` : ""}
      <div><h4>Response</h4><pre>${esc(e.resBody)}</pre></div>
    </div>`;
  log.prepend(d);
  while (log.children.length > MAX_LOG) log.lastElementChild.remove();
}

$("clearLogBtn").addEventListener("click", () => {
  $("callLog").innerHTML = `<p class="empty">No calls yet.</p>`;
});


async function callService(actor, method, url, payload) {
  const t0 = performance.now();
  const entry = { actor, method, url, reqBody: payload ? JSON.stringify(payload, null, 2) : null };
  try {
    const res = await fetch(url, {
      method,
      headers: payload ? { "Content-Type": "application/json" } : undefined,
      body: payload ? JSON.stringify(payload) : undefined,
    });
    const text = await res.text();
    entry.status = res.status;
    entry.ms = Math.round(performance.now() - t0);
    entry.resBody = pretty(text);
    addLog(entry);
    setOnline(true);
    let json = null;
    try { json = JSON.parse(text); } catch { /* not JSON */ }
    return { ok: res.ok, status: res.status, ms: entry.ms, json };
  } catch (err) {
    entry.status = "ERR";
    entry.ms = Math.round(performance.now() - t0);
    entry.resBody = err.message + "\n(Is the REST service running on port 8082?)";
    addLog(entry);
    setOnline(false);
    return { ok: false, status: 0, ms: entry.ms, error: err };
  }
}

const FLOWS = {
  write: {
    steps: [
      { ids: ["nodeEsp", "linkW1"], text: (c) => `① ESP32 sends <b>${esc(c.label)}</b> with a JSON reading. It only knows the service, not the database.` },
      { ids: ["nodeSvc", "linkW2", "nodeDb"], text: () => `② The service validates the reading and <b>INSERTs</b> it into H2. Only the service knows the table.` },
    ],
    done: (c, r) => `③ Stored ✓ The service answered <b>${r.status}</b> in <b>${r.ms} ms</b>.`,
    all: ["nodeEsp", "linkW1", "nodeSvc", "linkW2", "nodeDb"],
  },
  read: {
    steps: [
      { ids: ["nodeUser", "linkR1"], text: (c) => `① User asks the service: <b>${esc(c.label)}</b>. No SQL, no database access.` },
      { ids: ["nodeSvc", "linkR2", "nodeDb"], text: () => `② The service runs a <b>SELECT</b> on H2 and reads the latest rows.` },
    ],
    done: (c, r) => `③ <b>${c.rows}</b> row(s) came back from the database in <b>${r.ms} ms</b>, through the service.`,
    all: ["nodeUser", "linkR1", "nodeSvc", "linkR2", "nodeDb"],
  },
};

let flowBusy = false;

function clearFlow() {
  document.querySelectorAll(".node, .link").forEach((n) => n.classList.remove("active", "done", "fail"));
}
function mark(ids, cls) {
  ids.forEach((id) => $(id).classList.add(cls));
}
function caption(html, cls = "") {
  const c = $("caption");
  c.className = "caption " + cls;
  c.innerHTML = `<span>${html}</span>`; // one flex child, so inline spacing around <b> is preserved
}


async function animate(kind, resultPromise, ctx, onDone) {
  if (flowBusy || !$("animToggle").checked) {
    const r = await resultPromise;
    if (!r.ok) caption(`Call failed (${r.status || "no response"}). Is the REST service running on port 8082?`, "fail");
    else caption(FLOWS[kind].done({ ...ctx, rows: r.json?.length }, r), "ok");
    onDone(r);
    return;
  }

  flowBusy = true;
  const flow = FLOWS[kind];
  clearFlow();

  for (let i = 0; i < flow.steps.length; i++) {
    if (i > 0) mark(flow.steps[i - 1].ids, "done");
    mark(flow.steps[i].ids, "active");
    caption(flow.steps[i].text(ctx));
    await wait(STEP_MS);
    if (i === 0) {
      const r = await resultPromise; // usually already resolved by now
      if (!r.ok) {
        clearFlow();
        mark(flow.steps[0].ids, "fail");
        caption(`Call failed (${r.status || "no response"}). Is the REST service running on port 8082?`, "fail");
        onDone(r);
        await wait(LINGER_MS);
        clearFlow();
        flowBusy = false;
        return;
      }
    }
  }

  const r = await resultPromise;
  clearFlow();
  mark(flow.all, "done");
  caption(flow.done({ ...ctx, rows: r.json?.length }, r), "ok");
  onDone(r);
  await wait(LINGER_MS);
  clearFlow();
  flowBusy = false;
}

// ---------- ESP32 side: send a reading ----------
function drift(id, step, min, max) {
  const el = $(id);
  const v = parseFloat(el.value) + (Math.random() * 2 - 1) * step;
  el.value = Math.min(max, Math.max(min, v)).toFixed(1);
}

async function sendReading(auto = false) {
  const btn = $("sendBtn");
  const status = $("sendStatus");

  if (auto) {
    drift("humidity", 1.5, 0, 100);
    drift("pressure", 0.6, 900, 1100);
    drift("moisture", 2, 0, 100);
  }

  const payload = {
    deviceId: $("deviceId").value.trim(),
    humidity: parseFloat($("humidity").value),
    pressure: parseFloat($("pressure").value),
    moisture: parseFloat($("moisture").value),
  };

  if (!payload.deviceId || [payload.humidity, payload.pressure, payload.moisture].some(Number.isNaN)) {
    status.className = "status error";
    status.textContent = "Enter a device ID and three numeric values.";
    return;
  }

  btn.disabled = true;
  btn.textContent = "Sending...";
  status.className = "status";

  const call = callService("ESP32", "POST", API_BASE, payload);
  await animate("write", call, { label: "POST /api/sensors" }, (r) => {
    status.className = "status " + (r.ok ? "success" : "error");
    status.textContent = r.ok
      ? `✓ Stored for ${payload.deviceId} (service replied ${r.status} in ${r.ms} ms)`
      : `Error: ${r.error ? r.error.message : "service returned " + r.status}`;
  });

  btn.disabled = false;
  btn.textContent = "Send Sensor Data";
}

$("sensorForm").addEventListener("submit", (e) => {
  e.preventDefault();
  sendReading(false);
});

let streamTimer = null;
$("streamToggle").addEventListener("change", (e) => {
  clearInterval(streamTimer);
  if (e.target.checked) {
    sendReading(true);
    streamTimer = setInterval(() => sendReading(true), 3000);
  }
});

// ---------- User side: fetch readings ----------
let seen = new Set();
let lastQuery = null;

function fmtTime(iso) {
  const d = new Date(iso);
  if (!iso || isNaN(d)) return esc(iso ?? "");
  const time = d.toLocaleTimeString([], { hour12: false });
  return d.toDateString() === new Date().toDateString()
    ? time
    : d.toLocaleDateString([], { day: "2-digit", month: "short" }) + " " + time;
}

function renderRows(list, query) {
  const tbody = $("tableBody");

  if (!list || list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty">No readings in the database${query ? " for " + esc(query) : ""}. Send one from the ESP32 side.</td></tr>`;
    seen = new Set();
    lastQuery = query;
    return;
  }

  const flash = lastQuery === query; // only highlight new rows when re-fetching the same query
  const next = new Set();
  tbody.innerHTML = list
    .map((row) => {
      const key = [row.recordedAt, row.deviceId, row.humidity, row.pressure, row.moisture, row.protocol].join("|");
      next.add(key);
      const isNew = flash && !seen.has(key);
      const via = row.protocol ? `<span class="via ${esc(row.protocol.toLowerCase())}">${esc(row.protocol)}</span>` : `<span class="via">—</span>`;
      return `
      <tr class="${isNew ? "new" : ""}">
        <td title="${esc(row.recordedAt)}">${fmtTime(row.recordedAt)}</td>
        <td>${esc(row.deviceId)}</td>
        <td>${Number(row.humidity).toFixed(1)}</td>
        <td>${Number(row.pressure).toFixed(1)}</td>
        <td>${Number(row.moisture).toFixed(1)}</td>
        <td>${via}</td>
      </tr>`;
    })
    .join("");
  seen = next;
  lastQuery = query;
}

async function loadReadings() {
  const query = $("queryDevice").value.trim();
  const url = query ? `${API_BASE}/${encodeURIComponent(query)}` : API_BASE;
  const path = url.replace("http://localhost:8082", "");

  const call = callService("USER", "GET", url);
  await animate("read", call, { label: `GET ${path}` }, (r) => {
    if (!r.ok) {
      $("tableBody").innerHTML = `<tr><td colspan="6" class="empty">Could not load data. Is the REST service running on port 8082?</td></tr>`;
      $("fetchMeta").textContent = "";
      return;
    }
    renderRows(r.json, query);
    $("fetchMeta").textContent =
      `${r.json.length} row(s) read from the H2 database via the service in ${r.ms} ms · ${new Date().toLocaleTimeString([], { hour12: false })}`;
  });
}

$("refreshBtn").addEventListener("click", loadReadings);
$("queryDevice").addEventListener("keydown", (e) => {
  if (e.key === "Enter") loadReadings();
});

let pollTimer = null;
$("pollToggle").addEventListener("change", (e) => {
  clearInterval(pollTimer);
  if (e.target.checked) {
    loadReadings();
    pollTimer = setInterval(loadReadings, 3000);
  }
});

loadReadings();
