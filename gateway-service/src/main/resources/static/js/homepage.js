document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "/api";
  const BACKEND_ORIGIN = "";

  const tabsContainer = document.querySelector(".product-tabs");
  const productsGrid = document.querySelector(".products-grid");
  const viewMoreLink = document.querySelector(".view-more a");

  let currentFilterParams = { type: "all", sortBy: "date" };

  // --- FUNCTIONS ---

  const fetchAndDisplayProducts = async ({
    type = "all",
    sortBy = "date",
    limit = 4, // Tên biến 'limit' ở đây không quan trọng
  } = {}) => {
    productsGrid.innerHTML = `<p class="loading-text">Đang tải dữ liệu...</p>`;
    try {
      // ✅ SỬA LỖI Ở ĐÂY:
      // Gửi 'size: limit' thay vì 'limit' để khớp với API
      const params = new URLSearchParams({ sortBy, size: limit });

      if (type !== "all") {
        params.append("type", type);
      }

      const response = await fetch(
        `${API_BASE_URL}/listings?${params.toString()}`
      );
      if (!response.ok) throw new Error(`Lỗi mạng: ${response.statusText}`);

      const pageData = await response.json();
      const listings = pageData.content;

      productsGrid.innerHTML = "";

      if (listings.length === 0) {
        productsGrid.innerHTML = `<p class="not-found-text">Không tìm thấy tin đăng nào.</p>`;
        return;
      }

      listings.forEach((listing) => {
        productsGrid.insertAdjacentHTML(
          "beforeend",
          createProductCardHTML(listing)
        );
      });
    } catch (error) {
      console.error("Không thể tải dữ liệu sản phẩm:", error);
      productsGrid.innerHTML = `<p class="error-text">Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại sau.</p>`;
    }
  };

  /**
   * Hàm cập nhật đường dẫn cho nút "Xem thêm"
   */
  const updateViewMoreLink = () => {
    if (!viewMoreLink) return;

    const params = new URLSearchParams();
    if (currentFilterParams.type !== "all") {
      params.append("type", currentFilterParams.type);
    }
    if (currentFilterParams.sortBy !== "date") {
      params.append("sortBy", currentFilterParams.sortBy);
    }

    // Tạo ra URL mới, ví dụ: /product-all.html?type=car
    viewMoreLink.href = `/product-all.html?${params.toString()}`;
  };

  // Thay thế hàm này trong file: homepage.js
  const createProductCardHTML = (listing) => {
    const product = listing.product;
    const spec = product.specification;

    //=====================like========================
    const pid = product.productId;
    // Nếu likedSet đã được khởi tạo ở phía dưới (initPage sẽ load trước khi render),
    // isLiked sẽ là true/false
    const isLiked =
      typeof likedSet !== "undefined" && likedSet.has(Number(pid));
    //=====================like========================

    // --- 1. Logic cho Nhãn Kiểm Định ---
    const verifiedBadgeHTML = listing.verified
      ? `<span class="product-card-verified-badge">
           <i class="fas fa-check-circle"></i> Đã Kiểm Định
         </span>`
      : "";

    // --- 2. Logic cho Thanh Tình Trạng ---
    const conditionText = spec?.condition?.conditionName || "Không rõ";
    let conditionPercent = 0;
    // Tìm số đầu tiên trong chuỗi tình trạng (ví dụ: "Tốt 85-94%" sẽ lấy 85)
    if (conditionText.includes("%")) {
      const match = conditionText.match(/(\d+)/);
      if (match) {
        conditionPercent = parseInt(match[1], 10);
      }
    }

    // Tạo HTML cho thanh tiến trình
    const conditionBarHTML = `
      <div class="condition-bar" title="${conditionText}">
        <div class="condition-bar-fill" style="width: ${conditionPercent}%;"></div>
      </div>
      <span class="condition-text">${conditionText}</span>
    `;

    // --- 3. Logic Ảnh (giữ nguyên) ---
    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : `https://via.placeholder.com/300x200.png?text=${product.productType.toUpperCase()}`;
    const image = `<img src="${imageUrl}" alt="${product.productName}" class="product-image">`; // Class này lấy từ CSS

    // --- 4. Cấu trúc HTML cuối cùng ---
    return `
            <div class="product-card" data-productid="${pid}">

                <button
                  class="favorite-btn ${isLiked ? "active" : ""}"
                  aria-label="${isLiked ? "Bỏ thích" : "Thêm vào yêu thích"}"
                  data-productid="${pid}">
                  <i class="${isLiked ? "fas" : "far"} fa-heart"></i>
                </button>  

                ${image}
                <div class="product-info">
                    
                    <div class="product-title-wrapper">
                      <h4>${product.productName}</h4>
                      ${verifiedBadgeHTML}
                    </div>

                    <p class="brand">${spec?.brand || "N/A"}</p>
                    <p class="price">${formatPrice(product.price)}</p>
                    <div class="details">
                        <span><i class="fas fa-map-marker-alt"></i> ${
                          listing.location || "N/A"
                        }</span>
                        <span><i class="fas fa-calendar-alt"></i> ${
                          spec?.yearOfManufacture || "N/A"
                        }</span>
                        <span><i class="fas fa-bolt"></i> ${
                          spec?.batteryCapacity || "N/A"
                        }</span>
                        ${
                          product.productType !== "battery"
                            ? `<span><i class="fas fa-road"></i> ${
                                spec?.mileage || 0
                              } km</span>`
                            : ""
                        }
                    </div>
                    
                    ${conditionBarHTML} 

                    <div style="display: flex; align-items: center; gap: 10px; width: 100%;" class="product-actions">
                        <a 
                            href="/product_detail.html?id=${product.productId}" 
                            style="white-space: nowrap; flex: 1; text-align: center; display: flex; justify-content: center; align-items: center;"
                            class="btn-green-sm"
                        >
                            Xem chi tiết
                        </a>
                        
                        <button 
                            style="white-space: nowrap; flex: 1;"
                            class="btn-buy-css btn-buy-now" 
                            data-productid="${product.productId}"
                        >
                            Mua ngay
                        </button>
                    </div>

                    </div>
                </div>
            </div>
        `;
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    // Luôn hiển thị giá đầy đủ, định dạng VNĐ
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };
  // --- EVENT LISTENERS ---
  tabsContainer.addEventListener("click", (e) => {
    const target = e.target;
    if (!target.classList.contains("tab-btn")) return;

    tabsContainer.querySelector(".active").classList.remove("active");
    target.classList.add("active");

    const filterText = target.textContent.trim();
    let params = { type: "all", sortBy: "date" };

    switch (filterText) {
      case "Ô tô điện":
        params.type = "car";
        break;
      case "Xe đạp điện":
        params.type = "bike";
        break;
      case "Xe máy điện":
        params.type = "motorbike";
        break;
      case "Pin":
        params.type = "battery";
        break;
      case "Giá tốt":
        params.sortBy = "price";
        params.type = currentFilterParams.type; // Giữ lại type cũ khi sort theo giá
        break;
      case "Tất cả":
        params.type = "all";
        params.sortBy = "date"; // Reset về sort theo ngày
        break;
      case "Mới nhất":
        params.type = currentFilterParams.type; // Giữ lại type
        params.sortBy = "date"; // Sort theo ngày
        break;
    }

    // Cập nhật trạng thái lọc và link "Xem thêm"
    currentFilterParams = params;
    updateViewMoreLink();
    fetchAndDisplayProducts({ ...params, limit: 4 }); // Luôn chỉ lấy 4 sản phẩm
  });

  // --- INITIAL LOAD ---
  fetchAndDisplayProducts(); // <-- Tự động gọi với limit = 4 (mặc định)
  updateViewMoreLink(); // Cập nhật link cho lần đầu tải trang
});

//-------------------Cart---------------------------------
// Gọi API cart-service để add sản phẩm
// Gọi API cart-service để add sản phẩm — gửi X-User-Id từ localStorage
const Toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 3000, // Thời gian hiển thị 3s
  timerProgressBar: true, // Có thanh chạy bên dưới
  didOpen: (toast) => {
    toast.onmouseenter = Swal.stopTimer;
    toast.onmouseleave = Swal.resumeTimer;
  },
  // Class này kết hợp với CSS ở Bước 2 để tạo màu nền
  customClass: { popup: "colored-toast" },
});

// Hàm helper để gọi nhanh
function showToast(icon, message) {
  Toast.fire({ icon: icon, title: message });
}

// 2. Hàm buyNow đã chỉnh sửa để dùng Swal thay cho Toastify
async function buyNow(productId) {
  try {
    const userId = localStorage.getItem("userId") || null;

    // --- Kiểm tra đăng nhập ---
    if (!userId) {
      showToast("warning", "Vui lòng đăng nhập để thêm vào giỏ hàng!");
      return;
    }

    const res = await fetch(
      `/api/carts/add-by-product/${productId}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Id": userId,
        },
      }
    );

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || "HTTP " + res.status);
    }

    const saved = await res.json();
    console.log("Đã thêm vào giỏ:", saved);

    // --- THÀNH CÔNG: Hiển thị Toast xanh với thanh chạy ---
    showToast("success", "Đã thêm sản phẩm vào giỏ hàng!");
  } catch (err) {
    console.error("buyNow failed", err);

    // --- THẤT BẠI: Hiển thị Toast đỏ với thanh chạy ---
    showToast("error", "Thêm thất bại hoặc sản phẩm đã có trong giỏ!");
  }
}

//khi click vào "mua ngay", lấy productId từ data-productid và gọi buyNow(pid)
document.addEventListener("click", (e) => {
  const btn = e.target.closest(".btn-buy-now");
  if (!btn) return;
  const pid = btn.dataset.productid;
  if (!pid) return;
  buyNow(pid);
});
//-------------------Cart---------------------------------

// -------------------- LIKE  --------------------
const LIKE_API_BASE = "/api/likes";

let likedSet = new Set(); // sẽ chứa các productId đã like

// Lấy danh sách liked hiện tại từ like-service
const fetchLikedSet = async () => {
  try {
    const userId = localStorage.getItem("userId");
    const headers = {};
    if (userId) headers["X-User-Id"] = userId;

    const res = await fetch(LIKE_API_BASE, { headers });
    if (!res.ok) throw new Error("Không thể lấy liked list");
    const likes = await res.json();
    likedSet = new Set(likes.map((l) => Number(l.productId)));
  } catch (err) {
    console.warn("fetchLikedSet failed", err);
    likedSet = new Set();
  }
};

// Toggle like: nếu đã like -> xóa, nếu chưa -> thêm
async function toggleLike(productId, btnEl) {
  try {
    const pid = Number(productId);
    const userId = localStorage.getItem("userId");
    if (!userId) {
      alert("Vui lòng đăng nhập để thay đổi trạng thái yêu thích.");
      return;
    }

    const headers = { "X-User-Id": userId };

    if (likedSet.has(pid)) {
      // xóa
      const res = await fetch(`${LIKE_API_BASE}/by-product/${pid}`, {
        method: "DELETE",
        headers,
      });
      if (!res.ok && res.status !== 204) {
        // nếu server trả lỗi, báo cho user
        const txt = await res.text().catch(() => "");
        throw new Error(txt || "Xóa like thất bại: HTTP " + res.status);
      }
      likedSet.delete(pid);
      btnEl.classList.remove("active");
      const icon = btnEl.querySelector("i");
      if (icon) {
        icon.classList.remove("fas");
        icon.classList.add("far");
      }
      btnEl.setAttribute("aria-label", "Thêm vào yêu thích");
    } else {
      // thêm
      const res = await fetch(`${LIKE_API_BASE}/add-by-product/${pid}`, {
        method: "POST",
        headers,
      });
      if (!res.ok) {
        if (res.status === 409) {
          // already exists on server -> treat as success and add to set
          likedSet.add(pid);
        } else {
          const txt = await res.text().catch(() => "");
          throw new Error(txt || "Thêm like thất bại: HTTP " + res.status);
        }
      } else {
        const saved = await res.json();
        likedSet.add(pid);
        // nếu muốn lưu likeId cho UI, có thể: btnEl.dataset.likeId = saved.id;
      }
      btnEl.classList.add("active");
      const icon = btnEl.querySelector("i");
      if (icon) {
        icon.classList.remove("far");
        icon.classList.add("fas");
      }
      btnEl.setAttribute("aria-label", "Bỏ thích");
    }
  } catch (err) {
    console.error("toggleLike error", err);
    alert("Không thể thay đổi trạng thái yêu thích. Vui lòng thử lại.");
  }
}
// Delegated event listener: bắt click cho tất cả .favorite-btn
document.addEventListener("click", async (e) => {
  const favBtn = e.target.closest(".favorite-btn");
  if (!favBtn) return;
  const pid = favBtn.dataset.productid;
  if (!pid) return;
  // disable button tạm để tránh spam
  favBtn.disabled = true;
  try {
    await toggleLike(pid, favBtn);
    } finally {
    favBtn.disabled = false;
  }
});
// INIT: trước khi render products, load likedSet -> sau đó gọi hàm render products
/* (async function initPage() {
  try {
    // 1) load danh sách liked trước
    await fetchLikedSet();
  } catch (e) {
    console.warn("fetchLikedSet failed", e);
  }

  // 2) render products bằng hàm nào đang thực sự tồn tại
  try {
    if (typeof fetchAndDisplayProducts === "function") {
      await fetchAndDisplayProducts({
        type: currentFilterParams?.type ?? "all",
        sortBy: currentFilterParams?.sortBy ?? "date",
        limit: 4,
      });
    } else if (typeof loadProducts === "function") {
      await loadProducts();
    } else if (typeof displayProducts === "function") {
      await displayProducts();
    } else if (typeof fetchProducts === "function") {
      await fetchProducts();
    } else {
      console.warn("Không tìm thấy hàm render products phù hợp");
    }
  } catch (err) {
    console.warn("Render products failed", err);
  }

  // 3) update link nếu có
  try {
    if (typeof updateViewMoreLink === "function") {
      updateViewMoreLink();
    }
  } catch (e) {
    console.warn("updateViewMoreLink failed", e);
  }
})();*/

// -------------------- END LIKE  --------------------
