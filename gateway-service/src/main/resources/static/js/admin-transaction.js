// admin-transaction.js

// public/js/admin-transaction.js
// ✅ Đã sửa tên biến để tránh trùng lặp
const ADMIN_TRANS_API_BASE = "/api/admin-trans"; 
const SSE_ADMIN_STREAM = "/api/stream/admin";
const PUBLIC_COMPLAINT_API = "/api/complaints";

function escapeHtml(s) {
  if (!s && s !== 0) return "";
  return String(s).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[
        c
      ])
  );
}
function formatCurrency(v) {
  if (v == null) return "—";
  try {
    return Number(v).toLocaleString("vi-VN") + " ₫";
  } catch (e) {
    return String(v);
  }
}

/* --- helper: auth headers --- */
function getAuthHeaders(withJson = false) {
  const headers = { Accept: "application/json" };
  if (withJson) headers["Content-Type"] = "application/json";
  try {
    const token = localStorage.getItem("token");
    if (token) headers["Authorization"] = `Bearer ${token}`;
  } catch (e) {}
  return headers;
}

/**
 * fetchAllTransactions(adminUserId)
 */
async function fetchAllTransactions(adminUserId) {
  // ✅ SỬA: Dùng ADMIN_TRANS_API_BASE thay vì API_BASE
  const url = adminUserId
    ? `${ADMIN_TRANS_API_BASE}/transactions?userId=${encodeURIComponent(adminUserId)}`
    : `${ADMIN_TRANS_API_BASE}/transactions/all`;
    
  const res = await fetch(url, {
    headers: getAuthHeaders(false),
  });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(txt || "HTTP " + res.status);
  }
  return res.json();
}

function buildRow(tx) {
  const id = tx.id;
  const buyerName = escapeHtml(
    tx.customer_full_name ||
      tx.fullName ||
      tx.buyerName ||
      `User#${tx.userId || ""}`
  );
  const productName = escapeHtml(tx.productName || "—");
  const price = tx.price != null ? tx.price : null;
  const status = (tx.status || "waiting_delivery").toLowerCase();

  const imageUrl = tx.productImage || null;
  let imageHtml = "";
  if (imageUrl) {
    const fullImageUrl = imageUrl.startsWith("http")
      ? imageUrl
      : `${imageUrl}`;
    imageHtml = `<img src="${fullImageUrl}" alt="Ảnh sản phẩm" />`;
  }

  const statusSelect = `
    <select class="tx-status" data-id="${id}">
      <option value="waiting_delivery" ${
        status === "waiting_delivery" ? "selected" : ""
      }>Chờ giao hàng</option>
      <option value="completed" ${
        status === "completed" ? "selected" : ""
      }>Hoàn thành</option>
      <option value="returned" ${
        status === "returned" ? "selected" : ""
      }>Trả hàng</option>
    </select>
  `;

  const complaintCount = Number(tx.complaintCount || 0);

  const row = document.createElement("tr");
  row.dataset.txId = id;
  row.dataset.address = tx.address || "";

  row.innerHTML = `
    <td class="col-check"><input class="row-check" type="checkbox" data-id="${id}" /></td>
    <td class="col-user"><strong>${buyerName}</strong></td>
    <td class="col-image">${imageHtml}</td> 
    <td class="col-product">${productName}</td>
    <td class="col-price">${formatCurrency(price)}</td>
    <td class="col-status">${statusSelect}</td>
    <td class="col-complaint" data-purchase-id="${id}" style="cursor:pointer;text-align:center">—(${complaintCount})—</td>
    <td class="col-actions">
      <button class="action-btn delete" data-action="delete" data-id="${id}">Xóa</button>
    </td>
  `;
  return row;
}

function showInlineError(msg) {
  const container = document.querySelector(".admin-main") || document.body;
  const existing = document.getElementById("admin-error-box");
  if (existing) existing.remove();
  const err = document.createElement("div");
  err.id = "admin-error-box";
  err.style.background = "#ffe9e9";
  err.style.border = "1px solid #f5c2c2";
  err.style.color = "#7a1c1c";
  err.style.padding = "12px";
  err.style.margin = "12px 0";
  err.style.borderRadius = "8px";
  err.textContent = msg;
  container.prepend(err);
}

async function deleteTransaction(id) {
  // ✅ SỬA: Dùng ADMIN_TRANS_API_BASE
  const res = await fetch(`${ADMIN_TRANS_API_BASE}/transactions/${id}`, {
    method: "DELETE",
    headers: getAuthHeaders(false),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(body || "HTTP " + res.status);
  }
  return res.json();
}

async function updateTransaction(id, payload) {
  // ✅ SỬA: Dùng ADMIN_TRANS_API_BASE
  const res = await fetch(`${ADMIN_TRANS_API_BASE}/transactions/${id}`, {
    method: "PUT",
    headers: getAuthHeaders(true),
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(body || "HTTP " + res.status);
  }
  return res.json();
}

async function initializeApp() {
  const tbody = document.querySelector(".admin-table tbody");
  if (!tbody) {
    showInlineError("Không tìm thấy table để hiển thị dữ liệu.");
    return;
  }

  let adminUserId = null;
  try {
    const u = JSON.parse(localStorage.getItem("user") || "null");
    adminUserId = u?.userId || u?.id || null;
  } catch (e) {
    adminUserId = null;
  }

  function ensureAdminComplaintModal() {
    if (window._adminComplaintModalEl) return window._adminComplaintModalEl;
    const backdrop = document.createElement("div");
    backdrop.className = "complaint-modal-backdrop admin-complaint-modal";
    backdrop.innerHTML = `
      <div class="complaint-modal-dialog admin-complaint-modal-dialog" role="dialog" aria-modal="true">
        <div class="complaint-modal-header">
          <h3 class="complaint-modal-title">Khiếu nại - Quản trị</h3>
          <button class="complaint-modal-close" aria-label="Đóng">&times;</button>
        </div>
        <div class="complaint-modal-body">
          <div class="summary-line summary-product"></div>
          <div class="summary-line summary-user"></div>
          <div class="existing-complaints" style="max-height:320px;overflow:auto;padding-right:8px"></div>
          <label class="label">Trả lời (gửi tới người gửi)</label>
          <textarea class="complaint-textarea admin-reply" rows="6" placeholder="Viết trả lời cho người mua..."></textarea>
          <div class="complaint-status"></div>
        </div>
        <div class="complaint-modal-footer">
          <button class="btn small complaint-cancel">Hủy</button>
          <button class="btn btn-primary complaint-send">Gửi trả lời</button>
        </div>
      </div>
    `;
    document.body.appendChild(backdrop);
    const dialogEl = backdrop.querySelector(".complaint-modal-dialog");
    if (dialogEl) dialogEl.classList.add("admin-complaint-modal-dialog");
    const closeBtn = backdrop.querySelector(".complaint-modal-close");
    const cancelBtn = backdrop.querySelector(".complaint-cancel");
    closeBtn.addEventListener("click", hideAdminComplaintModal);
    cancelBtn.addEventListener("click", hideAdminComplaintModal);
    backdrop.addEventListener("click", (ev) => {
      if (ev.target === backdrop) hideAdminComplaintModal();
    });
    document.addEventListener("keydown", (ev) => {
      if (!backdrop) return;
      if (backdrop.classList.contains("open") && ev.key === "Escape")
        hideAdminComplaintModal();
    });

    backdrop
      .querySelector(".complaint-send")
      .addEventListener("click", async () => {
        const statusEl = backdrop.querySelector(".complaint-status");
        const replyArea = backdrop.querySelector(".admin-reply");
        const replyText = replyArea.value.trim();
        if (!replyText) {
          statusEl.textContent = "Vui lòng nhập nội dung trả lời.";
          statusEl.classList.add("error");
          return;
        }
        statusEl.classList.remove("error");
        statusEl.textContent = "Đang gửi trả lời...";
        const sendBtn = backdrop.querySelector(".complaint-send");
        sendBtn.disabled = true;

        let aId = adminUserId;
        try {
          if (!aId) {
            const user = JSON.parse(localStorage.getItem("user") || "null");
            aId = user?.userId || user?.id || null;
          }
        } catch (e) {}
        if (!aId) {
          statusEl.textContent = "Không xác định adminUserId.";
          sendBtn.disabled = false;
          return;
        }

        const purchaseId = backdrop.dataset.purchaseId;
        if (!purchaseId) {
          statusEl.textContent =
            "Không tìm thấy khiếu nại / purchaseId để trả lời.";
          sendBtn.disabled = false;
          return;
        }

        try {
          const payload = {
            purchaseId: Number(purchaseId),
            senderName: `Admin#${aId}`,
            senderPhone: "",
            senderEmail: "",
            detail: replyText,
            adminUserId: Number(aId),
          };

          const res = await fetch(PUBLIC_COMPLAINT_API, {
            method: "POST",
            headers: getAuthHeaders(true),
            body: JSON.stringify(payload),
          });

          if (!res.ok) {
            const txt = await res.text().catch(() => "");
            throw new Error(txt || `HTTP ${res.status}`);
          }

          const saved = await res.json();
          const nowStr = saved?.createdAt || new Date().toISOString();
          appendMessageToAdminModal(backdrop, {
            senderName: `Admin#${aId}`,
            content: replyText,
            createdAt: nowStr,
          });

          replyArea.value = "";
          statusEl.textContent = "Gửi trả lời thành công.";
          sendBtn.disabled = false;

          try {
            const tr = document.querySelector(`tr[data-tx-id='${purchaseId}']`);
            if (tr) {
              const cell = tr.querySelector("td.col-complaint");
              if (cell) {
                const m = cell.textContent.match(/\((\d+)\)/);
                const current = m ? Number(m[1]) : 0;
                cell.textContent = `—(${current + 1})—`;
              }
            }
          } catch (e) {}

          if (saved && saved.id) {
            backdrop.dataset.complaintId = saved.id;
          }
        } catch (err) {
          statusEl.textContent = "Gửi thất bại: " + err.message;
          sendBtn.disabled = false;
          console.error("Reply failed:", err);
        }
      });

    window._adminComplaintModalEl = backdrop;
    return backdrop;
  }

  window.ensureAdminComplaintModal = ensureAdminComplaintModal;

  async function loadComplaintsForAdminModal(purchaseId) {
    const modal = ensureAdminComplaintModal();
    const container = modal.querySelector(".existing-complaints");
    container.innerHTML = "Đang tải...";
    let aId = adminUserId;
    try {
      if (!aId) {
        const u = JSON.parse(localStorage.getItem("user") || "null");
        aId = u?.userId || u?.id || null;
      }
    } catch (e) {
      aId = null;
    }
    if (!aId) {
      container.innerHTML =
        "<div style='color:#b91c1c'>Không xác định adminUserId</div>";
      return;
    }
    try {
      // ✅ SỬA: Dùng ADMIN_TRANS_API_BASE
      const res = await fetch(
        `${ADMIN_TRANS_API_BASE}/complaints/purchase/${encodeURIComponent(
          purchaseId
        )}?userId=${encodeURIComponent(aId)}`,
        { headers: getAuthHeaders(false) }
      );
      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(text || "HTTP " + res.status);
      }
      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        container.innerHTML =
          "<div style='color:#6b7280'>Chưa có khiếu nại.</div>";
        const tr = document.querySelector(`tr[data-tx-id='${purchaseId}']`);
        if (tr) {
          const cell = tr.querySelector("td.col-complaint");
          if (cell) cell.textContent = "—(0)—";
        }
        return;
      }

      list.sort((a, b) => {
        const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return ta - tb;
      });

      container.innerHTML = "";
      list.forEach((c) => {
        const sender = c.senderName || "Người gửi";
        const isSenderAdmin = sender.toLowerCase().startsWith("admin");
        appendMessageToAdminModal(modal, {
          senderName: sender,
          content: c.detail || "",
          createdAt: c.createdAt || new Date().toISOString(),
          isAdmin: isSenderAdmin,
        });

        if (c.adminResponse) {
          appendMessageToAdminModal(modal, {
            senderName: "Admin (Phản hồi)",
            content: c.adminResponse,
            createdAt: c.repliedAt || new Date().toISOString(),
            isAdmin: true,
          });
        }
      });

      container.scrollTop = container.scrollHeight;
      const tr = document.querySelector(`tr[data-tx-id='${purchaseId}']`);
      if (tr) {
        const cell = tr.querySelector("td.col-complaint");
        if (cell) cell.textContent = `—(${list.length})—`;
      }
      if (list.length > 0) {
        modal.dataset.complaintId = list[list.length - 1].id;
      } else {
        delete modal.dataset.complaintId;
      }
    } catch (err) {
      container.innerHTML = `<div style="color:#b91c1c">Không thể tải khiếu nại: ${escapeHtml(
        err.message
      )}</div>`;
      console.error("Load complaints failed:", err);
    }
  }

  function openAdminComplaintModal({
    purchaseId = null,
    productName = "",
    userName = "",
  } = {}) {
    const m = ensureAdminComplaintModal();
    m.querySelector(
      ".summary-product"
    ).textContent = `Sản phẩm: ${productName}`;
    m.querySelector(".summary-user").textContent = `Người đặt: ${userName}`;
    m.classList.add("open");
    document.body.style.overflow = "hidden";
    if (purchaseId) {
      m.dataset.purchaseId = purchaseId;
      loadComplaintsForAdminModal(purchaseId);
    }
    try {
      const u = JSON.parse(localStorage.getItem("user") || "null");
      const aId = u?.userId || u?.id || adminUserId || null;
      if (aId) startAdminSse(aId);
    } catch (e) {}
  }
  window.openAdminComplaintModal = openAdminComplaintModal;

  function hideAdminComplaintModal() {
    const m = window._adminComplaintModalEl;
    if (!m) return;
    m.classList.remove("open");
    document.body.style.overflow = "";
    delete m.dataset.complaintId;
    delete m.dataset.purchaseId;
    const area = m.querySelector(".admin-reply");
    if (area) area.value = "";
    const status = m.querySelector(".complaint-status");
    if (status) status.textContent = "";
  }

  let adminSse = null;
  function startAdminSse(adminUserIdForSse) {
    if (!adminUserIdForSse || adminSse) return;
    try {
      adminSse = new EventSource(
        `${SSE_ADMIN_STREAM}?adminUserId=${encodeURIComponent(
          adminUserIdForSse
        )}`
      );
      adminSse.onmessage = (ev) => {
        try {
          const text = ev.data;
          if (!text) return;
          const payload = JSON.parse(text);
          if (!payload || payload.type !== "complaint.message") return;
          const modal = window._adminComplaintModalEl;
          if (
            modal &&
            modal.dataset.purchaseId &&
            String(modal.dataset.purchaseId) === String(payload.purchaseId)
          ) {
            appendMessageToAdminModal(modal, {
              senderName: payload.senderName,
              content: payload.content,
              createdAt: payload.createdAt,
            });
            loadComplaintsForAdminModal(modal.dataset.purchaseId).catch(
              () => {}
            );
          } else {
            const tr = document.querySelector(
              `tr[data-tx-id='${payload.purchaseId}']`
            );
            if (tr) {
              const cell = tr.querySelector("td.col-complaint");
              if (cell) {
                cell.style.fontWeight = "700";
                cell.style.color = "#165e3b";
                cell.title = "Có khiếu nại mới";
                try {
                  const m = cell.textContent.match(/\((\d+)\)/);
                  const current = m ? Number(m[1]) : 0;
                  cell.textContent = `—(${current + 1})—`;
                } catch (e) {}
              }
            }
          }
        } catch (e) {}
      };
      adminSse.onerror = (e) => {
        console.warn("Admin SSE error", e);
      };
    } catch (e) {
      console.warn("Start admin SSE failed", e);
    }
  }

  function appendMessageToAdminModal(
    modal,
    { senderName, content, createdAt, isAdmin = undefined } = {}
  ) {
    const container = modal.querySelector(".existing-complaints");
    if (!container) return;

    const isAdminMsg =
      typeof isAdmin !== "undefined"
        ? Boolean(isAdmin)
        : typeof senderName === "string" &&
          senderName.toLowerCase().startsWith("admin");

    const conv = document.createElement("div");
    conv.className = "conv-item";
    conv.style.display = "flex";
    conv.style.flexDirection = "column";
    conv.style.gap = "8px";
    conv.style.marginBottom = "6px";

    const bubble = document.createElement("div");
    bubble.className = "msg-bubble " + (isAdminMsg ? "msg-admin" : "msg-user");

    let timeStr = "";
    if (createdAt) {
        const dateStr = createdAt.endsWith("Z") ? createdAt : createdAt + "Z";
        timeStr = new Date(dateStr).toLocaleString("vi-VN"); 
    }

    bubble.innerHTML = `
    <div style="font-weight:700">${escapeHtml(
      senderName || ""
    )} • ${timeStr}</div>
    <div style="margin-top:6px">${escapeHtml(content || "")}</div>
  `;

    conv.appendChild(bubble);
    container.appendChild(conv);
    container.scrollTop = container.scrollHeight;
  }

  async function loadAndRender() {
    try {
      tbody.innerHTML = `<tr><td colspan="8" style="padding:18px;text-align:center">Đang tải...</td></tr>`;
      const list = await fetchAllTransactions(adminUserId);
      if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="padding:18px;text-align:center;color:#6b7280">Chưa có giao dịch.</td></tr>`;
        return;
      }
      tbody.innerHTML = "";
      list.forEach((tx) => tbody.appendChild(buildRow(tx)));
    } catch (err) {
      console.error("Load error", err);
      showInlineError("Lỗi khi tải danh sách giao dịch: " + err.message);
      tbody.innerHTML = `<tr><td colspan="8" style="padding:18px;text-align:center;color:#6b7280">Không thể tải dữ liệu.</td></tr>`;
    }
  }

  await loadAndRender();

  document.addEventListener("click", async (ev) => {
    const dbtn = ev.target.closest("button.action-btn.delete");
    if (!dbtn) return;
    const id = dbtn.dataset.id;
    if (!confirm("Xác nhận xóa giao dịch này?")) return;
    try {
      await deleteTransaction(id);
      const tr = document.querySelector(`tr[data-tx-id='${id}']`);
      if (tr) tr.remove();
    } catch (err) {
      alert("Xóa thất bại: " + err.message);
    }
  });

  document.addEventListener("change", async (ev) => {
    const sel = ev.target.closest("select.tx-status");
    if (sel) {
      const id = sel.dataset.id;
      const newStatus = sel.value;
      try {
        await updateTransaction(id, { status: newStatus });
      } catch (err) {
        alert("Cập nhật trạng thái thất bại: " + err.message);
      }
      return;
    }
  });

  document.addEventListener("click", (ev) => {
    const cell = ev.target.closest("td.col-complaint");
    if (!cell) return;
    const tr = cell.closest("tr");
    if (!tr) return;
    const buyerName = (
      tr.querySelector(".col-user strong")?.textContent || ""
    ).trim();
    const productName = (
      tr.querySelector(".col-product")?.textContent || ""
    ).trim();
    const address = tr.dataset.address || "";
    const purchaseId = tr.dataset.txId || cell.dataset.purchaseId || null;

    if (typeof window.openAdminComplaintModal === "function") {
      window.openAdminComplaintModal({
        purchaseId,
        productName,
        userName: buyerName,
      });
      return;
    }

    if (typeof window.openComplaintModal === "function") {
      window.openComplaintModal({ productName, userName: buyerName, address });
      return;
    }
  });

  const selectAll = document.getElementById("select-all");
  function updateSelectAll() {
    const all = Array.from(document.querySelectorAll(".row-check"));
    if (!selectAll) return;
    selectAll.checked = all.length > 0 && all.every((c) => c.checked);
  }
  document.addEventListener("change", (ev) => {
    if (ev.target.id === "select-all") {
      const checked = ev.target.checked;
      document
        .querySelectorAll(".row-check")
        .forEach((cb) => (cb.checked = checked));
    }
    if (ev.target.classList.contains("row-check")) updateSelectAll();
  });

  try {
    if (adminUserId) startAdminSse(adminUserId);
    else {
      const u = JSON.parse(localStorage.getItem("user") || "null");
      const aId = u?.userId || u?.id || null;
      if (aId) startAdminSse(aId);
    }
  } catch (e) {}

  window.refreshAdminTransactions = loadAndRender;
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeApp);
} else {
  initializeApp();
}
