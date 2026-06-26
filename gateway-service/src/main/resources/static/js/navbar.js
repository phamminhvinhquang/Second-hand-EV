// File: js/navbar.js

function updateNavbarState() {
  // 1. Lấy các element của menu mới
  const menuToggle = document.getElementById("userMenuToggle");
  const menuDropdown = document.getElementById("userMenuDropdown");

  if (!menuToggle || !menuDropdown) return;

  // 2. Kiểm tra trạng thái đăng nhập
  const token = localStorage.getItem("token");
  const userStr = localStorage.getItem("user");
  let user = null;
  try {
    user = JSON.parse(userStr);
  } catch (e) {}

  // 3. Xây dựng nội dung Menu dựa trên trạng thái
  let menuHTML = "";

  if (token && user) {
    // --- ĐÃ ĐĂNG NHẬP ---
    const userName = user.fullName || user.name || "Người dùng";
    // Lấy roles từ localStorage (đã được auth.js lưu vào user.roles)
    const roles = Array.isArray(user.roles) ? user.roles : [];

    // Chuẩn hóa thành chữ hoa
    const upperRoles = roles.map((r) => String(r).toUpperCase());

    // Xác định quyền
    const isAdmin =
      upperRoles.includes("ADMIN") || upperRoles.includes("ROLE_ADMIN");
    const isStaff =
      upperRoles.includes("STAFF") || upperRoles.includes("ROLE_STAFF");

    // Role gốc để xử lý: ADMIN > STAFF > USER
    let displayRole = "USER";
    if (isAdmin) displayRole = "ADMIN";
    else if (isStaff) displayRole = "STAFF";

    // Chuyển sang tiếng Việt
    let displayRoleText = "Khách Hàng";
    if (displayRole === "ADMIN") displayRoleText = "Quản Lý";
    else if (displayRole === "STAFF") displayRoleText = "Nhân Viên";

    // MENU QUẢN TRỊ SẼ LÀ NÚT + SUBMENU (xổ phải)
    let managementHTML = "";
    if (isAdmin || isStaff) {
      managementHTML = `
        <div class="menu-divider"></div>

        <div class="admin-wrapper" style="position: relative;">
            <!-- Nút toggle quản trị -->
            <button id="adminPanelToggle" class="menu-item" style="
                width:100%;
                display:flex;
                align-items:center;
                justify-content:space-between;
                padding-right: 12px;
            ">
              <span style="display:flex; align-items:center; gap:10px;">
                  <i class="fas fa-tools"></i> Quản Trị
              </span>
              <i class="fas fa-chevron-right"></i>
            </button>

            <!-- SUBMENU xổ phải (đã bám vào wrapper) -->
            <div id="adminSubmenu" class="admin-submenu">
              ${
                isAdmin
                  ? `
                <a href="/admin-revenue.html"><i class="fas fa-chart-line"></i> Doanh thu</a>
                <a href="/admin-roles.html"><i class="fas fa-user-shield"></i> Vai trò</a>
                <a href="/admin_wallet_dashboard.html"><i class="fas fa-wallet"></i> Ví hệ thống</a>
                <a href="/payroll.html"><i class="fas fa-file-invoice-dollar"></i> Lương nhân viên</a>
                <a href="/admin-listings.html"><i class="fas fa-tasks"></i> Quản lý tin đăng</a>
                <a href="/admin-transaction.html"><i class="fas fa-exchange-alt"></i> Giao dịch</a>
              `
                  : `
                <a href="/admin-listings.html"><i class="fas fa-tasks"></i> Quản lý tin đăng</a>
                <a href="/admin-transaction.html"><i class="fas fa-exchange-alt"></i> Giao dịch</a>
              `
              }
            </div>
        </div>
      `;
    }

    // -----------------------------

    menuHTML = `
      <div class="menu-header">
        <span class="menu-user-name">${userName}</span>
        <span class="menu-user-role">${displayRoleText}</span>


      </div>
      
      <a href="/profile.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="far fa-user-circle"></i> Hồ sơ cá nhân
        
      </a>
      <a href="/edit_news.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-list-ul"></i> Tin đăng của bạn
      </a>
      <a href="/Product_Listings.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-plus-circle"></i> Đăng tin mới
      </a>
      <a href="/cart.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fa-solid fa-cart-shopping"></i> Giỏ hàng
      </a>
       <a href="/purchase.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-box-open"></i> Đơn hàng của tôi
      </a>
      <a href="/contract-history.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-file-contract"></i> Lịch sử hợp đồng
      </a>
      <a href="/wallet.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-wallet"></i> Ví của tôi
      </a>

      ${managementHTML}
      
      <div class="menu-divider"></div>
    
      
      <button class="menu-item auth-item logout" id="btnLogoutItem" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-sign-out-alt"></i> ĐĂNG XUẤT
      </button>
    `;
  } else {
    // --- CHƯA ĐĂNG NHẬP ---
    menuHTML = `
      <div class="menu-header">
        <span class="menu-user-name">Khách</span>
        <span class="menu-user-role">Vui lòng đăng nhập</span>
      </div>
      
      <a href="/login.html" class="auth-item" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-sign-in-alt"></i> ĐĂNG NHẬP
      </a>
      <a href="/register.html" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-user-plus"></i> Đăng ký tài khoản
      </a>
      
      <div class="menu-divider"></div>
      
      <a href="#" style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-question-circle"></i> Trợ giúp
      </a>
    `;
  }

  // 4. Gán HTML vào Dropdown
  menuDropdown.innerHTML = menuHTML;

  // --- SỰ KIỆN MỞ SUBMENU QUẢN TRỊ ---
  const adminToggle = document.getElementById("adminPanelToggle");
  const adminSub = document.getElementById("adminSubmenu");

  if (adminToggle && adminSub) {
    adminToggle.addEventListener("click", (e) => {
      e.stopPropagation();
      adminSub.classList.toggle("show");
    });
  }

  // 5. Xử lý sự kiện Toggle Menu
  const newToggle = menuToggle.cloneNode(true);
  menuToggle.parentNode.replaceChild(newToggle, menuToggle);

  newToggle.addEventListener("click", (e) => {
    e.stopPropagation();
    menuDropdown.classList.toggle("show");
    newToggle.classList.toggle("active");
  });

  // 6. Xử lý sự kiện Đăng xuất
  const btnLogout = document.getElementById("btnLogoutItem");
  if (btnLogout) {
    btnLogout.addEventListener("click", () => {
      // Ưu tiên 1: Dùng hàm logout() xịn trong auth.js
      if (typeof logout === "function") {
        logout();
      } else {
        // Ưu tiên 2: Fallback (Dự phòng khi auth.js chưa load kịp)
        console.warn("⚠️ Auth.js chưa load, dùng logout dự phòng.");

        // Xóa sạch dữ liệu (Bổ sung cho đủ bộ)
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        localStorage.removeItem("userId"); // <--- Thêm cái này
        localStorage.removeItem("fcmToken_sent");
        localStorage.removeItem("fcmUser_sent");

        // Xóa bộ nhớ đệm tab (Chống zombie user)
        window.name = "";

        // 🔥 CHUYỂN HƯỚNG 1 LẦN DUY NHẤT (Có kèm tín hiệu)
        window.location.href = "/login.html?logout=success";
      }
    });
  }

  // 7. Click ra ngoài thì đóng menu
  document.addEventListener("click", (e) => {
    if (!newToggle.contains(e.target) && !menuDropdown.contains(e.target)) {
      menuDropdown.classList.remove("show");
      newToggle.classList.remove("active");
    }
  });
}

// Lắng nghe sự kiện từ component-loader
document.addEventListener("componentsLoaded", updateNavbarState);
