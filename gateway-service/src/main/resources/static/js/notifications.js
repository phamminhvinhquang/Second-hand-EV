// File: /js/notifications.js
// ĐÃ TỐI ƯU: Xóa mượt (Optimistic UI) - Chạy hiệu ứng và API song song

document.addEventListener("DOMContentLoaded", () => {
  const API_BASE_URL = "/api";
  const WS_URL = "/ws-notification/ws";
  const PAGE_SIZE = 24;

  // --- KHỞI TẠO KÊNH GIAO TIẾP ---
  const notifChannel = new BroadcastChannel("notification_sync_channel");

  // --- LOGIC AUTH (GIỮ NGUYÊN) ---
  function getAuthInfo() {
    const token = localStorage.getItem("token");
    const userString = localStorage.getItem("user");

    if (!token || !userString) {
      console.warn("Notifications: Người dùng chưa đăng nhập.");
      return null;
    }
    try {
      const user = JSON.parse(userString);
      return {
        token: token,
        userId: user.id,
      };
    } catch (e) {
      console.error("Notifications: Lỗi parse user data", e);
      return null;
    }
  }

  const authInfo = getAuthInfo();
  const listContainer = document.getElementById("all-notifications-list");
  const paginationContainer = document.getElementById("paginationContainer");
  const deleteAllBtn = document.getElementById("delete-all-btn");

  if (!authInfo) {
    if (listContainer) {
      listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Vui lòng <a href="/login.html" style="color: blue; text-decoration: underline;">đăng nhập</a> để xem thông báo.</p>`;
    }
    if (deleteAllBtn) deleteAllBtn.style.display = "none";
    if (paginationContainer) paginationContainer.style.display = "none";
    return;
  }

  const LOGGED_IN_USER_ID = authInfo.userId;
  const AUTH_TOKEN = authInfo.token;

  let currentPage = 0;
  let totalPages = 0;
  let stompClient = null;

  // --- TOAST CONFIG ---
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
  });

  const swalTheme = {
    cancelButtonColor: "#6c757d",
  };
  const modalAnimation = {};

  // --- API HELPER ---
  const callApi = async (url, method = "GET", body = null) => {
    const options = {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${AUTH_TOKEN}`,
      },
      cache: "no-store",
    };
    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE_URL}${url}`, options);

    if (response.status === 401 || response.status === 403) {
      Toast.fire({
        icon: "error",
        title: "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.",
      });
      localStorage.clear();
      window.location.href = "/login.html";
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
    return response.status === 204 ? null : response.json();
  };

  // --- CORE FUNCTIONS ---
  const fetchNotifications = async (page = 0) => {
    currentPage = page;
    listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Đang tải...</p>`;
    if (deleteAllBtn) deleteAllBtn.disabled = true;

    try {
      const pageData = await callApi(
        `/notifications/user/${LOGGED_IN_USER_ID}?page=${page}&size=${PAGE_SIZE}`
      );

      const notifications = pageData.content;
      totalPages = pageData.totalPages;

      listContainer.innerHTML = "";

      if (notifications.length === 0) {
        listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Bạn không có thông báo nào.</p>`;
        if (deleteAllBtn) deleteAllBtn.disabled = true;
      } else {
        // Dùng map + join để render 1 lần cho mượt
        listContainer.innerHTML = notifications
          .map(createNotificationHTML)
          .join("");

        if (deleteAllBtn) deleteAllBtn.disabled = false;
      }

      renderPagination();
    } catch (error) {
      if (error.message !== "Unauthorized") {
        console.error("Lỗi fetchNotifications:", error);
        listContainer.innerHTML = `<p style="text-align: center; padding: 20px; color: red;">Không thể tải thông báo.</p>`;
        if (deleteAllBtn) deleteAllBtn.disabled = true;
      }
    }
  };

  const createNotificationHTML = (notif) => {
    if (!notif || !notif.id || !notif.message || !notif.createdAt) {
      return "";
    }
    const isRead = notif.read === true || notif.isRead === true;
    const unreadClass = !isRead ? "unread" : "";
    let iconClass = "fa-bell";

    const msgLower = notif.message.toLowerCase();
    if (msgLower.includes("duyệt")) {
      iconClass = "fa-check-circle text-green-500";
    } else if (msgLower.includes("từ chối")) {
      iconClass = "fa-times-circle text-red-500";
    } else if (msgLower.includes("kiểm định")) {
      iconClass = "fa-shield-alt text-blue-500";
    }

    return `
        <div class="notification-item ${unreadClass} animate__animated animate__fadeInUp" data-notification-id="${
      notif.id
    }">
            <a href="${
              notif.link || "#"
            }" class="notification-link-content" data-id="${notif.id}">
                <div class="notification-icon-wrapper">
                    <i class="fas ${iconClass}"></i>
                </div>
                <div class="notification-content">
                    <p class="notification-message">${notif.message}</p>
                    <p class="notification-time">
                        ${new Date(notif.createdAt).toLocaleString("vi-VN")}
                    </p>
                </div>
            </a>
            <button class="delete-notification-btn" data-delete-id="${
              notif.id
            }" title="Xóa thông báo">
                &times;
            </button>
        </div>
      `;
  };

  // --- [QUAN TRỌNG] HÀM XÓA ĐÃ TỐI ƯU ---
  const handleDeleteNotification = (button) => {
    const id = button.dataset.deleteId;
    const itemToRemove = button.closest(".notification-item");

    if (!itemToRemove) return;

    // 1. Ngăn người dùng click nhiều lần vào item đang xóa
    itemToRemove.style.pointerEvents = "none";

    // 2. Thêm class animation biến mất
    itemToRemove.classList.add(
      "animate__animated",
      "animate__fadeOut",
      "animate__faster"
    );

    // 3. Gọi API NGẦM (Không await ở đây để chặn UI)
    callApi(`/notifications/${id}`, "DELETE")
      .then(() => {
        // API thành công (thường sẽ xong sau khi animation xong)
        // Gửi tín hiệu cho tab khác
        notifChannel.postMessage({ type: "REFRESH_DATA" });
        document.dispatchEvent(new CustomEvent("notification-deleted"));
        Toast.fire({ icon: "success", title: "Đã xóa thông báo." });
      })
      .catch((error) => {
        if (error.message !== "Unauthorized") {
          console.error("Lỗi xóa:", error);
          // Nếu lỗi, reload lại danh sách để phục hồi item đã bị xóa oan (hiếm gặp)
          fetchNotifications(currentPage);
          Toast.fire({ icon: "error", title: "Lỗi khi xóa, đang tải lại..." });
        }
      });

    // 4. Xử lý DOM ngay sau khi animation kết thúc (khoảng 300-500ms)
    // Bất chấp API xong chưa, cứ xóa visual đi cho mượt
    itemToRemove.addEventListener(
      "animationend",
      () => {
        itemToRemove.remove();

        // Kiểm tra nếu danh sách trống sau khi xóa DOM
        if (listContainer.children.length === 0) {
          if (currentPage > 0) {
            fetchNotifications(currentPage - 1);
          } else {
            listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Bạn không có thông báo nào.</p>`;
            if (deleteAllBtn) deleteAllBtn.disabled = true;
          }
        }
      },
      { once: true }
    );
  };

  const handleDeleteAll = async () => {
    const result = await Swal.fire({
      title: "Xóa TẤT CẢ thông báo?",
      text: "Hành động này không thể hoàn tác.",
      icon: "warning",
      showCancelButton: true,
      confirmButtonText: "Xóa tất cả",
      cancelButtonText: "Hủy",
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#6c757d",
      ...swalTheme,
      ...modalAnimation,
    });

    if (result.isConfirmed) {
      try {
        // Xóa ngay trên UI cho cảm giác nhanh
        listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Đang xóa...</p>`;

        await callApi(`/notifications/user/${LOGGED_IN_USER_ID}`, "DELETE");

        listContainer.innerHTML = `<p style="text-align: center; padding: 20px">Bạn không có thông báo nào.</p>`;
        paginationContainer.innerHTML = "";
        if (deleteAllBtn) deleteAllBtn.disabled = true;

        Toast.fire({ icon: "success", title: "Đã xóa tất cả thông báo." });

        // Bắn tín hiệu
        notifChannel.postMessage({ type: "REFRESH_DATA" });
        document.dispatchEvent(new CustomEvent("notification-deleted"));
      } catch (error) {
        if (error.message !== "Unauthorized") {
          console.error("Lỗi khi xóa tất cả:", error);
          Toast.fire({
            icon: "error",
            title: "Xóa thất bại. Vui lòng thử lại.",
          });
          fetchNotifications(currentPage); // Rollback nếu lỗi
        }
      }
    }
  };

  const handleNotificationClick = async (linkElement, event) => {
    if (event.button !== 0 || event.ctrlKey || event.metaKey) return;
    // Không preventDefault ngay để cho phép trình duyệt xử lý link,
    // nhưng ở đây ta cần xử lý mark-read trước
    event.preventDefault();

    const id = linkElement.dataset.id;
    const link = linkElement.href;
    const notificationItem = linkElement.closest(".notification-item");

    if (notificationItem && notificationItem.classList.contains("unread")) {
      // Gọi mark read ngầm, không await chặn chuyển trang
      callApi(`/notifications/${id}/read`, "POST")
        .then(() => {
          notifChannel.postMessage({ type: "REFRESH_DATA" });
          document.dispatchEvent(new CustomEvent("notification-read"));
        })
        .catch((err) => console.error(err));

      notificationItem.classList.remove("unread");
    }

    let finalLink = link;
    try {
      const url = new URL(link);
      const listingParamId =
        url.searchParams.get("id") || url.searchParams.get("listing_id");
      if (
        listingParamId &&
        (url.pathname.includes("product_detail.html") ||
          url.pathname.includes("manage-listings.html") ||
          url.pathname.includes("edit_news.html"))
      ) {
        const newUrl = new URL("/edit_news.html", window.location.origin);
        newUrl.searchParams.set("listing_id", listingParamId);
        finalLink = newUrl.href;
      }
    } catch (err) {}

    window.location.href = finalLink;
  };

  // --- EVENT DELEGATION ---
  listContainer.addEventListener("click", (e) => {
    const deleteButton = e.target.closest(".delete-notification-btn");
    const linkContent = e.target.closest(".notification-link-content");

    if (deleteButton) {
      e.preventDefault();
      e.stopPropagation();
      handleDeleteNotification(deleteButton); // Không cần await vì ta xử lý optimistic
    } else if (linkContent) {
      handleNotificationClick(linkContent, e);
    }
  });

  // --- PAGINATION ---
  const renderPagination = () => {
    paginationContainer.innerHTML = "";
    if (totalPages <= 1) return;

    paginationContainer.innerHTML += `
        <button class="pagination-btn" data-page="${currentPage - 1}" ${
      currentPage === 0 ? "disabled" : ""
    }>Trước</button>`;

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

  const getPaginationRange = (currentPage, totalPages) => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i);
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

  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      const page = parseInt(button.dataset.page, 10);
      fetchNotifications(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  });

  if (deleteAllBtn) {
    deleteAllBtn.addEventListener("click", handleDeleteAll);
  }

  // --- SYNC & SOCKET ---
  notifChannel.onmessage = (event) => {
    if (event.data && event.data.type === "REFRESH_DATA") {
      fetchNotifications(currentPage);
    }
  };

  const onNotificationReceived = (payload) => {
    if (currentPage !== 0) {
      document.dispatchEvent(new CustomEvent("notification-received-external"));
      return;
    }
    try {
      const notif = JSON.parse(payload.body);
      const html = createNotificationHTML(notif);
      if (!html) return;

      const emptyMessage = listContainer.querySelector("p");
      if (
        emptyMessage &&
        emptyMessage.textContent.includes("Bạn không có thông báo nào")
      ) {
        listContainer.innerHTML = "";
      }

      listContainer.insertAdjacentHTML("afterbegin", html);
      const newItem = listContainer.firstChild;
      if (newItem && newItem.classList) {
        newItem.classList.add("row-updated");
        setTimeout(() => newItem.classList.remove("row-updated"), 1500);
      }
      if (deleteAllBtn && deleteAllBtn.disabled) deleteAllBtn.disabled = false;
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
    }
  };

  const connectWebSocket = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;

      stompClient.connect(
        { Authorization: `Bearer ${AUTH_TOKEN}` },
        () => {
          stompClient.subscribe(
            `/user/${LOGGED_IN_USER_ID}/topic/notifications`,
            onNotificationReceived
          );
        },
        (error) => {
          console.warn("Socket connect error, retrying...");
          setTimeout(connectWebSocket, 5000);
        }
      );
    } catch (e) {
      setTimeout(connectWebSocket, 10000);
    }
  };

  // --- INIT ---
  fetchNotifications(0);
  connectWebSocket();

  document.addEventListener("notification-deleted", () => {
    if (currentPage === 0) fetchNotifications(0);
    notifChannel.postMessage({ type: "REFRESH_DATA" });
  });
  document.addEventListener("notification-read", () => {
    if (currentPage === 0) fetchNotifications(0);
    notifChannel.postMessage({ type: "REFRESH_DATA" });
  });
});
