// /js/profile.js
const API_USER = "/api/user"; // <-- chỉnh nếu backend ở nơi khác
const ITEMS_PER_PAGE = 5; // Số lượng mục mỗi trang
let currentPage = 1; // Trang hiện tại

/* ---------------- helpers ---------------- */
function getToken() {
  return localStorage.getItem("token");
}

function getLocalUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch (e) {
    return null;
  }
}

function setLocalUser(u) {
  try {
    localStorage.setItem("user", JSON.stringify(u || {}));
  } catch (e) {
    console.warn(e);
  }
}

// ❌ Đã xóa hàm showMsg(alert) cũ gây ra bảng đen xấu xí
// Chúng ta sẽ dùng showToast("success/error", "message") trực tiếp bên dưới

/** Parse response body flexibly (json or text) */
async function parseResponseBody(res) {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    try {
      return await res.json();
    } catch (e) {
      return null;
    }
  } else {
    try {
      return await res.text();
    } catch (e) {
      return null;
    }
  }
}

/** Wrapper fetch with auth header */
async function authFetch(url, opts = {}) {
  const token = getToken();
  const headers = Object.assign({
      "Content-Type": "application/json"
    },
    opts.headers || {}
  );
  if (token) headers["Authorization"] = "Bearer " + token;
  try {
    const res = await fetch(
      url,
      Object.assign({}, opts, {
        headers,
        mode: "cors"
      })
    );
    if (res.status === 401) {
      console.warn("Received 401 from", url);
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      setTimeout(() => (window.location.href = "/login.html"), 150);
    }
    return res;
  } catch (err) {
    console.error("authFetch error", err, "url:", url, "opts:", opts);
    throw err;
  }
}

/* ---------------- UI helpers ---------------- */
function fillFormFromUser(user) {
  if (!user) return;
  const name = document.getElementById("pfName");
  const email = document.getElementById("pfEmail");
  const phone = document.getElementById("pfPhone");
  const address = document.getElementById("pfAddress");
  if (name) name.value = user.name || "";
  if (email) {
    email.value = user.email || "";
    email.disabled = true;
  }
  if (phone) phone.value = user.phone || "";
  if (address) address.value = user.address || "";
}

/* ---------------- Profile operations ---------------- */
async function initProfile() {
  try {
    console.log("Loading profile from", `${API_USER}/me`);
    const res = await authFetch(`${API_USER}/me`, { method: "GET" });

    if (!res.ok) {
      console.warn("load profile failed:", res.status);
      return null;
    }

    const user = await res.json();

    // 🔥 luôn lưu user mới nhất
    setLocalUser(user);

    // 🔥 luôn render UI theo server, không dùng old localStorage
    fillFormFromUser(user);

    return user;
  } catch (err) {
    console.error("initProfile error", err);

    // fallback cuối: lấy từ localStorage
    const local = getLocalUser();
    if (local) fillFormFromUser(local);

    return null;
  }
}


async function updateProfileOnServer(payload) {
  console.log("Updating profile...", payload);
  const res = await authFetch(`${API_USER}/profile`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const body = await parseResponseBody(res).catch(() => null);
    const txt = body ? (typeof body === "string" ? body : JSON.stringify(body)) : "";
    throw new Error((txt || `Update failed`) + ` (${res.status})`);
  }
  return await parseResponseBody(res).catch(() => null);
}

/* ---------------- Change password ---------------- */
async function changePasswordOnServer(currentPassword, newPassword) {
  const res = await authFetch(`${API_USER}/change-password`, {
    method: "POST",
    body: JSON.stringify({
      currentPassword,
      newPassword
    }),
  });
  if (!res.ok) {
    const body = await parseResponseBody(res).catch(() => null);
    const msg = body ? (typeof body === "string" ? body : JSON.stringify(body)) : `Failed (${res.status})`;
    return {
      ok: false,
      message: msg
    };
  }
  const body = await parseResponseBody(res).catch(() => null);
  return {
    ok: true,
    data: body || null
  };
}

/* ---------------- Items & transactions ---------------- */
async function loadHistory() {
  const node = document.getElementById("historyListContainer");
  if (!node) return;

  node.innerHTML = '<div class="text-center py-12"><i class="fas fa-circle-notch fa-spin text-3xl text-emerald-500"></i><p class="mt-3 text-gray-500 text-sm">Đang tải dữ liệu...</p></div>';
  const oldPag = document.getElementById("paginationContainer");
  if (oldPag) oldPag.innerHTML = "";

  try {
    const res = await authFetch(`${API_USER}/me/history`, {
      method: "GET"
    });
    if (!res.ok) {
      node.innerHTML = "<div class='text-center text-red-500 py-8'>Không thể tải lịch sử giao dịch.</div>";
      return;
    }
    const txs = await res.json();
    txs.sort((a, b) => new Date(b.createdAt || b.created_at) - new Date(a.createdAt || a.created_at));
    window.allHistoryData = txs;
    renderPage(1);
  } catch (err) {
    console.error(err);
    node.innerHTML = "<div class='text-center text-red-500'>Lỗi kết nối đến máy chủ.</div>";
  }
}

function renderPage(page) {
  const node = document.getElementById("historyListContainer");
  let paginationNode = document.getElementById("paginationContainer");

  if (!window.allHistoryData || window.allHistoryData.length === 0) {
    node.innerHTML = `<div class="flex flex-col items-center justify-center py-16 bg-gray-50 rounded-xl border border-dashed border-gray-300">
                        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4"><i class="fas fa-receipt text-2xl text-gray-400"></i></div>
                        <p class="text-gray-600 font-medium">Chưa có giao dịch nào</p>
                      </div>`;
    if (paginationNode) paginationNode.innerHTML = "";
    return;
  }

  const totalItems = window.allHistoryData.length;
  const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);

  if (page < 1) page = 1;
  if (page > totalPages) page = totalPages;
  currentPage = page;

  const start = (currentPage - 1) * ITEMS_PER_PAGE;
  const end = start + ITEMS_PER_PAGE;
  const itemsToShow = window.allHistoryData.slice(start, end);

  let html = itemsToShow.map((t, i) => {
    const realIndex = start + i;
    const dateStr = t.createdAt || t.created_at || new Date().toISOString();
    const dateObj = new Date(dateStr);
    const date = dateObj.toLocaleDateString("vi-VN");
    const time = dateObj.toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit"
    });
    const amountVal = t.amount || t.totalAmount || 0;
    const money = new Intl.NumberFormat("vi-VN").format(amountVal);
    let pName = t.productName || t.productNames || t.product_name || "Giao dịch thanh toán";
    if (pName.includes("Nạp tiền vào ví")) pName = "Nạp tiền vào ví";

    let rawImg = t.productImg || t.product_img || t.image || "";
    if (rawImg && rawImg.includes("listing-service:8080")) {
      rawImg = rawImg.replace("http://listing-service:8080", "http://localhost:8080");
    }

    const sName = t.sellerName || t.seller_name || (t.sellerId ? `NBH #${t.sellerId}` : "Hệ thống");
    const method = t.method || "VÍ ĐIỆN TỬ";
    let isOrder = t.type === "order" || t.type === "order-item";
    if (!isOrder && !pName.toLowerCase().includes("nạp tiền")) isOrder = true;

    let imgHtml = "";
    if (isOrder && rawImg) {
      const imgSrc = rawImg.startsWith("http") ? rawImg : `http://localhost:8080${rawImg}`;
      imgHtml = `<img src="${imgSrc}" class="w-20 h-20 object-cover rounded-lg border border-gray-100 shadow-sm" onerror="this.src='https://via.placeholder.com/150?text=No+Img'">`;
    } else if (isOrder) {
      imgHtml = `<div class="w-20 h-20 bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 border border-gray-200"><i class="fas fa-car-side text-2xl"></i></div>`;
    } else {
      imgHtml = `<div class="w-20 h-20 bg-emerald-50 rounded-lg flex items-center justify-center text-emerald-600 border border-emerald-100"><i class="fas fa-plus text-2xl"></i></div>`;
    }

    let statusHtml = `<span class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-100"><i class="fas fa-check mr-1"></i>Thành công</span>`;
    if (t.status === "PENDING") statusHtml = `<span class="text-xs font-bold text-yellow-600 bg-yellow-50 px-2.5 py-1 rounded-full border border-yellow-100"><i class="fas fa-clock mr-1"></i>Đang chờ</span>`;
    if (t.status === "FAILED") statusHtml = `<span class="text-xs font-bold text-red-600 bg-red-50 px-2.5 py-1 rounded-full border border-red-100"><i class="fas fa-times mr-1"></i>Thất bại</span>`;

    const moneyClass = isOrder ? "text-gray-800" : "text-emerald-600";
    const moneySign = isOrder ? "-" : "+";

    return `<div class="group bg-white border border-gray-200 rounded-xl p-5 mb-4 hover:shadow-lg hover:border-emerald-200 transition-all duration-300 cursor-default">
                        <div class="flex flex-col sm:flex-row gap-5 items-start">
                            <div class="flex-shrink-0">${imgHtml}</div>
                            <div class="flex-grow w-full">
                                <div class="flex justify-between items-start">
                                    <div>
                                        <h4 class="font-bold text-lg text-gray-800 mb-1 group-hover:text-emerald-700 transition-colors line-clamp-1" title="${pName}">${pName}</h4>
                                        <div class="flex items-center gap-3 text-sm text-gray-500 mb-2">
                                            <span class="flex items-center gap-1"><i class="fas fa-store text-gray-400"></i> ${sName}</span>
                                            <span class="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs font-medium uppercase">${method}</span>
                                        </div>
                                        <div class="text-xs text-gray-400">${time} - ${date}</div>
                                    </div>
                                    <div class="text-right flex flex-col items-end gap-2">
                                        <div class="font-bold text-xl ${moneyClass}">${moneySign}${money} ₫</div>
                                        ${statusHtml}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="mt-4 pt-3 border-t border-gray-100 flex justify-end items-center">
                            <span class="text-xs text-gray-400 mr-auto font-mono">Mã GD: ${t.transactionId?.substring(0, 8) || "N/A"}...</span>
                            <button class="text-sm text-gray-600 font-medium hover:text-emerald-600 hover:underline transition-colors flex items-center gap-1" onclick="showDetail(${realIndex})">
                                Xem chi tiết <i class="fas fa-chevron-right text-xs"></i>
                            </button>
                        </div>
                    </div>`;
  }).join("");

  node.innerHTML = html;

  if (totalPages > 1) {
    let pagesHtml = `<div class="flex justify-center items-center gap-4 mt-6">
                            <button onclick="changePage(${currentPage - 1})" class="w-10 h-10 flex items-center justify-center rounded-full border hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed" ${currentPage === 1 ? "disabled" : ""}>
                                <i class="fas fa-chevron-left text-gray-600"></i>
                            </button>
                            <span class="text-gray-700 font-medium">Trang ${currentPage} / ${totalPages}</span>
                            <button onclick="changePage(${currentPage + 1})" class="w-10 h-10 flex items-center justify-center rounded-full border hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed" ${currentPage === totalPages ? "disabled" : ""}>
                                <i class="fas fa-chevron-right text-gray-600"></i>
                            </button>
                        </div>`;
    if (!paginationNode) {
      paginationNode = document.createElement("div");
      paginationNode.id = "paginationContainer";
      node.parentNode.appendChild(paginationNode);
    }
    paginationNode.innerHTML = pagesHtml;
  } else {
    if (paginationNode) paginationNode.innerHTML = "";
  }
}

function changePage(newPage) {
  renderPage(newPage);
  const listContainer = document.getElementById("historyListContainer");
  if (listContainer) {
    listContainer.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });
  }
}

function showDetail(index) {
  const t = window.allHistoryData[index];
  if (!t) return;

  const money = new Intl.NumberFormat("vi-VN").format(t.amount || t.totalAmount || 0);
  const pName = t.productName || t.productNames || t.product_name || "Giao dịch hệ thống";
  const txId = t.transactionId || t.transaction_id || "---";
  const sName = t.sellerName || t.seller_name || "Hệ thống";
  const dateStr = t.createdAt || t.created_at;
  const date = dateStr ? new Date(dateStr).toLocaleString("vi-VN") : "N/A";

  let headerColor = "bg-gradient-to-r from-emerald-500 to-teal-600";
  let iconStatus = "fa-check-circle";
  let statusText = "Giao dịch thành công";
  let moneyColor = "text-emerald-600";
  let moneySign = "+";

  let isOrder = t.type === "order" || t.type === "order-item";
  if (!isOrder && !pName.toLowerCase().includes("nạp tiền")) isOrder = true;

  if (isOrder) {
    moneyColor = "text-gray-800";
    moneySign = "-";
  }

  if (t.status === "FAILED" || t.status === "CANCELED") {
    headerColor = "bg-gradient-to-r from-red-500 to-pink-600";
    iconStatus = "fa-times-circle";
    statusText = "Giao dịch thất bại";
  } else if (t.status === "PENDING") {
    headerColor = "bg-gradient-to-r from-yellow-400 to-orange-500";
    iconStatus = "fa-clock";
    statusText = "Đang xử lý";
  }

  Swal.fire({
    html: `<div class="overflow-hidden rounded-t-xl -mt-5 -mx-5 mb-5">
                <div class="${headerColor} p-6 text-center text-white relative">
                    <div class="bg-white/20 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-3 backdrop-blur-sm">
                        <i class="fas ${iconStatus} text-4xl text-white"></i>
                    </div>
                    <h3 class="text-xl font-bold uppercase tracking-wide">${statusText}</h3>
                    <p class="text-white/80 text-sm mt-1">${date}</p>
                </div>
            </div>
            <div class="px-2 pb-2">
                <div class="text-center mb-6">
                    <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Tổng thanh toán</p>
                    <h2 class="text-4xl font-bold ${moneyColor}">${moneySign}${money} <span class="text-xl align-top">₫</span></h2>
                </div>
                <div class="bg-gray-50 rounded-lg border border-gray-200 p-4 text-sm space-y-3">
                    <div class="flex justify-between items-start">
                        <span class="text-gray-500 w-1/3 text-left">Nội dung</span>
                        <span class="text-gray-800 font-semibold w-2/3 text-right text-balance">${pName}</span>
                    </div>
                    <div class="border-t border-dashed border-gray-300 my-2"></div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-500">Đơn vị</span>
                        <span class="text-gray-900 font-medium">${sName}</span>
                    </div>
                    <div class="flex justify-between items-center">
                         <span class="text-gray-500">Phương thức</span>
                         <span class="bg-white border border-gray-200 px-2 py-1 rounded text-xs font-bold text-gray-600 uppercase shadow-sm">${t.method || "VÍ ĐIỆN TỬ"}</span>
                    </div>
                    <div class="flex justify-between items-center pt-2">
                        <span class="text-gray-500">Mã tham chiếu</span>
                        <div class="flex items-center gap-2">
                            <span class="font-mono text-gray-600 text-xs bg-gray-200 px-2 py-1 rounded">${txId.substring(0,8)}...</span>
                            <button onclick="navigator.clipboard.writeText('${txId}'); Swal.showValidationMessage('Đã sao chép mã!');" class="text-emerald-600 hover:text-emerald-800" title="Sao chép mã đầy đủ">
                                <i class="far fa-copy"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>`,
    showCloseButton: true,
    showConfirmButton: false,
    width: "400px",
    padding: "1.5rem",
    customClass: {
      popup: "rounded-2xl shadow-2xl",
      closeButton: "text-white hover:text-gray-200 focus:shadow-none",
    },
  });
}

/* ---------------- UI wiring ---------------- */
function enableEmailEdit() {
  const e = document.getElementById("pfEmail");
  if (!e) return;
  e.disabled = false;
  e.focus();
}

function wireButtons() {
  // Edit email
  const editEmailBtn = document.getElementById("editEmailBtn");
  if (editEmailBtn) editEmailBtn.addEventListener("click", enableEmailEdit);

  // Profile form submit
  const profileForm = document.getElementById("profileForm");
  if (profileForm)
    profileForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = document.getElementById("pfName").value.trim();
      const email = document.getElementById("pfEmail").value.trim().toLowerCase();
      const phone = document.getElementById("pfPhone").value.trim();
      const address = document.getElementById("pfAddress").value.trim();

      if (!name) return showToast("warning", "Tên không được để trống."); // ✅ showToast
      if (email && !/^\S+@\S+\.\S+$/.test(email))
        return showToast("warning", "Email không hợp lệ."); // ✅ showToast

      try {
        const resBody = await updateProfileOnServer({
          name,
          email,
          phone,
          address
        });
        let updatedUser = null;
        let note = null;
        if (resBody && resBody.user) {
          updatedUser = resBody.user;
          note = resBody.note;
        } else {
          updatedUser = resBody;
        }

        if (updatedUser) {
          setLocalUser(updatedUser);
          fillFormFromUser(updatedUser);
        }

        if (note === "email-changed") {
          // Dùng Swal.fire đẹp thay vì alert xấu xí
          await Swal.fire({
            icon: 'info',
            title: 'Email đã thay đổi',
            text: 'Vui lòng đăng nhập lại để cập nhật phiên làm việc.',
            confirmButtonText: 'Đăng nhập lại'
          });
          localStorage.removeItem("token");
          localStorage.removeItem("user");
          window.location.href = "/login.html";
          return;
        }

        // 🔥 ĐÂY LÀ DÒNG BẠN CẦN:
        showToast("success", "Cập nhật thông tin thành công."); // ✅ Hình 2 của bạn

      } catch (err) {
        console.error("Update profile error", err);
        showToast("error", "Cập nhật thất bại: " + (err.message || err)); // ✅ showToast
      }
    });

  // Change password
  const openChangePassBtn = document.getElementById("openChangePassBtn");
  const changePassArea = document.getElementById("changePassArea");
  const cancelChangePass = document.getElementById("cancelChangePass");
  if (openChangePassBtn && changePassArea)
    openChangePassBtn.addEventListener("click", () => changePassArea.classList.remove("hidden"));
  if (cancelChangePass && changePassArea)
    cancelChangePass.addEventListener("click", () => {
      changePassArea.classList.add("hidden");
      const cur = document.getElementById("currentPass");
      const np = document.getElementById("newPass");
      const np2 = document.getElementById("newPass2");
      if (cur) cur.value = "";
      if (np) np.value = "";
      if (np2) np2.value = "";
    });

  const changePassForm = document.getElementById("changePassForm");
  if (changePassForm)
    changePassForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const cur = document.getElementById("currentPass").value;
      const np = document.getElementById("newPass").value;
      const np2 = document.getElementById("newPass2").value;
      if (!cur || !np) return showToast("warning", "Vui lòng điền đủ thông tin."); // ✅
      if (np !== np2) return showToast("error", "Mật khẩu mới và xác nhận không khớp."); // ✅

      try {
        const r = await changePasswordOnServer(cur, np);
        if (!r.ok) {
          showToast("error", "Đổi mật khẩu thất bại: " + r.message); // ✅
          return;
        }
        showToast("success", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."); // ✅
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        setTimeout(() => {
          window.location.href = "/login.html";
        }, 1500);
      } catch (err) {
        console.error("changePassword error", err);
        showToast("error", "Lỗi khi đổi mật khẩu."); // ✅
      }
    });

  // Add / Cancel / Save item buttons
  const addItemBtn = document.getElementById("addItemBtn");
  if (addItemBtn)
    addItemBtn.addEventListener("click", () => {
      const addForm = document.getElementById("addForm");
      if (addForm) addForm.classList.remove("hidden");
    });
  const cancelItemBtn = document.getElementById("cancelItemBtn");
  if (cancelItemBtn)
    cancelItemBtn.addEventListener("click", (e) => {
      e.preventDefault();
      const addForm = document.getElementById("addForm");
      if (addForm) {
        addForm.classList.add("hidden");
        addForm.removeAttribute("data-edit-id");
      }
    });

  const saveItemBtn = document.getElementById("saveItemBtn");
  if (saveItemBtn)
    saveItemBtn.addEventListener("click", async (e) => {
      e.preventDefault();
      const title = document.getElementById("itemTitle").value.trim();
      if (!title) return showToast("warning", "Nhập tên mục."); // ✅
      const payload = {
        title,
        type: document.getElementById("itemType").value,
        price: Number(document.getElementById("itemPrice").value || 0),
        year: document.getElementById("itemYear").value,
        km: document.getElementById("itemKM").value,
        condition: document.getElementById("itemCondition").value,
      };
      const addForm = document.getElementById("addForm");
      const editId = addForm ? addForm.getAttribute("data-edit-id") : null;
      try {
        if (editId) {
          const res = await authFetch(`${API_USER}/items/${editId}`, {
            method: "PUT",
            body: JSON.stringify(payload),
          });
          if (!res.ok)
            throw new Error(await parseResponseBody(res).catch(() => ""));
          showToast("success", "Cập nhật mục thành công."); // ✅
        } else {
          const res = await authFetch(`${API_USER}/items`, {
            method: "POST",
            body: JSON.stringify(payload),
          });
          if (!res.ok)
            throw new Error(await parseResponseBody(res).catch(() => ""));
          showToast("success", "Tạo mục thành công."); // ✅
        }
        if (addForm) {
          addForm.classList.add("hidden");
          addForm.removeAttribute("data-edit-id");
        }
        await loadItems(); // Hàm này chưa thấy khai báo trong code bạn gửi, nhưng giữ nguyên logic cũ
      } catch (err) {
        console.error("save item error", err);
        showToast("error", "Lưu mục thất bại."); // ✅
      }
    });

  // delegate edit/delete clicks
  const itemsList = document.getElementById("itemsList");
  if (itemsList) {
    itemsList.addEventListener("click", async (e) => {
      const editBtn = e.target.closest(".editItemBtn");
      const delBtn = e.target.closest(".delItemBtn");
      if (editBtn) {
        const id = editBtn.getAttribute("data-id");
        try {
          const res = await authFetch(`${API_USER}/items/${id}`, {
            method: "GET",
          });
          if (!res.ok) return showToast("error", "Không thể tải mục để sửa."); // ✅
          const it = await res.json();
          const addForm = document.getElementById("addForm");
          if (!addForm) return;
          document.getElementById("itemTitle").value = it.title || it.name || "";
          document.getElementById("itemType").value = it.type || "car";
          document.getElementById("itemPrice").value = it.price || "";
          document.getElementById("itemYear").value = it.year || "";
          document.getElementById("itemKM").value = it.km || "";
          document.getElementById("itemCondition").value = it.condition || "";
          addForm.setAttribute("data-edit-id", id);
          addForm.classList.remove("hidden");
        } catch (err) {
          console.error(err);
          showToast("error", "Lỗi hệ thống."); // ✅
        }
      } else if (delBtn) {
        const id = delBtn.getAttribute("data-id");
        if (!confirm("Bạn có chắc muốn xóa?")) return;
        try {
          const res = await authFetch(`${API_USER}/items/${id}`, {
            method: "DELETE",
          });
          if (!res.ok)
            throw new Error(await parseResponseBody(res).catch(() => ""));
          showToast("success", "Đã xóa."); // ✅
          await loadItems();
        } catch (err) {
          console.error(err);
          showToast("error", "Xóa thất bại."); // ✅
        }
      }
    });
  }
}

/* ---------------- init ---------------- */
document.addEventListener("DOMContentLoaded", () => {
  if (!getToken()) {
    window.location.href = "/login.html";
    return;
  }


  wireButtons();

  (async () => {
    await initProfile();
  })();

  const tabLinks = document.querySelectorAll(".sidebar-link");
  const tabPanels = document.querySelectorAll(".tab-panel");
  const activeClasses = "bg-emerald-100 text-emerald-700 font-semibold";
  const inactiveClasses = "text-gray-700 hover:bg-gray-100";
  let historyLoaded = false;

  tabLinks.forEach((link) => {
    link.addEventListener("click", () => {
      const tabId = link.getAttribute("data-tab");

      tabLinks.forEach((item) => {
        item.classList.remove(...activeClasses.split(" "));
        item.classList.add(...inactiveClasses.split(" "));
      });
      link.classList.add(...activeClasses.split(" "));
      link.classList.remove(...inactiveClasses.split(" "));

      tabPanels.forEach((panel) => {
        panel.classList.add("hidden");
      });
      const activePanel = document.getElementById(`tab-${tabId}`);
      if (activePanel) {
        activePanel.classList.remove("hidden");
      }

      if (tabId === "history" && !historyLoaded) {
        loadHistory();
        historyLoaded = true;
      }
    });
  });
});
