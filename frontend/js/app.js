"use strict";
/* PlaceSure: a small vanilla-JavaScript SPA. The server owns every eligibility decision. */
const paths = {
  grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  users:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M16 3a4 4 0 0 1 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
  building:
    "M4 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16 M16 9h3a1 1 0 0 1 1 1v11 M2 21h20 M8 7h4 M8 11h4 M8 15h4 M9 21v-2h2v2",
  shield: "M12 3 3 7v5c0 5 9 10 9 10s9-5 9-10V7l-9-4Z M8 12l3 3 5-6",
  history: "M3 11a9 9 0 1 1 2.6 7.4 M3 4v7h7 M12 7v5l3 2",
  chart: "M4 3v18h17 M8 16v-4 M13 16V7 M18 16v-7",
  settings:
    "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M20 12l2-1-2-4-2 .4L16 4l-4-1-1 2-4 1-2 4 1 2-1 4 4 2 3 1 3-1 4-1 2-4-1-3Z",
  search: "m21 21-4.5-4.5 M19 10.5a8.5 8.5 0 1 1-17 0 8.5 8.5 0 0 1 17 0",
  arrow: "M5 12h14 M13 6l6 6-6 6",
  chevron: "m9 5 7 7-7 7",
  left: "m15 5-7 7 7 7",
  down: "m6 9 6 6 6-6",
  plus: "M12 5v14 M5 12h14",
  download: "M12 3v12 M7 10l5 5 5-5 M4 16v4h16v-4",
  check: "m5 12 4 4L19 6",
  x: "m6 6 12 12 M6 18 18 6",
  edit: "m16 3 5 5-12 12-6 1 1-6L16 3Z M14 5l5 5",
  trash: "M3 6h18 M9 6V3h6v3 M5 6l1 15h12l1-15 M10 10v7 M14 10v7",
  eye: "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
  calendar:
    "M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14H3V6a2 2 0 0 1 2-2Z",
  file: "M14 2H5v20h14V7l-5-5Z M14 2v6h5 M8 12h8 M8 16h6",
  print: "M6 9V3h12v6 M6 18H3V9h18v9h-3 M6 14h12v8H6z M17 12h1",
  info: "M12 11v6 M12 7h.01 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0",
  alert: "m12 3 10 18H2L12 3Z M12 9v5 M12 17h.01",
  spark: "m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z",
  mail: "M3 5h18v14H3z M3 5l9 7 9-7",
  book: "M3 3h7l2 2 2-2h7v16h-7l-2 2-2-2H3V3Z M12 5v16",
  database:
    "M20 5c0 2-3.6 3-8 3S4 7 4 5s3.6-3 8-3 8 1 8 3Z M4 5v14c0 2 3.6 3 8 3s8-1 8-3V5 M4 12c0 2 3.6 3 8 3s8-1 8-3",
  refresh:
    "M20 7A9 9 0 0 0 5 5L2 8 M2 2v6h6 M4 17a9 9 0 0 0 15 2l3-3 M16 16h6v6",
  menu: "M4 6h16 M4 12h16 M4 18h16",
  home: "m3 10 9-7 9 7v11h-6v-7H9v7H3V10Z",
  external: "M14 3h7v7 M21 3l-11 11 M10 3H3v18h18v-7",
  briefcase: "M3 7h18v14H3z M8 7V3h8v4 M3 12s9 5 18 0 M10 13h4",
  target: "M21 12a9 9 0 1 1-9-9 M17 12a5 5 0 1 1-5-5 M12 12l9-9 M16 3h5v5",
  code: "m8 6-6 6 6 6 M16 6l6 6-6 6 M14 3l-4 18",
  clock: "M12 7v5l3 2 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0",
  filter: "M3 5h18 M6 12h12 M10 19h4",
  loader: "M21 12a9 9 0 1 1-9-9",
  award: "M8 14 6 22l6-3 6 3-2-8 M19 8A7 7 0 1 1 5 8a7 7 0 0 1 14 0",
};
const icon = (name, size = 17, cls = "") =>
  `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="${cls}"><path d="${paths[name] || paths.file}"/></svg>`;
const esc = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const initials = (name) =>
  String(name || "?")
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
const avatar = (name, id = 0, cls = "") =>
  `<span class="avatar tone-${Number(id) % 5} ${cls}">${esc(initials(name))}</span>`;
const companyMark = (name, id = 0, large = false) =>
  `<span class="company-mark brand-${Number(id) % 5} ${large ? "large" : ""}">${esc(String(name || "?")[0])}<span style="font-size:.55em;margin-left:1px">${Number(id) % 2 ? "↗" : "•"}</span></span>`;
const money = (salary) =>
  salary ? `₹${esc(salary)}` : "Package not specified";
const decimal = (n) => (n == null ? "Not provided" : Number(n).toFixed(2));
const date = (
  s,
  options = { day: "numeric", month: "short", year: "numeric" },
) =>
  s
    ? new Intl.DateTimeFormat("en-IN", options).format(
        new Date(s.length === 10 ? s + "T12:00:00" : s),
      )
    : "Not scheduled";
const dateTime = (s) =>
  date(s, {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
const chips = (items, max = 99) =>
  (items || [])
    .slice(0, max)
    .map((x) => `<span class="chip">${esc(x)}</span>`)
    .join("") +
  ((items || []).length > max
    ? `<span class="chip">+${items.length - max}</span>`
    : "");
const statusBadge = (eligible) =>
  `<span class="badge ${eligible ? "success" : "danger"}"><i></i>${eligible ? "Eligible" : "Not eligible"}</span>`;
const passBadge = (passed) =>
  `<span class="badge ${passed ? "success" : "danger"}">${icon(passed ? "check" : "x", 10)}${passed ? "PASS" : "FAIL"}</span>`;
const disclaimer = `<div class="disclaimer">${icon("info", 13)}<span>Eligibility confirms that the student meets the configured criteria. It does not guarantee shortlisting, an interview, or a job offer.</span></div>`;
const main = document.getElementById("main");
const state = {
  page: "overview",
  meta: { departments: [], skills: [] },
  students: {
    page: 1,
    size: Number(localStorage.getItem("placesure-page-size")) || 10,
    q: "",
    department: "",
    minCgpa: "",
    maxArrears: "",
    sort: "createdAt",
    direction: "desc",
  },
  companies: {
    page: 1,
    size: 12,
    q: "",
    department: "",
    maxCgpa: "",
    sort: "driveDate",
    direction: "asc",
  },
  history: {
    page: 1,
    size: 10,
    q: "",
    companyId: "",
    status: "",
    from: "",
    to: "",
  },
  report: {
    type: "summary",
    companyId: "",
    studentId: "",
    status: "",
    department: "",
    from: "",
    to: "",
    page: 1,
  },
  checker: { student: null, company: null, result: null },
  charts: [],
  renderId: 0,
};
const pageNames = {
  overview: "Overview",
  students: "Students",
  companies: "Companies",
  checker: "Eligibility Checker",
  history: "Evaluation History",
  reports: "Reports",
  settings: "Settings & About",
};
let currentReport = null,
  modalReturnFocus = null,
  searchTimer = null,
  modalLoadToken = 0;
class ApiError extends Error {
  constructor(message, code, fields) {
    super(message);
    this.code = code;
    this.fields = fields || {};
  }
}
async function api(path, options = {}) {
  let response;
  try {
    response = await fetch("/api" + path, {
      ...options,
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {}),
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
  } catch (e) {
    throw new ApiError(
      "Unable to reach the server. Check that PlaceSure is running.",
      "NETWORK",
    );
  }
  let body;
  try {
    body = await response.json();
  } catch (e) {
    throw new ApiError(
      "The server returned an unreadable response. Please try again.",
      "RESPONSE",
    );
  }
  if (!response.ok)
    throw new ApiError(
      body.error?.message || "Something went wrong. Please try again.",
      body.error?.code,
      body.error?.fields,
    );
  return body.data;
}
const query = (values) => {
  const p = new URLSearchParams();
  Object.entries(values).forEach(([k, v]) => {
    if (v !== "" && v != null && k !== "pageSize") p.set(k, v);
  });
  return p.toString();
};
function toast(message, type = "success") {
  const el = document.createElement("div");
  el.className = "toast " + (type === "error" ? "error" : "");
  el.innerHTML =
    icon(type === "error" ? "alert" : "check", 17) +
    `<span>${esc(message)}</span><button class="icon-button" aria-label="Dismiss notification">${icon("x", 13)}</button>`;
  el.querySelector("button").onclick = () => el.remove();
  document.getElementById("toast-root").appendChild(el);
  setTimeout(() => el.remove(), 5500);
}
function pageHeading(title, description, actions = "") {
  return `<div class="page-heading"><div><h1>${esc(title)}</h1><p>${esc(description)}</p></div><div class="page-actions">${actions}</div></div>`;
}
function loading() {
  return `<div class="loading-state" role="status" aria-label="Loading workspace"><div class="skeleton skeleton-line"></div><div class="skeleton skeleton-line" style="width:320px;height:10px"></div><div class="skeleton-grid">${Array(4).fill('<div class="skeleton skeleton-card"></div>').join("")}</div><div class="skeleton skeleton-large"></div><span class="sr-only">Loading live database records…</span></div>`;
}
function emptyState(
  title,
  description,
  action = "",
  ico = "search",
  compact = false,
) {
  return `<div class="empty-state ${compact ? "compact" : ""}"><div class="empty-icon">${icon(ico, compact ? 20 : 25)}</div><h3>${esc(title)}</h3><p>${esc(description)}</p>${action}</div>`;
}
function errorState(error) {
  return `<div class="error-state"><div class="empty-icon">${icon("database", 25)}</div><h2>${error.code === "DATABASE_UNAVAILABLE" ? "Your data is temporarily out of reach" : "We couldn’t load this workspace"}</h2><p>${esc(error.message)} Your saved records have not been changed.</p><button class="button primary" data-action="retry">${icon("refresh", 15)}Try again</button> <a class="button secondary" href="#/settings">Connection help</a></div>`;
}
function setMain(html) {
  const focused = document.activeElement,
    focusId = focused?.id,
    selection = focused?.selectionStart;
  main.innerHTML = html;
  if (focusId) {
    const next = document.getElementById(focusId);
    if (next) {
      next.focus({ preventScroll: true });
      if (selection != null && next.setSelectionRange && next.type !== "number")
        try {
          next.setSelectionRange(selection, selection);
        } catch (e) {}
    }
  }
}
function options(items, selected, placeholder = "All departments") {
  return (
    `<option value="">${esc(placeholder)}</option>` +
    items
      .map(
        (item) =>
          `<option value="${esc(item)}" ${String(selected) === String(item) ? "selected" : ""}>${esc(item)}</option>`,
      )
      .join("")
  );
}
function companyOptions(selected) {
  return (
    `<option value="">All companies</option>` +
    (state.meta.companies || [])
      .map(
        (c) =>
          `<option value="${c.id}" ${String(selected) === String(c.id) ? "selected" : ""}>${esc(c.name)}</option>`,
      )
      .join("")
  );
}
function studentOptions(selected) {
  return (
    `<option value="">All students</option>` +
    (state.meta.students || [])
      .map(
        (s) =>
          `<option value="${s.id}" ${String(selected) === String(s.id) ? "selected" : ""}>${esc(s.fullName)} · ${esc(s.registerNumber)}</option>`,
      )
      .join("")
  );
}
function statusOptions(selected) {
  return `<option value="">All results</option><option value="eligible" ${selected === "eligible" ? "selected" : ""}>Eligible</option><option value="ineligible" ${selected === "ineligible" ? "selected" : ""}>Not eligible</option>`;
}
function searchField(id, placeholder, value, filter, extra = "") {
  return `<div class="search-field ${extra}">${icon("search", 14)}<input class="input" type="search" id="${id}" placeholder="${esc(placeholder)}" value="${esc(value)}" data-filter="${filter}" autocomplete="off" aria-label="${esc(placeholder)}"></div>`;
}
function pagination(page, group) {
  const start = page.total ? (page.page - 1) * page.size + 1 : 0,
    end = Math.min(page.page * page.size, page.total);
  let pages = "";
  const from = Math.max(1, page.page - 1),
    to = Math.min(page.totalPages, from + 2);
  for (let i = from; i <= to; i++)
    pages += `<button class="${i === page.page ? "active" : ""}" data-action="paginate" data-group="${group}" data-page="${i}" aria-label="Page ${i}" ${i === page.page ? 'aria-current="page"' : ""}>${i}</button>`;
  return `<div class="pagination"><span>Showing <strong>${start}–${end}</strong> of <strong>${page.total}</strong> ${group === "history" ? "evaluations" : group === "report" ? "rows" : group}</span><div class="pagination-controls"><button data-action="paginate" data-group="${group}" data-page="${page.page - 1}" ${page.page <= 1 ? "disabled" : ""} aria-label="Previous page">${icon("left", 12)}</button>${pages}<button data-action="paginate" data-group="${group}" data-page="${page.page + 1}" ${page.page >= page.totalPages ? "disabled" : ""} aria-label="Next page">${icon("chevron", 12)}</button></div></div>`;
}
async function refreshMeta() {
  try {
    state.meta = await api("/meta");
    renderNav();
    setConnection(true);
  } catch (e) {
    setConnection(false);
  }
}
function setConnection(online) {
  const el = document.getElementById("connection-pill");
  el.classList.toggle("offline", !online);
  el.querySelector("span").textContent = online
    ? "Workspace online"
    : "Connection issue";
}
function renderNav() {
  document.getElementById("navigation").innerHTML = [
    ["overview", "grid"],
    ["students", "users"],
    ["companies", "building"],
    ["checker", "shield"],
    ["history", "history"],
    ["reports", "chart"],
    ["settings", "settings"],
  ]
    .map(
      ([key, ico], i) =>
        `${i === 5 ? '<div class="nav-separator"></div><div class="nav-label">INSIGHTS & WORKSPACE</div>' : ""}<a class="nav-item ${state.page === key ? "active" : ""}" href="#/${key}" ${state.page === key ? 'aria-current="page"' : ""}>${icon(ico, 17)}<span>${pageNames[key] === "Settings & About" ? "Settings & About" : pageNames[key]}</span>${["students", "companies"].includes(key) ? `<span class="nav-count">${state.meta[key === "students" ? "studentCount" : "companyCount"] ?? "—"}</span>` : ""}</a>`,
    )
    .join("");
}
function destroyCharts() {
  state.charts.forEach((c) => c.destroy());
  state.charts = [];
}
async function route() {
  const [path, qs = ""] = location.hash.replace(/^#\/?/, "").split("?");
  state.page = pageNames[path] ? path : "overview";
  if (!path) history.replaceState(null, "", "#/overview");
  const params = new URLSearchParams(qs);
  if (state.page === "checker") {
    if (params.has("student"))
      state.checker.student = Number(params.get("student"));
    if (params.has("company"))
      state.checker.company = Number(params.get("company"));
    state.checker.result = null;
  }
  document.getElementById("breadcrumb").textContent = pageNames[state.page];
  document.title = `${pageNames[state.page]} · PlaceSure`;
  renderNav();
  closeNav();
  destroyCharts();
  main.innerHTML = loading();
  window.scrollTo(0, 0);
  await renderPage();
}
async function renderPage() {
  const renderId = ++state.renderId;
  try {
    await {
      overview: renderOverview,
      students: renderStudents,
      companies: renderCompanies,
      checker: renderChecker,
      history: renderHistory,
      reports: renderReports,
      settings: renderSettings,
    }[state.page](renderId);
  } catch (e) {
    if (renderId === state.renderId) {
      setMain(errorState(e));
      if (e.code === "DATABASE_UNAVAILABLE" || e.code === "NETWORK")
        setConnection(false);
    }
  }
}
function validRender(id) {
  return id === state.renderId;
}
function closeNav() {
  document.getElementById("sidebar").classList.remove("open");
  document.getElementById("sidebar-backdrop").classList.remove("visible");
  document.getElementById("menu-toggle").setAttribute("aria-expanded", "false");
}

// OVERVIEW — every figure and chart is returned by the database-backed dashboard endpoint.
async function renderOverview(id) {
  const d = await api("/dashboard");
  if (!validRender(id)) return;
  const rate = d.totalChecks
    ? Math.round((d.eligibleMatches / d.totalChecks) * 100)
    : 0;
  const heroArt = `<svg class="hero-art" viewBox="0 0 330 180" fill="none" aria-hidden="true"><circle cx="200" cy="91" r="72" stroke="#52749b" stroke-opacity=".3"/><circle cx="200" cy="91" r="50" stroke="#52749b" stroke-opacity=".45" stroke-dasharray="3 6"/><path d="m55 145 71-35 74-19 76-44M125 110l35-70" stroke="#7096c5" stroke-opacity=".5" stroke-dasharray="4 5"/><rect x="168" y="59" width="63" height="64" rx="17" fill="#284367" stroke="#5476a0"/><path d="m186 91 10 10 18-22" stroke="#b9d8ff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="125" cy="111" r="16" fill="#223c60" stroke="#52749b"/><path d="M125 105v12m-6-6h12" stroke="#97b8e4" stroke-width="1.5"/><rect x="258" y="33" width="35" height="33" rx="9" fill="#243f62" stroke="#52749b"/><path d="m268 49 5 5 8-10" stroke="#9bbce3" stroke-width="1.7" stroke-linecap="round"/><circle cx="159" cy="39" r="6" fill="#719bcf"/><circle cx="56" cy="144" r="4" fill="#486b98"/><circle cx="259" cy="138" r="5" fill="#719bcf"/><path d="M237 118l18 17" stroke="#52749b" stroke-dasharray="3 4"/></svg>`;
  const stat = (label, value, ico, note, green = false) =>
    `<div class="stat-card ${green ? "green" : ""}"><div class="stat-label">${label}</div><div class="stat-icon">${icon(ico, 16)}</div><div class="stat-number">${value.toLocaleString("en-IN")}</div><div class="stat-note">${note}</div></div>`;
  const recents = d.recentEvaluations.length
    ? `<div class="table-scroll"><table class="dashboard-table"><thead><tr><th>STUDENT</th><th>COMPANY</th><th>RESULT</th><th class="eval-date">EVALUATED</th><th></th></tr></thead><tbody>${d.recentEvaluations.map((e) => `<tr><td><div class="person-cell">${avatar(e.studentName, e.studentId)}<span><span class="cell-name">${esc(e.studentName)}</span><span class="cell-sub">${esc(e.registerNumber)}</span></span></div></td><td class="company-cell">${esc(e.companyName)}</td><td>${statusBadge(e.eligible)}</td><td class="eval-date">${date(e.evaluatedAt, { day: "numeric", month: "short" })}</td><td><button class="icon-button" data-action="view-evaluation" data-id="${e.id}" aria-label="View evaluation ${e.id}">${icon("chevron", 12)}</button></td></tr>`).join("")}</tbody></table></div><div class="card-footer"><span>${d.totalChecks} completed evaluations</span><a class="text-link" href="#/history">View all evaluations ${icon("arrow", 12)}</a></div>`
    : emptyState(
        "Your first decision starts here",
        "Run an eligibility check to see your recent evaluations.",
        `<a class="button secondary sm" href="#/checker">Check eligibility ${icon("arrow", 12)}</a>`,
        "shield",
        true,
      );
  setMain(
    pageHeading(
      "Overview",
      "A clearer picture of every placement opportunity.",
      `<button class="button secondary" data-action="export-summary">${icon("download", 14)}Export report</button><a class="button primary" href="#/checker">${icon("plus", 15)}New evaluation</a>`,
    ) +
      `<section class="hero"><div class="hero-copy"><div class="hero-eyebrow">${icon("spark", 11)} EVERY STUDENT. A STEP FORWARD.</div><h2>Potential, meet opportunity.</h2><p>Turn student profiles into clear, confident placement decisions.</p><a class="button" href="#/checker">Check eligibility ${icon("arrow", 13)}</a></div>${heroArt}</section><section class="stats-grid" aria-label="Workspace statistics">${stat("Total students", d.totalStudents, "users", `Across ${d.departmentCount} departments`)}${stat("Placement partners", d.totalCompanies, "building", "Company-specific criteria")}${stat("Eligibility checks", d.totalChecks, "shield", "Every criterion, explained")}${stat("Eligible matches", d.eligibleMatches, "target", `<span class="trend">${icon("check", 9)}${rate}%</span> overall match rate`, true)}</section><section class="dashboard-grid"><div class="card"><div class="card-header"><div><h2>Eligibility by company</h2><p>How evaluated profiles match each opportunity</p></div><div class="legend"><span><i></i>Eligible</span><span><i class="gray"></i>Not eligible</span></div></div><div class="card-body"><div class="chart-area">${d.totalChecks ? '<canvas id="company-chart" role="img" aria-label="Company-wise eligible and ineligible evaluation counts"></canvas>' : emptyState("No evaluations yet", "Company comparisons will appear after your first check.", "", "chart", true)}</div></div></div><div class="card"><div class="card-header"><div><h2>Eligibility snapshot</h2><p>Every evaluation, one clear picture</p></div>${icon("chart", 15)}</div><div class="donut-area"><canvas id="status-chart" role="img" aria-label="Eligible versus ineligible evaluation chart"></canvas><div class="donut-center"><strong>${rate}<span style="font-size:15px;color:#6f849f">%</span></strong><span>overall match rate</span></div></div><div class="donut-key"><div><i></i>Eligible <strong>${d.eligibleMatches}</strong></div><div><i class="gray"></i>Not eligible <strong>${d.totalChecks - d.eligibleMatches}</strong></div></div></div></section><section class="dashboard-lower"><div class="card"><div class="card-header"><div><h2>Recent evaluations</h2><p>The latest decisions from your workspace</p></div><a href="#/history" class="text-link">View all ${icon("arrow", 12)}</a></div>${recents}</div><div class="right-stack"><div class="card"><div class="card-header"><div><h2>Upcoming placement drives</h2><p>Opportunities on the horizon</p></div>${icon("calendar", 15)}</div><div class="drive-list">${d.upcomingDrives.length ? d.upcomingDrives.map((c) => `<a class="drive-row" href="#/checker?company=${c.id}">${companyMark(c.name, c.id)}<div class="drive-info"><strong>${esc(c.name)}</strong><p>${esc(c.jobRole)}</p></div><div class="drive-date"><strong>${date(c.driveDate, { day: "2-digit" })}</strong><span>${date(c.driveDate, { month: "short" })}</span></div></a>`).join("") : emptyState("No drives scheduled", "Add a drive date to a company to see it here.", "", "calendar", true)}</div><div class="card-footer"><span>Find the right fit, ahead of time.</span><a href="#/companies" class="text-link">All companies ${icon("arrow", 12)}</a></div></div><div class="card"><div class="card-header"><div><h2>New to your workspace</h2><p>Recently added student profiles</p></div><a class="text-link" href="#/students">View all ${icon("arrow", 12)}</a></div><div class="card-body">${d.recentStudents.length ? d.recentStudents.map((s) => `<div class="new-student-row">${avatar(s.fullName, s.id)}<div><strong>${esc(s.fullName)}</strong><small>${esc(s.department)} <span style="padding:0 3px">·</span> Class of ${s.graduationYear}</small></div><a href="#/checker?student=${s.id}" class="icon-button" aria-label="Check eligibility for ${esc(s.fullName)}">${icon("chevron", 12)}</a></div>`).join("") : emptyState("A fresh start", "Add your first student profile.", "", "users", true)}</div></div></div></section>`,
  );
  if (window.Chart) {
    Chart.defaults.font.family = "Inter, sans-serif";
    Chart.defaults.color = "#9aa6b8";
    Chart.defaults.font.size = 10;
    if (d.totalChecks)
      state.charts.push(
        new Chart(document.getElementById("company-chart"), {
          type: "bar",
          data: {
            labels: d.companySummary.map((c) =>
              c.name.replace(" Systems", "").replace(" Digital", ""),
            ),
            datasets: [
              {
                label: "Eligible",
                data: d.companySummary.map((c) => c.eligible),
                backgroundColor: "#81ae9e",
                borderRadius: 3,
                barThickness: 19,
              },
              {
                label: "Not eligible",
                data: d.companySummary.map((c) => c.ineligible),
                backgroundColor: "#e0e7f0",
                borderRadius: 3,
                barThickness: 19,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 450 },
            plugins: {
              legend: { display: false },
              tooltip: {
                backgroundColor: "#203858",
                padding: 10,
                cornerRadius: 6,
              },
            },
            scales: {
              x: {
                grid: { display: false },
                border: { display: false },
                ticks: { font: { size: 9 }, maxRotation: 0 },
              },
              y: {
                beginAtZero: true,
                border: { display: false, dash: [3, 4] },
                grid: { color: "#eff2f7", drawTicks: false },
                ticks: { precision: 0, padding: 9, font: { size: 9 } },
                suggestedMax:
                  Math.max(
                    ...d.companySummary.map((c) =>
                      Math.max(c.eligible, c.ineligible),
                    ),
                    4,
                  ) + 1,
              },
            },
          },
        }),
      );
    state.charts.push(
      new Chart(document.getElementById("status-chart"), {
        type: "doughnut",
        data: {
          labels: ["Eligible", "Not eligible"],
          datasets: [
            {
              data: d.totalChecks
                ? [d.eligibleMatches, d.totalChecks - d.eligibleMatches]
                : [0, 1],
              backgroundColor: ["#78a993", "#e6ecf3"],
              borderWidth: 0,
              hoverOffset: 2,
              borderRadius: 3,
              spacing: 4,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: "81%",
          animation: { duration: 450 },
          plugins: {
            legend: { display: false },
            tooltip: {
              enabled: !!d.totalChecks,
              backgroundColor: "#203858",
              cornerRadius: 6,
            },
          },
        },
      }),
    );
  }
}

// STUDENTS
async function renderStudents(id) {
  const page = await api("/students?" + query(state.students));
  if (!validRender(id)) return;
  state.students.page = page.page;
  const f = state.students;
  setMain(
    pageHeading(
      "Students",
      "Every profile is the beginning of an opportunity.",
      `<button class="button primary" data-action="add-student">${icon("plus", 15)}Add student</button>`,
    ) +
      `<div class="section-toolbar"><strong>Student directory <span style="margin-left:7px">${page.total} ${page.total === 1 ? "student" : "students"}</span></strong><span>Academic profiles & skillsets</span></div><section class="card"><div class="filter-bar">${searchField("student-search", "Search name or register number", f.q, "students.q")}<select class="select compact" id="student-department" data-filter="students.department" aria-label="Filter by department">${options(state.meta.departments, f.department)}</select><select class="select compact" id="student-cgpa" data-filter="students.minCgpa" aria-label="Minimum CGPA"><option value="">All CGPAs</option>${[6, 7, 8, 9].map((n) => `<option value="${n}" ${String(f.minCgpa) === String(n) ? "selected" : ""}>CGPA ≥ ${n}.00</option>`).join("")}</select><select class="select compact" id="student-arrears" data-filter="students.maxArrears" aria-label="Maximum active arrears"><option value="">All arrears</option>${[0, 1, 2].map((n) => `<option value="${n}" ${String(f.maxArrears) === String(n) ? "selected" : ""}>${n === 0 ? "No active arrears" : "≤ " + n + " active arrears"}</option>`).join("")}</select><select class="select compact sort" id="student-sort" data-filter="students.order" aria-label="Sort students"><option value="createdAt:desc" ${f.sort === "createdAt" ? "selected" : ""}>Recently added</option><option value="name:asc" ${f.sort === "name" ? "selected" : ""}>Name · A to Z</option><option value="cgpa:desc" ${f.sort === "cgpa" ? "selected" : ""}>CGPA · High to low</option><option value="registerNumber:asc" ${f.sort === "registerNumber" ? "selected" : ""}>Register number</option></select></div>${page.items.length ? `<div class="table-scroll"><table class="student-table"><thead><tr><th>STUDENT</th><th>DEPARTMENT</th><th>CGPA</th><th>ACTIVE ARREARS</th><th>SKILLS</th><th>ACTIONS</th></tr></thead><tbody>${page.items.map((s) => `<tr><td><div class="person-cell">${avatar(s.fullName, s.id)}<button data-action="view-student" data-id="${s.id}"><span class="cell-name">${esc(s.fullName)}</span><span class="cell-sub">${esc(s.registerNumber)} · ${s.graduationYear}</span></button></div></td><td><span class="badge blue">${esc(s.department)}</span></td><td><div class="cgpa-cell">${decimal(s.cgpa)}<span class="cgpa-track"><i style="width:${Number(s.cgpa) * 10}%"></i></span></div></td><td><span class="badge ${s.activeArrears === 0 ? "success" : "warning"}">${s.activeArrears === 0 ? icon("check", 10) + "Clear" : s.activeArrears + " active"}</span></td><td><div class="chip-list">${chips(s.skills, 2) || '<span class="muted small">Not recorded</span>'}</div></td><td><div class="row-actions"><button class="icon-button" data-action="view-student" data-id="${s.id}" aria-label="View ${esc(s.fullName)}">${icon("eye", 14)}</button><button class="icon-button" data-action="edit-student" data-id="${s.id}" aria-label="Edit ${esc(s.fullName)}">${icon("edit", 14)}</button><button class="icon-button danger" data-action="delete-student" data-id="${s.id}" data-name="${esc(s.fullName)}" aria-label="Delete ${esc(s.fullName)}">${icon("trash", 14)}</button></div></td></tr>`).join("")}</tbody></table></div>${pagination(page, "students")}` : emptyState(f.q || f.department || f.minCgpa || f.maxArrears ? "No matching profiles" : "Your next success story starts here", f.q || f.department || f.minCgpa || f.maxArrears ? "Try adjusting your search or filters." : "Create a student profile to start matching skills with opportunities.", `<button class="button secondary sm" data-action="${page.total === 0 && !(f.q || f.department || f.minCgpa || f.maxArrears) ? "add-student" : "clear-students"}">${icon("plus", 13)}${page.total === 0 && !(f.q || f.department || f.minCgpa || f.maxArrears) ? "Add student" : "Clear filters"}</button>`, "users")}<div class="table-footnote">${icon("info", 12)}CGPA is on a 10-point scale. Historical arrears are tracked separately from active arrears.</div></section>`,
  );
}

// COMPANIES
async function renderCompanies(id) {
  const page = await api("/companies?" + query(state.companies));
  if (!validRender(id)) return;
  const f = state.companies;
  setMain(
    pageHeading(
      "Companies",
      "Distinct opportunities. Clearly defined requirements.",
      `<button class="button primary" data-action="add-company">${icon("plus", 15)}Add company</button>`,
    ) +
      `<div class="section-toolbar"><strong>Placement partners <span style="margin-left:7px">${page.total} opportunities</span></strong><span>Criteria tailored to every drive</span></div><div class="card"><div class="filter-bar" style="border-bottom:0">${searchField("company-search", "Search company or job role", f.q, "companies.q")}<select class="select compact" id="company-department" data-filter="companies.department" aria-label="Eligible department">${options(state.meta.departments, f.department, "All departments")}</select><select class="select compact" id="company-cgpa" data-filter="companies.maxCgpa" aria-label="Maximum minimum CGPA"><option value="">Any CGPA requirement</option>${[6, 6.5, 7, 7.5, 8].map((n) => `<option value="${n}" ${String(f.maxCgpa) === String(n) ? "selected" : ""}>Requires ≤ ${n.toFixed(2)}</option>`).join("")}</select><select class="select compact sort" id="company-sort" data-filter="companies.order" aria-label="Sort companies"><option value="driveDate:asc" ${f.sort === "driveDate" ? "selected" : ""}>Upcoming drives first</option><option value="name:asc" ${f.sort === "name" ? "selected" : ""}>Company · A to Z</option><option value="minCgpa:asc" ${f.sort === "minCgpa" ? "selected" : ""}>Minimum CGPA</option></select></div></div>${page.items.length ? `<div class="company-grid">${page.items.map((c) => `<article class="card company-card"><div class="company-card-top">${companyMark(c.name, c.id, true)}<div class="row-actions"><button class="icon-button" data-action="view-company" data-id="${c.id}" aria-label="View ${esc(c.name)}">${icon("eye", 14)}</button><button class="icon-button" data-action="edit-company" data-id="${c.id}" aria-label="Edit ${esc(c.name)}">${icon("edit", 14)}</button><button class="icon-button danger" data-action="delete-company" data-id="${c.id}" data-name="${esc(c.name)}" aria-label="Delete ${esc(c.name)}">${icon("trash", 14)}</button></div></div><div class="company-card-name"><h2>${esc(c.name)}</h2><p>${esc(c.jobRole)}</p><div class="company-salary">${icon("briefcase", 12)}${money(c.salaryPackage)}</div></div><div class="company-metrics"><div><span>MINIMUM CGPA</span><strong>${decimal(c.criteria?.minCgpa)} <small>/ 10</small></strong></div><div><span>MAX. ACTIVE ARREARS</span><strong>${c.criteria?.maxActiveArrears ?? "—"} <small>permitted</small></strong></div></div><div class="company-card-skills"><div class="eyebrow">REQUIRED SKILLS</div><div class="chip-list">${chips(c.criteria?.requiredSkills) || '<span class="chip">No skill restriction</span>'}</div></div><div class="company-card-meta">${icon("users", 12)}${esc(c.criteria?.departments?.join(", ") || "Open to all departments")}</div><div class="company-card-footer"><span>${icon("calendar", 12)} <span style="margin-left:3px">${c.driveDate ? date(c.driveDate, { day: "numeric", month: "short" }) : "Date to be announced"}</span></span><a class="text-link" href="#/checker?company=${c.id}">Check a match ${icon("arrow", 12)}</a></div></article>`).join("")}</div>${page.totalPages > 1 ? pagination(page, "companies") : ""}` : emptyState("No opportunities found", "Add a company or adjust your filters to find the right opportunity.", `<button class="button secondary sm" data-action="clear-companies">Clear filters</button>`, "building")}<p class="criteria-note">${icon("info", 13)}<span>All required skills must match. Optional department, graduation year, and historical-arrears rules apply only when configured. Open any company for its full criteria.</span></p>`,
  );
}

// ELIGIBILITY CHECKER
async function renderChecker(id) {
  const c = state.checker;
  const [student, company, students, companies] = await Promise.all([
    c.student ? api("/students/" + c.student) : null,
    c.company ? api("/companies/" + c.company) : null,
    c.student ? null : api("/students?size=6&sort=name&direction=asc"),
    c.company ? null : api("/companies?size=6&sort=name&direction=asc"),
  ]);
  if (!validRender(id)) return;
  const selectedStudent = student
    ? `<div class="selected-profile"><div class="selected-profile-header">${avatar(student.fullName, student.id, "lg")}<div><h3>${esc(student.fullName)}</h3><p>${esc(student.registerNumber)} · ${esc(student.department)} · ${student.graduationYear}</p></div></div><div class="profile-stat-row"><div><span>CGPA / 10</span><strong>${decimal(student.cgpa)}</strong></div><div><span>ACTIVE ARREARS</span><strong>${student.activeArrears}</strong></div><div><span>HISTORICAL</span><strong>${student.historyArrears}</strong></div></div><div class="eyebrow">RECORDED SKILLS</div><div class="chip-list">${chips(student.skills) || '<span class="muted small">No skills recorded</span>'}</div></div>`
    : pickerList("student", students.items);
  const selectedCompany = company
    ? `<div class="selected-profile"><div class="selected-profile-header">${companyMark(company.name, company.id, true)}<div><h3>${esc(company.name)}</h3><p>${esc(company.jobRole)}</p></div></div><div class="mini-criteria"><div><span>MINIMUM CGPA</span><strong>${decimal(company.criteria?.minCgpa)} / 10</strong></div><div><span>MAX. ACTIVE ARREARS</span><strong>${company.criteria?.maxActiveArrears ?? "Not configured"}</strong></div><div><span>HISTORICAL ARREARS</span><strong>${company.criteria?.maxHistoryArrears == null ? "Not considered" : "Maximum " + company.criteria.maxHistoryArrears}</strong></div><div><span>DEPARTMENTS</span><strong>${esc(company.criteria?.departments?.join(", ") || "All departments")}</strong></div><div><span>GRADUATION YEARS</span><strong>${esc(company.criteria?.graduationYears?.join(", ") || "Any year")}</strong></div><div><span>DRIVE DATE</span><strong>${company.driveDate ? date(company.driveDate, { day: "numeric", month: "short" }) : "Not scheduled"}</strong></div></div><div class="eyebrow">ALL REQUIRED SKILLS</div><div class="chip-list">${chips(company.criteria?.requiredSkills) || '<span class="chip">No skill restriction</span>'}</div></div>`
    : pickerList("company", companies.items);
  setMain(
    pageHeading(
      "Eligibility Checker",
      "Find the fit. Understand every decision.",
    ) +
      `<div class="steps" aria-label="Evaluation steps"><div class="step ${student ? "done" : "active"}"><span>${student ? icon("check", 12) : "01"}</span>Select student</div><div class="step-line"></div><div class="step ${company ? "done" : student ? "active" : ""}"><span>${company ? icon("check", 12) : "02"}</span>Select company</div><div class="step-line"></div><div class="step ${c.result ? "done" : student && company ? "active" : ""}"><span>${c.result ? icon("check", 12) : "03"}</span>Get clarity</div></div><section class="checker-selection"><div class="card picker-card"><div class="card-header"><div class="picker-title"><span class="step-icon">01</span><div><h2>Choose a student</h2><p>The profile behind the potential</p></div></div>${student ? '<button class="text-link button ghost sm" data-action="change-student">Change</button>' : icon("users", 17)}</div>${!student ? searchField("student-picker", "Search name or register number", "", "picker.student", "picker-search") : ""}<div id="student-picker-results">${selectedStudent}</div></div><div class="card picker-card"><div class="card-header"><div class="picker-title"><span class="step-icon">02</span><div><h2>Choose an opportunity</h2><p>Company criteria, clearly defined</p></div></div>${company ? '<button class="text-link button ghost sm" data-action="change-company">Change</button>' : icon("building", 17)}</div>${!company ? searchField("company-picker", "Search company or job role", "", "picker.company", "picker-search") : ""}<div id="company-picker-results">${selectedCompany}</div></div></section><div class="checker-action"><p>${icon("shield", 16)}Every configured criterion is evaluated by the Java backend and saved securely.</p><button class="button primary lg" data-action="evaluate" id="evaluate-button" ${!student || !company ? "disabled" : ""}>${icon("spark", 16)}${c.result ? "Check again" : "Check eligibility"}${icon("arrow", 15)}</button></div><section id="evaluation-result" tabindex="-1">${c.result ? evaluationMarkup(c.result) : `<div class="card result-empty">${emptyState("Clarity starts with a check", "Select a student and an opportunity. We’ll explain every pass, every gap, and the final decision.", "", "shield")}</div>`}</section>${disclaimer}`,
  );
}
function pickerList(type, items) {
  return `<div class="picker-results">${items.length ? items.map((x) => `<button class="picker-option" data-action="choose-${type}" data-id="${x.id}">${type === "student" ? avatar(x.fullName, x.id) : companyMark(x.name, x.id)}<span><strong>${esc(type === "student" ? x.fullName : x.name)}</strong><small>${esc(type === "student" ? x.registerNumber + " · " + x.department : x.jobRole)}</small></span>${icon("chevron", 13)}</button>`).join("") : emptyState("No matches found", `Try another search or add a ${type} first.`, type === "student" ? '<a class="text-link" href="#/students">Open students →</a>' : '<a class="text-link" href="#/companies">Open companies →</a>', type === "student" ? "users" : "building", true)}</div>`;
}
function evaluationMarkup(e) {
  return `<div class="card evaluation-card"><div class="result-banner ${e.eligible ? "" : "failed"}"><div class="result-badge-icon">${icon(e.eligible ? "shield" : "x", 26)}</div><div><div class="result-eyebrow">${e.eligible ? "ELIGIBLE MATCH" : "NOT ELIGIBLE"}</div><h2>${e.eligible ? "A match worth moving forward." : "Not a match. Now you know why."}</h2><p>${esc(e.studentName)} ${icon("arrow", 11)} ${esc(e.companyName)} · ${esc(e.jobRole)}</p></div><div class="result-score"><strong>${e.passedCount}/${e.passedCount + e.failedCount}</strong>criteria satisfied</div></div><div class="result-details-header"><h3>Every requirement, explained</h3><span>#PS-${String(e.id).padStart(4, "0")} · ${dateTime(e.evaluatedAt)}</span></div><div class="table-scroll"><table class="criteria-table"><thead><tr><th>CRITERION</th><th>STUDENT’S VALUE</th><th>COMPANY REQUIREMENT & EXPLANATION</th><th>RESULT</th></tr></thead><tbody>${e.details.map((d) => `<tr><td class="criterion-label">${esc(d.criterion)}</td><td class="actual">${esc(d.actual)}</td><td><div>${esc(d.requirement)}</div><div class="explanation">${esc(d.explanation)}</div></td><td>${passBadge(d.passed)}</td></tr>`).join("")}</tbody></table></div><div class="result-footer"><a href="#/history" class="text-link">Saved to evaluation history ${icon("arrow", 12)}</a><div class="result-toolbar"><button class="button secondary sm" data-action="print-evaluation" data-id="${e.id}">${icon("print", 13)}Print / Save PDF</button><button class="button secondary sm" data-action="export-evaluation" data-id="${e.id}">${icon("download", 13)}Export CSV</button></div></div></div>`;
}
async function runEvaluation(button) {
  if (!state.checker.student || !state.checker.company) return;
  button.disabled = true;
  button.innerHTML = icon("loader", 16, "spin") + "Evaluating criteria…";
  try {
    state.checker.result = await api("/evaluations", {
      method: "POST",
      body: {
        studentId: state.checker.student,
        companyId: state.checker.company,
      },
    });
    await renderPage();
    toast("Evaluation complete. Every criterion has been saved.");
    document
      .getElementById("evaluation-result")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (e) {
    toast(e.message, "error");
    button.disabled = false;
    button.innerHTML = icon("spark", 16) + "Check eligibility";
  }
}

// EVALUATION HISTORY
async function renderHistory(id) {
  const page = await api("/evaluations?" + query(state.history));
  if (!validRender(id)) return;
  const f = state.history;
  setMain(
    pageHeading(
      "Evaluation History",
      "A reliable record of every decision, exactly as it happened.",
      `<button class="button secondary" data-action="export-history">${icon("download", 14)}Export history</button><a class="button primary" href="#/checker">${icon("plus", 15)}New evaluation</a>`,
    ) +
      `<div class="section-toolbar"><strong>Evaluation log <span style="margin-left:7px">${page.total} records</span></strong><span>Criteria snapshots preserved</span></div><section class="card"><div class="filter-bar">${searchField("history-search", "Search student or register number", f.q, "history.q")}<select class="select compact" id="history-company" data-filter="history.companyId" aria-label="Filter company">${companyOptions(f.companyId)}</select><select class="select compact" id="history-status" data-filter="history.status" aria-label="Filter result">${statusOptions(f.status)}</select><div class="date-range"><input class="input date" type="date" id="history-from" data-filter="history.from" value="${esc(f.from)}" aria-label="From date"><span>to</span><input class="input date" type="date" id="history-to" data-filter="history.to" value="${esc(f.to)}" aria-label="To date"></div></div>${page.items.length ? `<div class="table-scroll"><table class="history-table"><thead><tr><th>EVALUATION</th><th>STUDENT</th><th>COMPANY</th><th>DATE & TIME</th><th>RESULT</th><th>CRITERIA</th><th></th></tr></thead><tbody>${page.items.map((e) => `<tr><td style="font-size:9px;color:#a0acbd">#PS-${String(e.id).padStart(4, "0")}</td><td><div class="person-cell">${avatar(e.studentName, e.studentId)}<span><span class="cell-name">${esc(e.studentName)}</span><span class="cell-sub">${esc(e.registerNumber)}</span></span></div></td><td><span class="cell-name">${esc(e.companyName)}</span><span class="cell-sub">${esc(e.jobRole)}</span></td><td class="nowrap" style="font-size:9px">${date(e.evaluatedAt)}<span class="cell-sub">${date(e.evaluatedAt, { hour: "2-digit", minute: "2-digit" })}</span></td><td>${statusBadge(e.eligible)}</td><td><div class="criterion-progress"><span class="progress-track"><i style="width:${(100 * e.passedCount) / (e.passedCount + e.failedCount)}%"></i></span>${e.passedCount} passed <span style="color:#c29b9e">· ${e.failedCount} failed</span></div></td><td><button class="icon-button" data-action="view-evaluation" data-id="${e.id}" aria-label="View evaluation ${e.id}">${icon("chevron", 14)}</button></td></tr>`).join("")}</tbody></table></div>${pagination(page, "history")}` : emptyState("No evaluations to show", "Run your first check, or adjust the filters to explore previous decisions.", `<a class="button secondary sm" href="#/checker">${icon("shield", 13)}Check eligibility</a>`, "history")}<div class="table-footnote">${icon("shield", 12)}These results use the student profile and company requirements saved at the time of evaluation.</div></section>`,
  );
}

// REPORTS
const reportTypes = [
  ["summary", "chart", "Overall summary", "The bigger placement picture"],
  [
    "student",
    "users",
    "Student-wise eligibility",
    "Latest match per opportunity",
  ],
  [
    "eligible",
    "shield",
    "Company-wise eligible",
    "Students who meet the criteria",
  ],
  [
    "ineligible",
    "filter",
    "Company-wise ineligible",
    "Understand the remaining gaps",
  ],
  [
    "failures",
    "target",
    "Failed-criterion analysis",
    "Where students need support",
  ],
  ["history", "history", "Evaluation history", "Every check, in one report"],
];
async function renderReports(id) {
  const report = await api("/reports?" + query(state.report));
  if (!validRender(id)) return;
  currentReport = report;
  const f = state.report;
  setMain(
    pageHeading(
      "Reports",
      "Meaningful insights, ready to share.",
      `<button class="button secondary" data-action="print-report">${icon("print", 14)}Print / Save PDF</button><button class="button primary" data-action="export-report">${icon("download", 14)}Export CSV</button>`,
    ) +
      `<div class="report-library" role="group" aria-label="Report type">${reportTypes.map(([type, ico, title, desc]) => `<button class="report-type ${f.type === type ? "active" : ""}" data-action="report-type" data-type="${type}" aria-pressed="${f.type === type}"><span>${icon(ico, 17)}</span><span><strong>${title}</strong><small>${desc}</small></span></button>`).join("")}</div><section class="card"><div class="report-filters"><div><label class="field-label" for="report-company">Company</label><select class="select" id="report-company" data-filter="report.companyId">${companyOptions(f.companyId)}</select></div><div><label class="field-label" for="report-student">Student</label><select class="select" id="report-student" data-filter="report.studentId">${studentOptions(f.studentId)}</select></div><div><label class="field-label" for="report-department">Department</label><select class="select" id="report-department" data-filter="report.department">${options(state.meta.departments, f.department)}</select></div><div><label class="field-label" for="report-status">Eligibility result</label><select class="select" id="report-status" data-filter="report.status" ${["eligible", "ineligible", "failures"].includes(f.type) ? "disabled" : ""}>${statusOptions(f.type === "eligible" ? "eligible" : f.type === "ineligible" ? "ineligible" : f.status)}</select></div><div><label class="field-label" for="report-from">Evaluated from</label><input class="input" type="date" id="report-from" data-filter="report.from" value="${esc(f.from)}"></div><div><label class="field-label" for="report-to">Evaluated through</label><input class="input" type="date" id="report-to" data-filter="report.to" value="${esc(f.to)}"></div><div style="align-self:end"><button class="button ghost sm" data-action="clear-report">${icon("refresh", 12)}Reset filters</button></div></div><div class="report-meta"><div><h2>${esc(report.title)}</h2><p>Generated ${dateTime(report.generatedAt)} · ${report.rows.length} rows</p></div><span class="badge outline">${icon("database", 10)}Live database report</span></div><div class="report-summary"><div><span>${esc(report.summary.basis)}</span><strong>${report.summary.total}</strong></div><div><span>ELIGIBLE</span><strong>${report.summary.eligible}<small>matches</small></strong></div><div><span>MATCH RATE</span><strong>${report.summary.total ? Math.round((report.summary.eligible / report.summary.total) * 100) : 0}<small>%</small></strong></div></div><div id="report-table-container">${reportTableMarkup(report)}</div><div class="report-scope">${icon("info", 13)}<span>${esc(report.scope)} Times display in your browser’s local timezone.</span></div></section>${disclaimer}`,
  );
}
function reportTableMarkup(r) {
  if (!r.rows.length)
    return emptyState(
      "No results in this view",
      "Try a different company, student, date range, or report type.",
      "",
      "file",
    );
  const size = 10,
    page = Math.min(
      state.report.page,
      Math.max(1, Math.ceil(r.rows.length / size)),
    );
  state.report.page = page;
  return `<div class="table-scroll"><table class="report-table"><thead><tr>${r.columns.map((c) => `<th>${esc(c.label.toUpperCase())}</th>`).join("")}</tr></thead><tbody>${r.rows
    .slice((page - 1) * size, page * size)
    .map(
      (row) =>
        `<tr>${r.columns.map((c) => `<td>${c.key === "result" ? statusBadge(row[c.key] === "Eligible") : c.key === "evaluatedAt" ? dateTime(row[c.key]) : esc(row[c.key])}</td>`).join("")}</tr>`,
    )
    .join(
      "",
    )}</tbody></table></div>${pagination({ page, size, total: r.rows.length, totalPages: Math.ceil(r.rows.length / size) }, "report")}`;
}

// SETTINGS & ABOUT
async function renderSettings(id) {
  let health;
  try {
    health = await api("/health");
  } catch (e) {
    health = {
      database: "Unavailable",
      version: "1.0.0",
      javaVersion: "Not connected",
      databaseProduct: "Check configuration",
      status: "offline",
    };
  }
  if (!validRender(id)) return;
  const connected = health.database === "Connected";
  setConnection(connected);
  setMain(
    pageHeading(
      "Settings & About",
      "A focused workspace. A transparent foundation.",
      `<button class="button secondary" data-action="refresh-connection">${icon("refresh", 14)}Check connection</button>`,
    ) +
      `<div class="settings-grid"><div><div class="card"><div class="card-header"><div><h2>Workspace health</h2><p>Real-time application and database connectivity</p></div><span class="badge ${connected ? "success" : "danger"}"><i></i>${connected ? "All systems connected" : "Connection issue"}</span></div><div class="card-body"><div class="settings-row"><div><strong>Database connection</strong><p>Profiles, criteria, snapshots, and reports</p></div><span>${esc(health.database)}</span></div><div class="settings-row"><div><strong>Database engine</strong><p>MySQL-compatible storage accessed through JDBC</p></div><span>${esc(health.databaseProduct)}</span></div><div class="settings-row"><div><strong>Java runtime</strong><p>Server-side eligibility rules using BigDecimal and Collections</p></div><span>${esc(health.javaVersion)}</span></div><div class="settings-row"><div><strong>Application version</strong><p>Single placement-officer workspace</p></div><span>PlaceSure ${esc(health.version)}</span></div></div></div><div class="card" style="margin-top:21px"><div class="card-header"><div><h2>Your preferences</h2><p>Saved in this browser, not on the server</p></div>${icon("settings", 15)}</div><div class="card-body"><div class="settings-row"><div><strong>Student directory page size</strong><p>Choose how many student profiles to show at once</p></div><select class="select compact" id="page-size-preference" data-action="page-size" aria-label="Default students per page">${[10, 25, 50].map((n) => `<option value="${n}" ${state.students.size === n ? "selected" : ""}>${n} per page</option>`).join("")}</select></div></div></div><div class="notice warning" style="margin-top:21px">${icon("info", 17)}<div><strong>Designed for a trusted demonstration environment.</strong><br>This workspace has no login or role system. Use fictional data for demonstrations. Add authentication, HTTPS, and access controls before an institutional deployment.</div></div>${!connected ? '<div class="notice" style="margin-top:15px"><div><strong>Connection checklist</strong><br>Start MySQL, import database/database.sql, and verify DB_URL, DB_USER, and DB_PASSWORD in your local .env. Then restart PlaceSure. Never share your database credentials.</div></div>' : ""}</div><div><div class="card"><div class="about-brand"><h2>PlaceSure<span style="color:#8fb9ef">.</span></h2><p>Know Your Eligibility. Own Your Opportunity.</p></div><div class="card-body"><p style="font-size:10px;color:#8d9eb6;line-height:1.9;margin-bottom:13px">A clear, explainable bridge between student potential and placement opportunity.</p><ul class="feature-list"><li>${icon("check", 13)}Company-specific, configurable criteria</li><li>${icon("check", 13)}Every pass and failure explained</li><li>${icon("check", 13)}Immutable evaluation snapshots</li><li>${icon("check", 13)}CSV exports and print-ready reports</li><li>${icon("check", 13)}Fictional demo data, real evaluations</li></ul></div></div><div class="card" style="margin-top:21px"><div class="card-header"><div><h2>Project resources</h2><p>Understand, run, and explain the project</p></div>${icon("book", 15)}</div><div class="card-body"><a class="resource-link" href="/docs/README.md" download>${icon("file", 15)}Setup & project guide${icon("download", 13)}</a><a class="resource-link" href="/docs/VIVA.md" download>${icon("book", 15)}Viva notes${icon("download", 13)}</a><a class="resource-link" href="/docs/API.md" download>${icon("code", 15)}REST API reference${icon("download", 13)}</a><a class="resource-link" href="/docs/database.sql" download>${icon("database", 15)}Database schema & demo data${icon("download", 13)}</a><a class="resource-link" href="/docs/TEST-RESULTS.md" download>${icon("shield", 15)}Executed test results${icon("download", 13)}</a></div></div></div></div>${disclaimer}`,
  );
}

// ACCESSIBLE MODALS, CRUD FORMS, DETAILS & CONFIRMATIONS
function openModal(title, subtitle, body, footer = "", className = "") {
  closeModal(false);
  modalReturnFocus = document.activeElement;
  const root = document.getElementById("modal-root");
  root.innerHTML = `<div class="modal-backdrop" data-action="backdrop-close"><section class="modal ${className}" role="dialog" aria-modal="true" aria-labelledby="modal-title"><div class="modal-header"><div><h2 id="modal-title">${esc(title)}</h2><p>${esc(subtitle)}</p></div><button class="icon-button" data-action="close-modal" aria-label="Close dialog">${icon("x", 18)}</button></div>${body}${footer ? `<div class="modal-footer">${footer}</div>` : ""}</section></div>`;
  document.body.style.overflow = "hidden";
  document.querySelector(".app-shell").inert = true;
  document.getElementById("sidebar").inert = true;
  setTimeout(() => {
    const focus =
      root.querySelector("[autofocus]") ||
      root.querySelector("input:not([type=hidden]),button");
    focus?.focus();
  }, 30);
}
function closeModal(restore = true) {
  modalLoadToken++;
  const root = document.getElementById("modal-root");
  if (!root.firstChild) return;
  root.innerHTML = "";
  document.body.style.overflow = "";
  document.querySelector(".app-shell").inert = false;
  document.getElementById("sidebar").inert = false;
  if (restore) modalReturnFocus?.focus();
}
const inputField = (label, name, type, value, attrs = "", help = "") =>
  `<div class="form-field"><label for="field-${name}">${label}</label><input class="input" id="field-${name}" name="${name}" type="${type}" value="${esc(value)}" ${attrs}>${help ? `<p class="field-help">${help}</p>` : ""}</div>`;
function tagEditor(name, values = []) {
  return `<div class="tag-editor" data-tag-name="${name}">${values.map((v) => `<span class="chip" data-tag-value="${esc(v)}">${esc(v)}<button type="button" data-action="remove-tag" aria-label="Remove ${esc(v)}">×</button></span>`).join("")}<input class="tag-entry" placeholder="Type a skill, then press Enter" aria-label="Add a skill" autocomplete="off" list="available-skills"></div>`;
}
function readTags(name) {
  const ed = document.querySelector(`[data-tag-name="${name}"]`);
  if (!ed) return [];
  addTag(ed, ed.querySelector("input").value);
  return [...ed.querySelectorAll("[data-tag-value]")].map(
    (x) => x.dataset.tagValue,
  );
}
function addTag(editor, value) {
  const input = editor.querySelector("input"),
    values = value
      .split(",")
      .map((v) => v.trim().replace(/\s+/g, " "))
      .filter(Boolean);
  for (const val of values) {
    if (val.length > 80) {
      toast("Each skill must be 80 characters or fewer.", "error");
      continue;
    }
    if (
      [...editor.querySelectorAll("[data-tag-value]")].some(
        (x) => x.dataset.tagValue.toLowerCase() === val.toLowerCase(),
      )
    )
      continue;
    const node = document.createElement("span");
    node.className = "chip";
    node.dataset.tagValue = val;
    node.innerHTML =
      esc(val) +
      `<button type="button" data-action="remove-tag" aria-label="Remove ${esc(val)}">×</button>`;
    editor.insertBefore(node, input);
  }
  input.value = "";
}
async function studentForm(id) {
  const token = ++modalLoadToken;
  let s = id
    ? await api("/students/" + id)
    : {
        graduationYear: new Date().getFullYear() + 1,
        activeArrears: 0,
        historyArrears: 0,
        skills: [],
      };
  if (token !== modalLoadToken) return;
  const req = '<span class="required">*</span>';
  const body = `<form class="modal-form" id="student-form" data-id="${id || ""}"><div class="modal-body"><div class="form-error" role="alert"></div><div class="form-grid">${inputField("Full name " + req, "fullName", "text", s.fullName, 'required maxlength="120" autocomplete="name" autofocus')}${inputField("Register number " + req, "registerNumber", "text", s.registerNumber, 'required maxlength="32" placeholder="e.g. PS27CS001"', "A unique student identifier. Saved in uppercase.")}${inputField("Email address " + req, "email", "email", s.email, 'required maxlength="160" placeholder="student@example.test" autocomplete="email"')}<div class="form-field"><label for="field-department">Department ${req}</label><input class="input" id="field-department" name="department" value="${esc(s.department)}" required maxlength="30" placeholder="e.g. CSE" list="known-departments"><datalist id="known-departments">${[...new Set(["CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", ...state.meta.departments])].map((d) => `<option value="${esc(d)}">`).join("")}</datalist></div>${inputField("Graduation year " + req, "graduationYear", "number", s.graduationYear, 'required min="2000" max="2100" step="1"')}${inputField("CGPA " + req, "cgpa", "number", s.cgpa, 'required min="0" max="10" step="0.01" placeholder="0.00 – 10.00"', "On a 10-point scale; up to two decimal places.")}${inputField("Active arrears " + req, "activeArrears", "number", s.activeArrears, 'required min="0" max="100" step="1"', "Current uncleared subjects.")}${inputField("History of arrears " + req, "historyArrears", "number", s.historyArrears, 'required min="0" max="100" step="1"', "Previously held arrears, tracked separately from active arrears.")}<div class="form-field full"><label>Skills <span class="optional">Exact skill names, not proficiency levels</span></label>${tagEditor("skills", s.skills)}<p class="field-help">Press Enter or comma to add. Java and JavaScript are treated as different skills.</p></div>${inputField('Phone number <span class="optional">Optional</span>', "phone", "tel", s.phone, 'maxlength="24" placeholder="e.g. +91 90000 00000" autocomplete="tel"')}</div><datalist id="available-skills">${state.meta.skills.map((sk) => `<option value="${esc(sk)}">`).join("")}</datalist></div><div class="modal-footer"><button type="button" class="button secondary" data-action="close-modal">Cancel</button><button type="submit" class="button primary">${icon("check", 14)}${id ? "Save changes" : "Add student"}</button></div></form>`;
  openModal(
    id ? "Edit student profile" : "Add a student",
    "Build a complete profile for reliable eligibility decisions.",
    body,
  );
}
async function companyForm(id) {
  const token = ++modalLoadToken;
  const c = id
    ? await api("/companies/" + id)
    : {
        criteria: {
          minCgpa: 6.5,
          maxActiveArrears: 0,
          maxHistoryArrears: null,
          requiredSkills: [],
          departments: [],
          graduationYears: [],
        },
      };
  if (token !== modalLoadToken) return;
  const r = c.criteria || {},
    req = '<span class="required">*</span>';
  const body = `<form class="modal-form" id="company-form" data-id="${id || ""}"><div class="modal-body"><div class="form-error" role="alert"></div><div class="form-grid">${inputField("Company name " + req, "name", "text", c.name, 'required maxlength="120" autofocus')}${inputField("Job role " + req, "jobRole", "text", c.jobRole, 'required maxlength="120"')}${inputField('Placement drive date <span class="optional">Optional</span>', "driveDate", "date", c.driveDate)}${inputField('Salary package <span class="optional">Optional</span>', "salaryPackage", "text", c.salaryPackage, 'maxlength="80" placeholder="e.g. 8–12 LPA"')}<div class="form-section-title">Academic requirements</div>${inputField("Minimum CGPA " + req, "minCgpa", "number", r.minCgpa, 'required min="0" max="10" step="0.01"', "Inclusive minimum, on a 10-point scale.")}${inputField("Maximum active arrears " + req, "maxActiveArrears", "number", r.maxActiveArrears, 'required min="0" max="100" step="1"', "A maximum count: 0 means no active arrears permitted.")}<div class="form-field"><label class="checkbox-label" for="consider-history"><input type="checkbox" id="consider-history" ${r.maxHistoryArrears != null ? "checked" : ""}>Consider historical arrears</label><input class="input" id="field-maxHistoryArrears" name="maxHistoryArrears" type="number" min="0" max="100" step="1" value="${r.maxHistoryArrears ?? 0}" ${r.maxHistoryArrears == null ? "disabled" : ""} aria-label="Maximum historical arrears"><p class="field-help">Unchecked means historical arrears are not considered.</p></div>${inputField('Graduation years <span class="optional">Optional</span>', "graduationYears", "text", (r.graduationYears || []).join(", "), 'placeholder="e.g. 2026, 2027"', "Leave blank to allow any graduation year.")}${inputField('Eligible departments <span class="optional">Optional</span>', "departments", "text", (r.departments || []).join(", "), 'maxlength="500" placeholder="e.g. CSE, IT, ECE"', "Comma-separated. Leave blank to allow all departments.")}<div class="form-field full"><label>Required skills <span class="optional">All listed skills must be present</span></label>${tagEditor("requiredSkills", r.requiredSkills)}<p class="field-help">Leave empty for no skill restriction. Matching is exact and case-insensitive.</p></div><div class="form-field full"><label for="field-notes">Additional notes <span class="optional">Optional</span></label><textarea class="textarea" id="field-notes" name="notes" maxlength="2000" placeholder="Helpful information about this placement drive…">${esc(c.notes)}</textarea><p class="field-help">Notes are advisory only. Configure mandatory rules in the fields above.</p></div></div><datalist id="available-skills">${state.meta.skills.map((sk) => `<option value="${esc(sk)}">`).join("")}</datalist></div><div class="modal-footer"><button type="button" class="button secondary" data-action="close-modal">Cancel</button><button type="submit" class="button primary">${icon("check", 14)}${id ? "Save criteria" : "Add company"}</button></div></form>`;
  openModal(
    id ? "Edit company & criteria" : "Add a placement partner",
    "Define exactly what an eligible match looks like.",
    body,
  );
}
async function viewStudent(id) {
  const token = ++modalLoadToken;
  const s = await api("/students/" + id);
  if (token !== modalLoadToken) return;
  const item = (label, value) =>
    `<div class="detail-item"><span>${label}</span><strong>${esc(value)}</strong></div>`;
  openModal(
    "Student profile",
    "Academic information & recorded skills",
    `<div class="modal-body"><div class="detail-header">${avatar(s.fullName, s.id, "lg")}<div><h2>${esc(s.fullName)}</h2><p>${esc(s.registerNumber)}</p></div></div><div class="detail-grid">${item("EMAIL", s.email)}${item("DEPARTMENT", s.department)}${item("GRADUATION YEAR", s.graduationYear)}${item("CGPA / 10", decimal(s.cgpa))}${item("ACTIVE ARREARS", s.activeArrears)}${item("HISTORY OF ARREARS", s.historyArrears)}${item("PHONE", s.phone || "Not provided")}<div class="detail-item full"><span>RECORDED SKILLS</span><div class="chip-list">${chips(s.skills) || "<strong>No skills recorded</strong>"}</div></div></div></div>`,
    `<button class="button secondary" data-action="edit-student" data-id="${s.id}">${icon("edit", 13)}Edit profile</button><a href="#/checker?student=${s.id}" class="button primary" data-action="close-on-nav">Check eligibility ${icon("arrow", 13)}</a>`,
  );
}
async function viewCompany(id) {
  const token = ++modalLoadToken;
  const c = await api("/companies/" + id),
    r = c.criteria || {};
  if (token !== modalLoadToken) return;
  const item = (label, value) =>
    `<div class="detail-item"><span>${label}</span><strong>${esc(value)}</strong></div>`;
  openModal(
    "Company requirements",
    "A complete view of the configured placement criteria",
    `<div class="modal-body"><div class="detail-header">${companyMark(c.name, c.id, true)}<div><h2>${esc(c.name)}</h2><p>${esc(c.jobRole)} · ${money(c.salaryPackage)}</p></div></div><div class="detail-grid">${item("MINIMUM CGPA", decimal(r.minCgpa))}${item("MAXIMUM ACTIVE ARREARS", r.maxActiveArrears)}${item("HISTORICAL ARREARS", r.maxHistoryArrears == null ? "Not considered" : "Maximum " + r.maxHistoryArrears)}${item("DRIVE DATE", c.driveDate ? date(c.driveDate) : "Not scheduled")}${item("ELIGIBLE DEPARTMENTS", r.departments?.join(", ") || "All departments")}${item("GRADUATION YEARS", r.graduationYears?.join(", ") || "Any year")}<div class="detail-item full"><span>ALL REQUIRED SKILLS</span><div class="chip-list">${chips(r.requiredSkills) || "<strong>No skill restriction</strong>"}</div></div>${c.notes ? `<div class="detail-item full"><span>ADVISORY NOTES · NOT AUTOMATICALLY EVALUATED</span><strong style="line-height:1.8">${esc(c.notes)}</strong></div>` : ""}</div></div>`,
    `<button class="button secondary" data-action="edit-company" data-id="${c.id}">${icon("edit", 13)}Edit criteria</button><a href="#/checker?company=${c.id}" class="button primary" data-action="close-on-nav">Check a match ${icon("arrow", 13)}</a>`,
  );
}
function confirmDelete(type, id, name) {
  openModal(
    `Delete ${type}?`,
    "This action cannot be undone.",
    `<div class="modal-body"><div class="confirm-icon">${icon("trash", 22)}</div><p class="confirm-copy">Remove <strong>${esc(name)}</strong> from the workspace?<br>Completed evaluation history and its saved criteria will be preserved.</p></div>`,
    `<button class="button secondary" data-action="close-modal" autofocus>Keep ${type}</button><button class="button danger" data-action="confirm-delete" data-type="${type}" data-id="${id}">${icon("trash", 13)}Delete ${type}</button>`,
    "small-modal",
  );
}
async function viewEvaluation(id) {
  const token = ++modalLoadToken;
  const e = await api("/evaluations/" + id);
  if (token !== modalLoadToken) return;
  openModal(
    `Evaluation #PS-${String(e.id).padStart(4, "0")}`,
    "An immutable snapshot of the original decision.",
    `<div class="modal-body">${evaluationMarkup(e)}<div style="padding:0 21px">${disclaimer}</div></div>`,
    `<button class="button secondary" data-action="print-evaluation" data-id="${e.id}">${icon("print", 13)}Print / Save PDF</button><button class="button primary" data-action="export-evaluation" data-id="${e.id}">${icon("download", 13)}Export CSV</button>`,
    "wide history-modal",
  );
}
function showFormError(form, e) {
  const target = form.querySelector(".form-error");
  target.textContent = e.message;
  target.classList.add("visible");
  form.querySelectorAll(".field-error").forEach((x) => x.remove());
  form
    .querySelectorAll(".field-invalid")
    .forEach((x) => x.classList.remove("field-invalid"));
  Object.entries(e.fields || {}).forEach(([name, message]) => {
    const input = form.querySelector(`[name="${name}"]`);
    if (input) {
      input.classList.add("field-invalid");
      const text = document.createElement("p");
      text.className = "field-error";
      text.textContent = message;
      input.after(text);
    }
  });
  target.scrollIntoView({ behavior: "smooth", block: "nearest" });
}
async function submitForm(form) {
  const button = form.querySelector("[type=submit]"),
    old = button.innerHTML,
    id = form.dataset.id;
  const f = new FormData(form);
  let payload;
  try {
    if (form.id === "student-form") {
      payload = {
        fullName: f.get("fullName"),
        registerNumber: f.get("registerNumber"),
        email: f.get("email"),
        department: f.get("department"),
        graduationYear: Number(f.get("graduationYear")),
        cgpa: f.get("cgpa"),
        activeArrears: Number(f.get("activeArrears")),
        historyArrears: Number(f.get("historyArrears")),
        phone: f.get("phone") || null,
        skills: readTags("skills"),
      };
    } else {
      const split = (value) =>
        String(value || "")
          .split(",")
          .map((v) => v.trim())
          .filter(Boolean);
      const years = split(f.get("graduationYears"));
      if (years.some((y) => !/^\d{4}$/.test(y)))
        throw new ApiError(
          "Graduation years must be comma-separated four-digit years.",
          "VALIDATION",
          { graduationYears: "For example: 2026, 2027" },
        );
      payload = {
        name: f.get("name"),
        jobRole: f.get("jobRole"),
        driveDate: f.get("driveDate") || null,
        salaryPackage: f.get("salaryPackage") || null,
        notes: f.get("notes") || null,
        criteria: {
          minCgpa: f.get("minCgpa"),
          maxActiveArrears: Number(f.get("maxActiveArrears")),
          maxHistoryArrears: document.getElementById("consider-history").checked
            ? Number(f.get("maxHistoryArrears"))
            : null,
          requiredSkills: readTags("requiredSkills"),
          departments: split(f.get("departments")),
          graduationYears: years.map(Number),
        },
      };
    }
    button.disabled = true;
    button.innerHTML = icon("loader", 14, "spin") + "Saving…";
    await api(
      "/" +
        (form.id === "student-form" ? "students" : "companies") +
        (id ? "/" + id : ""),
      { method: id ? "PUT" : "POST", body: payload },
    );
    closeModal();
    toast(
      id
        ? "Changes saved successfully."
        : form.id === "student-form"
          ? "Student profile added."
          : "Company and criteria added.",
    );
    await refreshMeta();
    await renderPage();
  } catch (e) {
    showFormError(form, e);
    button.disabled = false;
    button.innerHTML = old;
  }
}

// PRINT & CSV: no eligibility or report calculations are performed here.
function printDocument(title, content, existingWindow = null) {
  const win = existingWindow || window.open("", "_blank");
  if (!win) {
    toast("Allow pop-ups to open the print-friendly report.", "error");
    return;
  }
  win.document.write(
    `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${esc(title)} · PlaceSure</title><style>body{font:12px Arial,sans-serif;color:#172B4D;margin:36px;line-height:1.6}h1{font-size:23px;letter-spacing:-.5px;margin-bottom:4px}.brand{font-size:14px;font-weight:bold;color:#3977D5;margin-bottom:20px}.meta{color:#7a8597;font-size:10px;margin-bottom:23px}table{width:100%;border-collapse:collapse;font-size:10px;margin-top:20px}th,td{text-align:left;padding:10px 9px;border-bottom:1px solid #e6eaf0;vertical-align:top}th{background:#f5f7fb;color:#627694;font-size:9px}.pass{color:#23856a;font-weight:bold}.fail{color:#bd565e;font-weight:bold}.note{color:#7a8597;font-size:9px;border-top:1px solid #e6eaf0;padding-top:18px;margin-top:28px}.explain{font-size:9px;color:#7a8597;margin-top:5px}.print-button{background:#3977D5;color:white;border:0;padding:10px 17px;border-radius:5px;cursor:pointer;font-size:12px} @media print{body{margin:12mm}.print-button{display:none}thead{display:table-header-group}tr{break-inside:avoid}a{color:inherit;text-decoration:none}}</style></head><body><div class="brand">PlaceSure · Placement Eligibility Checker</div>${content}<div class="note">Eligibility confirms satisfaction of configured requirements only. It does not guarantee shortlisting, an interview, or a job offer.<br>Know Your Eligibility. Own Your Opportunity.</div><button class="print-button" id="print">Print / Save as PDF</button></body></html>`,
  );
  win.document.close();
  win.document
    .getElementById("print")
    .addEventListener("click", () => win.print());
  win.focus();
}
async function printEvaluation(id) {
  const win = window.open("", "_blank");
  if (!win) {
    toast("Allow pop-ups to open the print-friendly report.", "error");
    return;
  }
  win.document.write(
    '<p style="font:14px sans-serif;padding:25px">Preparing your evaluation report…</p>',
  );
  try {
    const e = await api("/evaluations/" + id);
    const content = `<h1>Placement eligibility report</h1><div class="meta">Evaluation #PS-${String(e.id).padStart(4, "0")} · ${esc(dateTime(e.evaluatedAt))}<br>${esc(e.studentName)} · ${esc(e.registerNumber)} · ${esc(e.department)}<br>${esc(e.companyName)} · ${esc(e.jobRole)}</div><h2 class="${e.eligible ? "pass" : "fail"}">${e.eligible ? "ELIGIBLE" : "NOT ELIGIBLE"} · ${e.passedCount} passed / ${e.failedCount} failed</h2><table><thead><tr><th>Criterion</th><th>Student value</th><th>Requirement & explanation</th><th>Result</th></tr></thead><tbody>${e.details.map((d) => `<tr><td>${esc(d.criterion)}</td><td>${esc(d.actual)}</td><td>${esc(d.requirement)}<div class="explain">${esc(d.explanation)}</div></td><td class="${d.passed ? "pass" : "fail"}">${d.passed ? "PASS" : "FAIL"}</td></tr>`).join("")}</tbody></table>`;
    printDocument("Evaluation " + e.id, content, win);
  } catch (e) {
    win.close();
    toast(e.message, "error");
  }
}
function printReport() {
  if (!currentReport) return;
  const r = currentReport;
  printDocument(
    r.title,
    `<h1>${esc(r.title)}</h1><div class="meta">Generated ${esc(dateTime(r.generatedAt))}<br>${esc(r.scope)}<br>${esc(reportFilterDescription())}</div><p>${r.summary.total} evaluation attempts · ${r.summary.eligible} eligible matches</p><table><thead><tr>${r.columns.map((c) => `<th>${esc(c.label)}</th>`).join("")}</tr></thead><tbody>${r.rows.map((row) => `<tr>${r.columns.map((c) => `<td>${esc(c.key === "evaluatedAt" ? dateTime(row[c.key]) : row[c.key])}</td>`).join("")}</tr>`).join("")}</tbody></table>`,
  );
}
function reportFilterDescription() {
  const f = state.report;
  return [
    f.companyId
      ? "Company: " +
        (state.meta.companies || []).find(
          (c) => String(c.id) === String(f.companyId),
        )?.name
      : "All companies",
    f.department ? "Department: " + f.department : "All departments",
    f.from ? "From " + f.from : "",
    f.to ? "Through " + f.to : "",
  ]
    .filter(Boolean)
    .join(" · ");
}
async function download(url) {
  try {
    const response = await fetch(url, { headers: { Accept: "text/csv" } });
    if (!response.ok) {
      const body = await response.json();
      throw new Error(
        body.error?.message ||
          "The report could not be exported. Please try again.",
      );
    }
    const blob = await response.blob(),
      objectUrl = URL.createObjectURL(blob),
      a = document.createElement("a");
    a.href = objectUrl;
    a.download =
      response.headers
        .get("Content-Disposition")
        ?.match(/filename="([^"]+)"/)?.[1] || "placesure-report.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(objectUrl), 10000);
    toast("Your CSV report is ready.");
  } catch (e) {
    toast(
      e.message || "The export failed. Check your connection and try again.",
      "error",
    );
  }
}

// GLOBAL SEARCH (Cmd/Ctrl+K)
function openSearch() {
  openModal(
    "Search workspace",
    "Find a student or company",
    `<div class="modal-body" style="padding:14px"><div class="search-field" style="max-width:none">${icon("search", 16)}<input class="input" id="command-search" placeholder="Search students, registers, companies…" type="search" autocomplete="off" autofocus></div><div id="command-results">${emptyState("Find your next step", "Type at least two characters to search the workspace.", "", "search", true)}</div></div><div class="command-help">Escape to close · Select a result to view its complete details</div>`,
    "",
    "search-modal",
  );
}
async function commandSearch(value) {
  if (value.trim().length < 2) {
    document.getElementById("command-results").innerHTML = emptyState(
      "Find your next step",
      "Type at least two characters to search the workspace.",
      "",
      "search",
      true,
    );
    return;
  }
  try {
    const [s, c] = await Promise.all([
      api("/students?" + query({ q: value, size: 4 })),
      api("/companies?" + query({ q: value, size: 4 })),
    ]);
    if (
      !document.getElementById("command-results") ||
      document.getElementById("command-search").value !== value
    )
      return;
    document.getElementById("command-results").innerHTML =
      (s.items.length
        ? `<div class="command-group-label" style="margin-top:17px">STUDENTS</div>${s.items.map((x) => `<button class="command-result" data-action="view-student" data-id="${x.id}">${avatar(x.fullName, x.id)}<span><strong>${esc(x.fullName)}</strong><small>${esc(x.registerNumber)} · ${esc(x.department)}</small></span>${icon("arrow", 13)}</button>`).join("")}`
        : "") +
      (c.items.length
        ? `<div class="command-group-label" style="margin-top:15px">COMPANIES</div>${c.items.map((x) => `<button class="command-result" data-action="view-company" data-id="${x.id}">${companyMark(x.name, x.id)}<span><strong>${esc(x.name)}</strong><small>${esc(x.jobRole)}</small></span>${icon("arrow", 13)}</button>`).join("")}`
        : "") +
      (!s.items.length && !c.items.length
        ? emptyState(
            "No matches yet",
            "Try a different name, register number, company, or job role.",
            "",
            "search",
            true,
          )
        : "");
  } catch (e) {
    toast(e.message, "error");
  }
}

// EVENT DELEGATION — no inline handlers and no unsafe rendering of user-controlled HTML.
document.addEventListener("click", async (event) => {
  const el = event.target.closest("[data-action]");
  if (!el || el.disabled) return;
  const action = el.dataset.action,
    id = Number(el.dataset.id);
  try {
    switch (action) {
      case "toggle-nav": {
        const open = document
          .getElementById("sidebar")
          .classList.toggle("open");
        document
          .getElementById("sidebar-backdrop")
          .classList.toggle("visible", open);
        el.setAttribute("aria-expanded", open);
        break;
      }
      case "retry":
        await renderPage();
        break;
      case "add-student":
        await studentForm();
        break;
      case "edit-student":
        await studentForm(id);
        break;
      case "view-student":
        await viewStudent(id);
        break;
      case "add-company":
        await companyForm();
        break;
      case "edit-company":
        await companyForm(id);
        break;
      case "view-company":
        await viewCompany(id);
        break;
      case "delete-student":
        confirmDelete("student", id, el.dataset.name);
        break;
      case "delete-company":
        confirmDelete("company", id, el.dataset.name);
        break;
      case "confirm-delete":
        el.disabled = true;
        try {
          await api(
            "/" +
              (el.dataset.type === "student" ? "students" : "companies") +
              "/" +
              id,
            { method: "DELETE" },
          );
          closeModal();
          toast("Record deleted. Evaluation history has been preserved.");
          await refreshMeta();
          await renderPage();
        } catch (e) {
          el.disabled = false;
          throw e;
        }
        break;
      case "close-modal":
        closeModal();
        break;
      case "backdrop-close":
        if (event.target === el) closeModal();
        break;
      case "close-on-nav":
        closeModal();
        break;
      case "remove-tag":
        el.closest("[data-tag-value]").remove();
        break;
      case "choose-student":
        state.checker.student = id;
        state.checker.result = null;
        await renderPage();
        break;
      case "choose-company":
        state.checker.company = id;
        state.checker.result = null;
        await renderPage();
        break;
      case "change-student":
        state.checker.student = null;
        state.checker.result = null;
        await renderPage();
        break;
      case "change-company":
        state.checker.company = null;
        state.checker.result = null;
        await renderPage();
        break;
      case "evaluate":
        await runEvaluation(el);
        break;
      case "view-evaluation":
        await viewEvaluation(id);
        break;
      case "print-evaluation":
        await printEvaluation(id);
        break;
      case "export-evaluation":
        await download("/api/evaluations/" + id + "/export");
        break;
      case "paginate":
        if (el.dataset.group === "report") {
          state.report.page = Number(el.dataset.page);
          document.getElementById("report-table-container").innerHTML =
            reportTableMarkup(currentReport);
        } else {
          state[el.dataset.group].page = Number(el.dataset.page);
          await renderPage();
        }
        break;
      case "clear-students":
        Object.assign(state.students, {
          q: "",
          department: "",
          minCgpa: "",
          maxArrears: "",
          page: 1,
        });
        await renderPage();
        break;
      case "clear-companies":
        Object.assign(state.companies, {
          q: "",
          department: "",
          maxCgpa: "",
          page: 1,
        });
        await renderPage();
        break;
      case "report-type":
        state.report.type = el.dataset.type;
        state.report.status = "";
        state.report.page = 1;
        await renderPage();
        break;
      case "clear-report":
        Object.assign(state.report, {
          companyId: "",
          studentId: "",
          department: "",
          status: "",
          from: "",
          to: "",
          page: 1,
        });
        await renderPage();
        break;
      case "export-report":
        download("/api/reports/export?" + query(state.report));
        break;
      case "print-report":
        printReport();
        break;
      case "export-summary":
        download("/api/reports/export?type=summary");
        break;
      case "export-history":
        download(
          "/api/reports/export?" + query({ ...state.history, type: "history" }),
        );
        break;
      case "refresh-connection":
        await refreshMeta();
        await renderPage();
        toast("Connection status refreshed.");
        break;
      case "open-search":
        openSearch();
        break;
    }
  } catch (e) {
    toast(
      e.message || "This action could not be completed. Please try again.",
      "error",
    );
  }
});
document.addEventListener("submit", (event) => {
  if (["student-form", "company-form"].includes(event.target.id)) {
    event.preventDefault();
    submitForm(event.target);
  }
});
let filterTimer;
const pickerTokens = { student: 0, company: 0 };
document.addEventListener("input", (event) => {
  const el = event.target;
  if (el.id === "command-search") {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => commandSearch(el.value), 220);
    return;
  }
  if (!el.dataset.filter) return;
  const [group, key] = el.dataset.filter.split(".");
  if (group === "picker") {
    const token = ++pickerTokens[key];
    clearTimeout(filterTimer);
    filterTimer = setTimeout(async () => {
      try {
        const page = await api(
          "/" +
            (key === "student" ? "students" : "companies") +
            "?" +
            query({ q: el.value, size: 8, sort: "name", direction: "asc" }),
        );
        if (
          token === pickerTokens[key] &&
          document.getElementById(key + "-picker-results")
        )
          document.getElementById(key + "-picker-results").innerHTML =
            pickerList(key, page.items);
      } catch (e) {
        toast(e.message, "error");
      }
    }, 220);
    return;
  }
  if (el.type === "search") {
    state[group][key] = el.value;
    state[group].page = 1;
    clearTimeout(filterTimer);
    filterTimer = setTimeout(renderPage, 280);
  }
});
document.addEventListener("change", (event) => {
  const el = event.target;
  if (el.id === "consider-history") {
    const input = document.getElementById("field-maxHistoryArrears");
    input.disabled = !el.checked;
    input.required = el.checked;
    return;
  }
  if (el.dataset.action === "page-size") {
    state.students.size = Number(el.value);
    state.students.page = 1;
    localStorage.setItem("placesure-page-size", el.value);
    toast("Your page-size preference has been saved.");
    return;
  }
  if (!el.dataset.filter || el.type === "search") return;
  const [group, key] = el.dataset.filter.split(".");
  if (group === "picker") return;
  if (["from", "to"].includes(key)) {
    const from = key === "from" ? el.value : state[group].from,
      to = key === "to" ? el.value : state[group].to;
    if (from && to && from > to) {
      el.value = state[group][key];
      toast("The start date must not be later than the end date.", "error");
      return;
    }
  }
  if (key === "order") {
    [state[group].sort, state[group].direction] = el.value.split(":");
  } else state[group][key] = el.value;
  state[group].page = 1;
  renderPage();
});
document.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    openSearch();
    return;
  }
  if (event.key === "Escape") {
    closeModal();
    closeNav();
    return;
  }
  if (
    event.target.classList.contains("tag-entry") &&
    (event.key === "Enter" || event.key === ",")
  ) {
    event.preventDefault();
    addTag(event.target.closest(".tag-editor"), event.target.value);
    return;
  }
  if (event.key === "Tab" && document.getElementById("modal-root").firstChild) {
    const focusable = [
        ...document.querySelectorAll(
          "#modal-root button:not([disabled]),#modal-root input:not([disabled]):not([type=hidden]),#modal-root select:not([disabled]),#modal-root textarea,#modal-root a[href]",
        ),
      ].filter((x) => x.getClientRects().length),
      first = focusable[0],
      last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
});
document.getElementById("sidebar-backdrop").addEventListener("click", closeNav);
window.addEventListener("hashchange", () => {
  closeModal(false);
  route();
});
document.getElementById("breadcrumb-icon").innerHTML = icon("home", 13);
document.getElementById("search-icon").innerHTML = icon("search", 14);
document.getElementById("account-icon").innerHTML = icon("down", 12);
document.getElementById("tip-icon").innerHTML = icon("spark", 20);
document.getElementById("menu-toggle").innerHTML = icon("menu", 19);
(async () => {
  await refreshMeta();
  await route();
})();
