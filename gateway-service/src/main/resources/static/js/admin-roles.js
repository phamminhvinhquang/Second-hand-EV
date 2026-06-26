// /js/admin-roles.js (lock/unlock focused, no modal)
const API_ADMIN = "/api/admin";
const ADMIN_USERS = `${API_ADMIN}/users`;

function getToken() {
  return localStorage.getItem("token");
}
function authHeaders() {
  const token = getToken();
  return token
    ? { "Content-Type": "application/json", Authorization: "Bearer " + token }
    : { "Content-Type": "application/json" };
}

/* ============================================================ */
/* --- PHẦN THÊM MỚI: GIAO DIỆN THÔNG BÁO ĐẸP (TOAST & CONFIRM) --- */
/* ============================================================ */

// 1. Hàm hiển thị thông báo góc phải (Thay thế alert)
function showToast(message, type = 'success') {
  const isError = type === 'error';
  const toast = document.createElement('div');
  toast.className = `fixed top-5 right-5 z-[9999] flex items-center w-auto min-w-[300px] p-4 mb-4 text-gray-600 bg-white rounded-lg shadow-xl border-l-4 ${isError ? 'border-red-500' : 'border-green-500'} transition-all duration-500 transform translate-y-[-20px] opacity-0`;
  
  const icon = isError 
    ? `<span class="inline-flex items-center justify-center w-8 h-8 mr-3 text-red-500 bg-red-100 rounded-full flex-shrink-0">⚠️</span>`
    : `<span class="inline-flex items-center justify-center w-8 h-8 mr-3 text-green-500 bg-green-100 rounded-full flex-shrink-0">✔</span>`;

  toast.innerHTML = `${icon}<div class="text-sm font-medium">${message}</div>`;
  document.body.appendChild(toast);

  requestAnimationFrame(() => toast.classList.remove('translate-y-[-20px]', 'opacity-0'));
  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-[-20px]');
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// 2. Hàm hiển thị hộp thoại xác nhận (Thay thế confirm)
function showConfirmDialog(message) {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = "fixed inset-0 bg-gray-900 bg-opacity-50 z-[10000] flex items-center justify-center transition-opacity duration-300";
    
    // HTML hộp thoại (Tailwind)
    overlay.innerHTML = `
      <div class="bg-white rounded-lg shadow-2xl p-6 max-w-sm w-full mx-4 transform scale-100">
        <div class="flex items-center mb-4">
          <div class="w-10 h-10 rounded-full bg-yellow-100 text-yellow-600 flex items-center justify-center mr-3">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
          </div>
          <h3 class="text-lg font-bold text-gray-800">Xác nhận</h3>
        </div>
        <p class="text-gray-600 mb-6 text-sm">${message}</p>
        <div class="flex justify-end space-x-3">
          <button id="btn-cancel" class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 text-sm font-medium">Hủy bỏ</button>
          <button id="btn-ok" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm font-medium shadow-md">Đồng ý</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    const close = (result) => {
      overlay.classList.add('opacity-0');
      setTimeout(() => overlay.remove(), 200);
      resolve(result);
    };

    overlay.querySelector('#btn-cancel').onclick = () => close(false);
    overlay.querySelector('#btn-ok').onclick = () => close(true);
  });
}
/* ============================================================ */


/* --- generic fetch helpers (GIỮ NGUYÊN CODE GỐC) --- */
async function apiGet(url) {
  console.log("GET", url);
  const res = await fetch(url, {
    headers: authHeaders(),
    method: "GET",
    mode: "cors",
  });
  if (res.status === 401) {
    // Thay alert
    showToast("Unauthorized. Hãy đăng nhập lại với tài khoản admin.", 'error');
    throw new Error("Unauthorized");
  }
  if (!res.ok) {
    const txt = await res.text().catch(() => res.statusText);
    throw new Error(txt || res.statusText);
  }
  return res.json();
}
async function apiPost(url, body = {}) {
  console.log("POST", url, body);
  const res = await fetch(url, {
    headers: authHeaders(),
    method: "POST",
    body: Object.keys(body).length ? JSON.stringify(body) : null,
    mode: "cors",
  });
  if (res.status === 401) {
    // Thay alert
    showToast("Unauthorized.", 'error');
    throw new Error("Unauthorized");
  }
  if (!res.ok) {
    const txt = await res.text().catch(() => res.statusText);
    throw new Error(txt || res.statusText);
  }
  // may return empty body
  const text = await res.text().catch(() => "");
  try {
    return JSON.parse(text);
  } catch (e) {
    return text || null;
  }
}
async function apiDelete(url) {
  console.log("DELETE", url);
  const res = await fetch(url, {
    headers: authHeaders(),
    method: "DELETE",
    mode: "cors",
  });
  if (res.status === 401) {
    // Thay alert
    showToast("Unauthorized.", 'error');
    throw new Error("Unauthorized");
  }
  if (!res.ok) {
    const txt = await res.text().catch(() => res.statusText);
    throw new Error(txt || res.statusText);
  }
  const text = await res.text().catch(() => "");
  try {
    return JSON.parse(text);
  } catch (e) {
    return text || null;
  }
}

/* --- specific back calls (GIỮ NGUYÊN CODE GỐC) --- */
/* expected backend endpoints:
   POST /api/admin/users/{id}/lock
   POST /api/admin/users/{id}/unlock
   POST /api/admin/users/{id}/roles  { role: "STAFF" }
   DELETE /api/admin/users/{id}/roles/{role}
*/
async function lockUserOnServer(userId) {
  return apiPost(`${ADMIN_USERS}/${userId}/lock`, {});
}
async function unlockUserOnServer(userId) {
  return apiPost(`${ADMIN_USERS}/${userId}/unlock`, {});
}
async function addRoleToUserOnServer(userId, role) {
  return apiPost(`${ADMIN_USERS}/${userId}/roles`, { role });
}
async function removeRoleOnServer(userId, role) {
  return apiDelete(
    `${ADMIN_USERS}/${userId}/roles/${encodeURIComponent(role)}`
  );
}

/* --- small DOM helper (GIỮ NGUYÊN) --- */
function el(tag, cls, inner) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (inner !== undefined) e.innerHTML = inner;
  return e;
}

/* --- load + render (GIỮ NGUYÊN LOGIC) --- */
async function loadUsers() {
  const tbody = document.getElementById("usersTbody");
  if (!tbody) {
    console.error("No #usersTbody");
    return;
  }
  tbody.innerHTML = `<tr><td colspan="5" class="p-3">Đang tải...</td></tr>`;
  try {
    const users = await apiGet(ADMIN_USERS);
    renderUsers(users);
  } catch (err) {
    console.error("loadUsers err", err);
    tbody.innerHTML = `<tr><td colspan="5" class="p-3 text-red-500">Không thể tải users: ${err.message}</td></tr>`;
  }
}

function renderUsers(users) {
  const tbody = document.getElementById("usersTbody");
  tbody.innerHTML = "";
  if (!users || users.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="p-3 text-gray-500">Không có người dùng.</td></tr>`;
    return;
  }

  users.forEach((u) => {
    const tr = el("tr", "border-t");

    tr.appendChild(el("td", "p-3", String(u.userId)));
    tr.appendChild(el("td", "p-3", u.name || ""));
    tr.appendChild(el("td", "p-3", u.email || ""));

    // status badge
    const statusText = (u.accountStatus || "active").toString().toLowerCase();
    const statusBadgeHtml =
      statusText === "locked"
        ? `<span class="inline-block px-2 py-1 rounded bg-red-100 text-red-700 text-xs">LOCKED</span>`
        : `<span class="inline-block px-2 py-1 rounded bg-green-100 text-green-700 text-xs">ACTIVE</span>`;
    tr.appendChild(el("td", "p-3", statusBadgeHtml));

    // roles cell (will be filled)
    const rolesCell = el(
      "td",
      "p-3",
      `<span id="roles-${u.userId}">Đang tải...</span>`
    );
    tr.appendChild(rolesCell);

    // actions: lock/unlock button + quick role add
    const actions = el("td", "p-3 flex items-center");

    // lock/unlock button (replace "Sửa Roles")
    const lockBtnLabel = statusText === "locked" ? "Mở khóa" : "Khóa tài khoản";
    const lockBtn = el(
      "button",
      "px-3 py-1 border rounded text-sm mr-3",
      lockBtnLabel
    );
    
    // --- [CHỈNH SỬA 1] Thay confirm mặc định bằng showConfirmDialog ---
    lockBtn.addEventListener("click", async () => {
      const willUnlock = statusText === "locked";
      const confirmMsg = willUnlock
        ? "Mở khóa tài khoản này?"
        : "Khóa tài khoản này?";
        
      // CŨ: if (!confirm(confirmMsg)) return;
      // MỚI:
      const isConfirmed = await showConfirmDialog(confirmMsg);
      if (!isConfirmed) return;

      try {
        const res = willUnlock
          ? await unlockUserOnServer(u.userId)
          : await lockUserOnServer(u.userId);
        // if server returns updated user -> update row directly
        if (res && typeof res === "object" && (res.userId || res.id)) {
          const updated = res.userId
            ? res
            : res.id
            ? { ...res, userId: res.id }
            : res;
          // update badge + button text + statusText local
          await reloadSingleUserRow(updated.userId);
        } else {
          // fallback: reload full list (safe)
          await loadUsers();
        }
        // Thay alert
        showToast("Thao tác thành công");
      } catch (err) {
        console.error("lock/unlock error", err);
        // Thay alert
        showToast("Thao tác thất bại: " + (err.message || err), 'error');
      }
    });
    actions.appendChild(lockBtn);

    // quick role select + add
    const quickSel = el("select", "border p-1 mr-2 text-sm");
    ["USER", "STAFF", "ADMIN"].forEach((r) =>
      quickSel.appendChild(el("option", "", r))
    );
    const addQuickBtn = el(
      "button",
      "px-3 py-1 bg-emerald-600 text-white rounded text-sm",
      "Thêm"
    );
    addQuickBtn.addEventListener("click", async () => {
      const role = quickSel.value;
      try {
        await addRoleToUserOnServer(u.userId, role);
        await fetchAndShowRoles(u.userId);
        // Thay alert
        showToast(`Đã thêm ${role}`);
      } catch (err) {
        console.error("add role error", err);
        // Thay alert
        showToast("Không thể thêm role: " + (err.message || err), 'error');
      }
    });
    actions.appendChild(quickSel);
    actions.appendChild(addQuickBtn);

    tr.appendChild(actions);
    tbody.appendChild(tr);

    // load roles badges
    fetchAndShowRoles(u.userId);
  });
}

/* try to reload just one row by fetching full list and finding that user.
   (Backend may provide single-user GET; if so replace this with GET /users/{id}) */
async function reloadSingleUserRow(userId) {
  try {
    const all = await apiGet(ADMIN_USERS);
    const user = all.find(
      (x) =>
        String(x.userId) === String(userId) || String(x.id) === String(userId)
    );
    if (!user) {
      await loadUsers();
      return;
    }
    // replace the whole table for simplicity: renderUsers(all) (keeps order)
    renderUsers(all);
  } catch (e) {
    console.error("reloadSingleUserRow fallback failed", e);
    await loadUsers();
  }
}

/* roles fetch + display */
async function fetchAndShowRoles(userId) {
  const container = document.getElementById(`roles-${userId}`);
  if (!container) return;
  container.textContent = "Đang tải...";
  try {
    const roles = await apiGet(`${ADMIN_USERS}/${userId}/roles`);
    container.innerHTML = "";
    if (!roles || roles.length === 0) {
      container.textContent = "—";
      return;
    }
    roles.forEach((r) => {
      const badge = el(
        "span",
        "inline-flex items-center mr-2 px-2 py-1 rounded bg-gray-100 text-xs"
      );
      badge.textContent = r;
      const del = el("button", "ml-2 text-red-500 text-xs", "✕");
      
      // --- [CHỈNH SỬA 2] Thay confirm mặc định bằng showConfirmDialog ---
      del.addEventListener("click", async () => {
        // CŨ: if (!confirm(`Xóa role ${r} khỏi user ${userId}?`)) return;
        // MỚI:
        const isConfirmed = await showConfirmDialog(`Xóa role ${r} khỏi user ${userId}?`);
        if (!isConfirmed) return;

        try {
          await removeRoleOnServer(userId, r);
          await fetchAndShowRoles(userId);
          // Thay alert
          showToast("Đã xóa role");
        } catch (err) {
          console.error("remove role error", err);
          // Thay alert
          showToast("Lỗi: " + (err.message || err), 'error');
        }
      });
      badge.appendChild(del);
      container.appendChild(badge);
    });
  } catch (err) {
    console.error("fetchAndShowRoles", err);
    container.textContent = "Lỗi tải roles";
  }
}

/* --- init --- */
document.addEventListener("DOMContentLoaded", () => {
  const search = document.getElementById("searchInput");
  if (search) {
    search.addEventListener("input", (e) => {
      const q = e.target.value.toLowerCase();
      document.querySelectorAll("#usersTbody tr").forEach((tr) => {
        const txt = tr.textContent.toLowerCase();
        tr.style.display = txt.includes(q) ? "" : "none";
      });
    });
  }
  loadUsers();
});
