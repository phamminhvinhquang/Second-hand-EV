/**
 * ===================================================================
 * FILE NÀY LÀ BẢN HOÀN CHỈNH (ĐÃ SỬA LỖI ẢNH VÀ LỖI 2 THÔNG BÁO)
 *
 * 1. Đặt file này ở THƯ MỤC GỐC
 * 2. Đọc "title", "body" từ "payload.data"
 * 3. Thêm "await fetch(image)" để tải trước ảnh
 * ===================================================================
 */

// Import script của Firebase
importScripts("https://www.gstatic.com/firebasejs/8.10.0/firebase-app.js");
importScripts(
  "https://www.gstatic.com/firebasejs/8.10.0/firebase-messaging.js"
);

// -----------------------------------------------------------
//  Config Firebase của bạn
// -----------------------------------------------------------
const firebaseConfig = {
  apiKey: "AIzaSyDYflGsxA3-wABQMM4oQAkagBACWptTROw",
  authDomain: "second-hand-ev-battery.firebaseapp.com",
  projectId: "second-hand-ev-battery",
  storageBucket: "second-hand-ev-battery.firebasestorage.app",
  messagingSenderId: "732419381903",
  appId: "1:732419381903:web:bd8ed62b62c79c6939aefa",
  measurementId: "G-FZ3Q0G3GPZ",
};

// Khởi tạo app
if (firebase.apps.length === 0) {
  firebase.initializeApp(firebaseConfig);
}

const messaging = firebase.messaging();

console.log("Firebase SW đã được tải.");

// -----------------------------------------------------------
// HÀM 1: XỬ LÝ KHI NHẬN THÔNG BÁO (BẢN HOÀN CHỈNH)
// -----------------------------------------------------------
messaging.onBackgroundMessage(async function (payload) {
  // 1. Thêm "async"
  console.log("[firebase-messaging-sw.js] Đã nhận thông báo nền: ", payload);

  // 2. Lấy dữ liệu cơ bản (ĐỌC TỪ DATA)
  const notificationTitle = payload.data.title; // <-- SỬA LỖI
  const notificationOptions = {
    body: payload.data.body, // <-- SỬA LỖI
    icon: "/images/logo.png", // (Logo chính)

    // 3. Lấy dữ liệu link
    data: {
      url: payload.data.link,
    },

    // 4. Lấy dữ liệu tùy chỉnh
    image: payload.data.image, // <-- Ảnh banner
    badge: payload.data.badge, // <-- Logo nhỏ (trên thanh status)

    // 5. Thêm nút bấm
    actions: [
      { action: "open_link", title: "Xem ngay" },
      { action: "dismiss", title: "Đóng" },
    ],
  };

  // 6. [SỬA LỖI ẢNH CHẬM] Tải trước ảnh banner
  if (payload.data.image) {
    try {
      // Lệnh 'await fetch' sẽ đợi cho đến khi ảnh được tải xong
      await fetch(payload.data.image);
      console.log("Đã tải trước ảnh thành công:", payload.data.image);
    } catch (err) {
      console.error("Lỗi khi tải trước ảnh:", err);
      // Nếu lỗi, vẫn hiển thị thông báo nhưng không có ảnh
      delete notificationOptions.image;
    }
  }

  // 7. Hiển thị thông báo (SAU KHI đã tải trước ảnh)
  return self.registration.showNotification(
    notificationTitle,
    notificationOptions
  );
});

// -----------------------------------------------------------
// ✅ HÀM 2: XỬ LÝ KHI CLICK (GIỮ NGUYÊN)
// -----------------------------------------------------------
self.addEventListener("notificationclick", function (event) {
  console.log("Đã nhấn vào thông báo: ", event.notification);

  // Lấy link chính
  const urlToOpen = event.notification.data.url;

  // Đóng thông báo
  event.notification.close();

  // XỬ LÝ NÚT BẤM
  if (event.action === "dismiss") {
    console.log('Người dùng nhấn "Đóng"');
    return;
  }

  // Nếu nhấn vào "Xem ngay" (open_link) HOẶC thân thông báo (!event.action)
  if (event.action === "open_link" || !event.action) {
    if (urlToOpen) {
      event.waitUntil(
        clients.matchAll({ type: "window" }).then(function (clientList) {
          // (Nâng cao: Nếu tab đó đã mở, focus vào nó)
          for (var i = 0; i < clientList.length; i++) {
            var client = clientList[i];
            if (client.url === urlToOpen && "focus" in client) {
              return client.focus();
            }
          }

          // (Cơ bản: Mở tab mới)
          if (clients.openWindow) {
            return clients.openWindow(urlToOpen);
          }
        })
      );
    }
  }
});
