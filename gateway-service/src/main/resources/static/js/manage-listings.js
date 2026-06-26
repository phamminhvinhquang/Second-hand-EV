document.addEventListener("DOMContentLoaded", () => {
  const LISTING_SERVICE_URL = "/api";
  const USER_SERVICE_URL = "/api";
  const REVIEW_SERVICE_URL = "/api";
  const NOTIFICATION_WS_URL = "/ws-notification/ws";
  const BACKEND_ORIGIN = "";
  const MAX_IMAGE_COUNT = 8;
  const PAGE_SIZE = 12; // Kích thước trang cho Tin Đăng
  const REVIEW_PAGE_SIZE = 10; // Kích thước trang cho Đánh Giá
  const COMPLAINT_API_BASE = LISTING_SERVICE_URL;
  // =============================================
  // BIẾN TOÀN CỤC CHO TRANG
  // =============================================
  let PROFILE_USER_ID = null;
  let CURRENT_USER_ID = null;
  let AUTH_TOKEN = null;
  let IS_OWNER = false;

  let allListings = [];
  let listingsByStatus = {
    PENDING: [],
    ACTIVE: [],
    REJECTED: [],
    SOLD: [],
  };
  let currentListingPage = 0;
  let totalPages = 0;

  // --- Biến Đánh giá ---
  let currentReviewPage = 0;
  let currentReviewRole = null; // null = ALL, 'BUYER', 'SELLER'
  // --- Kết thúc ---

  const urlParams = new URLSearchParams(window.location.search);
  const urlTab = urlParams.get("tab")?.toUpperCase();
  let currentListingTab = "ACTIVE";

  if (urlTab && ["ACTIVE", "SOLD", "PENDING", "REJECTED"].includes(urlTab)) {
    currentListingTab = urlTab;
  }

  // --- Cache ---
  const productCache = new Map(); // Cache cho ảnh sản phẩm
  const userCache = new Map(); // Cache cho tên người dùng
  // --- Kết thúc ---

  let currentEditingListing = null;
  let stompClient = null;
  const newFilesDataTransfer = new DataTransfer();
  let imagesToDelete = [];
  let originalImagesCopy = [];
  // =============================================
  // DOM ELEMENTS
  // =============================================
  const userNameEl = document.getElementById("user-name");
  const userEmailEl = document.getElementById("user-email");
  const userAddressEl = document.getElementById("user-address");
  const userPhoneEl = document.getElementById("user-phone");
  const avgRatingEl = document.getElementById("avg-rating");
  const totalReviewsEl = document.getElementById("total-reviews");
  const activeCountEl = document.getElementById("active-count");
  const soldCountEl = document.getElementById("sold-count");

  const mainTabs = document.querySelector(".main-tabs");
  const mainTabContents = document.querySelectorAll("main > .tab-content");

  const listingSubTabs = document.querySelector(".listing-sub-tabs");
  const listingTabPanes = {
    PENDING: document.getElementById("pending-listings"),
    ACTIVE: document.getElementById("active-listings"),
    REJECTED: document.getElementById("rejected-listings"),
    SOLD: document.getElementById("sold-listings"),
  };
  const paginationContainer = document.getElementById("paginationContainer");

  // --- DOM Đánh giá (CẬP NHẬT) ---
  const reviewFilterTabs = document.querySelector(".review-filter-tabs");
  const reviewsContainer = document.getElementById("reviews-container");
  const paginationReviews = document.getElementById("pagination-reviews");
  // --- Kết thúc ---

  const editModal = document.getElementById("editModal");
  const editForm = document.getElementById("editForm");
  const currentImagesPreview = document.getElementById("currentImagesPreview");
  const newImagesPreview = document.getElementById("newImagesPreview");
  const editImageUpload = document.getElementById("editImageUpload");
  const viewDetailsModal = document.getElementById("viewDetailsModal");
  const cancelEditBtn = document.getElementById("cancelEditBtn");
  const closeViewModalBtn = document.getElementById("closeViewModalBtn");

  // =============================================
  // HÀM HELPER (TỪ CODE CŨ)
  // =============================================
  const modalAnimation = {
    showClass: { popup: "animate__animated animate__zoomIn animate__faster" },
    hideClass: { popup: "animate__animated animate__zoomOut animate__faster" },
  };

  const Toast = Swal.mixin({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.onmouseenter = Swal.stopTimer;
      toast.onmouseleave = Swal.resumeTimer;
    },
    ...modalAnimation,
  });

  const swalTheme = { cancelButtonColor: "#6c757d" };

  const productTypeMap = {
    car: "Ô Tô Điện",
    motorbike: "Xe Máy Điện",
    bike: "Xe Đạp Điện",
    battery: "Pin Đã Qua Sử Dụng",
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const getStatusBadge = (status, isLarge = false) => {
    const statuses = {
      PENDING: "bg-yellow-100 text-yellow-800",
      ACTIVE: "bg-green-100 text-green-800",
      SOLD: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    const sizeClass = isLarge ? "px-3 py-1 text-sm" : "px-2 text-xs";
    return `<span class="${sizeClass} inline-flex leading-5 font-semibold rounded-full ${
      statuses[status] || "bg-gray-100 text-gray-800"
    }">${status}</span>`;
  };

  const parseDate = (dateString) => {
    if (!dateString) return null;
    try {
      const isoString = dateString.replace(" ", "T");
      const date = new Date(isoString);
      return isNaN(date.getTime()) ? null : date;
    } catch (err) {
      console.warn("Lỗi parse ngày:", err);
      return null;
    }
  };
  // =============================================
  // ✅ SỬA LỖI 1: Thêm hàm escapeHtml (fix 'escapeHtml is not defined')
  // =============================================
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
  // =============================================
  // ✅ KẾT THÚC SỬA LỖI 1
  // =============================================
  // =============================================
  // HÀM KHỞI TẠO (HÀM CHÍNH)
  // =============================================

  async function initializePage() {
    // 1. Lấy thông tin người đang đăng nhập
    const auth = getAuthInfo();
    if (auth) {
      CURRENT_USER_ID = auth.userId;
      AUTH_TOKEN = auth.token;
      userCache.set(CURRENT_USER_ID, auth.user.name); // Tự cache tên mình
    }

    // 2. Lấy ID của profile đang xem từ URL (ví dụ: edit_news.html?id=6)
    const currentUrlParams = new URLSearchParams(window.location.search);
    PROFILE_USER_ID = currentUrlParams.get("id");

    // 3. Quyết định logic (Xem profile người khác hay của chính mình)
    if (!PROFILE_USER_ID) {
      if (!CURRENT_USER_ID) {
        alert("Bạn cần đăng nhập để xem trang này.");
        window.location.href = "/login.html";
        return;
      }
      PROFILE_USER_ID = CURRENT_USER_ID;
      IS_OWNER = true;
    } else {
      PROFILE_USER_ID = Number(PROFILE_USER_ID);
      IS_OWNER = CURRENT_USER_ID && PROFILE_USER_ID === CURRENT_USER_ID;
    }

    // 4. Cài đặt giao diện cơ bản
    setupUIBasedOnOwnership();
    setupTabListeners();

    // --- [SỬA ĐỔI QUAN TRỌNG] XỬ LÝ CHUYỂN TAB TỰ ĐỘNG ---
    // Biến urlTab được khai báo ở đầu file (dòng 30), lấy từ ?tab=...
    if (urlTab === "REVIEWS") {
      console.log("Phát hiện tab REVIEWS, đang chuyển tab...");
      // Chuyển sang tab Đánh giá
      switchToMainTab("#reviews-tab-content");

      // Tự động cuộn xuống phần đánh giá sau khi load xong UI
      setTimeout(() => {
        if (reviewsContainer) {
          reviewsContainer.scrollIntoView({
            behavior: "smooth",
            block: "start",
          });
        }
      }, 500);
    } else {
      // Mặc định: Chuyển về tab Tin đăng
      switchToMainTab("#listings-tab-content");
      syncTabUI(); // Đồng bộ các tab con (Active, Sold...)
    }
    // -----------------------------------------------------

    // 5. Tải dữ liệu song song
    await Promise.all([
      fetchUserInfo(),
      fetchReviewStats(),
      fetchAllListings(), // Tải tin đăng
      loadReviews(0), // Tải đánh giá
    ]);

    // 6. Kết nối WebSocket nếu đã đăng nhập
    if (CURRENT_USER_ID) {
      connectWebSocket();
    }

    // 7. Highlight tin đăng nếu có param listing_id (cho trường hợp thông báo duyệt tin)
    highlightListingFromUrl();
  }

  // =============================================
  // ✅ SỬA LỖI 2: Bổ sung CSS cho Chat Modal (fix lỗi hiển thị)
  // =============================================
  (function injectChatStyles() {
    const styles = `
        .msg-bubble {
            max-width: 75%;
            padding: 8px 12px;
            border-radius: 12px;
            word-break: break-word; /* Để xuống dòng */
        }
        .conv-item {
            display: flex;
            flex-direction: column;
            gap: 8px;
            margin-bottom: 6px;
        }
        /* Tin nhắn của người dùng (USER/SELLER) - BÊN TRÁI */
        .msg-user {
            background-color: #f3f4f6; /* bg-gray-100 */
            color: #1f2937; /* text-gray-800 */
            align-self: flex-start;
        }
        /* Tin nhắn của ADMIN - BÊN PHẢI */
        .msg-admin {
            background-color: #d1fae5; /* bg-green-100 */
            color: #065f46; /* text-green-800 */
            align-self: flex-end;
        }
        /* --- THÊM MỚI: FIX Z-INDEX CHO MODAL --- */
        /* Đẩy Backdrop lên trên Header (Header thường z-index: 1000) */
        .modal-backdrop {
            z-index: 2000 !important; 
        }
        /* Đẩy Modal lên trên Backdrop */
        .modal {
            z-index: 2001 !important;
        }
        /* Đảm bảo Modal Khiếu nại cũng nằm trên cùng */
        #complaintModal {
            z-index: 2002 !important;
        }
            /* --- THÊM MỚI: ĐẨY SWEETALERT2 LÊN CAO NHẤT --- */
        /* Class này bao bọc toàn bộ popup của thư viện Swal */
        .swal2-container {
            z-index: 9999 !important; 
        }
    `;
    const styleSheet = document.createElement("style");
    styleSheet.type = "text/css";
    styleSheet.innerText = styles;
    document.head.appendChild(styleSheet);
  })();
  // =============================================
  // ✅ KẾT THÚC SỬA LỖI 2
  // =============================================

  function getAuthInfo() {
    const token = localStorage.getItem("token");
    const userString = localStorage.getItem("user");
    if (!token || !userString) return null;
    try {
      const user = JSON.parse(userString);
      const userId = user.id || user.userId;
      if (!userId) {
        console.warn("User object không có ID", user);
        return null;
      }
      return {
        token: token,
        user: user,
        userId: Number(userId),
      };
    } catch (e) {
      console.error("Lỗi parse user data, đang xóa localStorage:", e);
      if (typeof logout === "function") logout();
      else {
        localStorage.clear();
        window.location.href = "/login.html";
      }
      return null;
    }
  }

  function setupUIBasedOnOwnership() {
    const ownerOnlyTabs = document.querySelectorAll(".owner-only");
    if (IS_OWNER) {
      ownerOnlyTabs.forEach((tab) => tab.classList.remove("hidden"));
    } else {
      ownerOnlyTabs.forEach((tab) => tab.classList.add("hidden"));
    }
  }

  function syncTabUI() {
    const targetPaneId = `#${currentListingTab.toLowerCase()}-listings`;
    listingSubTabs
      .querySelectorAll(".tab-button")
      .forEach((btn) => btn.classList.remove("active"));
    Object.values(listingTabPanes).forEach((pane) =>
      pane.classList.remove("active")
    );
    const buttonToActivate = listingSubTabs.querySelector(
      `.tab-button[data-tab-target="${targetPaneId}"]`
    );
    if (buttonToActivate) buttonToActivate.classList.add("active");
    if (listingTabPanes[currentListingTab]) {
      listingTabPanes[currentListingTab].classList.add("active");
    }
  }

  function setupTabListeners() {
    // Tab chính (Tin đăng / Đánh giá)
    mainTabs.addEventListener("click", (e) => {
      const button = e.target.closest(".tab-button");
      if (!button) return;
      switchToMainTab(button.dataset.tabTarget);
    });

    // Sub-tab (Tin đăng)
    listingSubTabs.addEventListener("click", (e) => {
      const button = e.target.closest(".tab-button");
      if (!button) return;
      listingSubTabs
        .querySelectorAll(".tab-button")
        .forEach((btn) => btn.classList.remove("active"));
      button.classList.add("active");
      Object.values(listingTabPanes).forEach((pane) =>
        pane.classList.remove("active")
      );
      const targetPaneId = button.dataset.tabTarget.replace("#", "");
      document.getElementById(targetPaneId).classList.add("active");
      currentListingTab = targetPaneId.split("-")[0].toUpperCase();
      currentListingPage = 0;
      renderListingsPane();
    });

    // Sub-tab (Bộ lọc Đánh giá) (MỚI)
    reviewFilterTabs.addEventListener("click", (e) => {
      const button = e.target.closest(".tab-button");
      if (!button) return;

      reviewFilterTabs
        .querySelectorAll(".tab-button")
        .forEach((btn) => btn.classList.remove("active"));
      button.classList.add("active");

      const role = button.dataset.filterRole;
      currentReviewRole = role === "ALL" ? null : role;
      currentReviewPage = 0;
      loadReviews(0);
    });

    // Click vào tổng số review trên header
    totalReviewsEl.addEventListener("click", () => {
      // 1. Chuyển sang tab Đánh giá
      switchToMainTab("#reviews-tab-content");

      // 2. Reset về "Tất cả"
      reviewFilterTabs
        .querySelectorAll(".tab-button")
        .forEach((btn) => btn.classList.remove("active"));
      reviewFilterTabs
        .querySelector('[data-filter-role="ALL"]')
        .classList.add("active");

      currentReviewRole = null;
      currentReviewPage = 0;
      loadReviews(0);

      // 3. Cuộn xuống
      reviewsContainer.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }

  function switchToMainTab(targetSelector) {
    mainTabs
      .querySelectorAll(".tab-button")
      .forEach((btn) => btn.classList.remove("active"));
    mainTabContents.forEach((content) => content.classList.remove("active"));

    const activeBtn = Array.from(mainTabs.querySelectorAll(".tab-button")).find(
      (btn) => btn.dataset.tabTarget === targetSelector
    );
    if (activeBtn) activeBtn.classList.add("active");

    document.querySelector(targetSelector).classList.add("active");
  }

  // =============================================
  // HÀM TẢI DỮ LIỆU (CALL API)
  // =============================================

  async function callApi(serviceUrl, endpoint, method = "GET", body = null) {
    const options = {
      method,
      headers: {
        ...(AUTH_TOKEN && { Authorization: `Bearer ${AUTH_TOKEN}` }),
      },
    };
    if (body) {
      options.body = JSON.stringify(body);
      options.headers["Content-Type"] = "application/json";
    }
    const response = await fetch(`${serviceUrl}${endpoint}`, options);
    if (response.status === 401 || response.status === 403) {
      Toast.fire({
        icon: "error",
        title: "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.",
      });
      if (typeof logout === "function") logout();
      else window.location.href = "/login.html";
      throw new Error("Unauthorized");
    }
    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: `Lỗi HTTP: ${response.statusText}` }));
      throw new Error(
        errorData.message || `Lỗi không xác định (${response.status})`
      );
    }
    if (response.status === 204) return null;
    return response.json();
  }

  async function fetchUserInfo() {
    try {
      const user = await callApi(USER_SERVICE_URL, `/user/${PROFILE_USER_ID}`);
      userNameEl.textContent = user.name || "[Chưa có tên]";
      userEmailEl.innerHTML = `<i class="fas fa-envelope mr-2 text-gray-400"></i>${
        user.email || "Chưa cập nhật email"
      }`;
      userAddressEl.innerHTML = `<i class="fas fa-map-marker-alt mr-2 text-gray-400"></i>${
        user.address || "Chưa cập nhật địa chỉ"
      }`;
      userPhoneEl.innerHTML = `<i class="fas fa-phone-alt mr-2 text-gray-400"></i>${
        user.phone || "Chưa cập nhật SĐT"
      }`;

      // SỬA ĐỔI: Sử dụng đúng ID "start-chat-btn" từ file HTML
      const chatWithUserBtn = document.getElementById("start-chat-btn");

      if (chatWithUserBtn) {
        if (IS_OWNER) {
          // Nếu đây là trang của chính mình, ẩn nút chat
          chatWithUserBtn.classList.add("hidden");
        } else {
          // Nếu là trang của người khác, hiển thị và gán sự kiện
          chatWithUserBtn.classList.remove("hidden");

          chatWithUserBtn.onclick = () => {
            // 1. Kiểm tra người dùng đã đăng nhập chưa
            if (!CURRENT_USER_ID) {
              Toast.fire({
                icon: "error",
                title: "Vui lòng đăng nhập để chat!",
              });
              setTimeout(() => (window.location.href = "/login.html"), 1500);
              return;
            }

            // 2. Kiểm tra tự chat
            if (PROFILE_USER_ID == CURRENT_USER_ID) {
              Toast.fire({
                icon: "warning",
                title: "Bạn không thể chat với chính mình!",
              });
              return;
            }

            // 3. Chuẩn bị params (KHÔNG gửi thông tin sản phẩm)
            const params = new URLSearchParams({
              to: user.id, // ID của người chủ profile
              name: user.name, // Tên của người chủ profile
            });

            // 4. Chuyển hướng sang trang chat
            window.location.href = `/chat.html?${params.toString()}`;
          };
        }
      }
      // =============================================
      // >>> [KẾT THÚC CODE MỚI] <<<
      // =============================================
    } catch (e) {
      userNameEl.textContent = "Không thể tải thông tin";
      console.error("Lỗi fetchUserInfo:", e);
    }
  }

  async function fetchReviewStats() {
    try {
      const stats = await callApi(
        REVIEW_SERVICE_URL,
        `/reviews/user/${PROFILE_USER_ID}/stats`
      );
      // Thay emoji bằng thẻ <i> fontawesome, dùng .innerHTML thay vì .textContent
      avgRatingEl.innerHTML = `<i class="fas fa-star text-yellow-400 mr-1"></i> ${stats.averageRating.toFixed(
        1
      )} / 5`;
      totalReviewsEl.textContent = `${stats.totalReviews} đánh giá`;
    } catch (e) {
      avgRatingEl.innerHTML = `<i class="fas fa-star text-gray-300 mr-1"></i> Chưa có`;
      totalReviewsEl.textContent = "0 đánh giá";
      console.error("Lỗi fetchReviewStats:", e);
    }
  }

  async function fetchAllListings() {
    try {
      const data = await callApi(
        LISTING_SERVICE_URL,
        `/listings/user/${PROFILE_USER_ID}?page=0&size=100`
      );
      allListings = data.content;

      // Tính toán thống kê
      const activeListingsCount = allListings.filter(
        (l) => l.listingStatus === "ACTIVE"
      ).length;
      const soldListingsCount = allListings.filter(
        (l) => l.listingStatus === "SOLD"
      ).length;

      // Cập nhật UI thống kê
      activeCountEl.textContent = activeListingsCount;
      soldCountEl.textContent = soldListingsCount;

      listingsByStatus = {
        PENDING: allListings.filter((l) => l.listingStatus === "PENDING"),
        ACTIVE: allListings.filter((l) => l.listingStatus === "ACTIVE"),
        REJECTED: allListings.filter((l) => l.listingStatus === "REJECTED"),
        SOLD: allListings.filter((l) => l.listingStatus === "SOLD"),
      };
      currentListingPage = 0;
      renderListingsPane();
    } catch (error) {
      if (error.message !== "Unauthorized") {
        listingTabPanes.ACTIVE.innerHTML = `<p class='text-center text-red-500'>Lỗi khi tải tin đăng: ${error.message}</p>`;
      }
    }
  }

  /**
   * Tải và render tab "Đánh giá" với bộ lọc (MỚI)
   */
  async function loadReviews(page) {
    try {
      reviewsContainer.innerHTML = "<p>Đang tải đánh giá...</p>";
      // Xây dựng URL với role nếu có
      let url = `${REVIEW_SERVICE_URL}/reviews/user/${PROFILE_USER_ID}?page=${page}&size=${REVIEW_PAGE_SIZE}`;
      if (currentReviewRole) {
        url += `&role=${currentReviewRole}`;
      }

      const pageData = await callApi(
        REVIEW_SERVICE_URL,
        url.replace(REVIEW_SERVICE_URL, "")
      );

      currentReviewPage = pageData.number;

      renderReviewsPane(
        reviewsContainer,
        paginationReviews,
        pageData,
        loadReviews
      );
    } catch (e) {
      reviewsContainer.innerHTML = `<p class="text-gray-500">Không tải được đánh giá.</p>`;
    }
  }

  // =============================================
  // HÀM RENDER (HIỂN THỊ)
  // =============================================

  function renderListingsPane() {
    const listingsToShow = listingsByStatus[currentListingTab];
    const container = listingTabPanes[currentListingTab];
    if (!listingsToShow || listingsToShow.length === 0) {
      container.innerHTML =
        "<p class='text-center text-gray-500'>Không có tin đăng nào.</p>";
      paginationContainer.innerHTML = "";
      return;
    }
    totalPages = Math.ceil(listingsToShow.length / PAGE_SIZE);
    const startIndex = currentListingPage * PAGE_SIZE;
    const endIndex = startIndex + PAGE_SIZE;
    const paginatedListings = listingsToShow.slice(startIndex, endIndex);
    container.innerHTML = paginatedListings
      .map(createListingCardHTML)
      .filter((html) => html)
      .join("");

    // Gọi hàm renderPagination chung
    renderPagination(
      paginationContainer,
      currentListingPage,
      totalPages,
      (newPage) => {
        currentListingPage = newPage;
        renderListingsPane();
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    );
  }

  const createListingCardHTML = (listing) => {
    if (!listing.product) return ``;
    const {
      product,
      updatedOnce,
      listingStatus,
      listingId,
      adminNotes,
      updatedAt,
      verified,
    } = listing;
    const createdAt =
      product.createdAt || listing.listingDate || product.created_at;
    const createdDate = parseDate(createdAt);
    const updatedDate = parseDate(updatedAt);
    const imageUrl =
      product.images && product.images.length > 0
        ? `${BACKEND_ORIGIN}${product.images[0].imageUrl}`
        : "https://placehold.co/300x200/e2e8f0/4a5568?text=No+Image";

    const canEdit = IS_OWNER && !updatedOnce && listingStatus !== "SOLD";

    // Điều kiện hiển thị nút xóa: Phải là chủ sở hữu VÀ tin chưa bán (SOLD)
    const canDelete = IS_OWNER && listingStatus !== "SOLD";

    const reasonHtml =
      listingStatus === "REJECTED" && adminNotes
        ? `<p class="text-xs text-red-700 mt-2 cursor-pointer font-semibold action-btn" data-action="view-reason" data-reason="${String(
            adminNotes
          ).replace(/"/g, "&quot;")}">
          <i class="fas fa-info-circle"></i> Xem lý do từ chối
        </p>`
        : "";
    const verifiedBadge = verified
      ? `<span class="px-2 text-xs inline-flex items-center leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
        <i class="fas fa-check-circle" style="font-size: 10px; margin-right: 4px;"></i>Đã Kiểm Định
      </span>`
      : "";
    if (IS_OWNER) {
      // nút Xem luôn có, nút Khiếu nại chỉ show khi SOLD
      const viewBtn = `<button data-action="view" data-id="${listingId}" class="action-btn border border-[#178A48] text-[#178A48] px-3 py-1 text-sm rounded-md hover:bg-[#178A48] hover:text-white font-medium transition-all duration-200">Xem</button>`;

      const complaintButton =
        listingStatus === "SOLD"
          ? `<button data-action="complain" data-id="${listingId}" class="action-btn bg-[#13723a] text-white px-3 py-1 text-sm rounded-md hover:opacity-90 font-medium transition-all duration-200">Khiếu nại</button>`
          : "";

      const editBtn = canEdit
        ? `<button data-action="edit" data-id="${listingId}" class="action-btn bg-orange-500 text-white px-3 py-1 text-sm rounded-md hover:bg-orange-600 font-medium transition-all duration-200">Sửa</button>`
        : "";

      const deleteBtn = canDelete
        ? `<button data-action="delete" data-id="${listingId}" class="action-btn bg-red-600 text-white px-3 py-1 text-sm rounded-md hover:bg-red-700 font-medium transition-all duration-200">Xóa</button>`
        : "";

      // order: Xem | Khiếu nại | Sửa | Xóa
      actionButtonsHTML = `${viewBtn} ${complaintButton} ${editBtn} ${deleteBtn}`;
    } else {
      actionButtonsHTML = `
        <button data-action="view" data-id="${listingId}" class="action-btn border border-[#178A48] text-[#178A48] px-3 py-1 text-sm rounded-md hover:bg-[#178A48] hover:text-white font-medium transition-all duration-200">Xem Chi Tiết</button>
      `;
    }
    return `
    <div class="listing-card bg-white rounded-lg shadow-md overflow-hidden flex animate__animated animate__fadeInUp" data-listing-id="${listingId}">
      <img src="${imageUrl}" alt="${
      product.productName || "N/A"
    }" class="w-48 h-auto object-cover hidden sm:block">
      <div class="p-4 flex flex-col justify-between flex-grow">
        <div>
          <div class="flex justify-between items-start">
            <h3 class="text-lg font-bold text-gray-800">
              <a href="/product_detail.html?id=${
                product.productId
              }" target="_blank" title="Xem chi tiết sản phẩm">
                ${product.productName || "[Không có tiêu đề]"}
              </a>
            </h3>
            <div class="flex items-center space-x-2 flex-shrink-0">
              ${verifiedBadge} 
              ${getStatusBadge(listingStatus)}
            </div>
          </div>
          <p class="text-red-600 font-semibold mt-1">${formatPrice(
            product.price
          )}</p>
          <p class="text-sm text-gray-500 mt-1">
            Ngày cập nhật: ${
              updatedDate ? updatedDate.toLocaleDateString("vi-VN") : "N/A"
            }
          </p>
           <p class="text-sm text-gray-500 mt-1">
            Ngày đăng tin: ${
              createdDate ? createdDate.toLocaleDateString("vi-VN") : "N/A"
            }
          </p>
          ${reasonHtml}
        </div>
        <div class="flex justify-end space-x-2 mt-4">
          ${actionButtonsHTML}
        </div>
      </div>
    </div>`;
  };

  /**
   * Render khung Đánh giá
   */
  async function renderReviewsPane(
    container,
    paginationContainer,
    pageData,
    pageChangeHandler
  ) {
    if (pageData.empty) {
      container.innerHTML = `<p class="text-gray-500">Không có đánh giá nào.</p>`;
      paginationContainer.innerHTML = "";
      return;
    }

    // Tải thông tin (ảnh) cho các review
    const reviewsWithInfo = await Promise.all(
      pageData.content.map(async (reviewDTO) => {
        const productInfo = await getProductInfo(
          reviewDTO.productId,
          reviewDTO.productName
        );
        const reviewerLink = `/edit_news.html?id=${reviewDTO.reviewerId}`;

        return {
          review: reviewDTO,
          productInfo,
          reviewerLink,
        };
      })
    );

    // Render HTML
    container.innerHTML = reviewsWithInfo
      .map((item) => createReviewCardHTML(item))
      .join("");

    // Render pagination
    renderPagination(
      paginationContainer,
      pageData.number,
      pageData.totalPages,
      pageChangeHandler
    );
  }

  /**
   * Tạo HTML cho 1 thẻ đánh giá
   */
  function createReviewCardHTML({ review, productInfo, reviewerLink }) {
    // --- SỬA ĐỔI BẮT ĐẦU: Logic tạo sao icon ---
    let starsHtml = "";
    for (let i = 1; i <= 5; i++) {
      if (i <= review.rating) {
        // Sao vàng (đầy)
        starsHtml += '<i class="fas fa-star text-yellow-400 text-sm"></i>';
      } else {
        // Sao xám (rỗng) - Kiểu Shopee
        starsHtml += '<i class="fas fa-star text-gray-300 text-sm"></i>';
      }
    }
    // --- SỬA ĐỔI KẾT THÚC ---

    const productLink = review.productId
      ? `/product_detail.html?id=${review.productId}`
      : "#";
    const imageUrl = productInfo.imageUrl
      ? `${BACKEND_ORIGIN}${productInfo.imageUrl}`
      : "https://placehold.co/300x300/e2e8f0/4a5568?text=No+Image";
    const reviewerName = review.reviewerName || `User #${review.reviewerId}`;

    const reviewerNameHtml = `
      <h4 class="font-semibold text-gray-800">
        Đánh giá từ 
        <a href="${reviewerLink}" target="_blank" class="text-blue-600 hover:underline">${reviewerName}</a>
      </h4>`;

    let reviewDateStr = "";
    if (review.createdAt) {
      const dateRaw = review.createdAt.endsWith("Z")
        ? review.createdAt
        : review.createdAt + "Z";
      reviewDateStr = new Date(dateRaw).toLocaleString("vi-VN");
    }

    return `
    <div class="review-card flex flex-col sm:flex-row gap-4">
        <div class="flex-shrink-0 sm:w-1/3 p-3 bg-gray-50 rounded-lg border flex gap-3">
             <a href="${productLink}" target="_blank">
                <img src="${imageUrl}" alt="${
      productInfo.productName
    }" class="w-20 h-20 object-cover rounded-md">
             </a>
             <div class="flex-grow">
                <p class="text-sm text-gray-500">Giao dịch cho:</p>
                <a href="${productLink}" target="_blank" class="font-semibold text-gray-800 hover:underline">
                    ${review.productName}
                </a>
                <p class="text-red-600 font-bold text-sm mt-1">${formatPrice(
                  review.price
                )}</p>
             </div>
        </div>
        
        <div class="flex-grow">
            <div class="flex justify-between items-center mb-2">
                ${reviewerNameHtml}
                <div class="flex items-center space-x-1">
                    ${starsHtml}
                </div>
            </div>
            <p class="text-gray-700 italic">"${
              review.comment || "Không có bình luận."
            }"</p>
            <small class="text-gray-400 mt-2 block">${new Date(
              review.createdAt
            ).toLocaleString("vi-VN")}</small>
        </div>
    </div>
    `;
  }

  /**
   * Hàm helper: Lấy thông tin sản phẩm (ảnh)
   */
  async function getProductInfo(productId, fallbackName) {
    if (!productId) {
      return { productName: fallbackName || "Sản phẩm", imageUrl: null };
    }
    if (productCache.has(productId)) {
      return productCache.get(productId);
    }
    try {
      const product = await callApi(
        LISTING_SERVICE_URL,
        `/products/${productId}`
      );
      const imageUrl =
        product.images && product.images.length > 0
          ? product.images[0].imageUrl
          : null;
      const productInfo = {
        productName: fallbackName,
        imageUrl: imageUrl,
      };
      productCache.set(productId, productInfo);
      return productInfo;
    } catch (error) {
      return { productName: fallbackName, imageUrl: null };
    }
  }

  /**
   * Render lại các nút phân trang
   */
  const renderPagination = (
    container, // Container để render
    currentPage, // Trang hiện tại (từ 0)
    totalPages, // Tổng số trang
    pageChangeHandler // Hàm callback khi click
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

    // Nút số
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

  const updateListingCard = (listingData) => {
    const index = allListings.findIndex(
      (l) => l.listingId == listingData.listingId
    );
    if (index > -1) {
      allListings[index] = listingData;
    } else {
      allListings.unshift(listingData);
    }
    listingsByStatus = {
      PENDING: allListings.filter((l) => l.listingStatus === "PENDING"),
      ACTIVE: allListings.filter((l) => l.listingStatus === "ACTIVE"),
      REJECTED: allListings.filter((l) => l.listingStatus === "REJECTED"),
      SOLD: allListings.filter((l) => l.listingStatus === "SOLD"),
    };
    renderListingsPane();
    const updatedCard = document.querySelector(
      `[data-listing-id="${listingData.listingId}"]`
    );
    if (updatedCard) {
      updatedCard.classList.add("bg-teal-100", "transition", "duration-500");
      setTimeout(() => {
        if (updatedCard) updatedCard.classList.remove("bg-teal-100");
      }, 1000);
    }
  };

  const highlightListingFromUrl = () => {
    const urlParams = new URLSearchParams(window.location.search);
    const highlightListingId = urlParams.get("listing_id");
    if (highlightListingId) {
      setTimeout(() => {
        const cardToHighlight = document.querySelector(
          `[data-listing-id="${highlightListingId}"]`
        );
        if (cardToHighlight) {
          cardToHighlight.classList.add("listing-highlight");
          requestAnimationFrame(() => {
            cardToHighlight.scrollIntoView({
              behavior: "smooth",
              block: "center",
            });
          });
          setTimeout(() => {
            if (cardToHighlight)
              cardToHighlight.classList.remove("listing-highlight");
          }, 2500);
        }
        const url = new URL(window.location);
        url.searchParams.delete("listing_id");
        url.searchParams.delete("tab");
        window.history.replaceState({}, "", url);
      }, 150);
    }
  };

  // =============================================
  // LOGIC MODAL (PHẦN LỚN TỪ CODE CŨ)
  // =============================================

  const closeModal = (modal) => modal.classList.add("hidden");

  const openViewDetailsModal = (listing) => {
    const { product, phone, location, listingStatus, listingDate, verified } =
      listing;
    const spec = product.specification;
    document.getElementById("viewTitle").textContent = product.productName;
    document.getElementById("viewPrice").textContent = formatPrice(
      product.price
    );
    document.getElementById("viewProductType").textContent =
      productTypeMap[product.productType] || "Không xác định";
    document.getElementById("viewBrand").textContent = spec.brand || "N/A";
    document.getElementById("viewLocation").textContent = location;
    document.getElementById("viewPhone").textContent = phone;
    document.getElementById("viewDate").textContent = new Date(
      listingDate
    ).toLocaleDateString("vi-VN");
    const verifiedBadge = verified
      ? `<span class="px-3 py-1 text-sm inline-flex items-center leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
           <i class="fas fa-check-circle" style="font-size: 12px; margin-right: 6px;"></i>Đã Kiểm Định
         </span>`
      : "";
    document.getElementById("viewStatus").innerHTML = `${getStatusBadge(
      listingStatus,
      true
    )} ${verifiedBadge}`;
    document.getElementById("viewDescription").textContent =
      product.description;
    const viewImagesContainer = document.getElementById("viewImages");
    viewImagesContainer.innerHTML = "";
    if (product.images && product.images.length > 0) {
      product.images.forEach((img) => {
        viewImagesContainer.innerHTML += `<div class="aspect-square overflow-hidden rounded-md"><img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover"></div>`;
      });
    } else {
      viewImagesContainer.innerHTML =
        '<p class="text-sm text-gray-500 col-span-full">Không có hình ảnh.</p>';
    }
    const viewSpecsContainer = document.getElementById("viewSpecs");
    const createSpecItem = (label, value) =>
      value
        ? `<div><strong class="text-gray-600">${label}:</strong> <span>${value}</span></div>`
        : "";
    let specsHTML = [
      createSpecItem("Bảo hành", spec.warrantyPolicy),
      createSpecItem("Loại Pin", spec.batteryType),
      createSpecItem("Thời gian sạc", spec.chargeTime),
      createSpecItem("Số lần sạc", spec.chargeCycles),
    ].join("");
    if (product.productType !== "battery") {
      specsHTML += [
        createSpecItem(
          "Quãng đường / 1 lần sạc ",
          spec.rangePerCharge ? `${spec.rangePerCharge} km` : null
        ),
        createSpecItem(
          "Số km đã đi",
          spec.mileage ? `${spec.mileage} km` : null
        ),
        createSpecItem("Dung lượng pin", spec.batteryCapacity),
        createSpecItem("Màu sắc", spec.color),
        createSpecItem(
          "Tốc độ tối đa",
          spec.maxSpeed ? `${spec.maxSpeed} km/h` : null
        ),
      ].join("");
    } else {
      specsHTML += [
        createSpecItem("Dung lượng", spec.batteryCapacity),
        createSpecItem("Thời gian đã dùng", spec.batteryLifespan),
        createSpecItem("Tương thích Xe", spec.compatibleVehicle),
      ].join("");
    }
    viewSpecsContainer.innerHTML = specsHTML;
    viewDetailsModal.classList.remove("hidden");
  };

  const openEditModal = (listing) => {
    currentEditingListing = listing;
    const { product, phone, location, listingStatus } = listing;
    const spec = product.specification;
    editForm.reset();
    newFilesDataTransfer.items.clear();
    newImagesPreview.innerHTML = "";

    // [CẬP NHẬT] Lưu bản sao SÂU của mảng ảnh gốc
    originalImagesCopy = JSON.parse(JSON.stringify(listing.product.images));

    imagesToDelete = [];

    document.getElementById("editListingId").value = listing.listingId;
    document.getElementById("editProductType").value =
      productTypeMap[product.productType] || "Không xác định";
    document.getElementById("editProductName").value =
      product.productName || "";
    document.getElementById("editBrand").value = spec.brand || "";
    document.getElementById("editPrice").value = product.price || "";
    document.getElementById("editDescription").value =
      product.description || "";
    document.getElementById("editPhone").value = phone || "";
    document.getElementById("editLocation").value = location || "";
    document.getElementById("editWarranty").value = spec.warrantyPolicy || "";
    renderCurrentImages(product.images);
    renderSpecificFields(product.productType, spec);
    const allFields = editForm.querySelectorAll("input, textarea, select");
    if (listingStatus === "ACTIVE") {
      const nonEditableFieldIDs = [
        "editProductType",
        "editProductName",
        "editBrand",
      ];
      allFields.forEach((field) => {
        let isSpecField = field.id.startsWith("edit_");
        if (
          field.type !== "hidden" &&
          (nonEditableFieldIDs.includes(field.id) || isSpecField)
        ) {
          field.disabled = true;
        } else {
          field.disabled = false;
        }
      });
    } else {
      allFields.forEach((field) => {
        if (field.type !== "hidden") field.disabled = false;
      });
    }
    document.getElementById("editProductType").disabled = true;
    editModal.classList.remove("hidden");
  };

  const renderCurrentImages = (images) => {
    currentImagesPreview.innerHTML = "";
    if (images && images.length > 0) {
      images.forEach((img) => {
        const imgContainer = document.createElement("div");
        imgContainer.className =
          "image-container relative aspect-square overflow-hidden rounded-md";
        imgContainer.innerHTML = `
                <img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover">
                <div class="delete-img-btn" data-image-id="${img.imageId}">×</div>
            `;
        currentImagesPreview.appendChild(imgContainer);
      });
    } else {
      currentImagesPreview.innerHTML = `<p class="text-sm text-gray-500 col-span-full">Chưa có ảnh nào.</p>`;
    }
  };

  const renderSpecificFields = (type, spec) => {
    const container = document.getElementById("editSpecificFields");
    const createInput = (label, name, value, type = "text") =>
      `<div><label for="edit_${name}" class="block text-sm font-medium text-gray-700">${label}</label><input type="${type}" id="edit_${name}" name="${name}" value="${
        value || ""
      }" class="mt-1 block w-full form-input border-gray-300 rounded-md shadow-sm"></div>`;

    let fieldsHTML = [
      createInput("Loại Pin", "batteryType", spec.batteryType),
      createInput("Thời Gian Sạc", "chargeTime", spec.chargeTime),
      createInput("Số Lần Sạc", "chargeCycles", spec.chargeCycles, "number"),
    ].join(""); // Đã có join, OK

    if (type !== "battery") {
      // Đoạn này trong file gốc của bạn CÓ join(""), nên hiển thị đúng
      fieldsHTML += [
        createInput(
          "Quãng Đường / 1 Lần Sạc (Km) ",
          "rangePerCharge",
          spec.rangePerCharge,
          "number"
        ),
        createInput("Số Km Đã Đi", "mileage", spec.mileage, "number"),
        createInput("Dung Lượng Pin", "batteryCapacity", spec.batteryCapacity),
        createInput("Màu Sắc", "color", spec.color),
        createInput(
          "Tốc Độ Tối Đa (Km/h)",
          "maxSpeed",
          spec.maxSpeed,
          "number"
        ),
      ].join("");
    } else {
      // ĐOẠN CẦN SỬA: Thêm .join("") vào cuối mảng
      fieldsHTML += [
        createInput("Dung lượng", "batteryCapacity", spec.batteryCapacity),
        createInput(
          "Thời Gian Đã Dùng",
          "batteryLifespan",
          spec.batteryLifespan
        ),
        createInput(
          "Tương Thích Xe",
          "compatibleVehicle",
          spec.compatibleVehicle
        ),
      ].join(""); // <--- THÊM VÀO ĐÂY
    }
    container.innerHTML = fieldsHTML;
  };

  const handleFileSelect = (e) => {
    const incomingFiles = Array.from(e.target.files);
    if (incomingFiles.length === 0) return;

    // --- [LOGIC MỚI] Tính toán tổng số lượng ảnh (Cũ + Đang chờ upload + Vừa chọn) ---

    // 1. Đếm ảnh cũ đang hiển thị
    const currentImagesCount =
      currentImagesPreview.querySelectorAll(".image-container").length;

    // 2. Đếm ảnh mới đã chọn trước đó
    const pendingNewImagesCount = newFilesDataTransfer.files.length;

    // 3. Số ảnh vừa chọn thêm
    const incomingCount = incomingFiles.length;

    const totalExpected =
      currentImagesCount + pendingNewImagesCount + incomingCount;

    if (totalExpected > MAX_IMAGE_COUNT) {
      Swal.fire({
        icon: "warning",
        title: "Quá giới hạn ảnh",
        text: `Tối đa ${MAX_IMAGE_COUNT} ảnh. Bạn đang có ${currentImagesCount} ảnh cũ + ${pendingNewImagesCount} ảnh mới chờ upload. Không thể thêm ${incomingCount} ảnh nữa.`,
        confirmButtonText: "Đã hiểu",
        ...swalTheme,
      });
      // Reset input file
      e.target.value = "";
      return;
    }
    // -------------------------------------------------------------------------------

    incomingFiles.forEach((file) => newFilesDataTransfer.items.add(file));
    e.target.files = newFilesDataTransfer.files;
    renderNewImagesPreview();
  };

  const renderNewImagesPreview = () => {
    newImagesPreview.innerHTML = "";
    Array.from(newFilesDataTransfer.files).forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const imgContainer = document.createElement("div");
        imgContainer.className =
          "image-container relative aspect-square overflow-hidden rounded-md";
        imgContainer.innerHTML = `
                <img src="${e.target.result}" class="w-full h-full object-cover">
                <div class="delete-img-btn" data-file-index="${index}">×</div>
            `;
        newImagesPreview.appendChild(imgContainer);
      };
      reader.readAsDataURL(file);
    });
  };

  const handleDeleteNewImage = (e) => {
    const deleteButton = e.target.closest(".delete-img-btn[data-file-index]");
    if (!deleteButton) return;
    const indexToRemove = parseInt(deleteButton.dataset.fileIndex, 10);
    newFilesDataTransfer.items.remove(indexToRemove);
    editImageUpload.files = newFilesDataTransfer.files;
    renderNewImagesPreview();
  };

  const handleDeleteExistingImage = async (e) => {
    const deleteButton = e.target.closest(".delete-img-btn[data-image-id]");
    if (!deleteButton) return;
    const imageId = deleteButton.dataset.imageId;

    if (!imageId) return;

    // 1. Đánh dấu ảnh này cần được xóa khi submit
    imagesToDelete.push(imageId);

    // 2. Xóa khỏi UI và khỏi currentEditingListing.product.images (Cần thiết cho việc Check Limit)
    deleteButton.parentElement.remove();
    currentEditingListing.product.images =
      currentEditingListing.product.images.filter(
        (img) => img.imageId != imageId
      );

    // 3. Cập nhật UI nếu không còn ảnh cũ
    if (currentImagesPreview.children.length === 0) {
      currentImagesPreview.innerHTML = `<p class="text-sm text-gray-500 col-span-full">Chưa có ảnh nào.</p>`;
    }

    // 4. Hiển thị thông báo Toast
    Toast.fire({
      icon: "success",
      title: "Ảnh đã được đánh dấu xóa (sẽ xóa khi Lưu).",
    });
  };
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    const submitButton = editForm.querySelector('button[type="submit"]');

    // --- KIỂM TRA SỐ LƯỢNG ẢNH ---
    const currentImagesCount =
      currentImagesPreview.querySelectorAll(".image-container").length;
    const newImagesCount = newFilesDataTransfer.files.length;
    const totalImages = currentImagesCount + newImagesCount;
    const originalButtonText = submitButton.querySelector("span").textContent;
    const listingId = document.getElementById("editListingId").value;

    // Check 1: Phải có ít nhất 1 ảnh
    if (totalImages === 0) {
      Swal.fire({
        title: "Thiếu hình ảnh",
        text: "Bạn phải có ít nhất một hình ảnh (ảnh cũ hoặc ảnh mới) để cập nhật tin đăng.",
        icon: "error",
        ...swalTheme,
        ...modalAnimation,
        confirmButtonColor: "#f97316",
      });
      return;
    }

    // Check 2: Không được quá 8 ảnh (MAX_IMAGE_COUNT đã được khai báo ở đầu file)
    if (totalImages > MAX_IMAGE_COUNT) {
      Swal.fire({
        title: "Quá nhiều hình ảnh",
        text: `Bạn chỉ được phép có tối đa ${MAX_IMAGE_COUNT} ảnh. Hiện tại tổng cộng là ${totalImages} ảnh (Cũ: ${currentImagesCount}, Mới: ${newImagesCount}). Vui lòng xóa bớt.`,
        icon: "error",
        ...swalTheme,
        ...modalAnimation,
        confirmButtonColor: "#dc2626",
      });
      return;
    }
    // ---------------------------------------------------------

    submitButton.disabled = true;
    submitButton.querySelector("span").textContent = "Đang xử lý...";

    try {
      // =======================================================================
      // BƯỚC 1: XÓA CÁC ẢNH CŨ ĐÃ ĐÁNH DẤU (imagesToDelete)
      // Hành động này chỉ xảy ra khi người dùng bấm LƯU.
      // =======================================================================
      if (imagesToDelete.length > 0) {
        submitButton.querySelector(
          "span"
        ).textContent = `Đang xóa ${imagesToDelete.length} ảnh cũ...`;
        const deletePromises = imagesToDelete.map((imageId) =>
          callApi(
            LISTING_SERVICE_URL,
            `/listings/${listingId}/delete-image/${imageId}`,
            "POST" // Endpoint deleteImageFromListing
          )
        );
        await Promise.all(deletePromises);
      }
      // =======================================================================

      // BƯỚC 2: TẢI LÊN CÁC ẢNH MỚI
      const imageFiles = newFilesDataTransfer.files;
      if (imageFiles && imageFiles.length > 0) {
        submitButton.querySelector("span").textContent = "Đang thêm ảnh...";
        const imageFormData = new FormData();
        for (const file of imageFiles) {
          imageFormData.append("images", file);
        }
        const imageResponse = await fetch(
          `${LISTING_SERVICE_URL}/listings/${listingId}/add-images`,
          {
            method: "POST",
            body: imageFormData,
            headers: {
              Authorization: `Bearer ${AUTH_TOKEN}`,
            },
          }
        );
        if (!imageResponse.ok) throw new Error("Thêm ảnh mới thất bại.");
      }

      // BƯỚC 3: CẬP NHẬT THÔNG TIN TIN ĐĂNG
      submitButton.querySelector("span").textContent = "Đang lưu thông tin...";
      const formData = new FormData(e.target);
      const data = Object.fromEntries(formData.entries());
      data.price = parseInt(String(data.price).replace(/\D/g, "")) || 0;
      [
        "chargeCycles",
        "rangePerCharge",
        "mileage",
        "maxSpeed",
        "yearOfManufacture",
      ].forEach((key) => {
        if (data[key]) data[key] = parseInt(data[key], 10);
        else delete data[key];
      });

      const updatedListing = await callApi(
        LISTING_SERVICE_URL,
        `/listings/${listingId}/update-details`,
        "PUT",
        data
      );

      // BƯỚC 4: THÀNH CÔNG VÀ ĐÓNG MODAL
      closeModal(editModal);
      Swal.fire({
        title: "Cập nhật thành công!",
        text: "Tin đăng của bạn đã được cập nhật và gửi đi để duyệt lại.",
        icon: "success",
        ...swalTheme,
        ...modalAnimation,
        confirmButtonColor: "#f97316",
      });
      updateListingCard(updatedListing);

      // Reset mảng imagesToDelete sau khi LƯU thành công
      imagesToDelete = [];
    } catch (error) {
      Swal.fire({
        title: "Lỗi khi cập nhật",
        text: error.message,
        icon: "error",
        ...swalTheme,
        ...modalAnimation,
        confirmButtonColor: "#f97316",
      });
    } finally {
      submitButton.disabled = false;
      submitButton.querySelector("span").textContent = originalButtonText;
    }
  };
  // =============================================
  // LOGIC SỰ KIỆN (EVENT LISTENERS)
  // =============================================

  document
    .getElementById("listings-tab-content")
    .addEventListener("click", (e) => {
      const button = e.target.closest(".action-btn");
      if (!button) return;
      const action = button.dataset.action;
      if (action === "view-reason") {
        const rawReason =
          button.dataset.reason || "Không có lý do được cung cấp.";
        let formattedReason = rawReason;
        const noteSeparator = ". Ghi chú: ";

        const separatorIndex = rawReason.indexOf(noteSeparator);
        if (separatorIndex !== -1) {
          const presetPart = rawReason.substring(0, separatorIndex + 1);
          const customPart = rawReason.substring(
            separatorIndex + noteSeparator.length
          );
          formattedReason = `${presetPart}<br><br><strong>Ghi chú:</strong> ${customPart}`;
        }
        Swal.fire({
          title: "Lý do từ chối",
          html: `<div class="text-left p-2 bg-gray-50 rounded whitespace-pre-wrap">${formattedReason}</div>`,
          icon: "info",
          confirmButtonText: "Đã hiểu",
          confirmButtonColor: "#dc2626",
          ...swalTheme,
          ...modalAnimation,
        });
        return;
      }
      const listingId = button.dataset.id;
      const listing = allListings.find((l) => l.listingId == listingId);
      if (!listing) return;
      currentEditingListing = listing;
      if (action === "edit") {
        let confirmTitle = "Xác nhận chỉnh sửa?";
        let confirmHtml = `<div class="text-left px-4">
                            <p class="mb-2">Bạn có chắc chắn muốn chỉnh sửa tin này?</p>
                            <ul class="list-disc list-inside space-y-1">
                                <li>Bạn chỉ có thể chỉnh sửa tin đăng <strong>một lần duy nhất</strong>.</li>
                                <li>Tin sau khi sửa sẽ được <strong>gửi để duyệt lại</strong>.</li>
                            </ul>
                         </div>`;
        if (listing.verified) {
          confirmTitle =
            '<i class="fas fa-exclamation-triangle text-orange-500"></i> Cảnh Báo Mất Kiểm Định!';
          confirmHtml += `<div class="mt-4 p-3 bg-orange-50 border border-orange-300 rounded-lg text-left">
                           <p class="font-bold text-orange-800">Tin này đã được "Kiểm Định".</p>
                           <p class="text-orange-700 text-sm">Chỉnh sửa sẽ <strong>XÓA vĩnh viễn</strong> nhãn "Kiểm Định".</p>
                         </div>`;
        }
        Swal.fire({
          title: confirmTitle,
          html: confirmHtml,
          icon: listing.verified ? "warning" : "question",
          showCancelButton: true,
          confirmButtonText: "Đồng ý sửa",
          cancelButtonText: "Hủy",
          ...swalTheme,
          ...modalAnimation,
          confirmButtonColor: "#f97316",
        }).then((result) => {
          if (result.isConfirmed) {
            openEditModal(listing);
          }
        });
      } else if (action === "view") {
        openViewDetailsModal(listing);
      } else if (action === "complain") {
        //// Mở modal khiếu nại (hàm do IIFE setupComplaintModal export)
        if (typeof window.openComplaintModal === "function") {
          window.openComplaintModal(listing);
        } else {
          console.warn("openComplaintModal chưa được khởi tạo");
        }
      } else if (action === "delete") {
        Swal.fire({
          title: "Xác nhận xóa tin đăng?",
          text: "Hành động này không thể hoàn tác.",
          icon: "warning",
          showCancelButton: true,
          confirmButtonText: "Đúng, xóa nó!",
          cancelButtonText: "Hủy",
          confirmButtonColor: "#dc2626",
          ...swalTheme,
          ...modalAnimation,
        }).then(async (result) => {
          if (result.isConfirmed) {
            try {
              await callApi(
                LISTING_SERVICE_URL,
                `/listings/${listingId}`,
                "DELETE"
              );
              Toast.fire({ icon: "success", title: "Đã xóa tin đăng." });
              fetchAllListings();
            } catch (error) {
              Toast.fire({
                icon: "error",
                title: `Lỗi khi xóa: ${error.message}`,
              });
            }
          }
        });
      }
    });

  closeViewModalBtn.addEventListener("click", () =>
    closeModal(viewDetailsModal)
  );
  editForm.addEventListener("submit", handleFormSubmit);
  currentImagesPreview.addEventListener("click", handleDeleteExistingImage);
  newImagesPreview.addEventListener("click", handleDeleteNewImage);
  editImageUpload.addEventListener("change", handleFileSelect);
  cancelEditBtn.addEventListener("click", () => {
    // 1. Reset mảng ảnh cần xóa (Ngăn không cho xóa trên server)
    imagesToDelete = [];

    // [FIX KHÔI PHỤC] Khôi phục lại mảng ảnh gốc đã lưu
    if (currentEditingListing && currentEditingListing.product) {
      currentEditingListing.product.images = originalImagesCopy;
      // 2. RE-RENDER UI để hiển thị lại các ảnh đã bị xóa khỏi DOM
      renderCurrentImages(currentEditingListing.product.images);
    }

    // 3. Đóng modal
    closeModal(editModal);
  });

  // =============================================
  // LOGIC WEBSOCKET (TỪ CODE CŨ)
  // =============================================
  const connectWebSocket = () => {
    if (!AUTH_TOKEN) return;
    try {
      const socket = new SockJS(NOTIFICATION_WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;
      const headers = { Authorization: `Bearer ${AUTH_TOKEN}` };

      stompClient.connect(
        headers,
        (frame) => {
          console.log("User đã kết nối WebSocket (để nhận thông báo):", frame);
          stompClient.subscribe(
            `/user/${CURRENT_USER_ID}/topic/notifications`,
            (payload) => {
              console.log("Nhận được thông báo cá nhân:", payload.body);
              if (IS_OWNER) {
                console.log(
                  "Đang ở trang cá nhân, tải lại danh sách tin đăng..."
                );
                Toast.fire({
                  icon: "info",
                  title: "Trạng thái tin đăng vừa được cập nhật.",
                });
                fetchAllListings();
                // Tải lại tab đánh giá (về trang 0)
                loadReviews(0);
              }
            }
          );

          // --- Lắng nghe sự kiện review ---
          stompClient.subscribe(
            `/user/${CURRENT_USER_ID}/topic/reviews`,
            (payload) => {
              const reviewEvent = JSON.parse(payload.body);
              console.log("Nhận được sự kiện review mới:", reviewEvent);
              if (IS_OWNER) {
                Toast.fire({
                  icon: "info",
                  title: `Bạn vừa nhận được một đánh giá mới!`,
                });
                // Tải lại tab "Đánh giá"
                loadReviews(0);
                // Tải lại thống kê sao
                fetchReviewStats();
              }
            }
          );
          // --- Kết thúc ---
        },
        (error) => {
          console.error("Lỗi WebSocket User:", error.toString());
          if (
            error.toString().includes("403") ||
            error.toString().includes("401")
          ) {
            console.error("WS Auth thất bại. Đang đăng xuất.");
            if (typeof logout === "function") logout();
            else window.location.href = "/login.html";
          } else {
            setTimeout(connectWebSocket, 7000 + Math.random() * 3000);
          }
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS cho User:", e);
      setTimeout(connectWebSocket, 10000);
    }
  };

  initializePage();

  // ==================================================// ==================================================
  // ==================================================
  // ==================================================
  // ==================================================
  // ==================================================
  // ==================================================
  // ==================================================
  // ========== Complaint modal logic ==========
  (function setupComplaintModal() {
    // DOM refs (Đảm bảo tất cả các ref đều được khai báo)
    const complaintModal = document.getElementById("complaintModal");
    const cancelComplaint = document.getElementById("cancelComplaint");
    const submitComplaint = document.getElementById("submitComplaint");
    const complaintProductName = document.getElementById(
      "complaintProductName"
    );
    const complaintBuyerName = document.getElementById("complaintBuyerName");
    const complaintBuyerAddress = document.getElementById(
      "complaintBuyerAddress"
    );
    const complaintPrevious = document.getElementById("complaintPrevious");
    const complaintReason = document.getElementById("complaintReason");

    let activeComplaintListing = null;

    // --- ✅ SỬA LỖI GIAO DIỆN: Sử dụng logic render từ purchase.js ---
    // (Helper này đã được cập nhật để giống hệt purchase.js, chỉ thay "Bạn" -> "Người Bán")
    function appendMessageToSellerModal(
      modal,
      { senderName, content, createdAt, isAdmin = undefined } = {}
    ) {
      const container = modal.querySelector(".existing-complaints");
      if (!container) return;

      // Logic từ purchase.js
      const adminFlag =
        typeof isAdmin !== "undefined"
          ? Boolean(isAdmin)
          : typeof senderName === "string" &&
            senderName.toLowerCase().startsWith("admin");

      const conv = document.createElement("div");
      conv.className = "conv-item";
      conv.style.display = "flex";
      conv.style.flexDirection = "column"; // Dùng 'column' giống purchase.js
      conv.style.gap = "8px";
      conv.style.marginBottom = "6px";

      const bubble = document.createElement("div");
      // Dùng class CSS (msg-admin / msg-user) giống purchase.js
      bubble.className = "msg-bubble " + (adminFlag ? "msg-admin" : "msg-user");

      let timeStr = "";
      if (createdAt) {
        const dateStr = createdAt.endsWith("Z") ? createdAt : createdAt + "Z";
        timeStr = new Date(dateStr).toLocaleString("vi-VN");
      }

      // Đổi "Bạn" (từ purchase.js) thành "Người Bán"
      bubble.innerHTML = `
            <div style="font-weight:700">${escapeHtml(
              senderName || (adminFlag ? "Admin" : "Người Bán")
            )} • ${timeStr}</div>
            <div style="margin-top:6px">${escapeHtml(content || "")}</div>
        `;

      conv.appendChild(bubble);
      container.appendChild(conv);
      container.scrollTop = container.scrollHeight;
    }
    // --- Kết thúc sửa lỗi giao diện ---

    // Helper: Lấy Header xác thực
    function getAuthHeaders(withJson = false) {
      const headers = { Accept: "application/json" };
      if (withJson) headers["Content-Type"] = "application/json";
      try {
        const token = localStorage.getItem("token");
        if (token) headers["Authorization"] = `Bearer ${token}`;
      } catch (e) {}
      return headers;
    }

    // --- Load existing complaints for seller view ---
    async function loadExistingComplaintsForSellerModal(purchaseId) {
      const modal = window._complaintModalEl || complaintModal;
      const container = modal.querySelector(".existing-complaints");
      container.innerHTML = "Đang tải lịch sử khiếu nại...";

      const sellerId = Number(PROFILE_USER_ID);

      try {
        // URL gốc của LISTING_SERVICE_URL là "http://localhost:8080/api"
        // Chúng ta cần thay thế "8080/api" bằng "8096" để được "http://localhost:8096"
        const adminEndpoint = `${LISTING_SERVICE_URL.replace(
          "9000/api", // Thay thế cả "8080/api"
          "9000" // Chỉ bằng "8096"
        )}/api/admin-trans/complaints/purchase/${purchaseId}?userId=${sellerId}`;

        const res = await fetch(adminEndpoint, {
          headers: {
            Authorization: `Bearer ${AUTH_TOKEN}`,
            Accept: "application/json",
          },
        });

        if (!res.ok) {
          if (res.status === 403)
            throw new Error(
              "Bạn không có quyền xem khiếu nại này (403 Forbidden)."
            );
          const errorText = await res
            .text()
            .catch(() => "Lỗi máy chủ không xác định");
          throw new Error(
            `Không tải được lịch sử (Lỗi ${res.status}): ${errorText}`
          );
        }
        const list = await res.json();

        if (!Array.isArray(list) || list.length === 0) {
          container.innerHTML =
            "<div style='color:#6b7280'>Chưa có khiếu nại/trao đổi trước đó.</div>";
          return;
        }

        // Sort ascending (oldest first)
        list.sort((a, b) => {
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return ta - tb;
        });

        container.innerHTML = "";
        list.forEach((c) => {
          const sender = c.senderName || "Người gửi";
          const isAdminMsg = sender.toLowerCase().startsWith("admin");

          appendMessageToSellerModal(modal, {
            senderName: sender,
            content: c.detail || "",
            createdAt: c.createdAt || new Date().toISOString(),
            isAdmin: isAdminMsg,
          });

          if (c.adminResponse) {
            appendMessageToSellerModal(modal, {
              senderName: "Admin (Phản hồi)",
              content: c.adminResponse,
              createdAt: c.repliedAt || new Date().toISOString(),
              isAdmin: true,
            });
          }
        });

        container.scrollTop = container.scrollHeight;
      } catch (err) {
        container.innerHTML = `<div style="color:#b91c1c">Không thể tải khiếu nại: ${escapeHtml(
          err.message
        )}</div>`;
        console.error("Load seller complaints failed:", err);
      }
    }
    // --- END Load existing complaints ---

    // --- Fetch Purchase Info Helper ---
    async function fetchPurchaseInfoForSeller(productId, sellerId) {
      // Endpoint này mong đợi {listingId} trong URL, nhưng logic của nó so sánh với 'productId'
      const purchaseApiUrl = `/api/admin-trans/purchase/by-seller-listing/${productId}?sellerId=${sellerId}`;

      try {
        // === 🛑 SỬA LỖI 404: URL BỊ LẶP "/api/api" ===
        // Sửa: .replace("8080/api", "8096")
        // URL gốc là "http://localhost:8080/api"
        // Thay thế "8080/api" bằng "8096" -> "http://localhost:8096"
        // URL cuối cùng: "http://localhost:8096" + "/api/admin-trans/..."
        const purchase = await callApi(
          LISTING_SERVICE_URL.replace("9000/api", "9000"), // SỬA LỖI TẠI ĐÂY
          purchaseApiUrl
        );

        return {
          purchaseId: purchase.id,
          buyerName: purchase.fullName || "Người đặt (N/A)",
          buyerAddress: purchase.address || "Địa chỉ (N/A)",
          buyerPhone: purchase.phone,
        };
      } catch (e) {
        console.warn("Could not fetch Purchase Info for Seller:", e.message);
        // Ném lỗi gốc 404 từ backend
        throw new Error(
          `Lỗi tải thông tin giao dịch (Lỗi không xác định (404)).`
        );
      }
    }
    // --- END Fetch Purchase Info Helper ---

    // Hàm openComplaintModal CHÍNH
    function openComplaintModal(listing) {
      // 1. Gán dữ liệu cơ bản
      activeComplaintListing = listing;
      const prod = listing.product || {};

      // 2. Cập nhật tên sản phẩm và chuẩn bị UI
      complaintProductName.textContent = prod.productName || "N/A";

      // 3. Reset lịch sử và thông tin Buyer (CHUẨN BỊ UI)
      const historyContainer = complaintModal.querySelector(
        ".existing-complaints"
      );
      if (historyContainer)
        historyContainer.innerHTML =
          "<p class='text-center text-gray-500'>Đang tải thông tin...</p>";

      // Clear Buyer info fields before loading
      complaintBuyerName.textContent = "...";
      complaintBuyerAddress.textContent = "...";

      // Backend (AdminTransactionController) đang mong đợi Product ID, không phải Listing ID
      const productIdToFetch = prod.productId;

      if (!productIdToFetch) {
        const errorMsg = "Lỗi: Tin đăng này thiếu mã sản phẩm (productId).";
        complaintBuyerName.textContent = errorMsg;
        complaintBuyerAddress.textContent = errorMsg;
        if (historyContainer)
          historyContainer.innerHTML = `<p class='text-center text-red-500'>${errorMsg}</p>`;

        complaintModal.dataset.senderName =
          listing.sellerName || PROFILE_USER_ID;
        complaintModal.classList.remove("hidden");
        return; // Dừng lại, không fetch
      }

      // 4. Lấy thông tin Purchase/Buyer (BẤT ĐỒNG BỘ)
      // Gửi 'productIdToFetch'
      fetchPurchaseInfoForSeller(productIdToFetch, PROFILE_USER_ID)
        .then((info) => {
          // Sau khi có info, gán Purchase ID thật và thông tin Buyer
          complaintModal.dataset.purchaseId = info.purchaseId;
          complaintBuyerName.textContent = info.buyerName;
          complaintBuyerAddress.textContent = info.buyerAddress;

          // Lần load lịch sử đầu tiên (sau khi có Purchase ID thực)
          loadExistingComplaintsForSellerModal(info.purchaseId);
        })
        .catch((e) => {
          // Xử lý lỗi tải thông tin (từ throw new Error trong fetchPurchaseInfoForSeller)
          const errorMessage = e.message;
          complaintBuyerName.textContent = errorMessage;
          complaintBuyerAddress.textContent = errorMessage;
          if (historyContainer)
            historyContainer.innerHTML = `<p class='text-center text-red-500'>Lỗi tải lịch sử chat: ${errorMessage}</p>`;
        });

      // 5. Gắn metadata và hiển thị modal
      complaintModal.dataset.senderName = listing.sellerName || PROFILE_USER_ID;
      complaintModal.classList.remove("hidden");
    }

    // Hàm closeComplaintModal
    function closeComplaintModal() {
      activeComplaintListing = null;
      complaintModal.classList.add("hidden");
      delete complaintModal.dataset.purchaseId;
      delete complaintModal.dataset.senderName;
      // Xóa nội dung chat cũ và text area
      const historyContainer = complaintModal.querySelector(
        ".existing-complaints"
      );
      if (historyContainer) historyContainer.innerHTML = "";
      if (complaintReason) complaintReason.value = "";
    }

    // --- Handle Seller Complaint Submission ---
    submitComplaint.addEventListener("click", async () => {
      if (!activeComplaintListing) return;

      const purchaseId = complaintModal.dataset.purchaseId;

      if (!purchaseId) {
        Toast.fire({
          icon: "error",
          title: "Lỗi: Không tìm thấy ID Giao dịch. Không thể gửi.",
        });
        return;
      }

      const reason = (complaintReason.value || "").trim();
      if (!reason) {
        Toast.fire({
          icon: "warning",
          title: "Vui lòng nhập nội dung khiếu nại.",
        });
        return;
      }
      submitComplaint.disabled = true;
      submitComplaint.textContent = "Đang gửi...";

      try {
        const sellerName =
          userCache.get(PROFILE_USER_ID) || `Seller#${PROFILE_USER_ID}`;

        const sellerPhone = userPhoneEl.textContent
          .replace(/.*>|<\/i>/g, "")
          .trim();
        const sellerEmail = userEmailEl.textContent
          .replace(/.*>|<\/i>/g, "")
          .trim();

        const payload = {
          purchaseId: Number(purchaseId),
          senderName: sellerName,
          senderPhone: sellerPhone,
          senderEmail: sellerEmail,
          detail: reason,
        };
        const COMPLAINT_API_URL = `${LISTING_SERVICE_URL.replace(
          "9000/api", // Sửa lỗi URL
          "9000"
        )}/api/complaints`;

        const response = await fetch(COMPLAINT_API_URL, {
          method: "POST",
          headers: getAuthHeaders(true),
          body: JSON.stringify(payload),
        });
        if (!response.ok) {
          const text = await response.text().catch(() => "Unknown error");
          throw new Error(text);
        }
        const savedComplaint = await response.json();

        appendMessageToSellerModal(complaintModal, {
          senderName: sellerName,
          content: reason,
          createdAt: savedComplaint.createdAt || new Date().toISOString(),
          isAdmin: false,
        });

        complaintReason.value = "";
        Toast.fire({
          icon: "success",
          title: "Gửi phản hồi thành công. ",
        });
      } catch (err) {
        console.error("Lỗi gửi khiếu nại (Seller):", err);
        Toast.fire({
          icon: "error",
          title: `Gửi khiếu nại thất bại: ${err.message}`,
        });
      } finally {
        submitComplaint.disabled = false;
        submitComplaint.textContent = "Gửi";
      }
    });

    // --- Event Listeners cho Modal ---
    cancelComplaint.addEventListener("click", closeComplaintModal);

    const complaintOverlay = document.getElementById("complaintOverlay");
    if (complaintOverlay) {
      complaintOverlay.addEventListener("click", closeComplaintModal);
    } else {
      complaintModal.addEventListener("click", (e) => {
        if (e.target === complaintModal) {
          closeComplaintModal();
        }
      });
    }

    // Expose functions
    window.openComplaintModal = openComplaintModal;
    window.appendMessageToSellerModal = appendMessageToSellerModal;
    window._complaintModalEl = complaintModal;
  })();
});
