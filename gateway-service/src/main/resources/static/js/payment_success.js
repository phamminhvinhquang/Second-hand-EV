// === JS Trang Kết Quả Thanh Toán (Refactored & Improved) ===

document.addEventListener("DOMContentLoaded", async function () {
  // 1. Khởi tạo UI Libraries
  if (window.AOS) AOS.init({ duration: 800, once: true });
  if (window.lucide) lucide.createIcons();

  // 2. Constants & Config
  const API_CONFIG = {
    PAYMENT_INFO: "/api/payments/info",
    PURCHASE: "/api/purchases",
  };

  const DOM = {
    loading: document.getElementById("pageLoading"), // Giả sử bạn có div loading
    customerInfo: document.getElementById("customerInfo"),
    extraNote: document.getElementById("extraNote"),
    successCard: document.getElementById("paymentSuccess"),
    failedCard: document.getElementById("paymentFailed"),
    paymentActions: document.getElementById("paymentActions"),
    redirectCountdown: document.getElementById("redirectCountdown"), // Thêm div này vào HTML
  };

  // 3. Helper Functions
  const utils = {
    getQueryParam: (param) => {
      const urlParams = new URLSearchParams(window.location.search);
      return urlParams.get(param);
    },
    formatMethod: (method) => {
      const map = {
        vnpay: "VNPAY",
        momo: "MoMo",
        evwallet: "Ví EV",
        online: "Chuyển khoản ngân hàng",
      };
      return map[method.toLowerCase()] || method.toUpperCase();
    },
    showElement: (el) => el && el.classList.remove("hidden"),
    hideElement: (el) => el && el.classList.add("hidden"),
    setUnknownText: (id, text) => {
      const el = document.getElementById(id);
      if (el) el.textContent = text || "Chưa cập nhật";
    },
  };

  // 4. Logic xử lý Purchase (Backend Call)
  async function ensurePurchaseCreated(txId) {
    const url = `${API_CONFIG.PURCHASE}/from-transaction/${encodeURIComponent(txId)}`;
    const MAX_RETRIES = 3;

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        
        const data = await res.json();
        console.info(`[Purchase] Synced successfully (Attempt ${attempt})`, data);
        return data;
      } catch (err) {
        console.warn(`[Purchase] Sync failed attempt ${attempt}:`, err);
        if (attempt < MAX_RETRIES) await new Promise((r) => setTimeout(r, 1000));
      }
    }
    console.error(`[Purchase] Failed to sync after ${MAX_RETRIES} attempts.`);
    return null;
  }

  // 5. Logic hiển thị kết quả
  async function renderPaymentResult() {
    // Lấy Transaction ID
    const transactionId = utils.getQueryParam("transactionId") || utils.getQueryParam("orderId");

    if (!transactionId) {
      Swal.fire({
        icon: "error",
        title: "Lỗi đường dẫn",
        text: "Không tìm thấy mã giao dịch!",
        confirmButtonText: "Về trang chủ",
      }).then(() => (window.location.href = "/"));
      return;
    }

    try {
      // Hiện loading nếu có
      if (DOM.loading) utils.showElement(DOM.loading);

      // Gọi API lấy thông tin thanh toán
      const res = await fetch(`${API_CONFIG.PAYMENT_INFO}/${transactionId}`);
      if (!res.ok) throw new Error("Không thể lấy thông tin giao dịch");
      const data = await res.json();

      // Ẩn loading
      if (DOM.loading) utils.hideElement(DOM.loading);

      console.log("Transaction Data:", data);

      // Fill thông tin khách hàng
      utils.showElement(DOM.customerInfo);
      utils.setUnknownText("cName", data.fullName);
      utils.setUnknownText("cPhone", data.phone);
      utils.setUnknownText("cEmail", data.email);
      utils.setUnknownText("cAddress", data.address);
      utils.setUnknownText("cMethod", utils.formatMethod(data.method || ""));

      utils.showElement(DOM.extraNote);

      // Điều hướng logic theo trạng thái
      const status = (data.status || "").toUpperCase();

      switch (status) {
        case "SUCCESS":
          await handleSuccess(data, transactionId);
          break;

        case "CANCELED":
          handleCanceled();
          break;

        case "FAILED":
          handleFailed(data);
          break;

        default: // PENDING
          handlePending();
          break;
      }
    } catch (error) {
      console.error(error);
      utils.hideElement(DOM.successCard);
      utils.showElement(DOM.failedCard);
      if (DOM.extraNote) {
        DOM.extraNote.innerHTML = `<p class="text-red-600">Không thể tải dữ liệu giao dịch. Vui lòng liên hệ CSKH.</p>`;
      }
    }
  }

  // --- Handler: Thành công ---
  async function handleSuccess(data, transactionId) {
    utils.showElement(DOM.successCard);
    utils.hideElement(DOM.failedCard);

    // Update UI text
    if (DOM.extraNote) {
      DOM.extraNote.innerHTML = `
        <div class="bg-green-50 border border-green-200 rounded-lg p-4">
            <p class="text-green-700 font-semibold flex items-center gap-2">
                <i data-lucide="check-circle" class="w-5 h-5"></i>
                Thanh toán thành công qua ${utils.formatMethod(data.method)}
            </p>
            <p class="text-sm text-green-600 mt-1">Hệ thống đang tạo hợp đồng số hóa...</p>
        </div>
      `;
      lucide.createIcons();
    }

    // Chỉ chạy logic tạo Purchase nếu là Order
    if (data.type === "order") {
        // Chạy ngầm sync purchase
        const purchaseTask = ensurePurchaseCreated(transactionId).then((purchase) => {
            if (purchase && purchase.userId) {
                // Update LocalStorage User ID an toàn
                try {
                    const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
                    if (currentUser.id !== purchase.userId) {
                         currentUser.id = purchase.userId;
                         currentUser.userId = purchase.userId;
                         localStorage.setItem("user", JSON.stringify(currentUser));
                    }
                } catch (e) { console.error("LS Error", e); }
            }
        });

        // Countdown Timer chuyển hướng
        let secondsLeft = 5; // Thời gian đếm ngược
        const countdownEl = DOM.redirectCountdown || document.createElement("div");
        if (!DOM.redirectCountdown) DOM.successCard.appendChild(countdownEl);
        
        countdownEl.className = "mt-4 text-center text-gray-500 text-sm";

        const timer = setInterval(() => {
            countdownEl.innerHTML = `Tự động chuyển sang ký hợp đồng trong <span class="font-bold text-green-600">${secondsLeft}s</span>...`;
            secondsLeft--;

            if (secondsLeft < 0) {
                clearInterval(timer);
                window.location.href = `contract.html?transactionId=${transactionId}`;
            }
        }, 1000);

        // Hiển thị nút thủ công nếu người dùng không muốn đợi
        const btnContainer = document.getElementById("paymentActions");
        if(btnContainer) {
            btnContainer.innerHTML += `
                <a href="contract.html?transactionId=${transactionId}" 
                   class="inline-block mt-3 px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium transition">
                   Ký hợp đồng ngay
                </a>
            `;
        }
    }
  }

  // --- Handler: Đã hủy ---
  function handleCanceled() {
    utils.showElement(DOM.failedCard);
    utils.hideElement(DOM.successCard);
    if (DOM.extraNote) {
      DOM.extraNote.innerHTML = `<p class="text-yellow-600 font-medium">Bạn đã hủy giao dịch. Tài khoản chưa bị trừ tiền.</p>`;
    }
  }

  // --- Handler: Thất bại ---
  function handleFailed(data) {
    utils.showElement(DOM.failedCard);
    utils.hideElement(DOM.successCard);

    let message = "Giao dịch thất bại. Vui lòng thử lại.";
    if ((data.method || "").toLowerCase() === "evwallet") {
      message = "Thất bại do <strong>số dư ví không đủ</strong>. Vui lòng nạp thêm tiền.";
    }

    if (DOM.extraNote) {
      DOM.extraNote.innerHTML = `<p class="text-red-600 font-medium">${message}</p>`;
    }
    
    // Thêm nút thử lại
    if (DOM.paymentActions) {
        DOM.paymentActions.innerHTML = `
            <a href="cart.html" 
              class="px-6 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg">
              Quay lại giỏ hàng
            </a>
        `;
    }

  }

  // --- Handler: Đang xử lý ---
  function handlePending() {
    utils.showElement(DOM.failedCard); // Hoặc tạo card pending riêng
    utils.hideElement(DOM.successCard);
    if (DOM.extraNote) {
      DOM.extraNote.innerHTML = `
        <div class="flex items-center gap-2 text-yellow-600">
            <span class="loading loading-spinner loading-sm"></span>
            <span>Giao dịch đang chờ xử lý từ ngân hàng...</span>
        </div>`;
    }
  }

  // === START ===
  await renderPaymentResult();
});
