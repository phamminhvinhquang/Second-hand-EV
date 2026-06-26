// ============================================================
// ✅ deposit.js - Đã nâng cấp giao diện với SweetAlert2
// ============================================================

document.addEventListener("DOMContentLoaded", async () => {
  const API_BASE = "/api/payments"; // transaction-service

  // ============================================================
  // 🧠 1️⃣ Lấy user hiện tại (Logic đồng bộ localStorage/window.name)
  // ============================================================

  let currentUser = null;

  try {
    // Ưu tiên lấy từ localStorage
    const stored = localStorage.getItem("user");

    if (stored) {
      currentUser = JSON.parse(stored);
    } else if (window.name && window.name.startsWith("{")) {
      // Đồng bộ lại localStorage nếu window.name có dữ liệu
      try {
        const parsed = JSON.parse(window.name);
        currentUser = parsed;
        localStorage.setItem("user", window.name);
        if (parsed.userId || parsed.id)
          localStorage.setItem("userId", parsed.userId || parsed.id);
        console.log("[deposit.js] Đồng bộ user từ window.name:", parsed);
      } catch (err) {
        console.warn("[deposit.js] window.name không hợp lệ, reset:", err);
        window.name = "";
      }
    }
  } catch (err) {
    console.warn("[deposit.js] Không thể đọc user:", err);
  }

  // ============================================================
  // 👤 2️⃣ Xác định userId hiện tại
  // ============================================================

  const USER_ID = Number(
    currentUser?.userId ||
    currentUser?.id ||
    localStorage.getItem("userId")
  );

  console.log("👤 [deposit.js] USER_ID hiện tại:", USER_ID);

  // ============================================================
  // 🚫 3️⃣ Kiểm tra đăng nhập (Dùng SweetAlert thay alert)
  // ============================================================

  if (!USER_ID || isNaN(USER_ID) || USER_ID <= 0) {
    await Swal.fire({
      icon: 'warning',
      title: 'Yêu cầu đăng nhập',
      text: 'Bạn cần đăng nhập để thực hiện nạp tiền.',
      confirmButtonText: 'Đăng nhập ngay',
      confirmButtonColor: '#3085d6',
      allowOutsideClick: false
    });
    window.location.href = "login.html";
    return; // Dừng script
  }

  // ============================================================
  // 💳 4️⃣ Bắt sự kiện form submit
  // ============================================================

  const depositForm = document.getElementById("depositForm");
  if (!depositForm) return; // Tránh lỗi nếu không tìm thấy form

  depositForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const amountInput = document.getElementById("amount");
    const methodInput = document.getElementById("method");
    const btn = document.getElementById("submitBtn");
    
    // Reset thông báo cũ (nếu có dùng thẻ message)
    const msg = document.getElementById("message");
    if(msg) msg.textContent = "";

    const amountVal = amountInput.value.trim();
    const methodVal = methodInput.value.trim();

    // === Validate Dữ liệu đầu vào ===
    if (!amountVal || isNaN(amountVal) || parseFloat(amountVal) < 1000) {
      Swal.fire({
        icon: 'error',
        title: 'Số tiền không hợp lệ',
        text: 'Vui lòng nhập số tiền tối thiểu 1.000đ.',
      });
      return;
    }

    if (!methodVal) {
      Swal.fire({
        icon: 'info',
        title: 'Chưa chọn phương thức',
        text: 'Vui lòng chọn phương thức thanh toán (VNPay/MoMo).',
      });
      return;
    }

    // === Hiển thị Loading Spinner ===
    // Khóa nút bấm để tránh click nhiều lần
    btn.disabled = true; 
    
    Swal.fire({
      title: 'Đang tạo giao dịch...',
      html: 'Vui lòng đợi trong giây lát để kết nối cổng thanh toán.',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    try {
      const payload = {
        type: "deposit",              // 🧩 loại giao dịch
        userId: USER_ID,              // 🧩 id người nạp
        amount: parseFloat(amountVal), // 🧩 số tiền
        paymentMethod: methodVal       // 🧩 phương thức
      };

      console.log("[deposit.js] Gửi yêu cầu nạp tiền:", payload);

      const res = await fetch(`${API_BASE}/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const errText = await res.text(); // Đọc lỗi từ server nếu có
        throw new Error(`Lỗi máy chủ (${res.status}): ${errText}`);
      }

      const data = await res.json();
      console.log("[deposit.js] Phản hồi từ backend:", data);

      if (data.redirectUrl) {
        // Thành công -> Chuyển hướng
        await Swal.fire({
          icon: 'success',
          title: 'Tạo đơn thành công!',
          text: 'Đang chuyển hướng tới cổng thanh toán...',
          timer: 1500,
          showConfirmButton: false
        });
        
        window.location.href = data.redirectUrl;
      } else {
        throw new Error("Server không trả về đường dẫn thanh toán.");
      }

    } catch (err) {
      console.error("[deposit.js] Lỗi:", err);
      
      // Hiển thị lỗi chi tiết
      Swal.fire({
        icon: 'error',
        title: 'Giao dịch thất bại',
        text: err.message || 'Đã xảy ra lỗi không xác định. Vui lòng thử lại.',
        confirmButtonText: 'Đóng',
        confirmButtonColor: '#e74c3c'
      });

      // Mở lại nút bấm
      btn.disabled = false;
    }
  });
});
