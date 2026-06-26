// File: /js/chat-badge.js

// Cấu hình URL (Đảm bảo khớp với backend của bạn)
const CHAT_SERVICE_URL_BADGE = "";
const currentUserIdBadge = localStorage.getItem("userId"); // Lấy ID user đăng nhập

// THAY ĐỔI DUY NHẤT: Đổi từ DOMContentLoaded sang componentsLoaded
document.addEventListener("componentsLoaded", () => {
  if (currentUserIdBadge) {
    // 1. Lấy số lượng tin chưa đọc ban đầu
    fetchUnreadCount();

    // 2. Kết nối WebSocket để nghe tin nhắn mới (Realtime)
    connectBadgeWebSocket();
  }
});

function fetchUnreadCount() {
  fetch(`${CHAT_SERVICE_URL_BADGE}/api/chat/unread-count/${currentUserIdBadge}`)
    .then((res) => res.json())
    .then((count) => {
      updateBadgeUI(count);
    })
    .catch((err) => console.error("Lỗi lấy số tin nhắn:", err));
}

function updateBadgeUI(count) {
  const badge = document.getElementById("msgBadge");
  if (!badge) return;

  badge.setAttribute("data-count", count);

  if (count <= 0) {
    // Dùng class mới: chat-badge-hidden
    badge.classList.add("chat-badge-hidden");
    badge.innerText = "0";
  } else {
    // Dùng class mới: chat-badge-hidden
    badge.classList.remove("chat-badge-hidden");

    if (count > 99) {
      badge.innerText = "99+";
    } else {
      badge.innerText = count;
    }
  }
}

// Kết nối WebSocket chỉ để cập nhật Badge (Nhẹ hơn logic chat full)
function connectBadgeWebSocket() {
  // Sử dụng thư viện SockJS và Stomp đã có trong index.html
  const socket = new SockJS(
    `${CHAT_SERVICE_URL_BADGE}/ws-chat/ws?userId=${currentUserIdBadge}`
  );
  const stompClient = Stomp.over(socket);

  // Tắt debug log cho đỡ rối console
  stompClient.debug = null;

  stompClient.connect(
    {},
    function (frame) {
      console.log("🔵 Chat Badge Connected");

      // Đăng ký lắng nghe tin nhắn cá nhân
      stompClient.subscribe(
        `/queue/messages/${currentUserIdBadge}`,
        function (messageOutput) {
          const msg = JSON.parse(messageOutput.body);

          // 1. [CŨ] Xử lý tin nhắn MỚI (Tăng count)
          if (
            msg.senderId != currentUserIdBadge &&
            msg.type !== "READ_RECEIPT" &&
            msg.type !== "RECALL"
          ) {
            console.log("Badge: Nhận tin nhắn mới, TĂNG count");
            // Tăng số lượng lên 1
            const badge = document.getElementById("msgBadge");
            let currentCount = parseInt(
              badge.getAttribute("data-count") || "0"
            );
            updateBadgeUI(currentCount + 1);

            // Tùy chọn: Phát âm thanh thông báo nhỏ
            // playNotificationSound();

            // 2. [MỚI] Xử lý sự kiện ĐÃ ĐỌC (Fetch lại count)
          } else if (msg.type === "READ_RECEIPT") {
            console.log("Badge: Nhận tin đã đọc, GỌI API fetch count");
            // Khi nhận được thông báo "đã đọc" từ 1 tab/trang khác
            // Cách an toàn nhất là gọi lại API để lấy số lượng chính xác
            fetchUnreadCount();
          }

          // Các loại tin nhắn khác (RECALL,...) có thể xử lý ở đây nếu cần
        }
      );
    },
    function (error) {
      console.log("Lỗi kết nối Badge socket, thử lại sau 5s...");
      setTimeout(connectBadgeWebSocket, 5000);
    }
  );
}
