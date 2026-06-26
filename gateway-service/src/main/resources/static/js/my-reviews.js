document.addEventListener("DOMContentLoaded", () => {
  // =============================================
  // 1. CẤU HÌNH URL
  // =============================================
  const REVIEW_SERVICE_URL = "/api/reviews";
  const USER_SERVICE_URL = "/api/user";
  const LISTING_SERVICE_URL_PRODUCTS = "/api/products";
  const LISTING_SERVICE_ORIGIN = "";

  const PAGE_SIZE = 10;
  let CURRENT_USER_ID = null;
  let CURRENT_USER_NAME = null;
  let AUTH_INFO = null;

  // Biến quản lý phân trang
  let currentReviewTab = "TO_REVIEW";
  let toReviewPage = 0;
  let completedPage = 0;

  const userCache = new Map();
  const productCache = new Map();

  // =============================================
  // 2. DOM ELEMENTS
  // =============================================
  const mainTabs = document.querySelector(".main-tabs");
  const mainTabContents = document.querySelectorAll("main > .tab-content");
  const toReviewContainer = document.getElementById(
    "tasks-to-review-container"
  );
  const completedContainer = document.getElementById(
    "tasks-completed-container"
  );
  const paginationToReview = document.getElementById("pagination-to-review");
  const paginationCompleted = document.getElementById("pagination-completed");

  // Modal Elements
  const reviewModalBackdrop = document.getElementById("reviewModalBackdrop");
  const reviewModalContent = document.getElementById("reviewModalContent");
  const reviewForm = document.getElementById("review-form");
  const cancelReviewBtn = document.getElementById("cancelReviewBtn");
  const submitReviewBtn = document.getElementById("submitReviewBtn");

  const modalTitle = document.getElementById("modal-title");
  const modalProductName = document.getElementById("modal-product-name");
  const modalProductIdInput = document.getElementById("modalProductId");
  const modalReviewIdInput = document.getElementById("modalReviewId");
  const modalMessage = document.getElementById("modal-message");
  const modalComment = document.getElementById("comment");

  // Star Rating Elements
  const starContainer = document.getElementById("star-container");
  const stars = starContainer.querySelectorAll("i");
  const ratingInput = document.getElementById("rating-input");
  const ratingText = document.getElementById("rating-text");

  const ratingLabels = {
    1: "Rất tệ",
    2: "Tệ",
    3: "Bình thường",
    4: "Tốt",
    5: "Tuyệt vời",
  };

  // =============================================
  // 3. HÀM HELPER & LOGIC SAO
  // =============================================
  const formatPrice = (price) => {
    if (!price) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  function setupStarRating() {
    stars.forEach((star) => {
      star.addEventListener("mouseover", () => {
        if (starContainer.classList.contains("readonly")) return;
        highlightStars(parseInt(star.dataset.value));
      });
      star.addEventListener("click", () => {
        if (starContainer.classList.contains("readonly")) return;
        const val = parseInt(star.dataset.value);
        ratingInput.value = val;
        highlightStars(val);
        ratingText.textContent = ratingLabels[val];
      });
    });
    starContainer.addEventListener("mouseleave", () => {
      if (starContainer.classList.contains("readonly")) return;
      highlightStars(parseInt(ratingInput.value));
    });
  }

  function highlightStars(value) {
    stars.forEach((s) => {
      const sVal = parseInt(s.dataset.value);
      if (sVal <= value) {
        s.classList.add("active");
        s.classList.remove("text-gray-300");
      } else {
        s.classList.remove("active");
        s.classList.add("text-gray-300");
      }
    });
  }

  // =============================================
  // 4. INIT
  // =============================================
  function initializePage() {
    setupStarRating();
    AUTH_INFO = getAuthInfo();
    if (!AUTH_INFO) {
      Swal.fire("Thông báo", "Bạn cần đăng nhập.", "warning").then(() => {
        window.location.href = "/login.html";
      });
      return;
    }
    CURRENT_USER_ID = AUTH_INFO.userId;
    CURRENT_USER_NAME = AUTH_INFO.user.name;

    loadToReviewPane(toReviewPage);
    setupTabListeners();
    setupModalListeners();
    setupCardClickListeners();
  }

  // =============================================
  // 5. API CALLS
  // =============================================
  async function getUserName(userId) {
    if (userCache.has(userId)) return userCache.get(userId);
    try {
      const res = await fetch(`${USER_SERVICE_URL}/${userId}`, {
        headers: { Authorization: `Bearer ${AUTH_INFO.token}` },
      });
      if (res.ok) {
        const data = await res.json();
        const name = data.name || `User #${userId}`;
        userCache.set(userId, name);
        return name;
      }
    } catch {}
    return `User #${userId}`;
  }

  async function getProductInfo(productId, fallbackName) {
    if (!productId) return { productName: fallbackName, imageUrl: null };
    if (productCache.has(productId)) return productCache.get(productId);
    try {
      const res = await fetch(`${LISTING_SERVICE_URL_PRODUCTS}/${productId}`, {
        headers: { Authorization: `Bearer ${AUTH_INFO.token}` },
      });
      if (res.ok) {
        const data = await res.json();
        const img = data.images?.length > 0 ? data.images[0].imageUrl : null;
        const info = { productName: data.productName, imageUrl: img };
        productCache.set(productId, info);
        return info;
      }
    } catch {}
    return { productName: fallbackName, imageUrl: null };
  }

  async function loadReviewTasks(url, container, pageIndex, isCompletedTab) {
    try {
      const response = await fetch(url, {
        headers: { "X-User-Id": CURRENT_USER_ID },
      });
      if (!response.ok) throw new Error("Lỗi tải dữ liệu");

      const data = await response.json();

      // Cập nhật biến page
      if (isCompletedTab) completedPage = data.number;
      else toReviewPage = data.number;

      if (data.empty) {
        container.innerHTML = `<div class="text-center py-12"><p class="text-gray-500">Không có dữ liệu.</p></div>`;
        const pagContainer = isCompletedTab
          ? paginationCompleted
          : paginationToReview;
        pagContainer.innerHTML = "";
      } else {
        const tasks = await Promise.all(
          data.content.map(async (task) => {
            const [seller, buyer, product] = await Promise.all([
              getUserName(task.sellerId),
              getUserName(task.buyerId),
              getProductInfo(task.productId, task.productName),
            ]);
            task.sellerName = seller;
            task.buyerName = buyer;
            task.productName = product.productName;
            task.mainImageUrl = product.imageUrl;
            return task;
          })
        );

        container.innerHTML = tasks
          .map((task) => {
            const myReview = task.reviews.find(
              (r) => r.reviewerId === CURRENT_USER_ID
            );
            return createTaskCard(task, myReview);
          })
          .join("");

        const pagContainer = isCompletedTab
          ? paginationCompleted
          : paginationToReview;
        renderPagination(
          pagContainer,
          data.number,
          data.totalPages,
          (newPage) => {
            if (isCompletedTab) loadCompletedPane(newPage);
            else loadToReviewPane(newPage);
            window.scrollTo({ top: 0, behavior: "smooth" });
          }
        );
      }
    } catch (error) {
      console.error(error);
    }
  }

  function loadToReviewPane(page) {
    const url = `${REVIEW_SERVICE_URL}/tasks/to-review?page=${page}&size=${PAGE_SIZE}`;
    loadReviewTasks(url, toReviewContainer, page, false);
  }

  function loadCompletedPane(page) {
    const url = `${REVIEW_SERVICE_URL}/tasks/completed?page=${page}&size=${PAGE_SIZE}`;
    loadReviewTasks(url, completedContainer, page, true);
  }

  // ==============================================================
  // 6. RENDER PAGINATION (LOGIC CHUẨN 100% TỪ MANAGE-LISTINGS)
  // ==============================================================
  const renderPagination = (
    container,
    currentPage,
    totalPages,
    pageChangeHandler
  ) => {
    container.innerHTML = "";
    if (totalPages <= 1) return;

    const getPaginationRange = (currentPage, totalPages) => {
      if (totalPages <= 7) {
        return Array.from({ length: totalPages }, (_, i) => i);
      }
      const pages = new Set();
      pages.add(0);
      if (currentPage > 2) pages.add("...");
      if (currentPage > 0) pages.add(currentPage - 1);
      pages.add(currentPage);
      if (currentPage < totalPages - 1) pages.add(currentPage + 1);
      if (currentPage < totalPages - 3) pages.add("...");
      pages.add(totalPages - 1);
      return Array.from(pages);
    };

    const pagesToShow = getPaginationRange(currentPage, totalPages);

    // Nút "Trước"
    const prevBtn = document.createElement("button");
    prevBtn.className = "pagination-btn";
    prevBtn.textContent = "Trước";
    prevBtn.disabled = currentPage === 0;
    prevBtn.addEventListener("click", () => pageChangeHandler(currentPage - 1));
    container.appendChild(prevBtn);

    // Nút số và dấu ...
    pagesToShow.forEach((page) => {
      if (page === "...") {
        const dots = document.createElement("span");
        dots.className = "pagination-dots";
        dots.textContent = "...";
        container.appendChild(dots);
      } else {
        const pageBtn = document.createElement("button");
        pageBtn.className = "pagination-btn";
        pageBtn.textContent = page + 1;
        if (page === currentPage) {
          pageBtn.classList.add("active");
        }
        pageBtn.addEventListener("click", () => pageChangeHandler(page));
        container.appendChild(pageBtn);
      }
    });

    // Nút "Sau"
    const nextBtn = document.createElement("button");
    nextBtn.className = "pagination-btn";
    nextBtn.textContent = "Sau";
    nextBtn.disabled = currentPage >= totalPages - 1;
    nextBtn.addEventListener("click", () => pageChangeHandler(currentPage + 1));
    container.appendChild(nextBtn);
  };

  // =============================================
  // 7. RENDER CARD (ĐÃ SỬA: THÊM LINK CHO ẢNH VÀ TÊN)
  // =============================================
  function createTaskCard(task, myReview) {
    const now = new Date();
    const expiresAt = new Date(task.expiresAt);
    const isExpired = now > expiresAt;
    const isBuyerTask = task.buyerId === CURRENT_USER_ID;

    let roleBadge = isBuyerTask
      ? `<span class="text-xs font-bold bg-blue-100 text-blue-800 px-2 py-1 rounded">Người mua</span>`
      : `<span class="text-xs font-bold bg-green-100 text-green-800 px-2 py-1 rounded">Người bán</span>`;

    let targetText = isBuyerTask
      ? `Người bán: ${task.sellerName}`
      : `Người mua: ${task.buyerName}`;
    let imgUrl = task.mainImageUrl
      ? `${LISTING_SERVICE_ORIGIN}${task.mainImageUrl}`
      : "https://placehold.co/150x150?text=No+Img";

    let buttonsHTML = "";
    if (myReview) {
      const reviewDate = new Date(myReview.createdAt);
      const canEdit =
        (now - reviewDate) / (1000 * 60 * 60 * 24) <= 15 && !myReview.updatedAt;

      buttonsHTML += `
            <button class="btn-action btn-view-my-review"
                data-product-name="${task.productName}"
                data-rating="${myReview.rating}"
                data-comment="${encodeURIComponent(myReview.comment || "")}"
            ><i class="fas fa-eye mr-2"></i> Xem đánh giá của bạn</button>
        `;
      if (canEdit) {
        buttonsHTML += `
                <button class="btn-action btn-edit mt-2" 
                    data-review-id="${myReview.id}"
                    data-product-id="${task.productId}"
                    data-product-name="${task.productName}"
                    data-rating="${myReview.rating}"
                    data-comment="${encodeURIComponent(myReview.comment || "")}"
                ><i class="fas fa-edit mr-2"></i> Sửa lại</button>
            `;
      }
    } else {
      if (isExpired) {
        buttonsHTML = `<button class="btn-action btn-disabled" disabled>Đã quá hạn</button>`;
      } else {
        buttonsHTML = `
                <button class="btn-action btn-review" 
                    data-product-id="${task.productId}"
                    data-product-name="${task.productName}"
                ><i class="fas fa-star mr-2"></i> Đánh giá ngay</button>
            `;
      }
    }

    // THÊM LINK <a> CHO ẢNH VÀ TIÊU ĐỀ
    return `
      <div class="review-task-card">
        <div class="card-image-container">
            <a href="/product_detail.html?id=${
              task.productId
            }" target="_blank" class="block w-full h-full">
                <img src="${imgUrl}" class="card-image" alt="${
      task.productName
    }" />
            </a>
            <div class="absolute top-0 left-0 p-1">${roleBadge}</div>
        </div>
        <div class="card-content">
            <h3 class="font-bold text-lg">
                <a href="/product_detail.html?id=${
                  task.productId
                }" target="_blank" class="hover:text-green-700 hover:underline transition-colors">
                    ${task.productName}
                </a>
            </h3>
            <p class="text-sm text-gray-600">${targetText}</p>
            <p class="text-red-600 font-bold mt-1">${formatPrice(
              task.price
            )}</p>
        </div>
        <div class="card-action">
            ${buttonsHTML}
        </div>
      </div>
    `;
  }

  // =============================================
  // 8. MODAL & FORM LOGIC
  // =============================================
  function openReviewModal(mode, data) {
    modalMessage.textContent = "";
    reviewForm.reset();

    modalProductName.textContent = data.productName;
    modalProductIdInput.value = data.productId || "";

    starContainer.classList.remove("readonly");
    modalComment.disabled = false;
    submitReviewBtn.classList.remove("hidden");
    cancelReviewBtn.textContent = "Hủy";

    if (mode === "view") {
      modalTitle.textContent = "Đánh giá của bạn";
      submitReviewBtn.classList.add("hidden");
      cancelReviewBtn.textContent = "Đóng";

      const rating = parseInt(data.rating);
      ratingInput.value = rating;
      modalComment.value = data.comment;
      ratingText.textContent = ratingLabels[rating] || "";

      modalComment.disabled = true;
      starContainer.classList.add("readonly");
      highlightStars(rating);
    } else if (mode === "edit") {
      modalTitle.textContent = "Sửa đánh giá";
      modalReviewIdInput.value = data.reviewId;
      submitReviewBtn.textContent = "Lưu thay đổi";

      const rating = parseInt(data.rating);
      ratingInput.value = rating;
      modalComment.value = data.comment;
      ratingText.textContent = ratingLabels[rating] || "";
      highlightStars(rating);
    } else {
      modalTitle.textContent = "Viết đánh giá";
      modalReviewIdInput.value = "";
      submitReviewBtn.textContent = "Gửi đánh giá";
      ratingInput.value = "0";
      ratingText.textContent = "";
      highlightStars(0);
    }

    reviewModalBackdrop.classList.remove("hidden");
    reviewModalContent.classList.remove("hidden");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const ratingVal = parseInt(ratingInput.value);
    if (ratingVal === 0) {
      modalMessage.textContent = "Vui lòng chọn số sao!";
      return;
    }

    submitReviewBtn.disabled = true;
    submitReviewBtn.innerHTML =
      '<i class="fas fa-spinner fa-spin"></i> Đang gửi...';

    const reviewId = modalReviewIdInput.value;
    const isEdit = !!reviewId;

    const payload = {
      productId: parseInt(modalProductIdInput.value),
      rating: ratingVal,
      comment: modalComment.value.trim(),
      reviewerName: CURRENT_USER_NAME,
    };

    try {
      const url = isEdit
        ? `${REVIEW_SERVICE_URL}/${reviewId}`
        : REVIEW_SERVICE_URL;
      const method = isEdit ? "PUT" : "POST";

      const res = await fetch(url, {
        method: method,
        headers: {
          "Content-Type": "application/json",
          "X-User-Id": CURRENT_USER_ID,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error("Gửi thất bại");

      closeReviewModal();
      Swal.fire({
        icon: "success",
        title: isEdit ? "Đã cập nhật!" : "Đã gửi đánh giá!",
        timer: 1500,
        showConfirmButton: false,
      });

      loadToReviewPane(toReviewPage);
      loadCompletedPane(completedPage);
    } catch (err) {
      modalMessage.textContent = "Có lỗi xảy ra, vui lòng thử lại.";
    } finally {
      submitReviewBtn.disabled = false;
      submitReviewBtn.textContent = isEdit ? "Lưu thay đổi" : "Gửi đánh giá";
    }
  }

  // =============================================
  // 9. EVENT LISTENERS
  // =============================================
  function setupTabListeners() {
    mainTabs.addEventListener("click", (e) => {
      const btn = e.target.closest(".tab-button");
      if (!btn) return;
      document
        .querySelectorAll(".tab-button")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      mainTabContents.forEach((c) => c.classList.remove("active"));
      document.querySelector(btn.dataset.tabTarget).classList.add("active");

      const newTab = btn.dataset.tabTarget.includes("to-review")
        ? "TO_REVIEW"
        : "COMPLETED";
      if (newTab !== currentReviewTab) {
        currentReviewTab = newTab;
        if (newTab === "TO_REVIEW") loadToReviewPane(toReviewPage);
        else loadCompletedPane(completedPage);
      }
    });
  }

  function setupModalListeners() {
    cancelReviewBtn.addEventListener("click", closeReviewModal);
    reviewModalBackdrop.addEventListener("click", closeReviewModal);
    reviewModalContent.addEventListener("click", (e) => e.stopPropagation());
    reviewForm.addEventListener("submit", handleSubmit);
  }

  function setupCardClickListeners() {
    const handleCardClick = (e) => {
      const btnReview = e.target.closest(".btn-review");
      if (btnReview) {
        openReviewModal("create", {
          productId: btnReview.dataset.productId,
          productName: btnReview.dataset.productName,
        });
        return;
      }
      const btnEdit = e.target.closest(".btn-edit");
      if (btnEdit) {
        openReviewModal("edit", {
          reviewId: btnEdit.dataset.reviewId,
          productId: btnEdit.dataset.productId,
          productName: btnEdit.dataset.productName,
          rating: btnEdit.dataset.rating,
          comment: decodeURIComponent(btnEdit.dataset.comment),
        });
        return;
      }
      const btnView = e.target.closest(".btn-view-my-review");
      if (btnView) {
        openReviewModal("view", {
          productName: btnView.dataset.productName,
          rating: btnView.dataset.rating,
          comment: decodeURIComponent(btnView.dataset.comment),
        });
        return;
      }
    };
    toReviewContainer.addEventListener("click", handleCardClick);
    completedContainer.addEventListener("click", handleCardClick);
  }

  function getAuthInfo() {
    const t = localStorage.getItem("token");
    const uStr = localStorage.getItem("user");
    if (!t || !uStr) return null;
    try {
      return {
        token: t,
        user: JSON.parse(uStr),
        userId: Number(JSON.parse(uStr).id || JSON.parse(uStr).userId),
      };
    } catch {
      return null;
    }
  }

  initializePage();
});
