// like-notif.js
(function () {
  const API_NOTIF = "/api/notif"; // like-service

  // --- 1. XỬ LÝ SỰ KIỆN CLICK (QUAN TRỌNG NHẤT) ---
  document.addEventListener("click", function (e) {
    // A. Nếu click vào nút Trái tim (#notifToggle)
    const toggleBtn = e.target.closest("#notifToggle");
    if (toggleBtn) {
      e.preventDefault();
      e.stopPropagation();

      const dropdown = document.getElementById("notifDropdown");
      if (!dropdown) return;

      // Kiểm tra trạng thái hiện tại để đóng/mở
      const isHidden =
        dropdown.hidden || dropdown.getAttribute("aria-hidden") === "true";

      if (isHidden) {
        openDropdown(dropdown, toggleBtn);
      } else {
        closeDropdown(dropdown, toggleBtn);
      }
      return; // Kết thúc xử lý
    }

    // B. Nếu click vào nút "Đánh dấu đã đọc"
    const markAllBtn = e.target.closest("#btnMarkAllRead");
    if (markAllBtn) {
      e.preventDefault();
      e.stopPropagation();
      handleMarkAllRead();
      return;
    }

    // C. Nếu click vào nút (×) để xóa 1 thông báo
    const closeItemBtn = e.target.closest(".notif-item-close");
    if (closeItemBtn) {
      e.preventDefault();
      e.stopPropagation();
      handleDeleteItem(closeItemBtn);
      return;
    }

    // D. Nếu click vào Link thông báo (để xem)
    const linkItem = e.target.closest(".notif-link");
    if (linkItem) {
      // Không preventDefault để link chuyển trang bình thường
      handleLinkClick(linkItem);
      return;
    }

    // E. Nếu click RA NGOÀI dropdown -> Đóng lại
    const dropdown = document.getElementById("notifDropdown");
    if (dropdown && !dropdown.hidden) {
      // Nếu click không nằm trong dropdown
      if (!e.target.closest("#notifDropdown")) {
        const btn = document.getElementById("notifToggle");
        closeDropdown(dropdown, btn);
      }
    }
  });

  // --- 2. CÁC HÀM HỖ TRỢ LOGIC ---

  function openDropdown(dropdown, toggleBtn) {
    dropdown.hidden = false;
    dropdown.setAttribute("aria-hidden", "false");
    if (toggleBtn) {
      toggleBtn.setAttribute("aria-expanded", "true");
      toggleBtn.classList.add("active");
    }
    // Gọi API lấy dữ liệu khi mở
    fetchNotifications();
  }

  function closeDropdown(dropdown, toggleBtn) {
    // Nếu không truyền vào, tự tìm
    if (!dropdown) dropdown = document.getElementById("notifDropdown");
    if (!toggleBtn) toggleBtn = document.getElementById("notifToggle");

    if (dropdown) {
      dropdown.hidden = true;
      dropdown.setAttribute("aria-hidden", "true");
    }
    if (toggleBtn) {
      toggleBtn.setAttribute("aria-expanded", "false");
      toggleBtn.classList.remove("active");
    }
  }

  function handleMarkAllRead() {
    const badge = document.getElementById("notifBadge");
    // 1. Ẩn badge
    if (badge) {
      badge.hidden = true;
      badge.style.display = "none";
    }
    // 2. UI update (làm mờ các item)
    const notifBody = document.querySelector(".notif-body");
    if (notifBody) {
      const items = notifBody.querySelectorAll(".notif-item");
      items.forEach((item) => {
        item.dataset.seen = "true";
      });
    }
    // 3. Gọi API (nếu cần)
    // ...
  }

  async function handleDeleteItem(btn) {
    const id = btn.dataset.id;
    const item = btn.closest(".notif-item");
    if (!item) return;

    // Snapshot để rollback
    const snapshot = item.cloneNode(true);
    const parent = item.parentElement;

    // Xóa UI ngay lập tức cho mượt
    item.remove();
    decrementBadgeIfUnseen(snapshot);

    const userId = localStorage.getItem("userId");
    if (!userId) return;

    try {
      const res = await fetch(`${API_NOTIF}/${encodeURIComponent(id)}`, {
        method: "DELETE",
        headers: { "X-User-Id": userId },
      });

      if (!res.ok && res.status !== 204 && res.status !== 404) {
        // Fallback mark-read
        await fetch(`${API_NOTIF}/${encodeURIComponent(id)}/mark-read`, {
          method: "POST",
          headers: { "X-User-Id": userId },
        });
      }
      // Refresh badge chuẩn từ server
      refreshBadgeFromServer();
    } catch (err) {
      console.warn("Delete failed", err);
      // Rollback nếu lỗi
      if (parent) parent.insertBefore(snapshot, parent.firstChild);
    }
  }

  async function handleLinkClick(linkEl) {
    const id = linkEl.dataset.id;
    const userId = localStorage.getItem("userId");
    if (!userId) return;

    // Đánh dấu đã xem (fire and forget)
    fetch(`${API_NOTIF}/${encodeURIComponent(id)}/mark-read`, {
      method: "POST",
      headers: { "X-User-Id": userId },
    })
      .then(() => {
        refreshBadgeFromServer();
      })
      .catch(() => {});

    // Giảm badge cục bộ ngay lập tức
    const item = linkEl.closest(".notif-item");
    if (item) {
      item.dataset.seen = "true";
      decrementBadgeIfUnseen(item);
    }
  }

  // --- 3. API & RENDER ---

  async function fetchNotifications() {
    const body = document.querySelector(".notif-body");
    if (!body) return;

    body.innerHTML =
      '<p class="muted" style="padding:10px; text-align:center; color:#888">Đang tải...</p>';

    try {
      const userId = localStorage.getItem("userId");
      if (!userId) {
        body.innerHTML =
          '<p class="muted" style="padding:10px; text-align:center;">Vui lòng đăng nhập.</p>';
        return;
      }

      const res = await fetch(API_NOTIF, { headers: { "X-User-Id": userId } });
      if (!res.ok) throw new Error("Failed");
      const raw = await res.json();

      const notifs = dedupeNotifications(raw);
      renderNotifications(notifs, body);
      updateBadge(notifs);
    } catch (err) {
      console.error(err);
      body.innerHTML =
        '<p class="muted" style="padding:10px; text-align:center; color:red">Lỗi tải thông báo.</p>';
    }
  }

  function renderNotifications(notifs, container) {
    if (!notifs || notifs.length === 0) {
      container.innerHTML =
        '<p class="muted" style="padding:20px; text-align:center; color:#666">Bạn chưa có thông báo nào.</p>';
      return;
    }

    const list = document.createElement("div");
    list.className = "notif-list";

    notifs.slice(0, 50).forEach((n) => {
      const el = document.createElement("div");
      el.className = "notif-item";
      el.dataset.id = n.id;
      el.dataset.seen = !!n.seen; // true/false string

      const displayTitle = buildDisplayTitle(n);
      const displayBody = buildPriceDisplay(n);

      // Thêm style background cho tin chưa đọc để dễ nhìn
      if (!n.seen) {
        el.style.backgroundColor = "#f0fdf4"; // xanh rất nhạt
      }

      el.innerHTML = `
        <button class="notif-item-close" data-id="${escapeHtml(
          n.id
        )}" aria-label="Xóa">×</button>
        <a href="${escapeHtml(
          n.link || "/liked.html"
        )}" class="notif-link" data-id="${escapeHtml(n.id)}">
          <div class="notif-title">${escapeHtml(displayTitle)}</div>
          <div class="notif-body-text">${escapeHtml(displayBody)}</div>
          <div class="notif-meta"><small>${
            n.createdAt ? new Date(n.createdAt).toLocaleString("vi-VN") : ""
          }</small></div>
        </a>
      `;
      list.appendChild(el);
    });

    container.innerHTML = "";
    container.appendChild(list);
  }

  // --- 4. BADGE UTILS ---

  function updateBadge(notifs) {
    const badge = document.getElementById("notifBadge");
    if (!badge) return;
    const unseen = (notifs || []).filter((n) => !n.seen).length;

    if (unseen > 0) {
      badge.hidden = false;
      badge.style.display = "inline-block"; // Force hiển thị
      badge.textContent = unseen > 99 ? "99+" : unseen;
    } else {
      badge.hidden = true;
      badge.style.display = "none";
    }
  }

  function decrementBadgeIfUnseen(itemEl) {
    const badge = document.getElementById("notifBadge");
    if (!badge || badge.hidden) return;

    // Kiểm tra xem item vừa thao tác có phải là chưa đọc không
    const isSeen = itemEl.dataset.seen === "true";

    if (!isSeen) {
      let current = parseInt(badge.textContent) || 0;
      let newVal = Math.max(0, current - 1);
      if (newVal === 0) {
        badge.hidden = true;
        badge.style.display = "none";
      } else {
        badge.textContent = newVal > 99 ? "99+" : newVal;
      }
    }
  }

  async function refreshBadgeFromServer() {
    const userId = localStorage.getItem("userId");
    if (!userId) return;
    try {
      const r = await fetch(API_NOTIF, { headers: { "X-User-Id": userId } });
      if (r.ok) {
        const arr = await r.json();
        updateBadge(dedupeNotifications(arr));
      }
    } catch (e) {}
  }

  // --- Helper Functions (Giữ nguyên logic cũ) ---
  function dedupeNotifications(arr) {
    if (!Array.isArray(arr)) return [];
    const map = new Map();
    arr.forEach((n) => {
      const key = n.productId ? `p:${n.productId}` : `i:${n.id}`;
      const time = n.createdAt ? new Date(n.createdAt).getTime() : 0;
      const existing = map.get(key);
      if (!existing || time >= existing.time) {
        map.set(key, { notif: n, time });
      }
    });
    return Array.from(map.values())
      .map((v) => v.notif)
      .sort((a, b) => {
        return (
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
      });
  }

  function getProductName(n) {
    if (!n) return null;
    const c = [
      n.productName,
      n.product_name,
      n.product?.productName,
      n.data?.productName,
    ];
    return c.find((x) => x && String(x).trim() !== "") || null;
  }

  function buildDisplayTitle(n) {
    const orig = n.title || "";
    const name = getProductName(n);
    if (!name) return orig;
    if (/Tin đăng/i.test(orig))
      return orig.replace(/Tin đăng/i, `Tin đăng "${name}"`);
    return `Tin đăng "${name}" ${orig}`.trim();
  }

  function buildPriceDisplay(n) {
    // ... Logic format giá cũ của bạn ...
    if (!n) return "";
    const formatVND = (num) =>
      new Intl.NumberFormat("vi-VN").format(Number(num));
    let oldP = n.oldPrice ?? n.data?.oldPrice;
    let newP = n.newPrice ?? n.data?.newPrice;

    if (oldP && newP) return `Giá: ${formatVND(oldP)} đ → ${formatVND(newP)} đ`;
    if (newP) return `Giá: mới ${formatVND(newP)} đ`;
    if (oldP) return `Giá: ${formatVND(oldP)} đ`;
    return n.body || "";
  }

  function escapeHtml(str) {
    return String(str || "").replace(
      /[&<>"']/g,
      (m) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        }[m])
    );
  }

  // Khởi tạo Badge lúc đầu (chạy 1 lần)
  refreshBadgeFromServer();
})();
