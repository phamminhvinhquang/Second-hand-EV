// === JS cho trang thanh toán (tích hợp Spring Boot + Cart + Ví EV + SweetAlert2) ===

document.addEventListener("DOMContentLoaded", async function () {
  // Khởi tạo các thư viện UI nếu có
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  const form = document.getElementById("paymentForm");
  const orderSummary = document.querySelector(".order-summary");
  const PAYMENT_API = "/api/payments/create";

  // ====== Lấy cartIds & tổng tiền từ URL ======
  const urlParams = new URLSearchParams(window.location.search);
  const cartIdsParam = urlParams.get("cartIds");
  const totalParam = urlParams.get("total") || "0";

  const cartIds = cartIdsParam
    ? cartIdsParam.split(",").map((id) => id.trim()).filter(Boolean)
    : [];

  // Xử lý nếu không có sản phẩm
  if (cartIds.length === 0) {
    orderSummary.innerHTML = `<p class="text-red-600">Không có sản phẩm nào được chọn để thanh toán!</p>`;
    Swal.fire({
      icon: "warning",
      title: "Giỏ hàng trống",
      text: "Bạn chưa chọn sản phẩm nào để thanh toán.",
      confirmButtonText: "Quay lại mua sắm",
    }).then(() => {
      // Tùy chọn: chuyển hướng về trang chủ nếu muốn
      // window.location.href = "index.html";
    });
    return;
  }

  // ====== Hiển thị danh sách sản phẩm trong giỏ ======
  let itemsHtml = "";
  try {
    // Có thể thêm loading nhỏ ở khu vực này nếu muốn, nhưng ở đây ta render trực tiếp
    for (const id of cartIds) {
      const res = await fetch(`/api/carts/${id}`);

      if (!res.ok) continue;
      const cart = await res.json();

      itemsHtml += `
        <div class="flex justify-between text-gray-700">
          <span>${cart.productName || cart.productname}</span>
          <span class="font-bold text-green-600">
            ${Number(cart.price).toLocaleString("vi-VN")} đ
          </span>
        </div>
      `;
    }

    const totalClean = totalParam.replace(/[^\d]/g, "");
    const totalFormatted = Number(totalClean).toLocaleString("vi-VN") + " đ";

    orderSummary.innerHTML = `
      <h3 class="font-semibold text-lg mb-2">Thông tin đơn hàng</h3>
      <div class="space-y-2">${itemsHtml}</div>
      <div class="flex justify-between text-lg font-bold mt-4 border-t pt-3">
        <span>Tổng cộng</span>
        <span class="text-green-600">${totalFormatted}</span>
      </div>
    `;
  } catch (err) {
    console.error("Lỗi khi tải giỏ hàng:", err);
    orderSummary.innerHTML = `<p class="text-red-600">Không thể tải dữ liệu giỏ hàng.</p>`;
    Swal.fire({
      icon: "error",
      title: "Lỗi hệ thống",
      text: "Không thể tải thông tin giỏ hàng. Vui lòng thử lại sau.",
    });
  }

  // ====== Khi người dùng nhấn "Xác nhận thanh toán" ======
  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();
    const address = document.getElementById("address").value.trim();
    const methodEl = document.querySelector("input[name='payment']:checked");

    // Validate Form
    if (!name || !phone || !email || !address) {
      Swal.fire({
        icon: "warning",
        title: "Thiếu thông tin",
        text: "Vui lòng nhập đầy đủ thông tin khách hàng!",
        confirmButtonColor: "#f59e0b", // Màu vàng cam cảnh báo
      });
      return;
    }

    if (!methodEl) {
      Swal.fire({
        icon: "warning",
        title: "Chưa chọn phương thức",
        text: "Vui lòng chọn phương thức thanh toán!",
        confirmButtonColor: "#f59e0b",
      });
      return;
    }

    const method = methodEl.value.toLowerCase();
    const totalAmount = Number(totalParam.replace(/[^\d]/g, ""));
    const user = JSON.parse(localStorage.getItem("user"));
    const userId = user?.userId || user?.id;

    // Kiểm tra đăng nhập
    if (!userId) {
      Swal.fire({
        icon: "error",
        title: "Chưa đăng nhập",
        text: "Bạn cần đăng nhập trước khi thanh toán!",
        footer: '<a href="login.html">Đến trang đăng nhập ngay</a>',
        showConfirmButton: false,
        timer: 2000,
      }).then(() => {
        window.location.href = "login.html";
      });
      return;
    }

    // Lưu thông tin local storage
    localStorage.setItem("cartIds", cartIds.join(","));
    localStorage.setItem("cName", name);
    localStorage.setItem("cPhone", phone);
    localStorage.setItem("cEmail", email);
    localStorage.setItem("cAddress", address);
    localStorage.setItem("cMethod", method);
    localStorage.setItem("total", totalAmount);

    // ====== 1️⃣ Nếu chọn Ví EV ======
    if (method === "ev-wallet") {
      // 1. Hiển thị Loading (Giữ nguyên)
      Swal.fire({
        title: "Đang xử lý thanh toán...",
        text: "Vui lòng không tắt trình duyệt.",
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false, // Ẩn nút OK đi
        didOpen: () => {
          Swal.showLoading();
        },
      });

      try {
        const payRes = await fetch(PAYMENT_API, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            cartIds,
            totalAmount,
            paymentMethod: "evwallet",
            customer: { fullName: name, phone, email, address },
            type: "order",
            userId,
            amount: totalAmount,
          }),
        });

        // Kiểm tra response HTTP
        if (!payRes.ok) throw new Error("Lỗi kết nối API");
        
        const payData = await payRes.json();

        if (payData.status === "SUCCESS") {
          // === THÀNH CÔNG ===
          // KHÔNG gọi Swal.fire thông báo thành công.
          // KHÔNG gọi Swal.close().
          // Chuyển trang ngay lập tức. Màn hình Loading sẽ xoay cho đến khi trang mới tải xong.
          window.location.href = payData.redirectUrl;
          
        } else {
          // === THẤT BẠI (Lỗi nghiệp vụ: hết tiền, lỗi ví...) ===
          // Phải tắt Loading để người dùng không bị treo màn hình
          Swal.close();
          console.error("Lỗi thanh toán:", payData);
          // (Tùy chọn: Hiển thị dòng text đỏ nhỏ dưới nút submit nếu cần)
        }
      } catch (err) {
        // === LỖI MẠNG / CODE ===
        // Phải tắt Loading
        Swal.close();
        console.error("Lỗi try-catch:", err);
      }
      return;
    }

    // ====== 2️⃣ Thanh toán online ======
    if (method === "online") {
      Swal.fire({
        icon: "info",
        title: "Đang chuyển hướng",
        text: "Chuyển sang cổng thanh toán trực tuyến...",
        timer: 1500,
        timerProgressBar: true,
        showConfirmButton: false,
        didOpen: () => {
          Swal.showLoading();
        },
      }).then(() => {
        window.location.href = `online_payment.html?cartIds=${cartIds.join(",")}&total=${encodeURIComponent(totalParam)}`;
      });
      return;
    }
  });
});
