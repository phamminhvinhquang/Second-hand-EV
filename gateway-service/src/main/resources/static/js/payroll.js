
document.addEventListener("DOMContentLoaded", async () => {
  const API_BASE = "/api/payroll";
  
  const staffTableBody = document.getElementById("staffTableBody"); 
  const autoPayBtn = document.getElementById("autoPayBtn");

  if (!staffTableBody) {
      console.error("Không tìm thấy phần tử #staffTableBody!");
      return;
  }

  // ======= 1️⃣ Tải danh sách nhân viên (GIỮ NGUYÊN) =======
  async function loadStaffs() {
    staffTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px;">Đang tải danh sách nhân viên...</td></tr>`;

    try {
      const res = await fetch(`${API_BASE}/staff`);
      if (!res.ok) throw new Error("Không thể tải dữ liệu nhân viên");
      const data = await res.json();

      if (data.length === 0) {
        staffTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px;">Không có nhân viên nào.</td></tr>`;
        return;
      }
      
      staffTableBody.innerHTML = data.map((s) => `
        <tr>
          <td><strong>#${s.userId}</strong></td>
          <td style="color: #2ecc71; font-weight: bold;">
            ${Number(s.salary || 0).toLocaleString("vi-VN")} đ
          </td>
          <td>Ngày ${s.payDay || "-"}</td>
          <td>
            <span class="status ${s.status === "ACTIVE" ? "status-active" : "status-paused"}" 
                  style="padding: 5px 10px; border-radius: 4px; background: ${s.status === 'ACTIVE' ? '#e8f5e9' : '#ffebee'}; color: ${s.status === 'ACTIVE' ? '#2e7d32' : '#c62828'};">
              ${s.status === "ACTIVE" ? "Hoạt động" : "Tạm dừng"}
            </span>
          </td>
          <td>${s.lastPaid ? new Date(s.lastPaid).toLocaleDateString('vi-VN') : "-"}</td>
          <td class="text-center">
            <button class="btn-secondary" style="cursor:pointer; margin-right:5px;" onclick="openEditModal(${s.userId}, ${s.salary || 0}, ${s.payDay || 1})">
              Sửa
            </button>
            <button class="btn-primary" style="cursor:pointer;" onclick="payNow(${s.userId}, ${s.salary || 0})">
              Trả
            </button>
          </td>
        </tr>
      `).join("");

    } catch (err) {
      console.error(err);
      staffTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:red; padding:20px;">❌ Lỗi kết nối server</td></tr>`;
    }
  }

  // ======= 2️⃣ Chạy auto payroll (ĐÃ SỬA LOGIC KIỂM TRA LỖI) =======
  if (autoPayBtn) {
    autoPayBtn.addEventListener("click", async () => {
      const confirm = await Swal.fire({
        title: 'Xác nhận chạy tự động?',
        text: "Hệ thống sẽ quét và trả lương cho tất cả nhân viên đến hạn.",
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Chạy ngay',
        cancelButtonText: 'Hủy bỏ'
      });

      if (!confirm.isConfirmed) return;

      Swal.fire({
        title: 'Đang xử lý...',
        html: 'Vui lòng không tắt trình duyệt.',
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
      });

      try {
        const res = await fetch(`${API_BASE}/run-auto`, { method: "POST" });
        const msg = await res.text(); // Lấy nội dung tin nhắn

        // 🛠 SỬA Ở ĐÂY: Kiểm tra xem tin nhắn có chứa chữ "FAILED" hoặc lỗi không
        if (res.ok && !msg.includes("FAILED") && !msg.includes("Exception")) {
            await Swal.fire('Thành công', msg, 'success');
            loadStaffs();
        } else {
            // Nếu có chữ FAILED thì ném ra lỗi
            throw new Error(msg || "Có lỗi xảy ra khi chạy tự động");
        }
      } catch (err) {
        Swal.fire('Thất bại', err.message || 'Không thể chạy tự động!', 'error');
      }
    });
  }

  // ======= 3️⃣ Modal chỉnh sửa (GIỮ NGUYÊN) =======
  const modal = document.getElementById("editModal");
  
  window.openEditModal = (userId, salary, payDay) => {
    document.getElementById("salaryInput").value = salary;
    document.getElementById("payDayInput").value = payDay;
    if (modal) modal.style.display = "flex";
    window.currentUserId = userId;
  };

  const closeModal = () => {
    if (modal) modal.style.display = "none";
    window.currentUserId = null;
  };

  document.getElementById("closeModal")?.addEventListener("click", closeModal);
  document.getElementById("cancelEdit")?.addEventListener("click", closeModal);

  const editForm = document.getElementById("editForm");
  if (editForm) {
    editForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const salary = parseFloat(document.getElementById("salaryInput").value);
      const payDay = parseInt(document.getElementById("payDayInput").value);
      const status = "ACTIVE"; 

      if (!window.currentUserId) return;

      Swal.fire({
        title: 'Đang cập nhật...',
        didOpen: () => Swal.showLoading()
      });

      try {
        const res = await fetch(`${API_BASE}/staff/${window.currentUserId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ salary, payDay, status }),
        });

        if (res.ok) {
          await Swal.fire({
            icon: 'success',
            title: 'Cập nhật thành công',
            timer: 1500,
            showConfirmButton: false
          });
          closeModal();
          loadStaffs();
        } else {
            throw new Error("Cập nhật thất bại");
        }
      } catch (err) {
        Swal.fire('Lỗi', 'Không thể kết nối server!', 'error');
      }
    });
  }

  // ======= 4️⃣ Trả lương thủ công (ĐÃ SỬA LOGIC KIỂM TRA LỖI) =======
  window.payNow = async (userId, amount) => {
    const confirm = await Swal.fire({
      title: 'Xác nhận trả lương?',
      text: `Bạn sắp trả ${amount.toLocaleString("vi-VN")}đ cho nhân viên #${userId}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Xác nhận trả',
      confirmButtonColor: '#2ecc71',
      cancelButtonText: 'Hủy'
    });

    if (!confirm.isConfirmed) return;

    Swal.fire({
      title: 'Đang thực hiện giao dịch...',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    try {
      const res = await fetch(`${API_BASE}/run`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId,
          amount,
          periodLabel: new Date().toISOString().slice(0, 7), 
        }),
      });
      
      const msg = await res.text(); // Lấy nội dung phản hồi

      // 🛠 SỬA Ở ĐÂY: Kiểm tra res.ok VÀ nội dung tin nhắn không chứa FAILED
      if (res.ok && !msg.includes("FAILED") && !msg.includes("Exception")) {
        await Swal.fire('Thành công', msg, 'success');
        loadStaffs();
      } else {
        // Nếu server trả về FAILED, ném lỗi để nhảy xuống catch
        throw new Error(msg || "Giao dịch thất bại");
      }
    } catch (err) {
      // Lúc này popup sẽ hiện icon Error (dấu X đỏ) và tiêu đề Thất bại
      Swal.fire('Thất bại', err.message || 'Lỗi khi trả lương!', 'error');
    }
  };

  // ======= 5️⃣ Gọi lần đầu =======
  loadStaffs();
});
