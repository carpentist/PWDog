const pwdogBrowser = globalThis.browser ?? globalThis.chrome;
const pwdogIsFirefox = typeof globalThis.browser !== "undefined";

function pwdogCall(target, method, ...args) {
  if (pwdogIsFirefox) {
    return target[method](...args);
  }
  return new Promise((resolve, reject) => {
    target[method](...args, (result) => {
      const error = globalThis.chrome && globalThis.chrome.runtime
        ? globalThis.chrome.runtime.lastError
        : null;
      if (error) {
        reject(new Error(error.message));
      } else {
        resolve(result);
      }
    });
  });
}
