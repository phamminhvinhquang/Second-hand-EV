document.addEventListener("DOMContentLoaded", async () => {
  // Khởi tạo thư viện giao diện (nếu có)
  if (window.lucide) lucide.createIcons();
  if (window.AOS) AOS.init({ duration: 800, once: true });

  const API_BASE = "/api/wallet";
  
  // Element DOM
  const balanceEl = document.getElementById("platformBalance");
  const platformTableBody = document.querySelector("#platformTxTable tbody") || document.getElementById("platformTxTable");
  const userTableBody = document.querySelector("#userTxTable tbody") || document.getElementById("userTxTable");

  // --- CẤU HÌNH UTILS ---

  // Helper: Format tiền tệ
  const formatMoney = (amount) => {
    return Number(amount).toLocaleString("vi-VN") + " đ";
  };

  // Helper: Format ngày tháng (Giữ logic tính múi giờ của bạn nhưng viết gọn hơn)
  function formatVNTime(dateString) {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      // Nếu server trả về UTC chuẩn (có chữ Z hoặc +00:00), ta dùng toLocaleString
      // Nếu server trả về LocalTime server mà không có timezone, logic cũ của bạn cần thiết.
      // Ở đây tôi dùng Intl chuẩn, nó tự động chuyển về múi giờ trình duyệt (VN)
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
        hour12: false
      }).format(date);
    } catch (e) {
      return dateString; // Fallback nếu lỗi
    }
  }

  // Helper: Render Badge trạng thái giao dịch
  const renderTxType = (type, amount) => {
    const isCredit = type === 'CREDIT';
    const colorClass = isCredit ? 'text-green-600' : 'text-red-600';
    const sign = isCredit ? '+' : '-';
    const label = isCredit ? 'Nạp / Nhận' : 'Rút / Trừ';
    
    return {
      html: `<span class="badge ${isCredit ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}" 
              style="padding: 4px 8px; border-radius: 4px; font-size: 0.85em; font-weight: 600;">
              ${label}
             </span>`,
      amountHtml: `<span class="${colorClass}" style="font-weight: bold;">${sign} ${formatMoney(amount)}</span>`
    };
  };

  // --- LOGIC CHÍNH ---

  // Hiển thị Loading State ban đầu
  balanceEl.innerHTML = `<span style="font-size: 0.8em; color: gray;">Đang cập nhật...</span>`;
  const loadingRow = (cols) => `<tr><td colspan="${cols}" style="text-align:center; padding: 20px; color: gray;">Đang tải dữ liệu...</td></tr>`;
  
  platformTableBody.innerHTML = loadingRow(4);
  userTableBody.innerHTML = loadingRow(5);

  // Hàm gọi API chung để dễ xử lý lỗi
  const fetchData = async (url) => {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  };

  try {
    // Gọi song song 3 API để tiết kiệm thời gian
    const [balanceData, platformTxData, userTxData] = await Promise.allSettled([
      fetchData(`${API_BASE}/platform/balance`),
      fetchData(`${API_BASE}/platform/transactions`),
      fetchData(`${API_BASE}/all/transactions`)
    ]);

    // 1️⃣ Xử lý Số dư sàn
    if (balanceData.status === 'fulfilled') {
      const rawBal = balanceData.value.balance ?? balanceData.value.data?.balance ?? 0;
      balanceEl.textContent = formatMoney(rawBal);
      balanceEl.style.color = "#2ecc71"; // Màu xanh cho tiền
    } else {
      balanceEl.innerHTML = `<span style="color:red; font-size:0.8em;">⚠️ Lỗi</span>`;
      console.error("Lỗi balance:", balanceData.reason);
    }

    // 2️⃣ Xử lý Lịch sử ví sàn
    if (platformTxData.status === 'fulfilled') {
      const data = platformTxData.value;
      if (!data || data.length === 0) {
        platformTableBody.innerHTML = `<tr><td colspan="4" style="text-align:center; padding: 15px; color: #999;">Chưa có giao dịch nào</td></tr>`;
      } else {
        platformTableBody.innerHTML = data.map(tx => {
          const style = renderTxType(tx.txType, tx.amount);
          return `
            <tr>
              <td>${formatVNTime(tx.createdAt)}</td>
              <td>${style.html}</td>
              <td>${style.amountHtml}</td>
              <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${tx.description}">
                ${tx.description || '-'}
              </td>
            </tr>
          `;
        }).join("");
      }
    } else {
      platformTableBody.innerHTML = `<tr><td colspan="4" style="text-align:center; color: red;">Không thể tải dữ liệu</td></tr>`;
    }

    // 3️⃣ Xử lý Lịch sử ví User
    if (userTxData.status === 'fulfilled') {
      const data = userTxData.value;
      if (!data || data.length === 0) {
        userTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 15px; color: #999;">Chưa có giao dịch nào</td></tr>`;
      } else {
        userTableBody.innerHTML = data.map(tx => {
          const style = renderTxType(tx.txType, tx.amount);
          return `
            <tr>
              <td style="font-family: monospace; color: #555;">${tx.walletRefId}</td>
              <td>${formatVNTime(tx.createdAt)}</td>
              <td>${style.html}</td>
              <td>${style.amountHtml}</td>
              <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${tx.description}">
                ${tx.description || '-'}
              </td>
            </tr>
          `;
        }).join("");
      }
    } else {
      userTableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: red;">Không thể tải dữ liệu</td></tr>`;
    }

    // Toast thông báo thành công nhẹ nhàng
    const allSuccess = balanceData.status === 'fulfilled' && platformTxData.status === 'fulfilled' && userTxData.status === 'fulfilled';
    if(allSuccess) {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
        });
        Toast.fire({
            icon: 'success',
            title: 'Dữ liệu ví đã được cập nhật'
        });
    }

  } catch (globalError) {
    console.error("Lỗi hệ thống:", globalError);
    Swal.fire({
        icon: 'error',
        title: 'Lỗi kết nối',
        text: 'Không thể kết nối đến hệ thống ví. Vui lòng kiểm tra server!',
    });
  }
});
