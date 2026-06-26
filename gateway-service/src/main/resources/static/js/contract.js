document.addEventListener("DOMContentLoaded", async function () {
  const params = new URLSearchParams(window.location.search);
  const transactionId = params.get("transactionId");

  // 1️⃣ Kiểm tra ID ngay từ đầu
  if (!transactionId) {
    await Swal.fire({
      icon: "error",
      title: "Lỗi đường dẫn",
      text: "Không tìm thấy mã giao dịch (transactionId)!",
      allowOutsideClick: false,
      confirmButtonText: "Quay lại trang chủ",
    }).then(() => {
        window.location.href = "/index.html"; // Hoặc trang nào đó phù hợp
    });
    return;
  }

  console.log("Đang lấy thông tin cho transactionId:", transactionId);

  // ============================================================
  // 2️⃣ Lấy dữ liệu thanh toán từ Transaction-Service
  // ============================================================
  let userId, productName, totalAmount;

  try {
    // Có thể hiện loading nhẹ khi tải thông tin ban đầu (tuỳ chọn)
    const res = await fetch(`/api/payments/info/${transactionId}`);
    
    if (!res.ok) throw new Error(`Lỗi khi gọi API, mã ${res.status}`);

    const data = await res.json();
    console.log("Dữ liệu nhận được từ backend:", data);

    // Lưu thông tin
    userId = data.userId;
    productName = data.productName;
    totalAmount = data.totalAmount;

    // === Hiển thị thông tin khách hàng ===
    document.getElementById("cName").innerText = data.fullName || "Không rõ";
    document.getElementById("cPhone").innerText = data.phone || "Không rõ";
    document.getElementById("cEmail").innerText = data.email || "Không rõ";
    document.getElementById("cAddress").innerText = data.address || "Không rõ";
    document.getElementById("cMethod").innerText = (data.method || "Khác").toUpperCase();

    // === Hiển thị thông tin sản phẩm ===
    document.getElementById("productName").innerText = data.productName || "Không có";
    document.getElementById("productPrice").innerText = data.price 
        ? `${Number(data.price).toLocaleString()} đ` : "0 đ";
    document.getElementById("totalPrice").innerText = data.totalAmount 
        ? `${Number(data.totalAmount).toLocaleString()} đ` : "0 đ";

    // === Ngày ký hợp đồng ===
    document.getElementById("signDate").innerText = new Date().toLocaleDateString("vi-VN");

    if (userId) localStorage.setItem("userId", userId);

  } catch (err) {
    console.error("Không thể load thông tin hợp đồng:", err);
    Swal.fire({
      icon: "error",
      title: "Không thể tải dữ liệu",
      text: "Không kết nối được với Transaction-Service. Vui lòng thử lại sau!",
    });
  }

  // ============================================================
  // 3️⃣ Xử lý chữ ký (canvas) - Giữ nguyên logic vẽ
  // ============================================================
  const canvas = document.getElementById("signCanvas");
  const ctx = canvas.getContext("2d");
  let drawing = false;

  const startDraw = (x, y) => {
    drawing = true;
    ctx.beginPath();
    ctx.moveTo(x, y);
  };
  const draw = (x, y) => {
    if (!drawing) return;
    ctx.lineWidth = 3;
    ctx.lineCap = "round";
    ctx.strokeStyle = "#000";
    ctx.lineTo(x, y);
    ctx.stroke();
  };
  const stopDraw = () => (drawing = false);

  // Chuột
  canvas.addEventListener("mousedown", e => startDraw(e.offsetX, e.offsetY));
  canvas.addEventListener("mousemove", e => draw(e.offsetX, e.offsetY));
  canvas.addEventListener("mouseup", stopDraw);
  canvas.addEventListener("mouseleave", stopDraw);

  // Cảm ứng (mobile)
  canvas.addEventListener("touchstart", e => {
    e.preventDefault();
    const t = e.touches[0];
    const rect = canvas.getBoundingClientRect();
    startDraw(t.clientX - rect.left, t.clientY - rect.top);
  });
  canvas.addEventListener("touchmove", e => {
    e.preventDefault();
    const t = e.touches[0];
    const rect = canvas.getBoundingClientRect();
    draw(t.clientX - rect.left, t.clientY - rect.top);
  });
  canvas.addEventListener("touchend", stopDraw);

  // Xóa chữ ký
  window.clearSign = () => ctx.clearRect(0, 0, canvas.width, canvas.height);

  // ============================================================
  // 4️⃣ Xuất PDF và GỬI hợp đồng (ĐÃ NÂNG CẤP UI)
  // ============================================================
  window.downloadContract = async function () {
    // Kiểm tra xem người dùng đã ký chưa (kiểm tra canvas trống không)
    const blank = document.createElement('canvas');
    blank.width = canvas.width;
    blank.height = canvas.height;
    if(canvas.toDataURL() === blank.toDataURL()) {
        Swal.fire({
            icon: 'warning',
            title: 'Chưa ký tên',
            text: 'Vui lòng ký tên vào khung trước khi xác nhận!',
            confirmButtonColor: '#f39c12'
        });
        return;
    }

    // 1. Hiện Loading Spinner ngay lập tức
    Swal.fire({
      title: 'Đang xử lý hợp đồng...',
      html: 'Đang tạo file PDF và lưu dữ liệu.<br>Vui lòng không tắt trình duyệt.',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    try {
      const { jsPDF } = window.jspdf;
      const page = document.querySelector("#contractPage");
      const PDF_SCALE = 3;

      // Ẩn viền canvas và nút xóa
      const signCanvas = document.querySelector("#signCanvas");
      const clearButton = document.querySelector(".signature button.ghost");
      const originalBorder = signCanvas.style.border;
      const originalDisplay = clearButton.style.display;
      signCanvas.style.border = "none";
      clearButton.style.display = "none";

      // Chụp hợp đồng
      const A4_WIDTH = 210;      // mm
      const A4_HEIGHT = 297;     // mm

      // Chụp trang
      const canvasPDF = await html2canvas(page, {
          scale: 2,      // scale thấp để tránh bị nén ngang
          useCORS: true,
          logging: false,
      });

      // Chuyển ảnh
      const imgData = canvasPDF.toDataURL("image/jpeg", 1.0);

      // Tính tỷ lệ khớp A4
      const imgWidthPx = canvasPDF.width;
      const imgHeightPx = canvasPDF.height;
      const ratio = imgHeightPx / imgWidthPx;

      const pdf = new jsPDF("p", "mm", "a4");

      const pdfWidth = A4_WIDTH;
      const pdfHeight = pdfWidth * ratio;

      // Nếu dài hơn 1 trang → chia trang tự động
      let heightLeft = pdfHeight;
      let position = 0;

      pdf.addImage(imgData, "JPEG", 0, position, pdfWidth, pdfHeight);
      heightLeft -= A4_HEIGHT;

      while (heightLeft > 1) {
          pdf.addPage();
          position = heightLeft - pdfHeight;
          pdf.addImage(imgData, "JPEG", 0, position, pdfWidth, pdfHeight);
          heightLeft -= A4_HEIGHT;
      }


      // Chuyển PDF sang base64
      const pdfBlob = pdf.output("blob");
      const pdfBase64 = await new Promise((resolve) => {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result.split(",")[1]);
        reader.readAsDataURL(pdfBlob);
      });

      // Khôi phục giao diện
      signCanvas.style.border = originalBorder;
      clearButton.style.display = originalDisplay;

      // Chuẩn bị dữ liệu gửi
      const payload = {
        transactionId,
        signature: canvas.toDataURL("image/png"),
        userId,
        productName,
        totalAmount,
        pdfBase64
      };

      console.log("Gửi payload đến Contract-Service...");

      // Gửi API
      const resp = await fetch("/api/contracts/sign", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const result = await resp.json();

      if (resp.ok) {
        // 2. Thành công -> Hiện thông báo chúc mừng
        await Swal.fire({
          icon: 'success',
          title: 'Ký hợp đồng thành công!',
          text: 'Hợp đồng của bạn đã được lưu trữ an toàn.',
          confirmButtonText: 'Xem lịch sử hợp đồng',
          confirmButtonColor: '#2ecc71'
        });

        const uid = userId || localStorage.getItem("userId");
        window.location.href = `/contract-history.html?userId=${uid}`;

      } else {
        throw new Error(result.message || "Lỗi không xác định từ server");
      }

    } catch (err) {
      console.error("Lỗi quy trình:", err);
      
      // 3. Thất bại -> Hiện thông báo lỗi
      Swal.fire({
        icon: 'error',
        title: 'Gửi thất bại',
        text: `Đã xảy ra lỗi: ${err.message}. Vui lòng thử lại!`,
        confirmButtonText: 'Đóng',
        confirmButtonColor: '#e74c3c'
      });
    }
  };
});
