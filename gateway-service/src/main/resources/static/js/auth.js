

// --- CẤU HÌNH TOAST (Thông báo đẹp góc phải) ---
if (!window.Toast) {
  window.Toast = Swal.mixin({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.onmouseenter = Swal.stopTimer;
      toast.onmouseleave = Swal.resumeTimer;
    },
    customClass: { popup: "colored-toast" },
  });
}


function showToast(icon, message) {
  window.Toast.fire({ icon, title: message });
}

// ============================================================
// 🩵 0️⃣ TỰ ĐỒNG BỘ (ĐÃ SỬA: CHẶN KHÔI PHỤC KHI Ở TRANG LOGIN)
// ============================================================
try {
  // Kiểm tra xem user đang đứng ở trang nào
  const path = window.location.pathname;
  const isAuthPage = path.includes("login.html") || path.includes("register.html");

  if (isAuthPage) {
    // 🛑 NẾU ĐANG Ở TRANG LOGIN/REGISTER:
    // Bắt buộc xóa sạch mọi thứ để tránh "ma" user cũ hiện về
    window.name = ""; 
    localStorage.removeItem("user");
    localStorage.removeItem("userId");
    localStorage.removeItem("token");
    console.log("🧹 Đang ở trang Auth -> Dọn sạch dữ liệu cũ.");
  } else {
    // ✅ NẾU Ở TRANG TRONG (Index, Cart...): Mới cho phép đồng bộ
    const u = localStorage.getItem("user");
    const uid = localStorage.getItem("userId");

    // Logic cũ: Nếu localStorage mất mà window.name còn thì lấy lại
    if ((!u || !uid) && window.name && window.name.startsWith("{")) {
      try {
        const parsed = JSON.parse(window.name);
        localStorage.setItem("user", JSON.stringify(parsed));
        localStorage.setItem("userId", parsed.userId || parsed.id);
        console.log("✅ Đã đồng bộ user từ window.name:", parsed.userId || parsed.id);
      } catch (err) {
        window.name = "";
      }
    } else if (u && !window.name) {
      // Backup ngược lại vào window.name
      window.name = u;
    }
  }
} catch (err) {
  console.warn("⚠️ Lỗi logic đồng bộ:", err);
}

// ============================================================
const BACKEND_BASE = "";
const API_BASE = BACKEND_BASE + "/api/auth";

function valOrEmpty(id) {
  return document.getElementById(id)?.value.trim() || "";
}

// ============================================================
// ✅ LƯU TOKEN & USER
// ============================================================
function saveAuth(token, user) {
  let userIdFromToken = null;

  if (token) {
    localStorage.setItem("token", token);

    try {
      const payload = JSON.parse(atob(token.split(".")[1]));

      if (!user) user = {};

      // Gắn roles nếu có
      if (payload.roles) {
        user.roles = payload.roles;
      }

      // 🔥 LẤY USER ID TỪ JWT
      userIdFromToken = payload.userId || payload.id || payload.sub;

      if (userIdFromToken) {
        // Nếu user chưa có id thì gán
        user.id = user.id || userIdFromToken;
        user.userId = user.userId || userIdFromToken;
      }
    } catch (err) {
      console.error("Không thể decode JWT:", err);
    }

    document.cookie = `jwt_token=${token}; path=/; max-age=86400; SameSite=Lax`;
  }

  if (user) {
    localStorage.setItem("user", JSON.stringify(user));

    const finalUserId = user.userId || user.id || userIdFromToken;
    if (finalUserId) {
      localStorage.setItem("userId", finalUserId);
    }
  }

  console.log("💾 saveAuth() => userId:", user?.userId || user?.id || userIdFromToken);
}


// ============================================================
// ✅ TRÍCH TOKEN + USER TỪ RESPONSE
// ============================================================
async function extractLoginData(res) {
  let body = null;
  try {
    body = await res
      .clone()
      .json()
      .catch(() => null);
  } catch {
    body = null;
  }

  // ✅ Trường hợp phổ biến: { token, user }
  if (body && (body.token || body.token === ""))
    return { token: body.token, user: body.user || null };

  // ✅ Nếu token ở header
  const authHeader =
    res.headers.get("Authorization") || res.headers.get("authorization");
  if (authHeader?.startsWith("Bearer ")) {
    const token = authHeader.substring(7);
    return { token, user: body?.user || null };
  }

  // ✅ Nếu chỉ có user
  if (body) return { token: null, user: body.user || body };

  return { token: null, user: null };
}

// ============================================================
// ✅ ĐĂNG KÝ
// ============================================================
async function registerHandler(evt) {
  evt?.preventDefault();

  const name = valOrEmpty("name");
  const email = valOrEmpty("email").toLowerCase();
  const phone = valOrEmpty("phone");
  const password = valOrEmpty("password");
  const password2 = valOrEmpty("password2");
  const address = valOrEmpty("address");
  const cityName = valOrEmpty("cityName");

  // Đã xóa emoji ⚠️
  if (!name || (!email && !phone) || !password)
    return showToast("warning", "Vui lòng điền đầy đủ thông tin.");

  // Đã xóa emoji ❌
  if (password !== password2) return showToast("error", "Mật khẩu không khớp.");

  const payload = { name, email, phone, password, address, cityName };

  try {
    const res = await fetch(`${API_BASE}/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      // Đã xóa emoji ❌
      showToast("error", "Đăng ký thất bại: " + (txt || res.status));
      return;
    }

    const data = await extractLoginData(res);
    if (data.token && data.user) {
      saveAuth(data.token, data.user);
      // Đã xóa emoji 🎉
      showToast("success", "Đăng ký thành công! Đang vào trang chủ...");
      setTimeout(() => {
        window.location.href = "/index.html";
      }, 1500);
    } else {
      // Đã xóa emoji ✅
      showToast("info", "Đăng ký thành công. Vui lòng đăng nhập.");
      setTimeout(() => {
        window.location.href = "/login.html";
      }, 1500);
    }
  } catch (err) {
    console.error("Register error:", err);
    // Đã xóa emoji ❌
    showToast("error", "Lỗi kết nối đến server.");
  }
}

// ============================================================
// ✅ ĐĂNG NHẬP
// ============================================================
async function loginHandler(evt) {
  evt?.preventDefault();

  const id = valOrEmpty("idOrPhone").toLowerCase();
  const password = valOrEmpty("loginPass");

  // Đã xóa emoji ⚠️
  if (!id || !password)
    return showToast("warning", "Vui lòng nhập tài khoản và mật khẩu.");

  try {
    const res = await fetch(`${API_BASE}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identifier: id, password }),
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      if (res.status === 401 || res.status === 403) {
        // 👇 Đã xóa emoji ❌ ở đây theo yêu cầu của bạn
        showToast("error", "Sai email/SĐT hoặc mật khẩu!");
      } else {
        // Đã xóa emoji ❌
        showToast("error", "Đăng nhập thất bại: " + (txt || res.status));
      }
      return;
    }

    const data = await extractLoginData(res);
    if (!data.token) {
      // Đã xóa emoji ⚠️
      showToast("error", "Lỗi: Server không trả về Token.");
      return;
    }

    saveAuth(data.token, data.user);
    // Đã xóa emoji ✅
    showToast("success", "Đăng nhập thành công!");
    setTimeout(() => {
      window.location.href = "/index.html";
    }, 1000);
  } catch (err) {
    console.error("Login error:", err);
    // Đã xóa emoji ❌
    showToast("error", "Không thể kết nối đến server.");
  }
}
// ============================================================
// ✅ ĐĂNG XUẤT — CHUYỂN HƯỚNG NGAY & BÁO Ở TRANG LOGIN
// ============================================================
async function logout() {
  console.log("Bắt đầu quá trình đăng xuất...");

  try {
    const jwtToken = localStorage.getItem("token");
    const userString = localStorage.getItem("user");
    const fcmToken = localStorage.getItem("fcmToken_sent");

    if (jwtToken && userString && fcmToken) {
      const user = JSON.parse(userString);
      if (user.id) {
        await fetch("/api/devices/unregister", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${jwtToken}`,
          },
          body: JSON.stringify({ userId: user.id, token: fcmToken }),
        }).catch((err) => console.error("Lỗi hủy token ngầm:", err));
      }
    }
  } catch (err) {
    console.error("Lỗi logic logout:", err);
  } finally {
    // 1. Xóa Cookie
    document.cookie = "jwt_token=; path=/; max-age=0";

    // 2. Xóa LocalStorage
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    localStorage.removeItem("userId"); 
    localStorage.removeItem("fcmToken_sent");
    localStorage.removeItem("fcmUser_sent");
    
    // 3. Xóa bộ nhớ đệm Tab
    window.name = ""; 

    // 4. 🔥 CHUYỂN HƯỚNG NGAY KÈM TÍN HIỆU
    // (Không showToast ở đây nữa mà để trang Login show)
    window.location.href = "/login.html?logout=success";
  }
}
// ============================================================
// ✅ SOCIAL LOGIN DEMO
// ============================================================
function socialLogin(provider) {
  if (provider === "google") {
    // Chuyển hướng đến endpoint OAuth2 của Spring Boot (port 8084)
    window.location.href = BACKEND_BASE + "/oauth2/authorization/google";
  } else if (provider === "facebook") {
    alert("Facebook login chưa được triển khai!");
  }
}


// ============================================================
// ✅ SỰ KIỆN KHI TRANG LOAD
// ============================================================
document.addEventListener("DOMContentLoaded", () => {
  
  // ------------------------------------------------------------
  // 🛡️ 1. CHỐT CHẶN BẢO VỆ (CLIENT-SIDE GUARD)
  // ------------------------------------------------------------
  const currentPath = window.location.pathname;
  
  // Danh sách các trang cho phép vào tự do
  const publicPages = [
    "/login.html",
    "/register.html",
    "/index.html",
    "/",
    "/navbar.html",
    "/footer.html"
  ];

  const isPublicPage = publicPages.some(page => currentPath === page || currentPath.endsWith(page));
  const hasToken = localStorage.getItem("token");

  // Nếu không phải trang Public và không có Token => Đuổi về Login
  if (!isPublicPage && !hasToken) {
    console.warn("⛔ Phát hiện truy cập trái phép! Đang chuyển hướng về Login...");
    window.location.href = "/login.html";
    return;
  }

  // ------------------------------------------------------------
  // 🔥 2. (MỚI - QUAN TRỌNG) BẮT TÍN HIỆU LOGOUT ĐỂ HIỆN THÔNG BÁO
  // ------------------------------------------------------------
  // Kiểm tra xem trên URL có đuôi ?logout=success không
  const urlParams = new URLSearchParams(window.location.search);
  
  if (urlParams.get('logout') === 'success') {
      // Hiện thông báo đẹp (lúc này giao diện đã là trang Login trắng đẹp)
      showToast("success", "Đã đăng xuất thành công!");
      
      // Xóa chữ ?logout=success trên thanh địa chỉ cho sạch
      const newUrl = window.location.pathname;
      window.history.replaceState({}, document.title, newUrl);
  }
  // ------------------------------------------------------------


  // 3. GẮN SỰ KIỆN FORM (Code cũ)
  document
    .getElementById("loginForm")
    ?.addEventListener("submit", loginHandler);
  document
    .getElementById("registerForm")
    ?.addEventListener("submit", registerHandler);

  // 4. GẮN SỰ KIỆN SOCIAL LOGIN (Code cũ)
  const gbtn = document.getElementById("googleLogin");
  if (gbtn)
    gbtn.addEventListener("click", (e) => {
      e.preventDefault();
      socialLogin("google");
    });

  const fbbtn = document.getElementById("fbLogin");
  if (fbbtn)
    fbbtn.addEventListener("click", (e) => {
      e.preventDefault();
      socialLogin("facebook");
    });

  // 5. XỬ LÝ GOOGLE REDIRECT
  const hash = window.location.hash;
  let token = null;

  if (hash && hash.includes("token=")) {
    try {
      token = hash.split("token=")[1].split("&")[0];
    } catch (e) {
      console.warn("Lỗi parse hash token", e);
    }
  }

  if (token) {
    console.log("🎉 Phát hiện Token từ Google:", token);

    // 1) Lưu token NGAY LẬP TỨC
    saveAuth(token, null);

    // 2) GỌI API LẤY USER
    fetch(BACKEND_BASE + "/api/user/me", {
      headers: { Authorization: "Bearer " + token }
    })
      .then(r => (r.ok ? r.json() : null))
      .then(me => {
        if (me) {
          // 3) Lưu user vào localStorage
          saveAuth(token, me);

          // Nếu đang trang profile → cập nhật UI ngay
          if (typeof fillFormFromUser === "function") {
            fillFormFromUser(me);
          }
        }
      })
      .catch(e => console.error("Lỗi gọi /api/user/me:", e));

    // 4) Xóa hash & chuyển trang
    history.replaceState(null, null, " ");
    showToast("success", "Đăng nhập Google thành công!");
    setTimeout(() => {
      window.location.href = "/index.html";
    }, 1000);
  }



});
