document.addEventListener("DOMContentLoaded", async () => {
  const userId =
    localStorage.getItem("userId") ||
    new URLSearchParams(window.location.search).get("userId");

  // 1. Thay alert bằng SweetAlert2
  if (!userId) {
    await Swal.fire({
      icon: 'warning',
      title: 'Chưa đăng nhập',
      text: 'Bạn cần đăng nhập để xem lịch sử hợp đồng.',
      confirmButtonText: 'Đăng nhập ngay',
      confirmButtonColor: '#3085d6',
      allowOutsideClick: false, // Không cho bấm ra ngoài để đóng
      allowEscapeKey: false
    });
    
    window.location.href = "/login.html";
    return;
  }

  console.log("Đang tải danh sách hợp đồng của userId:", userId);
  const tbody = document.querySelector("#historyTable tbody");

  // Hiển thị loading dạng skeleton hoặc text tạm thời
  tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px;">Đang tải dữ liệu...</td></tr>`;

  try {
    const res = await fetch(`/api/contracts/user/${userId}`);
    
    if (!res.ok) {
        throw new Error(`Lỗi khi gọi API: ${res.status}`);
    }

    const data = await res.json();

    if (data.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 20px;">Chưa có hợp đồng nào.</td></tr>`;
      return;
    }

    // Render dữ liệu
    tbody.innerHTML = data
      .map(
        (c) => `
        <tr>
          <td><strong>#${c.id}</strong></td>
          <td>${c.productName || "<span class='text-muted'>(Không có dữ liệu)</span>"}</td>
          <td style="color: #2ecc71; font-weight: bold;">
            ${c.totalPrice ? Number(c.totalPrice).toLocaleString("vi-VN") + " đ" : "-"}
          </td>
          <td>${c.signedAt ? new Date(c.signedAt).toLocaleDateString("vi-VN") : "-"}</td>
          <td>
            ${
              c.pdfUrl
                ? `<button class="btn-view" onclick="openPdf('${c.pdfUrl}')" style="cursor:pointer; color:blue; text-decoration:underline; border:none; background:none;">
                    Xem PDF
                   </button>`
                : `<span class="text-muted" style="color: gray; font-style: italic;">Chưa có</span>`
            }
          </td>
        </tr>`
      )
      .join("");
      
      // (Tùy chọn) Thông báo nhỏ góc màn hình khi tải thành công
      const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000
      });
      Toast.fire({
        icon: 'success',
        title: 'Đã tải dữ liệu thành công'
      });

  } catch (err) {
    console.error("Lỗi khi tải lịch sử hợp đồng:", err);
    
    // Hiển thị lỗi đẹp hơn trên bảng
    tbody.innerHTML = `<tr><td colspan="5" style="color: #e74c3c; text-align:center; padding: 20px;">Không thể tải dữ liệu từ server. Vui lòng thử lại sau.</td></tr>`;
    
    // Hiển thị popup báo lỗi
    Swal.fire({
        icon: 'error',
        title: 'Đã xảy ra lỗi',
        text: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại đường truyền.',
        confirmButtonText: 'Đóng',
    });
  }
});

// ✅ Hàm mở PDF
function openPdf(url) {
  console.log("Opening PDF:", url);
  // Có thể thêm xác nhận trước khi mở nếu muốn
  window.open(url, "_blank", "noopener");
}
