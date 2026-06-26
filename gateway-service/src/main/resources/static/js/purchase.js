// /js/purchase.js
// kết hợp với backend endpoints hiện có:
// COMPLAINT_API = /api/complaints
// SSE_USER_STREAM = /api/stream/user

const PURCHASE_API_BASE = "/api/purchases";
const COMPLAINT_API = "/api/complaints";
const SSE_USER_STREAM = "/api/stream/user";

document.addEventListener("DOMContentLoaded", async () => {
  const styles = `
        /* --- CSS CŨ CHO CARD --- */
        .order-body-content {
            display: flex;
            gap: 16px;
            padding: 16px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        .purchase-image-wrapper {
            width: 90px;
            height: 90px;
            flex-shrink: 0;
            border-radius: 8px;
            overflow: hidden;
            border: 1px solid #eee;
            background-color: #f9f9f9;
        }
        .purchase-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
        .purchase-details {
            flex-grow: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
        }
        .purchase-product-link, .purchase-product-no-link {
            font-weight: 600;
            color: #333;
        }
        .order-card .shop-row {
            padding-bottom: 8px; 
            border-bottom: none;
        }

        /* --- CSS MỚI: THANH CUỘN & HIỆN 3 SẢN PHẨM --- */
        .view-panel {
            display: none;
            max-height: 720px; /* Mỗi card ~240px, 3 card = 720px */
            overflow-y: auto;
            padding-right: 8px;
            animation: fadeIn 0.4s ease-in-out;
        }
        .view-panel.active {
            display: block;
        }
        /* Hiệu ứng Fade in */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(5px); }
            to { opacity: 1; transform: translateY(0); }
        }
        /* Custom Scrollbar */
        .view-panel::-webkit-scrollbar { width: 8px; }
        .view-panel::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 4px; }
        .view-panel::-webkit-scrollbar-thumb { background: #178a48; border-radius: 4px; }
        .view-panel::-webkit-scrollbar-thumb:hover { background: #147a3f; }

        /* --- CSS MỚI: TAB TRƯỢT --- */
        .order-tabs ul {
            position: relative; /* Để neo thanh trượt */
            border-bottom: none;
        }
        /* Ẩn gạch chân CSS cũ */
        .order-tabs .tab.active::after { display: none !important; }
        
        /* Thanh gạch chân trượt */
        .tab-line {
            position: absolute;
            bottom: 0;
            left: 0;
            height: 3px;
            background: #178a48; /* Màu accent */
            border-radius: 3px 3px 0 0;
            transition: left 0.35s cubic-bezier(0.25, 0.8, 0.25, 1), width 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
            pointer-events: none;
        }
    `;
  const styleSheet = document.createElement("style");
  styleSheet.type = "text/css";
  styleSheet.innerText = styles;
  document.head.appendChild(styleSheet);
  // === 🛑 KẾT THÚC BỔ SUNG CSS ===

  // small helpers
  function escapeHtml(s) {
    if (!s && s !== 0) return "";
    return String(s).replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        }[c])
    );
  }

  const el = (tag, props = {}, children = []) => {
    const e = document.createElement(tag);
    Object.entries(props).forEach(([k, v]) => {
      if (k === "class") e.className = v;
      else if (k === "html") e.innerHTML = v;
      else if (k === "text") e.textContent = v;
      else e.setAttribute(k, v);
    });
    (Array.isArray(children) ? children : [children]).forEach((ch) => {
      if (!ch) return;
      if (typeof ch === "string") e.appendChild(document.createTextNode(ch));
      else e.appendChild(ch);
    });
    return e;
  };

  // --- TABS & VIEWS (LOGIC MỚI) ---
  const tabUl = document.querySelector(".order-tabs ul");
  // Tạo thanh trượt
  const tabLine = document.createElement("div");
  tabLine.className = "tab-line";
  tabLine.style.width = "0px";
  if (tabUl) tabUl.appendChild(tabLine);

  const tabEls = document.querySelectorAll(".order-tabs .tab");
  const viewWaiting = document.getElementById("view-waiting");
  const viewCompleted = document.getElementById("view-completed");
  const viewCancelled = document.getElementById("view-cancelled");

  function setActiveTab(key) {
    let activeTabEl = null;

    // Toggle class active cho Tab
    tabEls.forEach((t) => {
      const isActive = t.dataset.filter === key;
      t.classList.toggle("active", isActive);
      if (isActive) activeTabEl = t;
    });

    // Toggle class active cho Panel View
    document.querySelectorAll(".view-panel").forEach((v) => {
      v.classList.toggle("active", v.dataset.view === key);
      // Reset scroll về đầu khi chuyển tab
      if (v.dataset.view === key) v.scrollTop = 0;
    });

    // Di chuyển thanh gạch chân (Tab Line)
    if (activeTabEl && tabLine) {
      tabLine.style.width = `${activeTabEl.offsetWidth}px`;
      tabLine.style.left = `${activeTabEl.offsetLeft}px`;
    }
  }

  // Gắn sự kiện Click
  tabEls.forEach((t) => {
    t.addEventListener("click", () => setActiveTab(t.dataset.filter));
  });

  // Init: Đặt thanh trượt vào vị trí tab đang active ban đầu
  setTimeout(() => {
    const active = document.querySelector(".order-tabs .tab.active");
    if (active) setActiveTab(active.dataset.filter);
  }, 100);

  // --- END TABS LOGIC ---

  // complaint modal (lazy) with updated chat behavior
  let complaintModalEl = null;
  function ensureComplaintModal() {
    if (complaintModalEl) return complaintModalEl;

    const backdrop = el("div", {
      class: "complaint-modal-backdrop user-complaint-modal",
      role: "dialog",
      "aria-hidden": "true",
    });

    const dialog = el(
      "div",
      { class: "complaint-modal-dialog user-complaint-modal-dialog" },
      [
        el("header", { class: "complaint-modal-header" }, [
          el("h3", {
            class: "complaint-modal-title",
            text: "Khiếu nại đơn hàng",
          }),
          el("button", {
            class: "complaint-modal-close",
            "aria-label": "Đóng",
            html: "&times;",
          }),
        ]),
        el("div", { class: "complaint-modal-body" }, [
          el("div", { class: "complaint-order-summary" }, [
            el("div", { class: "summary-line summary-product", text: "" }),
            el("div", { class: "summary-line summary-user", text: "" }),
            el("div", { class: "summary-line summary-address", text: "" }),
          ]),
          // area to list existing complaints + admin responses (chat)
          el("div", { class: "existing-complaints", html: "" }),
          el("label", { class: "label", text: "Lý do khiếu nại" }),
          el("textarea", {
            class: "complaint-textarea",
            rows: "4",
            placeholder: "Mô tả chi tiết vấn đề...",
          }),
          el("div", { class: "complaint-status", text: "" }),
        ]),
        el("footer", { class: "complaint-modal-footer" }, [
          el("button", { class: "btn small complaint-cancel", text: "Hủy" }),
          el("button", { class: "btn primary complaint-send", text: "Gửi" }),
        ]),
      ]
    );
    backdrop.appendChild(dialog);
    document.body.appendChild(backdrop);
    complaintModalEl = backdrop;

    complaintModalEl
      .querySelector(".complaint-modal-close")
      .addEventListener("click", hideComplaintModal);
    complaintModalEl
      .querySelector(".complaint-cancel")
      .addEventListener("click", hideComplaintModal);
    backdrop.addEventListener("click", (ev) => {
      if (ev.target === backdrop) hideComplaintModal();
    });
    document.addEventListener("keydown", (ev) => {
      if (!complaintModalEl) return;
      if (complaintModalEl.classList.contains("open") && ev.key === "Escape")
        hideComplaintModal();
    });

    // SEND: after success append message locally (do not reload whole chat)
    complaintModalEl
      .querySelector(".complaint-send")
      .addEventListener("click", async () => {
        const detailEl = complaintModalEl.querySelector(".complaint-textarea");
        const detail = detailEl.value.trim();
        const statusEl = complaintModalEl.querySelector(".complaint-status");
        if (!detail) {
          statusEl.textContent = "Vui lòng mô tả chi tiết khiếu nại.";
          statusEl.classList.add("error");
          return;
        }
        statusEl.classList.remove("error");
        statusEl.textContent = "Đang gửi khiếu nại...";
        const sendBtn = complaintModalEl.querySelector(".complaint-send");
        sendBtn.disabled = true;

        // collect info (we stored metadata on modal)
        const purchaseId = complaintModalEl.dataset.purchaseId;
        const senderName = complaintModalEl.dataset.senderName || "Bạn";
        const senderPhone = complaintModalEl.dataset.senderPhone || "";
        const senderEmail = complaintModalEl.dataset.senderEmail || "";

        try {
          const res = await fetch(COMPLAINT_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              purchaseId: Number(purchaseId),
              senderName,
              senderPhone,
              senderEmail,
              detail,
            }),
          });
          if (!res.ok) {
            const body = await res.text().catch(() => "");
            throw new Error(body || "HTTP " + res.status);
          }
          const saved = await res.json();

          // append user's new message locally (no full reload)
          appendMessageToModal(complaintModalEl, {
            senderName: senderName || "Bạn",
            content: detail,
            createdAt: new Date().toISOString(),
            isAdmin: false,
          });

          detailEl.value = "";
          statusEl.textContent =
            "Khiếu nại đã gửi thành công. Hệ thống sẽ phản hồi.";
          sendBtn.disabled = false;

          // optional: keep modal.dataset.complaintId -> could be set to saved.id
          if (saved && saved.id)
            complaintModalEl.dataset.complaintId = saved.id;

          // small auto-close after short delay (optional)
          setTimeout(() => {
            // do not clear messages so user can still read admin replies
            // hideComplaintModal();
          }, 800);
        } catch (err) {
          statusEl.textContent = "Gửi không thành công: " + err.message;
          statusEl.classList.add("error");
          sendBtn.disabled = false;
          console.error("Complaint send failed:", err);
        }
      });

    return complaintModalEl;
  }

  // load existing complaint list (render oldest -> newest)
  async function loadExistingComplaintsForModal(purchaseId) {
    const modal = ensureComplaintModal();
    const container = modal.querySelector(".existing-complaints");
    container.innerHTML = "Đang tải lịch sử khiếu nại...";
    // find logged user id
    let uId = loggedUserId;
    if (!uId) {
      try {
        uId = Number(new URLSearchParams(window.location.search).get("userId"));
      } catch (e) {}
    }
    if (!uId) {
      container.innerHTML =
        "<div style='color:#6b7280'>Không có userId để tải khiếu nại.</div>";
      return;
    }
    try {
      const res = await fetch(
        `${COMPLAINT_API}/purchase/${encodeURIComponent(
          purchaseId
        )}?userId=${encodeURIComponent(uId)}`
      );
      if (!res.ok) {
        const body = await res.text().catch(() => "");
        throw new Error(body || "HTTP " + res.status);
      }
      const list = await res.json();
      if (!Array.isArray(list) || list.length === 0) {
        container.innerHTML =
          "<div style='color:#6b7280'>Chưa có khiếu nại trước đó.</div>";
        return;
      }

      // sort ascending (oldest first)
      list.sort((a, b) => {
        const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return ta - tb;
      });

      container.innerHTML = "";
      // For each complaint record: first user message (as purchased), then admin response bubble (if any).
      list.forEach((c) => {
        // user message
        appendMessageToModal(modal, {
          senderName: c.senderName || "Người mua",
          content: c.detail || "",
          createdAt: c.createdAt || new Date().toISOString(),
        });
        // admin response (if any)
        if (c.adminResponse) {
          appendMessageToModal(modal, {
            senderName: `Admin`,
            content: c.adminResponse || "",
            createdAt: c.repliedAt || new Date().toISOString(),
            isAdmin: true,
          });
        }
      });

      // scroll to bottom
      container.scrollTop = container.scrollHeight;

      // store complaint id (latest) for replying reference
      const modalEl = ensureComplaintModal();
      if (list.length > 0)
        modalEl.dataset.complaintId = list[list.length - 1].id;
      else delete modalEl.dataset.complaintId;
    } catch (err) {
      container.innerHTML = `<div style="color:#b91c1c">Không thể tải khiếu nại: ${escapeHtml(
        err.message
      )}</div>`;
      console.error("Load complaints failed:", err);
    }
  }
0
  function openComplaintModal({
    purchaseId = null,
    productName = "",
    userName = "",
    address = "",
    senderName = "",
    senderPhone = "",
    senderEmail = "",
  } = {}) {
    const m = ensureComplaintModal();
    m.querySelector(
      ".summary-product"
    ).textContent = `Sản phẩm: ${productName}`;
    m.querySelector(".summary-user").textContent = `Người đặt: ${userName}`;
    m.querySelector(".summary-address").textContent = `Địa chỉ: ${address}`;
    m.querySelector(".complaint-textarea").value = "";
    m.querySelector(".complaint-status").textContent = "";
    // store meta
    if (purchaseId) m.dataset.purchaseId = purchaseId;
    if (senderName) m.dataset.senderName = senderName;
    if (senderPhone) m.dataset.senderPhone = senderPhone;
    if (senderEmail) m.dataset.senderEmail = senderEmail;
    m.classList.add("open");
    document.body.style.overflow = "hidden";
    // load existing complaints for this purchase (oldest -> newest)
    if (purchaseId) {
      // clear current container first
      const cont = m.querySelector(".existing-complaints");
      if (cont) cont.innerHTML = "";
      loadExistingComplaintsForModal(purchaseId).catch((e) => {
        console.warn(e);
      });
    }
    setTimeout(() => m.querySelector(".complaint-textarea").focus(), 150);

    // ensure SSE running for user so admin replies come in realtime
    try {
      if (typeof loggedUserId !== "undefined" && loggedUserId)
        startUserSse(loggedUserId);
    } catch (e) {}
  }

  // small helper to await inside non-async caller
  function awaitLoadExistingComplaintsSafe(purchaseId) {
    loadExistingComplaintsForModal(purchaseId).catch((e) => {
      console.warn("Load complaints failed:", e);
    });
  }

  function hideComplaintModal() {
    if (!complaintModalEl) return;
    complaintModalEl.classList.remove("open");
    document.body.style.overflow = "";
    // keep messages intact (so user can re-open and see history)
    // cleanup dataset if needed
    delete complaintModalEl.dataset.purchaseId;
    delete complaintModalEl.dataset.senderName;
    delete complaintModalEl.dataset.senderPhone;
    delete complaintModalEl.dataset.senderEmail;
  }

  // SSE: subscribe once we have loggedUserId
  let userSse = null;
  function startUserSse(uid) {
    if (!uid || userSse) return;
    try {
      userSse = new EventSource(
        `${SSE_USER_STREAM}?userId=${encodeURIComponent(uid)}`
      );
      userSse.onmessage = (ev) => {
        try {
          const text = ev.data;
          if (!text) return;
          const payload = JSON.parse(text);
          if (!payload || payload.type !== "complaint.message") return;
          // If modal open and matches purchaseId -> append new message
          const modal = window._complaintModalEl || complaintModalEl;
          if (
            modal &&
            modal.dataset.purchaseId &&
            String(modal.dataset.purchaseId) === String(payload.purchaseId)
          ) {
            // payload from admin -> mark isAdmin = true
            const isAdmin =
              typeof payload.senderName === "string" &&
              payload.senderName.toLowerCase().startsWith("admin");
            appendMessageToModal(modal, {
              senderName:
                payload.senderName || (isAdmin ? "Admin" : "Người gửi"),
              content: payload.content,
              createdAt: payload.createdAt,
              isAdmin: isAdmin,
            });
          } else {
            // not currently open: optionally show badge / visual cue outside modal (not implemented here)
            // e.g. increment notification count on purchase page
          }
        } catch (e) {
          /* ignore non-json */
        }
      };
      userSse.onerror = (e) => {
        console.warn("User SSE error", e);
      };
    } catch (e) {
      console.warn("Start SSE failed", e);
    }
  }

  // helper to append messages to modal .existing-complaints container in chat style
  // now: admin messages appear LEFT, user/purchase messages appear RIGHT.
  function appendMessageToModal(
    modal,
    { senderName, content, createdAt, isAdmin = undefined } = {}
  ) {
    if (!modal) return;
    const container = modal.querySelector(".existing-complaints");
    if (!container) return;

    // Nếu caller cung cấp isAdmin thì dùng nó; nếu không, suy xét từ senderName
    const adminFlag =
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
    // gán class rõ ràng: msg-admin hoặc msg-user
    bubble.className = "msg-bubble " + (adminFlag ? "msg-admin" : "msg-user");

    let timeStr = "";
    if (createdAt) {
      const dateStr = createdAt.endsWith("Z") ? createdAt : createdAt + "Z";
      timeStr = new Date(dateStr).toLocaleString("vi-VN");
    }
    bubble.innerHTML = `
        <div style="font-weight:700">${escapeHtml(
          senderName || (adminFlag ? "Admin" : "Bạn")
        )} • ${timeStr}</div>
        <div style="margin-top:6px">${escapeHtml(content || "")}</div>
        `;

    conv.appendChild(bubble);
    container.appendChild(conv);
    container.scrollTop = container.scrollHeight;
  }

  // === 🛑 HÀM BUILD CARD ĐÃ ĐƯỢC CẬP NHẬT 🛑 ===
  function buildOrderCard(p) {
    const userName = p.fullName || "";
    const address = p.address || "";
    const phone = p.phone || "";
    const productName = p.productName || "Sản phẩm";
    const price = (p.price || 0).toLocaleString("vi-VN") + " ₫";
    const productId = p.productId;

    // 1. Lấy URL ảnh
    const imageUrl = p.productImage || null; // Dữ liệu từ DB (ví dụ: /uploads/abc.png)

    let statusKey = (p.status || "").toLowerCase();
    let statusText = "";
    if (statusKey === "waiting_delivery") statusText = "Chờ giao hàng";
    else if (statusKey === "completed") statusText = "Hoàn thành";
    else if (statusKey === "returned" || statusKey === "cancelled")
      statusText = "Trả hàng";
    else statusText = (p.status || "").replace("_", " ").toUpperCase();

    // 2. Tạo link sản phẩm
    let productElement;
    if (productId && productId > 0) {
      productElement = el("a", {
        href: `/product_detail.html?id=${productId}`,
        text: `${productName}`,
      });
      productElement.classList.add("purchase-product-link");
    } else {
      productElement = el("span", { text: `${productName}` });
      productElement.classList.add("purchase-product-no-link");
    }

    // 3. Tạo Header (Tên SP + Trạng thái)
    const shopRow = el("header", { class: "shop-row" }, [
      el("div", { class: "shop-name" }, [productElement]),
      el("div", {
        class: "shop-actions",
        html: `<span class="status-right">${statusText}</span>`,
      }),
    ]);

    // 4. Tạo Element ảnh (nếu có)
    let imageElement = null;
    if (imageUrl) {
      // listing-service (port 8080) là nơi phục vụ file static /uploads
      const fullImageUrl = imageUrl.startsWith("http")
        ? imageUrl
        : // Cần trỏ đúng vào port 8080 của listing-service
          `${imageUrl}`;

      imageElement = el("div", { class: "purchase-image-wrapper" }, [
        el("img", {
          src: fullImageUrl,
          alt: escapeHtml(productName),
          class: "purchase-image",
        }),
      ]);
    } else {
      // Tạo một ảnh placeholder nếu không có ảnh
      imageElement = el("div", { class: "purchase-image-wrapper" }, [
        el("span", { text: "...", style: "font-size: 30px; color: #ccc;" }),
      ]);
    }

    // 5. Tạo thông tin khách hàng
    const detailsRow = el("div", { class: "purchase-details" }, [
      el("div", {
        class: "detail-line",
        text: `Họ và tên: ${userName || "(chưa có)"}`,
      }),
      el("div", {
        class: "detail-line",
        text: `Số điện thoại: ${phone || "(chưa có)"}`,
      }),
      el("div", {
        class: "detail-line",
        text: `Địa chỉ: ${address || "(chưa có)"}`,
      }),
    ]);

    // 6. Tạo Body chính (bọc Ảnh + Thông tin KH)
    const bodyRow = el("div", { class: "order-body-content" }, [
      imageElement, // Thêm ảnh
      detailsRow, // Thêm thông tin
    ]);

    // 7. Tạo Buttons
    const buttons = [];
    if (statusKey === "completed") {
      const complainBtn = el("button", {
        class: "btn primary",
        text: "Khiếu nại",
      });
      complainBtn.dataset.purchaseId = p.id;
      complainBtn.addEventListener("click", (ev) => {
        const pid = ev.currentTarget.dataset.purchaseId;
        let senderName = "",
          senderPhone = "",
          senderEmail = "";
        try {
          const u = JSON.parse(localStorage.getItem("user") || "null");
          if (u) {
            senderName = u.fullName || u.name || u.username || "";
            senderPhone = u.phone || "";
            senderEmail = u.email || "";
          }
        } catch (e) {}
        openComplaintModal({
          purchaseId: pid,
          productName,
          userName,
          address,
          senderName,
          senderPhone,
          senderEmail,
        });
      });

      // buttons.push(reviewBtn, complainBtn); // Cũ
      buttons.push(complainBtn); // Mới: Chỉ push nút Khiếu nại
    }

    // 8. Tạo Footer (Tổng tiền + Buttons)
    const footer = el("footer", { class: "order-footer" }, [
      el("div", { class: "hint", text: "" }),
      el("div", { class: "order-actions" }, [
        el("div", {
          class: "total",
          html: `Thành tiền: <span class="total-amount">${price}</span>`,
        }),
        el("div", { class: "buttons" }, buttons),
      ]),
    ]);

    // 9. Build Card
    const card = el(
      "article",
      { class: "order-card", "data-id": p.id, "data-product-id": p.productId },
      [shopRow, bodyRow, footer]
    );
    return card;
  }
  // === 🛑 KẾT THÚC HÀM BUILD CARD 🛑 ===

  async function fetchJson(url, opts) {
    const r = await fetch(url, opts);
    if (!r.ok) throw new Error("HTTP " + r.status);
    return r.json();
  }

  const params = new URLSearchParams(window.location.search);
  const tx = params.get("transactionId") || params.get("orderId");
  const queryUserId = params.get("userId");

  let loggedUserId = null;
  try {
    const userStr = localStorage.getItem("user");
    const u = userStr ? JSON.parse(userStr) : null;
    loggedUserId = u?.userId || u?.id || null;
  } catch (e) {
    loggedUserId = null;
  }

  if (!loggedUserId && queryUserId) {
    loggedUserId = Number(queryUserId);
    console.log("Using userId from query param:", loggedUserId);
  }

  if (!loggedUserId && tx) {
    try {
      const created = await fetchJson(
        `${PURCHASE_API_BASE}/from-transaction/${encodeURIComponent(tx)}`
      );
      if (created && (created.userId || created.userID || created.user)) {
        const uid = created.userId || created.userID || created.user;
        loggedUserId = uid;
        const existingUser = JSON.parse(localStorage.getItem("user") || "{}");
        localStorage.setItem(
          "user",
          JSON.stringify({ ...existingUser, id: uid, userId: uid })
        );
        localStorage.setItem("latestTransactionId", tx);
        console.info(
          "Recovered userId from transaction and saved to localStorage:",
          uid
        );
      }
    } catch (e) {
      console.warn(
        "Không thể lấy purchase từ transaction để khôi phục userId:",
        e
      );
    }
  }

  // ensure SSE for user if we have userId
  if (typeof loggedUserId !== "undefined" && loggedUserId)
    startUserSse(loggedUserId);

  (async function handleTx() {
    if (!tx) {
      if (loggedUserId) await loadPurchases(loggedUserId);
      return;
    }

    try {
      const created = await fetchJson(
        `${PURCHASE_API_BASE}/from-transaction/${encodeURIComponent(tx)}`
      );

      const userId = created?.userId || created?.userID || loggedUserId;
      if (userId) {
        try {
          const existingUser = JSON.parse(localStorage.getItem("user") || "{}");
          localStorage.setItem(
            "user",
            JSON.stringify({ ...existingUser, id: userId, userId: userId })
          );
        } catch (e) {
          console.debug("Không thể lưu userId vào localStorage:", e);
        }

        await loadPurchases(userId);
      } else {
        if (created) {
          const card = buildOrderCard(created);
          viewWaiting.insertBefore(card, viewWaiting.firstChild);
          updateBadges();
        } else {
          if (loggedUserId) await loadPurchases(loggedUserId);
        }
      }

      setActiveTab("waiting_delivery");
    } catch (err) {
      console.error("Error creating purchase from transaction:", err);
      if (loggedUserId) await loadPurchases(loggedUserId);
    }
  })();

  async function loadPurchases(userId, opts = {}) {
    try {
      const list = await fetchJson(
        `${PURCHASE_API_BASE}?userId=${encodeURIComponent(userId)}`
      );

      viewWaiting.innerHTML = "";
      viewCompleted.innerHTML = "";
      viewCancelled.innerHTML = "";

      list.forEach((p) => {
        if (opts.excludeId && p.id === opts.excludeId) return;
        const c = buildOrderCard(p);
        if ((p.status || "").toLowerCase() === "waiting_delivery")
          viewWaiting.appendChild(c);
        else if ((p.status || "").toLowerCase() === "completed")
          viewCompleted.appendChild(c);
        else viewCancelled.appendChild(c);
      });

      updateBadges();
    } catch (err) {
      console.error("Load purchases error:", err);
    }
  }

  function updateBadges() {
    const waitingCount = viewWaiting.querySelectorAll(".order-card").length;
    const completedCount = viewCompleted.querySelectorAll(".order-card").length;
    const cancelledCount = viewCancelled.querySelectorAll(".order-card").length;
    document.querySelectorAll(".order-tabs .tab").forEach((t) => {
      const k = t.dataset.filter;
      const badge = t.querySelector(".badge");
      if (!badge) return;
      if (k === "waiting_delivery") badge.textContent = waitingCount;
      if (k === "completed") badge.textContent = completedCount;
      if (k === "cancelled") badge.textContent = cancelledCount;
    });
  }
});
