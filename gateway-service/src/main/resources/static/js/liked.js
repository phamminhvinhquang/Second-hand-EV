//============Tạo khoảng cách với header========================
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "80px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});

//============click nút trái tim========================

(function () {
  // Chọn tất cả các nút like
  const likeButtons = document.querySelectorAll(".btn-like");

  likeButtons.forEach((btn) => {
    // đảm bảo icon có class fa-heart; nếu không, bỏ qua
    const icon = btn.querySelector("i.fa-heart");

    // nếu không có icon thì skip
    if (!icon) return;

    // Khởi tạo ARIA nếu chưa
    if (!btn.hasAttribute("aria-pressed"))
      btn.setAttribute("aria-pressed", "false");

    // Nếu icon đang là 'fas' (solid) thì coi là đã like
    if (icon.classList.contains("fas")) {
      btn.classList.add("active");
      btn.setAttribute("aria-pressed", "true");
    } else {
      // đảm bảo outline nếu ban đầu là chưa like
      icon.classList.remove("fas");
      icon.classList.add("far");
      btn.classList.remove("active");
      btn.setAttribute("aria-pressed", "false");
    }

    btn.addEventListener("click", (e) => {
      const isActive = btn.classList.toggle("active");

      if (isActive) {
        // set solid red heart
        icon.classList.remove("far");
        icon.classList.add("fas");
        btn.setAttribute("aria-pressed", "true");
        btn.setAttribute("aria-label", "Bỏ thích");
      } else {
        // set outline white heart
        icon.classList.remove("fas");
        icon.classList.add("far");
        btn.setAttribute("aria-pressed", "false");
        btn.setAttribute("aria-label", "Thích");
      }

      // (tùy chọn) nếu muốn lưu trạng thái về server hoặc localStorage, làm ở đây
      // ex: localStorage.setItem('liked_'+id, isActive)
    });
  });
})();

//============click nút trái tim========================
const API_BASE = "/api/likes";
const PRODUCT_SERVICE_BASE = "";
const userId = localStorage.getItem("userId");

document.addEventListener("DOMContentLoaded", async () => {
  const listContainer = document.querySelector(".saved-list");
  const countEl = document.querySelector(".saved-title strong");

  async function loadLikes() {
    try {
      const headers = {};
      if (userId) headers["X-User-Id"] = userId;

      const res = await fetch(API_BASE, { headers });
      if (!res.ok) throw new Error("Không lấy được liked list");
      const likes = await res.json();
      renderLikes(likes);
      const countEl = document.querySelector(".saved-title strong");
      if (countEl) countEl.textContent = likes.length;
    } catch (err) {
      console.error("loadLikes error", err);
      const listContainer = document.querySelector(".saved-list");
      if (listContainer)
        listContainer.innerHTML = `<p class="error-text">Không tải được danh sách đã thích.</p>`;
    }
  }

  function renderLikes(likes) {
    if (!likes || likes.length === 0) {
      listContainer.innerHTML = `<p class="not-found-text">Bạn chưa thích sản phẩm nào.</p>`;
      return;
    }
    listContainer.innerHTML = "";

    likes.forEach((l) => {
      const item = document.createElement("div");
      item.className = "saved-item";
      item.dataset.likeId = l.id || "";
      item.dataset.productid = l.productId || "";

      // === PHẦN SỬA BẮT ĐẦU ===
      // 1. Xử lý URL ảnh trước
      let mainUrl = l.imgurl || "./images/product.jpg"; // Lấy URL từ DB

      // Nếu URL là đường dẫn tương đối (ví dụ: /uploads/...) và không phải fallback
      if (
        mainUrl &&
        !mainUrl.match(/^https?:\/\//) &&
        mainUrl !== "./images/product.jpg"
      ) {
        if (mainUrl.startsWith("/")) {
          // /uploads/file.jpg -> http://localhost:8080/uploads/file.jpg
          mainUrl = PRODUCT_SERVICE_BASE + mainUrl;
        } else {
          // uploads/file.jpg -> http://localhost:8080/uploads/file.jpg
          mainUrl = PRODUCT_SERVICE_BASE + "/" + mainUrl;
        }
      }
      // === PHẦN SỬA KẾT THÚC ===

      // build meta HTML only for fields that exist
      const metaParts = [];
      const isBattery =
        (l.productname || "").toLowerCase().includes("pin") ||
        (l.productname || "").toLowerCase().includes("battery");

      if (isBattery && l.batteryType && String(l.batteryType).trim() !== "") {
        metaParts.push(`
        <span class="meta-item battery-type" title="Loại pin">
          <i class="fas fa-microchip" aria-hidden="true"></i>
          <span class="meta-text">${escapeHtml(l.batteryType)}</span>
        </span>
        `);
      }
      if (l.brand && String(l.brand).trim() !== "") {
        metaParts.push(`
        <span class="meta-item brand" title="Thương hiệu">
          <i class="fas fa-tag" aria-hidden="true"></i>
          <span class="meta-text">${escapeHtml(l.brand)}</span>
        </span>
        `);
      }

      if (l.yearOfManufacture) {
        metaParts.push(`
        <span class="meta-item year" title="Năm sản xuất">
          <i class="fas fa-calendar-alt" aria-hidden="true"></i>
          <span class="meta-text">${escapeHtml(
            String(l.yearOfManufacture)
          )}</span>
        </span>
        `);
      }

      if (l.conditionName && String(l.conditionName).trim() !== "") {
        metaParts.push(`
        <span class="meta-item conditionName" title="Tình trạng">
          <i class="fas fa-info-circle" aria-hidden="true"></i>
          <span class="meta-text">${escapeHtml(l.conditionName)}</span>
        </span>
        `);
      }

      if (
        l.mileage !== undefined &&
        l.mileage !== null &&
        String(l.mileage).trim() !== ""
      ) {
        const km = l.mileage
          ? new Intl.NumberFormat("vi-VN").format(l.mileage) + " km"
          : "";
        if (km) {
          metaParts.push(`
          <span class="meta-item mileage" title="Quãng đường đã đi">
            <i class="fas fa-road" aria-hidden="true"></i>
            <span class="meta-text">${escapeHtml(km)}</span>
          </span>
          `);
        }
      }

      // build inner HTML
      item.innerHTML = `
      <a class="thumb" href="/product_detail.html?id=${l.productId}">
        <img src="${escapeHtml(
          mainUrl // <-- 2. SỬ DỤNG BIẾN 'mainUrl' ĐÃ ĐƯỢC SỬA
        )}" alt="${escapeHtml(l.productname)}" />
      </a>

      <div class="meta-wrap">
        <h3 class="item-title">
          <a href="/product_detail.html?id=${l.productId}">${escapeHtml(
        l.productname
      )}</a>
        </h3>

        <div class="price-and-meta">
          <div class="price">${formatPrice(
            l.price
          )} <span class="vnd">đ</span></div>

          <div class="meta-row">
            ${metaParts.join("")}
          </div>
        </div>
      </div>

      <div class="actions">
        <button class="btn-like active" aria-label="Bỏ thích" data-like-id="${
          l.id
        }" data-productid="${l.productId}">
          <i class="fas fa-heart"></i>
        </button>
      </div>
    `;

      listContainer.appendChild(item);
    });
  }

  function escapeHtml(str) {
    if (!str) return "";
    return str.replace(
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

  function formatPrice(price) {
    if (!price || price === 0) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  }

  // -----------------------------
  // Toggle handler: hỗ trợ cả "unlike" (DELETE) và "re-like" (POST)
  // - Nếu button có class 'active' => đang liked -> thực hiện DELETE
  // - Nếu button không có 'active' => thực hiện POST add-by-product/{productId}
  // UX: optimistic UI (thay đổi ngay), revert nếu request thất bại
  // Item KHÔNG bị xóa khỏi DOM ngay; sẽ biến mất khi reload trang (như bạn yêu cầu)
  // -----------------------------
  document.addEventListener("click", async (e) => {
    const btn = e.target.closest(".btn-like");
    if (!btn) return;

    const savedItem = btn.closest(".saved-item");
    const likeId = btn.dataset.likeId;
    const productId =
      btn.dataset.productid ||
      (savedItem ? savedItem.dataset.productid : undefined);
    if (!productId) {
      console.warn("Missing productId for like button");
      return;
    }

    const icon = btn.querySelector("i");
    if (!icon) return;
    const prevIconClass = icon.className;
    const wasActive = btn.classList.contains("active");
    const prevAria = btn.getAttribute("aria-label");
    btn.disabled = true;

    const headers = {};
    if (userId) headers["X-User-Id"] = userId;
    else {
      alert("Vui lòng đăng nhập để quản lý mục yêu thích.");
      btn.disabled = false;
      return;
    }

    try {
      if (wasActive) {
        // Unlike
        if (likeId) {
          const res = await fetch(`${API_BASE}/${likeId}`, {
            method: "DELETE",
            headers,
          });
          if (!res.ok && res.status !== 204)
            throw new Error(`HTTP ${res.status}`);
          btn.dataset.likeId = "";
          if (savedItem) savedItem.dataset.likeId = "";
        } else {
          const res = await fetch(`${API_BASE}/by-product/${productId}`, {
            method: "DELETE",
            headers,
          });
          if (!res.ok && res.status !== 204)
            throw new Error(`HTTP ${res.status}`);
        }
        icon.classList.remove("fas");
        icon.classList.add("far");
        btn.classList.remove("active");
        btn.setAttribute("aria-label", "Đã bỏ thích");
      } else {
        // Re-like
        const res = await fetch(`${API_BASE}/add-by-product/${productId}`, {
          method: "POST",
          headers,
        });
        if (!res.ok) {
          if (res.status === 409) {
            // sync id by reloading list
            const listRes = await fetch(API_BASE, { headers });
            if (listRes.ok) {
              const likes = await listRes.json();
              const found = likes.find(
                (x) => Number(x.productId) === Number(productId)
              );
              if (found) {
                btn.dataset.likeId = found.id;
                if (savedItem) savedItem.dataset.likeId = found.id;
              }
            }
          } else {
            throw new Error(`HTTP ${res.status}`);
          }
        } else {
          const saved = await res.json();
          btn.dataset.likeId = saved.id;
          if (savedItem) savedItem.dataset.likeId = saved.id;
        }
        icon.classList.remove("far");
        icon.classList.add("fas");
        btn.classList.add("active");
        btn.setAttribute("aria-label", "Bỏ thích");
      }
    } catch (err) {
      console.error("like action failed", err);
      // revert UI
      icon.className = prevIconClass;
      if (wasActive) btn.classList.add("active");
      else btn.classList.remove("active");
      btn.setAttribute("aria-label", prevAria || "Bỏ thích");
      alert("Không thể thay đổi trạng thái yêu thích. Vui lòng thử lại.");
    } finally {
      btn.disabled = false;
    }
  });

  // lần đầu tải
  await loadLikes();
});
