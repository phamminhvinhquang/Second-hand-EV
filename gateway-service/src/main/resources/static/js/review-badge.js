// File: /js/review-badge.js

// THAY ĐỔI DUY NHẤT: Đổi từ DOMContentLoaded sang componentsLoaded
document.addEventListener("componentsLoaded", () => {
  // CẤU HÌNH
  const REVIEW_SERVICE_URL = "/api/reviews"; // URL của Gateway
  const WS_URL = "/ws-review/ws"; // WebSocket qua Gateway

  const reviewBadge = document.getElementById("reviewBadge");
  let stompClient = null;
  let currentCount = 0;

  // 1. Lấy thông tin Auth
  function getAuthInfo() {
    const token = localStorage.getItem("token");
    const userString = localStorage.getItem("user");
    if (!token || !userString) return null;
    try {
      const user = JSON.parse(userString);
      return { token, userId: user.id || user.userId };
    } catch (e) {
      return null;
    }
  }

  const auth = getAuthInfo();
  if (!auth) return; // Chưa đăng nhập thì không chạy

  // 2. Hàm cập nhật giao diện Badge
  function updateBadgeUI(count) {
    currentCount = count;
    if (count <= 0) {
      reviewBadge.textContent = "0";
      reviewBadge.classList.add("hidden");
    } else {
      reviewBadge.classList.remove("hidden");
      reviewBadge.textContent = count > 99 ? "99+" : count;
    }
  }

  // 3. Gọi API lấy số lượng ban đầu
  async function fetchInitialCount() {
    try {
      // Giả sử bạn có API đếm: GET /api/reviews/tasks/count-pending
      // Nếu chưa có, bạn cần thêm endpoint này vào Controller Java
      const response = await fetch(
        `${REVIEW_SERVICE_URL}/tasks/count-pending`,
        {
          headers: {
            Authorization: `Bearer ${auth.token}`,
            "X-User-Id": auth.userId,
          },
        }
      );

      if (response.ok) {
        const count = await response.json();
        updateBadgeUI(count);
      }
    } catch (error) {
      console.error("Lỗi tải số lượng đánh giá:", error);
    }
  }

  // 4. Kết nối WebSocket (Realtime) - ĐÃ SỬA
  function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    // SỬA: Thêm Header xác thực vào đây
    const headers = {
      Authorization: `Bearer ${auth.token}`,
    };

    stompClient.connect(
      headers, // <-- Gửi kèm headers
      (frame) => {
        console.log("Review Badge connected WS");

        stompClient.subscribe(
          `/topic/user/${auth.userId}/review-tasks/new`,
          (payload) => {
            console.log("Nhận tín hiệu: Có nhiệm vụ đánh giá mới");
            // Tăng số đếm lên 1
            updateBadgeUI(currentCount + 1);
          }
        );

        stompClient.subscribe(
          `/topic/user/${auth.userId}/review-tasks/completed`,
          (payload) => {
            console.log("Nhận tín hiệu: Đã đánh giá xong");
            // Giảm số đếm đi 1
            updateBadgeUI(Math.max(0, currentCount - 1));
          }
        );
      },
      (error) => {
        console.warn("Lỗi WS Review Badge:", error);
        // Thử lại sau 5s nếu lỗi
        setTimeout(connectWebSocket, 5000);
      }
    );
  }

  // Khởi chạy
  fetchInitialCount();
  connectWebSocket();
});
