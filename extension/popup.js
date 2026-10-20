const DEFAULT_API_URL = "http://127.0.0.1:27831";

const statusEl = document.getElementById("status");
const hostEl = document.getElementById("host");
const listEl = document.getElementById("entries");
const settingsButton = document.getElementById("open-settings");

let activeTabId = null;
let currentHost = null;
let currentSettings = null;

settingsButton.addEventListener("click", () => {
  const opened = pwdogBrowser.runtime.openOptionsPage();
  if (opened && typeof opened.catch === "function") opened.catch(() => {});
  window.close();
});

function normalizeApiUrl(value) {
  try {
    const url = new URL(value);
    if (url.protocol !== "http:" || url.hostname !== "127.0.0.1") {
      return DEFAULT_API_URL;
    }
    return url.origin;
  } catch {
    return DEFAULT_API_URL;
  }
}

async function loadSettings() {
  const stored = await pwdogCall(pwdogBrowser.storage.local, "get", {
    apiUrl: DEFAULT_API_URL,
    token: ""
  });
  return {
    apiUrl: normalizeApiUrl(stored.apiUrl),
    token: String(stored.token || "").trim()
  };
}

function setStatus(message, kind = "info") {
  statusEl.textContent = message;
  statusEl.className = `status ${kind}`;
}

function clearEntries() {
  listEl.replaceChildren();
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }
  return { response, data };
}

async function init() {
  currentSettings = await loadSettings();
  if (!currentSettings.token) {
    setStatus("尚未配对。请先在 PWDog 桌面端解锁，点击“扩展配对”，再打开扩展设置。", "warn");
    return;
  }

  let tabs = [];
  try {
    tabs = await pwdogCall(pwdogBrowser.tabs, "query", { active: true, currentWindow: true });
  } catch {
    tabs = [];
  }
  const tab = tabs[0];
  if (!tab || typeof tab.id !== "number") {
    setStatus("无法读取当前标签页。", "error");
    return;
  }
  activeTabId = tab.id;

  let pageInfo = null;
  try {
    const results = await pwdogCall(pwdogBrowser.scripting, "executeScript", {
      target: { tabId: tab.id },
      func: () => ({
        host: location.hostname.toLowerCase(),
        protocol: location.protocol
      })
    });
    pageInfo = results && results[0] && results[0].result;
  } catch {
    setStatus("无法读取当前页面，请确认这是普通网页。", "error");
    return;
  }

  if (!pageInfo || !["http:", "https:"].includes(pageInfo.protocol) || !pageInfo.host) {
    setStatus("当前页面不支持自动填充。", "warn");
    return;
  }

  currentHost = pageInfo.host;
  hostEl.textContent = currentHost;
  await loadMatches();
}

async function loadMatches() {
  clearEntries();
  setStatus("正在从 PWDog 读取匹配条目...", "info");

  let result;
  try {
    result = await fetchJson(`${currentSettings.apiUrl}/match`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${currentSettings.token}`
      },
      body: JSON.stringify({ host: currentHost })
    });
  } catch {
    setStatus("无法连接 PWDog。请确认桌面程序正在运行。", "error");
    return;
  }

  const { response, data } = result;
  if (response.status === 401) {
    setStatus("配对令牌无效，请在扩展设置中重新填写。", "error");
    return;
  }
  if (response.status === 423) {
    setStatus("PWDog 已锁定，请先在桌面端解锁。", "warn");
    return;
  }
  if (!response.ok || !data) {
    setStatus(`读取失败（HTTP ${response.status}）。`, "error");
    return;
  }

  const entries = Array.isArray(data.entries) ? data.entries : [];
  if (entries.length === 0) {
    setStatus(`没有找到 ${currentHost} 的密码条目。`, "warn");
    return;
  }

  setStatus(`找到 ${entries.length} 个账户，请选择要填充的账户。`, "ok");
  for (const entry of entries) {
    const card = document.createElement("div");
    card.className = "entry";

    const info = document.createElement("div");
    info.className = "entry-info";

    const title = document.createElement("div");
    title.className = "entry-title";
    title.textContent = entry.title || "(未命名)";

    const username = document.createElement("div");
    username.className = "entry-username";
    username.textContent = entry.username || "(无用户名)";

    info.append(title, username);

    const fillButton = document.createElement("button");
    fillButton.type = "button";
    fillButton.textContent = "填充";
    fillButton.addEventListener("click", () => fillEntry(entry.id, fillButton));

    card.append(info, fillButton);
    listEl.append(card);
  }
}

async function fillEntry(entryId, button) {
  button.disabled = true;
  setStatus("正在填充...", "info");

  let result;
  try {
    result = await fetchJson(`${currentSettings.apiUrl}/fill`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${currentSettings.token}`
      },
      body: JSON.stringify({ host: currentHost, entryId })
    });
  } catch {
    button.disabled = false;
    setStatus("无法连接 PWDog。", "error");
    return;
  }

  const { response, data } = result;
  if (response.status === 401) {
    button.disabled = false;
    setStatus("配对令牌无效。", "error");
    return;
  }
  if (response.status === 423) {
    button.disabled = false;
    setStatus("PWDog 已锁定，请先解锁。", "warn");
    return;
  }
  if (!response.ok || !data) {
    button.disabled = false;
    setStatus(`填充失败（HTTP ${response.status}）。`, "error");
    return;
  }

  let scriptResult = null;
  try {
    const results = await pwdogCall(pwdogBrowser.scripting, "executeScript", {
      target: { tabId: activeTabId },
      func: fillInPage,
      args: [data.username || "", data.password || ""]
    });
    scriptResult = results && results[0] && results[0].result;
  } catch {
    button.disabled = false;
    setStatus("无法向当前页面写入。", "error");
    return;
  } finally {
    data.username = "";
    data.password = "";
  }

  if (!scriptResult || !scriptResult.ok) {
    button.disabled = false;
    setStatus(
      scriptResult && scriptResult.reason === "no-password-field"
        ? "页面中没有找到密码输入框。"
        : "填充失败。",
      "error"
    );
    return;
  }

  setStatus(
    scriptResult.passwordFields > 1
      ? "已填充第一个密码框，请检查页面后再登录。"
      : "已填充，请手动点击登录。",
    "ok"
  );
  setTimeout(() => window.close(), 900);
}

function fillInPage(username, password) {
  const isVisible = (element) => {
    if (!(element instanceof HTMLElement)) return false;
    if (element.disabled || element.readOnly) return false;
    const rect = element.getBoundingClientRect();
    const style = window.getComputedStyle(element);
    return rect.width > 0 && rect.height > 0 &&
      style.visibility !== "hidden" && style.display !== "none";
  };

  const setValue = (element, value) => {
    const setter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      "value"
    ).set;
    setter.call(element, value);
    element.dispatchEvent(new Event("input", { bubbles: true }));
    element.dispatchEvent(new Event("change", { bubbles: true }));
  };

  const passwordFields = Array.from(
    document.querySelectorAll('input[type="password"]')
  ).filter(isVisible);

  if (passwordFields.length === 0) {
    return { ok: false, reason: "no-password-field" };
  }

  const passwordField = passwordFields[0];
  const form = passwordField.form || document;
  const candidates = Array.from(form.querySelectorAll("input"))
    .filter((element) => element !== passwordField && isVisible(element))
    .filter((element) => {
      const type = (element.getAttribute("type") || "text").toLowerCase();
      return ["text", "email", "tel", "username", "search"].includes(type);
    });

  const hintPattern = /user|email|login|account|mail/i;
  const usernameField = candidates.find((element) =>
    hintPattern.test(
      `${element.name} ${element.id} ${element.autocomplete} ${element.placeholder}`
    )
  ) || candidates[0] || null;

  if (usernameField && username) {
    setValue(usernameField, username);
  }
  setValue(passwordField, password);
  passwordField.focus();

  return {
    ok: true,
    passwordFields: passwordFields.length,
    filledUsername: Boolean(usernameField && username)
  };
}

init();
