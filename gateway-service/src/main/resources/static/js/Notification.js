// File: /js/Notification.js

// THAY ĐỔI: Sử dụng BroadcastChannel để đồng bộ giữa các tab
document.addEventListener("componentsLoaded", () => {
  const API_BASE_URL = "/api";
  const WS_URL = "/ws-notification/ws";

  // --- [MỚI] KHỞI TẠO KÊNH GIAO TIẾP GIỮA CÁC TAB ---
  const notifChannel = new BroadcastChannel("notification_sync_channel");

  // --- 1. GIỮ NGUYÊN LOGIC AUTH CŨ ---
  function getAuthInfo() {
    const token = localStorage.getItem("token");
    const userString = localStorage.getItem("user");

    if (!token || !userString) {
      console.warn("Notification: Người dùng chưa đăng nhập.");
      return null;
    }
    try {
      const user = JSON.parse(userString);
      return {
        token: token,
        userId: user.id,
      };
    } catch (e) {
      console.error("Notification: Lỗi parse user data", e);
      return null;
    }
  }

  const authInfo = getAuthInfo();

  const bellContainer = document.getElementById("notification-bell-container");
  const bellIcon = document.getElementById("notification-bell-icon");
  const badge = document.getElementById("notification-badge");
  const dropdown = document.getElementById("notification-dropdown");
  const notificationList = document.getElementById("notification-list");
  const loadingText = document.getElementById("notification-loading");
  const markAllReadBtn = document.getElementById("mark-all-read-btn");

  if (!bellContainer || !bellIcon || !badge || !dropdown || !notificationList) {
    // Nếu trang không có navbar (ví dụ trang login), bỏ qua
    return;
  }

  // --- KIỂM TRA ĐĂNG NHẬP ---
  if (!authInfo) {
    bellContainer.style.display = "none";
    return;
  }

  const LOGGED_IN_USER_ID = authInfo.userId;
  const AUTH_TOKEN = authInfo.token;

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

  let hasFetchedNotifications = false;
  let stompClient = null;

  // --- GIỮ NGUYÊN LOGIC CALL API ---
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
      console.error("Notification: Token hết hạn hoặc không có quyền.");
      bellContainer.style.display = "none";
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

  const fetchUnreadCount = async () => {
    try {
      const count = await callApi(
        `/notifications/user/${LOGGED_IN_USER_ID}/unread-count`
      );

      if (typeof count === "number" && count > 0) {
        let displayCount = count;
        if (count > 99) {
          displayCount = "99+";
        }
        badge.textContent = displayCount;
        badge.classList.remove("hidden");
      } else {
        badge.textContent = "";
        badge.classList.add("hidden");
      }
    } catch (error) {
      if (error.message !== "Unauthorized") {
        console.error("Lỗi khi lấy số lượng thông báo:", error);
      }
    }
  };

  const createNotificationHTML = (notif) => {
    const isRead = notif.read === true || notif.isRead === true;
    const unreadClass = !isRead ? "unread" : "";
    let iconClass = "fa-bell";

    return `
      <div class="notification-item-container ${unreadClass}" data-notif-id="${
      notif.id
    }">
        <div class="notification-icon-wrapper">
            <i class="fas ${iconClass}"></i>
        </div>
        <a href="${
          notif.link || "#"
        }" class="notification-link-content" data-id="${notif.id}">
            <p class="text-sm">${notif.message}</p>
            <p class="text-xs mt-1">${new Date(notif.createdAt).toLocaleString(
              "vi-VN"
            )}</p>
        </a>
        <button class="notification-delete-btn" data-id="${
          notif.id
        }" title="Xóa">
            <i class="fas fa-times"></i>
        </button>
      </div>
    `;
  };

  const fetchNotifications = async () => {
    try {
      const data = await callApi(
        `/notifications/user/${LOGGED_IN_USER_ID}?page=0&size=8`
      );
      const notifications = data.content;
      if (loadingText) loadingText.style.display = "none";
      notificationList.innerHTML = "";

      if (notifications.length === 0) {
        notificationList.innerHTML =
          '<p class="empty-state-text">Bạn không có thông báo nào.</p>';
        if (markAllReadBtn) markAllReadBtn.style.display = "none";
      } else {
        notifications.forEach((notif) => {
          notificationList.innerHTML += createNotificationHTML(notif);
        });
        if (markAllReadBtn) markAllReadBtn.style.display = "block";
      }
      hasFetchedNotifications = true;
    } catch (error) {
      if (error.message !== "Unauthorized") {
        console.error("Lỗi khi lấy danh sách thông báo:", error);
        if (loadingText) loadingText.textContent = "Không thể tải thông báo.";
      }
      if (markAllReadBtn) markAllReadBtn.style.display = "none";
      hasFetchedNotifications = false;
    }
  };

  const markAllAsRead = async () => {
    try {
      await callApi(
        `/notifications/mark-all-as-read/user/${LOGGED_IN_USER_ID}`,
        "POST"
      );

      // Update UI tab hiện tại
      fetchUnreadCount();
      notificationList.querySelectorAll(".unread").forEach((el) => {
        el.classList.remove("unread");
      });

      // [MỚI] Bắn tín hiệu cho các tab khác
      notifChannel.postMessage({ type: "REFRESH_DATA" });
    } catch (error) {
      if (error.message !== "Unauthorized") {
        console.error("Lỗi khi đánh dấu đã đọc:", error);
      }
    }
  };

  bellIcon.addEventListener("click", (e) => {
    e.preventDefault();
    const isHidden = dropdown.classList.contains("hidden");
    if (isHidden) {
      dropdown.classList.remove("hidden");
      dropdown.style.opacity = "0";
      const bellRect = bellContainer.getBoundingClientRect();
      const dropdownWidth = dropdown.offsetWidth;
      const padding = 10;
      const bellCenter = bellRect.left + bellRect.width / 2;
      let dropdownLeft = bellCenter - dropdownWidth / 2;
      dropdown.style.left = "auto";
      dropdown.style.right = "auto";
      dropdown.style.top = `${bellRect.bottom + 12}px`;
      if (dropdownLeft < padding) {
        dropdown.style.left = `${padding}px`;
      } else if (dropdownLeft + dropdownWidth > window.innerWidth - padding) {
        dropdown.style.right = `${padding}px`;
      } else {
        dropdown.style.left = `${dropdownLeft}px`;
      }
      dropdown.style.transform = "translateY(-10px)";
      requestAnimationFrame(() => {
        dropdown.style.opacity = "1";
        dropdown.style.transform = "translateY(0)";
      });
      if (!hasFetchedNotifications) {
        if (loadingText) loadingText.style.display = "block";
        fetchNotifications();
      }
    } else {
      dropdown.style.opacity = "0";
      dropdown.style.transform = "translateY(-10px)";
      setTimeout(() => {
        dropdown.classList.add("hidden");
      }, 200);
    }
  });

  notificationList.addEventListener("click", async (e) => {
    const deleteBtn = e.target.closest(".notification-delete-btn");
    const link = e.target.closest(".notification-link-content");

    if (deleteBtn) {
      e.preventDefault();
      e.stopPropagation();
      const notifId = deleteBtn.dataset.id;
      const notifItem = deleteBtn.closest(".notification-item-container");
      try {
        await callApi(`/notifications/${notifId}`, "DELETE");
        notifItem.remove();

        if (
          notificationList.children.length === 0 ||
          (notificationList.children.length === 1 &&
            notificationList.children[0].tagName === "P")
        ) {
          notificationList.innerHTML =
            '<p class="empty-state-text">Bạn không có thông báo nào.</p>';
          if (markAllReadBtn) markAllReadBtn.style.display = "none";
        }

        fetchUnreadCount();

        // [MỚI] Bắn tín hiệu cho các tab khác
        notifChannel.postMessage({ type: "REFRESH_DATA" });

        Toast.fire({ icon: "success", title: "Đã xóa thông báo" });
        document.dispatchEvent(new CustomEvent("notification-deleted"));
      } catch (error) {
        if (error.message !== "Unauthorized") {
          Swal.fire({
            icon: "error",
            title: "Lỗi",
            text: "Không thể xóa thông báo!",
          });
        }
      }
      return;
    }
    if (link) {
      e.preventDefault();
      const notifId = link.dataset.id;
      const href = link.href;
      const notifItem = link.closest(".notification-item-container");
      let finalLink = href;
      try {
        const url = new URL(href);
        const listingParamId =
          url.searchParams.get("id") || url.searchParams.get("listing_id");
        if (
          listingParamId &&
          (url.pathname.includes("product_detail.html") ||
            url.pathname.includes("manage-listings.html"))
        ) {
          const newUrl = new URL(
            "/manage-listings.html",
            window.location.origin
          );
          newUrl.searchParams.set("listing_id", listingParamId);
          finalLink = newUrl.href;
        }
      } catch (err) {
        // Bỏ qua
      }
      try {
        if (notifItem.classList.contains("unread")) {
          await callApi(`/notifications/${notifId}/read`, "POST");
          notifItem.classList.remove("unread");
          fetchUnreadCount();

          // [MỚI] Bắn tín hiệu cho các tab khác trước khi chuyển trang
          notifChannel.postMessage({ type: "REFRESH_DATA" });
        }
      } catch (error) {
        if (error.message !== "Unauthorized") {
          console.error("Lỗi khi đánh dấu đã đọc:", error);
        }
      } finally {
        window.location.href = finalLink;
      }
    }
  });

  if (markAllReadBtn) {
    markAllReadBtn.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      markAllAsRead();
    });
  }

  document.addEventListener("click", (e) => {
    if (
      !bellContainer.contains(e.target) &&
      !dropdown.contains(e.target) &&
      !dropdown.classList.contains("hidden")
    ) {
      dropdown.style.opacity = "0";
      dropdown.style.transform = "translateY(-10px)";
      setTimeout(() => {
        dropdown.classList.add("hidden");
      }, 200);
    }
  });

  // --- [MỚI] LẮNG NGHE TÍN HIỆU TỪ TAB KHÁC ---
  notifChannel.onmessage = (event) => {
    // Nếu tab khác bảo refresh
    if (event.data && event.data.type === "REFRESH_DATA") {
      // Cập nhật số lượng trên Badge
      fetchUnreadCount();

      // Nếu dropdown đang mở, reload luôn list để đồng bộ trạng thái
      if (!dropdown.classList.contains("hidden")) {
        fetchNotifications();
      } else {
        // Nếu đóng, đánh dấu để lần sau mở ra nó tự fetch lại
        hasFetchedNotifications = false;
      }
    }
  };

  // --- LOGIC WEBSOCKET (GIỮ NGUYÊN) ---
  const onNotificationReceived = (payload) => {
    try {
      const notification = JSON.parse(payload.body);

      try {
        let currentCount = 0;
        const currentText = badge.textContent.trim();

        if (currentText && !isNaN(parseInt(currentText, 10))) {
          currentCount = parseInt(currentText, 10);
        }

        let newCount = currentCount + 1;
        let displayCount = newCount;

        if (newCount > 99) {
          displayCount = "99+";
        }

        badge.textContent = displayCount;
        badge.classList.remove("hidden");
      } catch (e) {
        console.error("Lỗi tự tăng số đếm, quay về fetch API", e);
        fetchUnreadCount();
      }

      if (!dropdown.classList.contains("hidden")) {
        const notificationHTML = createNotificationHTML(notification);
        const emptyText = notificationList.querySelector(".empty-state-text");

        if (emptyText) {
          notificationList.innerHTML = "";
          if (markAllReadBtn) markAllReadBtn.style.display = "block";
        }

        notificationList.insertAdjacentHTML("afterbegin", notificationHTML);
      } else {
        hasFetchedNotifications = false;
      }
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
    }
  };

  const connectWebSocket = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;

      const headers = {
        Authorization: `Bearer ${AUTH_TOKEN}`,
      };

      stompClient.connect(
        headers,
        (frame) => {
          console.log("Đã kết nối WebSocket (Chuông):", frame);
          stompClient.subscribe(
            `/user/${LOGGED_IN_USER_ID}/topic/notifications`,
            onNotificationReceived
          );
        },
        (error) => {
          console.error("Lỗi WebSocket:", error.toString());
          if (
            error.toString().includes("403") ||
            error.toString().includes("401")
          ) {
            console.error("WebSocket Auth thất bại. Dừng kết nối.");
            bellContainer.style.display = "none";
          } else {
            setTimeout(connectWebSocket, 5000 + Math.random() * 5000);
          }
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS:", e);
      setTimeout(connectWebSocket, 10000);
    }
  };

  // Khởi chạy
  fetchUnreadCount();
  connectWebSocket();

  // Lắng nghe tín hiệu từ notifications.js (Cùng tab)
  document.addEventListener("notification-deleted", () => {
    fetchUnreadCount();
    hasFetchedNotifications = false;
    if (!dropdown.classList.contains("hidden")) {
      fetchNotifications();
    }
    // [MỚI] Báo cho tab khác
    notifChannel.postMessage({ type: "REFRESH_DATA" });
  });

  document.addEventListener("notification-read", () => {
    fetchUnreadCount();
    hasFetchedNotifications = false;
    // [MỚI] Báo cho tab khác
    notifChannel.postMessage({ type: "REFRESH_DATA" });
  });

  document.addEventListener("notification-received-external", () => {
    fetchUnreadCount();
    hasFetchedNotifications = false;
  });
});
