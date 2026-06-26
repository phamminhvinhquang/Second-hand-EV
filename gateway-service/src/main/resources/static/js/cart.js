//============Tạo khoảng cách với header========================
window.addEventListener("load", () => {
  const navbar = document.querySelector(".navbar");

  // Tạo khoảng trống cho body để navbar không bị che khuất
  document.body.style.paddingTop = "60px"; // Điều chỉnh độ cao padding-top tùy thuộc vào chiều cao của navbar
});
//===================Cart backend ===================

const API_BASE = "/api/carts";
const PRODUCT_SERVICE_BASE = "";
const selectedIds = new Set(); // lưu các id được chọn

function formatVND(n) {
  return new Intl.NumberFormat("vi-VN").format(n) + " VND";
}

function formatKm(n) {
  if (!n && n !== 0) return "";
  return new Intl.NumberFormat("vi-VN").format(n) + " km";
}

// parse chuỗi "1.234.000 VND" -> number 1234000
function parseVND(formatted) {
  if (!formatted) return 0;
  const digits = String(formatted).replace(/[^\d]/g, "");
  return digits ? Number(digits) : 0;
}

//lấy dữ liệu giỏ hàng từ backend
//Gọi hàm render để hiển thị danh sách item lên UI
async function fetchCartAndRender() {
  try {
    const userId = localStorage.getItem("userId");
    const headers = {};
    if (userId) headers["X-User-Id"] = userId;

    // TỰ ĐỘNG: Đồng bộ hóa user cart trước khi fetch (xóa các item mồ côi)
    if (userId) {
      try {
        await fetch("/api/carts/reconcile-user", {
          headers,
        });
      } catch (err) {
        console.warn("reconcile-user failed (non-fatal):", err);
      }
    }

    const res = await fetch(API_BASE, { headers });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const items = await res.json();
    renderCart(items);
  } catch (err) {
    console.error("fetchCart error", err);
    document
      .querySelector(".cart")
      ?.insertAdjacentHTML(
        "beforeend",
        `<div class="error">Hiện tại chưa có dữ liệu</div>`
      );
  }
}

function updateSummarySelectionUI() {
  // tính tổng đã chọn
  const rows = document.querySelectorAll(".cart-row");
  let selTotal = 0;

  rows.forEach((r) => {
    const num = Number(r.dataset.total || r.dataset.price || 0) || 0;
    const id = String(
      r.dataset.id || r.querySelector(".btn-remove")?.dataset.id || ""
    );
    if (id && selectedIds.has(id)) {
      selTotal += num;
    }
  });

  // Lấy selected-total-row (nếu đã có)
  let selectedRow = document.querySelector(".selected-total-row");
  const checkoutBtn = document.querySelector(".btn-primary");

  // Nếu chưa có element, không tạo ở đây — renderCart sẽ tạo 1 lần duy nhất.
  if (selectedRow) {
    // Khi không có sản phẩm được chọn -> ẩn cả dòng
    if (selectedIds.size === 0) {
      selectedRow.style.display = "none";
    } else {
      selectedRow.style.display = ""; // hiển thị lại
      const selEl = selectedRow.querySelector(".selected-total");
      if (selEl) selEl.textContent = formatVND(selTotal);
    }
  }

  // Bật/tắt nút checkout: chỉ cho phép khi có ít nhất 1 item được chọn
  if (checkoutBtn) {
    checkoutBtn.disabled = selectedIds.size === 0;
    checkoutBtn.style.opacity = selectedIds.size === 0 ? "0.6" : "1";
  }
}

// gọi khi người dùng muốn thanh toán các sản phẩm đã chọn
async function checkoutSelected() {
  if (selectedIds.size === 0) {
    alert("Vui lòng chọn sản phẩm để thanh toán.");
    return;
  }

  const ids = Array.from(selectedIds);
  const total = document.querySelector(".selected-total")?.textContent || "";

  // ✅ Lưu vào localStorage để chuyển qua payment
  localStorage.setItem("cartIds", JSON.stringify(ids));
  localStorage.setItem("totalPrice", total);

  // ✅ Chuyển sang trang thanh toán
  window.location.href = `payment.html?cartIds=${ids.join(
    ","
  )}&total=${encodeURIComponent(total)}`;
}

//nhận mảng item từ backend và cập nhật DOM để hiển thị giỏ hàng
function renderCart(items) {
  const cartSection = document.querySelector(".cart .cart-list"); // Lấy container hiển thị giỏ hàng
  if (!cartSection) return; // nếu không tìm thấy element thì dừng

  // Xóa các hàng
  const existingRows = cartSection.querySelectorAll(".cart-row");
  existingRows.forEach((r) => r.remove());

  // --- Cập nhật phần xử lý hiển thị khi trống ---
  // Nếu muốn khi trống hiện chữ "Giỏ hàng trống" ở giữa khung cố định
  if (items.length === 0) {
    cartSection.innerHTML = `<div style="display:flex; justify-content:center; align-items:center; height:100%; color:#888;">Giỏ hàng trống</div>`;
    // Đảm bảo update UI bên phải
    updateSummarySelectionUI();
    // Ẩn dòng tổng tiền nếu cần
    let selectedRow = document.querySelector(".selected-total-row");
    if (selectedRow) selectedRow.style.display = "none";
    return;
  }

  let totalAll = 0; // Biến lưu tổng tiền
  items.forEach((item) => {
    const itemTotal = Number(item.total ?? item.price ?? 0) || 0;
    totalAll += itemTotal;

    // tạo row
    const row = document.createElement("div");
    row.className = "cart-row";
    row.dataset.productid = item.productId || item.id;
    row.dataset.id = String(item.id);
    row.dataset.total = String(itemTotal); // lưu numeric total
    row.dataset.price = String(item.price ?? item.total ?? 0);

    // build product-meta html only when value exists
    function buildMetaHtml(item) {
      const parts = [];

      if (item.brand && String(item.brand).trim() !== "") {
        parts.push(`
      <span class="meta-item brand">
        <i class="fas fa-tag" aria-hidden="true"></i>
        <span class="meta-text">${escapeHtml(item.brand)}</span>
      </span>
    `);
      }

      if (item.yearOfManufacture) {
        parts.push(`
      <span class="meta-item year">
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
        <span class="meta-text">${escapeHtml(
          String(item.yearOfManufacture)
        )}</span>
      </span>
    `);
      }

      if (item.conditionName && String(item.conditionName).trim() !== "") {
        parts.push(`
      <span class="meta-item conditionName">
        <i class="fas fa-info-circle" aria-hidden="true"></i>
        <span class="meta-text">${escapeHtml(item.conditionName)}</span>
      </span>
    `);
      }

      if (
        item.mileage !== undefined &&
        item.mileage !== null &&
        item.mileage !== ""
      ) {
        // ensure number formatting
        const kmText = item.mileage ? formatKm(item.mileage) : "";
        if (kmText) {
          parts.push(`
        <span class="meta-item mileage">
          <i class="fas fa-road" aria-hidden="true"></i>
          <span class="meta-text">${escapeHtml(kmText)}</span>
        </span>
      `);
        }
      }

      return parts.join("");
    }

    let mainUrl = item.imgurl || "./images/product.jpg"; // Lấy URL từ DB

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

    row.innerHTML = `
      <div class="select">
        <input type="checkbox" class="select-item" data-id="${
          item.id
        }" aria-label="Chọn sản phẩm" />
      </div>
      <div class="thumb">
        <img src="${escapeHtml(mainUrl)}" alt="${escapeHtml(
      item.productname
    )}" />
      </div>
      <div class="info">
        <div class="product-title">${escapeHtml(item.productname)}</div>
        <div class="product-meta">
          ${buildMetaHtml(item)}
        </div>
      </div>
      <div class="price">${formatVND(itemTotal)}</div>
      <div class="remove">
        <button class="btn-remove" aria-label="Xóa" data-id="${
          item.id
        }">Xóa</button>
      </div>
    `;

    // checkbox xử lý chọn/bo chọn
    const checkbox = row.querySelector(".select-item");
    checkbox.addEventListener("change", (e) => {
      const id = String(e.target.dataset.id || item.id);
      if (e.target.checked) {
        selectedIds.add(id);
      } else {
        selectedIds.delete(id);
      }
      updateSummarySelectionUI();
    });

    // Thêm sự kiện click cho toàn bộ dòng (row)
    row.addEventListener("click", (e) => {
      // 1. Nếu bấm vào nút "Xóa" -> Không làm gì cả (để sự kiện xóa chạy riêng)
      if (e.target.closest(".btn-remove")) return;

      // 2. Nếu bấm trực tiếp vào ô checkbox -> Không làm gì cả (để nó tự chạy mặc định)
      if (e.target.classList.contains("select-item")) return;

      // 3. Còn lại (bấm vào ảnh, tên, giá...) -> Đảo ngược trạng thái checkbox
      checkbox.checked = !checkbox.checked;

      // 4. Kích hoạt sự kiện 'change' để cập nhật lại Tổng tiền
      checkbox.dispatchEvent(new Event("change"));
    });

    // gắn event xóa
    const btn = row.querySelector(".btn-remove");
    btn.addEventListener("click", async () => {
      const id = btn.dataset.id;
      const numericTotal = Number(btn.dataset.total || row.dataset.total || 0);
      if (!id) return;
      // nếu item đang được chọn, bỏ nó khỏi set
      selectedIds.delete(String(id));
      await removeItem(id, row, numericTotal, btn);
      updateSummarySelectionUI();
    });

    cartSection.appendChild(row); // Thêm row mới vào DOM
  });

  // Tìm .total trong DOM và cập nhật tổng tiền đã tính
  // const totalEl = document.querySelector(".total");
  // if (totalEl) totalEl.textContent = formatVND(totalAll);

  // ensure selected-total element exists in UI (tạo nếu chưa có)
  let selectedRow = document.querySelector(".selected-total-row");
  if (!selectedRow) {
    const summary = document.querySelector(".summary");
    if (summary) {
      const div = document.createElement("div");
      div.className = "selected-total-row";
      div.style.display = "none";
      div.innerHTML = `<div class="line"><div class="muted">Tổng tiền</div><div class="selected-total"></div></div>`;
      // chèn phía trên nút thanh toán (ở trong .summary)
      const btn = summary.querySelector(".btn-primary");
      summary.insertBefore(div, btn);
      // gắn handler nút thanh toán
      btn.addEventListener("click", checkoutSelected);
      btn.disabled = true;
      btn.style.opacity = "0.6";
    }
  }

  updateSummarySelectionUI();
}
//thực hiện xóa 1 item
// Xóa ngay row trên client và trừ tổng ngay lập tức để UX mượt
async function removeItem(id, rowElement, itemTotal = 0, btn = null) {
  const totalEl = document.querySelector(".total");
  const cartSection = document.querySelector(".cart");

  // lưu trạng thái để phục hồi khi cần
  const previousTotal = totalEl ? parseVND(totalEl.textContent) : null;
  const parent = rowElement.parentNode; // thường là cartSection
  const nextSibling = rowElement.nextSibling; // để phục hồi vị trí chính xác

  try {
    // disable nút để tránh click nhiều lần
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Đang xóa...";
    }

    // --- Optimistic UI: remove row và cập nhật tổng ngay ---
    if (parent) parent.removeChild(rowElement);
    if (totalEl && previousTotal !== null) {
      const newTotal = Math.max(0, previousTotal - Number(itemTotal || 0));
      totalEl.textContent = formatVND(newTotal);
    }

    // gọi API DELETE
    const res = await fetch(`${API_BASE}/${id}`, { method: "DELETE" });

    // nếu server trả lỗi -> throw để rollback
    if (!res.ok && res.status !== 204)
      throw new Error("Delete failed " + res.status);
  } catch (err) {
    console.error("removeItem error", err);

    // rollback: phục hồi row vào vị trí ban đầu
    try {
      const container = parent || cartSection;
      if (container) {
        if (nextSibling && nextSibling.parentNode === container) {
          container.insertBefore(rowElement, nextSibling);
        } else {
          container.appendChild(rowElement);
        }
      }
    } catch (re) {
      console.warn("Could not restore row to DOM", re);
    }

    // phục hồi tổng tiền
    if (totalEl && previousTotal !== null) {
      totalEl.textContent = formatVND(previousTotal);
    }

    // reset nút (khôi phục text + trạng thái)
    if (btn) {
      btn.disabled = false;
      btn.textContent = "Xóa";
    }

    // báo lỗi cho user
    alert("Xóa không thành công. Vui lòng thử lại.");
  }
}

function escapeHtml(text) {
  if (!text) return "";
  return text.replace(
    /[&<>"']/g,
    (m) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[
        m
      ])
  );
}

document.addEventListener("DOMContentLoaded", () => {
  fetchCartAndRender();
});
