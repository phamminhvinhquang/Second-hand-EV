document.addEventListener("DOMContentLoaded", function () {
  // Khởi tạo các thư viện giao diện nếu có
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  const confirmBtn = document.getElementById("confirmOnlinePay");

  if (!confirmBtn) return; // Tránh lỗi nếu script chạy sai trang

  confirmBtn.addEventListener("click", async function () {
    // 1️⃣ Lấy dữ liệu từ localStorage
    const cartIdsStr = localStorage.getItem("cartIds");
    const totalStr = localStorage.getItem("total");
    const name = localStorage.getItem("cName");
    const phone = localStorage.getItem("cPhone");
    const email = localStorage.getItem("cEmail");
    const address = localStorage.getItem("cAddress");
    const userId = localStorage.getItem("userId");
    
    // Lấy phương thức thanh toán đang được chọn (Radio button)
    const selectedMethod = document.querySelector('input[name="method"]:checked');

    // 2️⃣ Validate dữ liệu (Kiểm tra kỹ hơn)
    if (!cartIdsStr || !name || !phone || !email || !address) {
      await Swal.fire({
        icon: "error",
        title: "Thiếu thông tin",
        text: "Dữ liệu giỏ hàng hoặc khách hàng bị thiếu. Vui lòng thử lại!",
        confirmButtonText: "Quay lại trang trước"
      });
      window.location.href = "payment.html";
      return;
    }

    if (!userId) {
      await Swal.fire({
        icon: "warning",
        title: "Chưa đăng nhập",
        text: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
        confirmButtonText: "Đến trang đăng nhập"
      });
      window.location.href = "/login.html";
      return;
    }

    if (!selectedMethod) {
      Swal.fire({
        icon: "info",
        title: "Chưa chọn phương thức",
        text: "Vui lòng chọn một cổng thanh toán (VNPAY / MOMO)!",
      });
      return;
    }

    // Chuẩn bị dữ liệu
    const cartIds = cartIdsStr.split(",").map(id => parseInt(id.trim()));
    const totalAmount = parseFloat(totalStr) || 0;
    const method = selectedMethod.value.toLowerCase();

    // 3️⃣ Hiển thị Loading (Chặn người dùng click lung tung)
    Swal.fire({
      title: `Đang kết nối tới ${method.toUpperCase()}...`,
      html: "Vui lòng không tắt trình duyệt.",
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    }); 
    console.log("CART IDS SEND:", cartIds);
    console.log("TOTAL:", totalAmount);
    try {
      // 4️⃣ Gọi API
      const res = await fetch("/api/payments/create", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type: "order",
          cartIds: cartIds,
          totalAmount: totalAmount,
          paymentMethod: method,
          userId: parseInt(userId),
          customer: {
            fullName: name,
            phone: phone,
            email: email,
            address: address
          }
        })
      });

      if (!res.ok) throw new Error(`Server error: ${res.status}`);

      const data = await res.json();
      console.log("Backend trả về:", data);

      // 5️⃣ Xử lý kết quả
      if (data && data.redirectUrl) {
        // Lưu transactionId để dùng cho trang kết quả sau này
        localStorage.setItem("transactionId", data.transactionId);

        // Thông báo thành công nhanh và chuyển hướng
        await Swal.fire({
          icon: "success",
          title: "Tạo đơn hàng thành công!",
          text: "Đang chuyển hướng sang trang thanh toán...",
          timer: 1500,
          showConfirmButton: false
        });

        window.location.href = data.redirectUrl;
      } else {
        throw new Error("API không trả về đường dẫn thanh toán (redirectUrl)");
      }

    } catch (error) {
      console.error("Lỗi khi tạo giao dịch:", error);
      
      Swal.fire({
        icon: "error",
        title: "Thanh toán thất bại",
        text: "Không thể kết nối đến máy chủ thanh toán. Vui lòng thử lại sau!",
        confirmButtonText: "Đóng"
      });
    }
  });
});
