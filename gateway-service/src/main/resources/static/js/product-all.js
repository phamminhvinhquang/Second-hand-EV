document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "";
  const BACKEND_ORIGIN = "";
  const PAGE_SIZE = 24; // Số sản phẩm mỗi trang

  // --- CẤU HÌNH TOAST (THÔNG BÁO) ---
  // Khai báo Toast duy nhất ở đây để dùng chung cho cả file
  const Toast = Swal.mixin({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 3000, // 3s
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.onmouseenter = Swal.stopTimer;
      toast.onmouseleave = Swal.resumeTimer;
    },
    customClass: { popup: "colored-toast" },
  });

  function showToast(icon, message) {
    Toast.fire({ icon: icon, title: message });
  }

  // --- DOM ELEMENTS ---
  const productsGrid = document.querySelector(".products-grid");
  const sectionTitle = document.getElementById("sectionTitle");
  const paginationContainer = document.getElementById("paginationContainer");

  // --- STATE ---
  let currentPage = 0;
  let totalPages = 0;
  let currentFilter = {
    type: "all",
    sortBy: "date",
  };

  // --- FUNCTIONS ---

  const fetchAndDisplayProducts = async (page = 0) => {
    currentPage = page;
    productsGrid.innerHTML = `<p class="loading-text">Đang tải dữ liệu...</p>`;
    try {
      const params = new URLSearchParams({
        sortBy: currentFilter.sortBy,
        page: currentPage,
        size: PAGE_SIZE,
      });
      if (currentFilter.type !== "all") {
        params.append("type", currentFilter.type);
      }

      const response = await fetch(
        `${API_BASE_URL}/listings?${params.toString()}`
      );
      if (!response.ok) throw new Error(`Lỗi mạng: ${response.statusText}`);

      const pageData = await response.json();
      const listings = pageData.content;
      totalPages = pageData.totalPages;

      productsGrid.innerHTML = "";

      if (listings.length === 0) {
        productsGrid.innerHTML = `<p class="not-found-text">Không tìm thấy tin đăng nào.</p>`;
      } else {
        listings.forEach((listing) => {
          productsGrid.insertAdjacentHTML(
            "beforeend",
            createProductCardHTML(listing)
          );
        });
      }

      renderPagination();
      updateTitle();
    } catch (error) {
      console.error("Không thể tải dữ liệu sản phẩm:", error);
      productsGrid.innerHTML = `<p class="error-text">Đã xảy ra lỗi khi tải dữ liệu.</p>`;
    }
  };

  const createProductCardHTML = (listing) => {
    if (!listing || !listing.product || !listing.product.specification) {
      console.warn("Dữ liệu listing không đầy đủ:", listing);
      return "";
    }

    const product = listing.product;
    const spec = product.specification;

    //=====================like========================
    const pid = product.productId;
    const isLiked =
      typeof likedSet !== "undefined" && likedSet.has(Number(pid));
    //=====================like========================

    const verifiedBadgeHTML = listing.verified
      ? `<span class="product-card-verified-badge"><i class="fas fa-check-circle"></i> Đã Kiểm Định</span>`
      : "";

    const conditionText = spec.condition?.conditionName || "Không rõ";
    let conditionPercent = 0;
    if (conditionText.includes("%")) {
      const match = conditionText.match(/(\d+)/);
      if (match) conditionPercent = parseInt(match[1], 10);
    }
    const conditionBarHTML = `
      <div class="condition-bar" title="${conditionText}">
        <div class="condition-bar-fill" style="width: ${conditionPercent}%;"></div>
      </div>
      <span class="condition-text">${conditionText}</span>`;

    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : `https://via.placeholder.com/300x200.png?text=${
            product.productType?.toUpperCase() || "IMG"
          }`;
    const image = `<img src="${imageUrl}" alt="${
      product.productName || ""
    }" class="product-image">`;

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
                <h4>${product.productName || "[Không có tiêu đề]"}</h4>
                ${verifiedBadgeHTML}
              </div>
              <p class="brand">${spec.brand || "N/A"}</p>
              <p class="price">${formatPrice(product.price)}</p>
              <div class="details">
                  <span><i class="fas fa-map-marker-alt"></i> ${
                    listing.location || "N/A"
                  }</span>
                  <span><i class="fas fa-calendar-alt"></i> ${
                    spec.yearOfManufacture || "N/A"
                  }</span>
                  <span><i class="fas fa-bolt"></i> ${
                    spec.batteryCapacity || "N/A"
                  }</span>
                  ${
                    product.productType !== "battery"
                      ? `<span><i class="fas fa-road"></i> ${
                          spec.mileage || 0
                        } km</span>`
                      : ""
                  }
              </div>
              ${conditionBarHTML}
              <div class="product-actions">
                  <a href="/product_detail.html?id=${
                    product.productId
                  }" class="btn-green-sm">Xem chi tiết</a> 
                  <button 
                    style="white-space: nowrap; flex: 1;"
                    class="btn-buy-css btn-buy-now" 
                    data-productid="${product.productId}">Mua ngay
                  </button>
              </div>
          </div>
      </div>`;
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const getTypeName = (type) => {
    switch (type) {
      case "car":
        return "Ô tô điện";
      case "bike":
        return "Xe đạp điện";
      case "motorbike":
        return "Xe máy điện";
      case "battery":
        return "Pin";
      default:
        return "Tất cả tin đăng";
    }
  };

  const updateTitle = () => {
    if (sectionTitle) {
      sectionTitle.textContent = getTypeName(currentFilter.type);
    }
  };

  // --- PAGINATION ---
  const renderPagination = () => {
    paginationContainer.innerHTML = "";
    if (totalPages <= 1) return;

    paginationContainer.innerHTML += `
      <button class="pagination-btn" data-page="${currentPage - 1}" ${
      currentPage === 0 ? "disabled" : ""
    }>Trước</button>`;

    const getPaginationRange = (curr, total) => {
      if (total <= 7) return Array.from({ length: total }, (_, i) => i);
      const pages = new Set([0, total - 1, curr, curr - 1, curr + 1]);
      const sorted = Array.from(pages)
        .filter((p) => p >= 0 && p < total)
        .sort((a, b) => a - b);
      const res = [];
      let prev = null;
      for (const p of sorted) {
        if (prev !== null && p - prev > 1) res.push("...");
        res.push(p);
        prev = p;
      }
      return res;
    };

    const pagesToShow = getPaginationRange(currentPage, totalPages);

    pagesToShow.forEach((page) => {
      if (page === "...") {
        paginationContainer.innerHTML += `<span class="pagination-dots">...</span>`;
      } else {
        paginationContainer.innerHTML += `
            <button class="pagination-btn ${
              page === currentPage ? "active" : ""
            }" data-page="${page}">${page + 1}</button>`;
      }
    });

    paginationContainer.innerHTML += `
      <button class="pagination-btn" data-page="${currentPage + 1}" ${
      currentPage >= totalPages - 1 ? "disabled" : ""
    }>Sau</button>`;
  };

  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      const page = parseInt(button.dataset.page, 10);
      window.scrollTo({ top: 0, behavior: "smooth" });
      fetchAndDisplayProducts(page);
    }
  });

  // --- CART FUNCTION (ĐÃ GỘP VÀO ĐÂY) ---
  async function buyNow(productId) {
    try {
      const userId = localStorage.getItem("userId") || null;
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

      showToast("success", "Đã thêm sản phẩm vào giỏ hàng!");
    } catch (err) {
      console.error("buyNow failed", err);
      showToast("error", "Thêm thất bại hoặc sản phẩm đã có trong giỏ!");
    }
  }

  // Global click listener for Buy Now
  document.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-buy-now");
    if (!btn) return;
    const pid = btn.dataset.productid;
    if (!pid) return;
    buyNow(pid);
  });

  // --- LIKE FUNCTION ---
  const LIKE_API_BASE = "/api/likes";
  window.likedSet = new Set();

  const fetchLikedSet = async () => {
    try {
      const userId = localStorage.getItem("userId");
      if (!userId) return;
      const res = await fetch(LIKE_API_BASE, {
        headers: { "X-User-Id": userId },
      });
      if (res.ok) {
        const likes = await res.json();
        window.likedSet = new Set(likes.map((l) => Number(l.productId)));
      }
    } catch (err) {
      console.warn("fetchLikedSet failed", err);
    }
  };

  async function toggleLike(productId, btnEl) {
    try {
      const pid = Number(productId);
      const userId = localStorage.getItem("userId");
      if (!userId) {
        showToast("warning", "Vui lòng đăng nhập để yêu thích!");
        return;
      }
      const headers = { "X-User-Id": userId };

      if (window.likedSet.has(pid)) {
        const res = await fetch(`${LIKE_API_BASE}/by-product/${pid}`, {
          method: "DELETE",
          headers,
        });
        if (res.ok || res.status === 204) {
          window.likedSet.delete(pid);
          btnEl.classList.remove("active");
          const icon = btnEl.querySelector("i");
          if (icon) {
            icon.classList.remove("fas");
            icon.classList.add("far");
          }
          btnEl.setAttribute("aria-label", "Thêm vào yêu thích");
        }
      } else {
        const res = await fetch(`${LIKE_API_BASE}/add-by-product/${pid}`, {
          method: "POST",
          headers,
        });
        if (res.ok || res.status === 409) {
          window.likedSet.add(pid);
          btnEl.classList.add("active");
          const icon = btnEl.querySelector("i");
          if (icon) {
            icon.classList.remove("far");
            icon.classList.add("fas");
          }
          btnEl.setAttribute("aria-label", "Bỏ thích");
        }
      }
    } catch (err) {
      console.error("toggleLike error", err);
      showToast("error", "Lỗi thao tác yêu thích.");
    }
  }

  document.addEventListener("click", async (e) => {
    const favBtn = e.target.closest(".favorite-btn");
    if (!favBtn) return;
    const pid = favBtn.dataset.productid;
    if (!pid) return;
    favBtn.disabled = true;
    try {
      await toggleLike(pid, favBtn);
    } finally {
      favBtn.disabled = false;
    }
  });

  // --- INIT ---
  (async function initPage() {
    const urlParams = new URLSearchParams(window.location.search);
    currentFilter.type = urlParams.get("type") || "all";
    currentFilter.sortBy = urlParams.get("sortBy") || "date";

    await fetchLikedSet();
    fetchAndDisplayProducts(0);
  })();
});
