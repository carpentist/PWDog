const DEFAULT_API_URL = "http://127.0.0.1:27831";

const apiUrlInput = document.getElementById("apiUrl");
const tokenInput = document.getElementById("token");
const saveButton = document.getElementById("save");
const testButton = document.getElementById("test");
const statusEl = document.getElementById("status");

function setStatus(message, kind = "info") {
  statusEl.textContent = message;
  statusEl.className = `status ${kind}`;
}

function normalizeApiUrl(value) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error("接口地址格式不正确。");
  }
  if (url.protocol !== "http:" || url.hostname !== "127.0.0.1") {
    throw new Error("接口地址必须是 http://127.0.0.1 开头的本机地址。");
  }
  return url.origin;
}

async function load() {
  const stored = await chrome.storage.local.get({
    apiUrl: DEFAULT_API_URL,
    token: ""
  });
  apiUrlInput.value = stored.apiUrl || DEFAULT_API_URL;
  tokenInput.value = stored.token || "";
}

saveButton.addEventListener("click", async () => {
  try {
    const apiUrl = normalizeApiUrl(apiUrlInput.value.trim());
    const token = tokenInput.value.trim();
    if (token.length < 32) {
      throw new Error("配对令牌看起来不完整。");
    }
    await chrome.storage.local.set({ apiUrl, token });
    apiUrlInput.value = apiUrl;
    setStatus("已保存。", "ok");
  } catch (error) {
    setStatus(error.message || "保存失败。", "error");
  }
});

testButton.addEventListener("click", async () => {
  try {
    const apiUrl = normalizeApiUrl(apiUrlInput.value.trim());
    const token = tokenInput.value.trim();
    if (!token) {
      throw new Error("请先填写配对令牌。");
    }
    const response = await fetch(`${apiUrl}/health`, {
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (response.status === 401) {
      setStatus("连接失败：配对令牌无效。", "error");
      return;
    }
    if (!response.ok) {
      setStatus(`连接失败（HTTP ${response.status}）。`, "error");
      return;
    }
    const data = await response.json();
    if (data.locked) {
      setStatus("连接成功，但 PWDog 当前已锁定。", "warn");
    } else {
      setStatus(`连接成功，当前账户：${data.account || "未知"}。`, "ok");
    }
  } catch (error) {
    setStatus(error.message || "无法连接 PWDog。", "error");
  }
});

load();
