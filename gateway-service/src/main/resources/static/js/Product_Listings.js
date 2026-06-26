// --- CONFIGURATION ---
const API_BASE_URL = "/api";
const MAX_IMAGE_COUNT = 8; // <--- THÊM DÒNG NÀY
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 10 MB in bytes
let isSubmitting = false;
let allImageFiles = [];
let currentSuggestedPrice = null;

// --- HÀM LẤY AUTH TỪ LOCALSTORAGE (THEO auth.js) ---
function getAuthInfo() {
  const token = localStorage.getItem("token"); // <-- DÙNG KEY TỪ auth.js
  const userString = localStorage.getItem("user"); // <-- DÙNG KEY TỪ auth.js

  if (!token || !userString) {
    console.error("Chưa đăng nhập! Đang chuyển về trang đăng nhập.");
    // Thay '/login.html' bằng URL trang đăng nhập của bạn
    window.location.href = "/login.html";
    return null; // Dừng thực thi
  }

  try {
    const user = JSON.parse(userString);
    // DTO của user-service có { id, name, phone, address, roles, ... }
    return {
      token: token,
      user: user,
      userId: user.id, // Lấy id từ DTO
    };
  } catch (e) {
    console.error("Lỗi parse user data, đang xóa localStorage:", e);
    localStorage.clear();
    window.location.href = "/login.html"; // Về trang đăng nhập
    return null;
  }
}

// --- KHỞI CHẠY LẤY AUTH ---
const authInfo = getAuthInfo();
if (!authInfo) {
  // Dừng toàn bộ script nếu không có auth
  throw new Error("Người dùng chưa đăng nhập. Dừng script.");
}

// --- SỬ DỤNG BIẾN TOÀN CỤC MỚI ---
const LOGGED_IN_USER_ID = authInfo.userId;
const AUTH_TOKEN = authInfo.token;
const LOGGED_IN_USER = authInfo.user;
// ----------------------------------------

// --- SWEETALERT CONFIG ---
const Toast = Swal.mixin({
  toast: true,
  position: "top-end",
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.onmouseenter = Swal.stopTimer;
    toast.onmouseleave = Swal.resumeTimer;
  },
  showClass: {
    popup: "animate__animated animate__fadeInRight animate__faster",
  },
  hideClass: {
    popup: "animate__animated animate__fadeOutRight animate__faster",
  },
});

const swalTheme = {
  customClass: {
    confirmButton:
      "bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded-lg",
    cancelButton:
      "bg-gray-300 hover:bg-gray-400 text-gray-800 font-bold py-2 px-4 rounded-lg ml-3",
  },
  buttonsStyling: false,
  showClass: { popup: "animate__animated animate__zoomIn animate__faster" },
  hideClass: { popup: "animate__animated animate__zoomOut animate__faster" },
};

// --- DỮ LIỆU USER THẬT TỪ USER-SERVICE ---
// async function getRealUserData(userId) { ... } // <-- ĐÃ XÓA HÀM NÀY

// --- DOM ELEMENT REFERENCES ---
const productTypeSelector = document.getElementById("productTypeSelector");
const categoryInput = document.getElementById("category");
const specificFieldsContainer = document.getElementById("specificFields");
const sellForm = document.getElementById("sellForm");
const submitButton = sellForm?.querySelector('button[type="submit"]');
const imagePreview = document.getElementById("imagePreview");
const imageUploadInput = document.getElementById("imageUpload");
const brandField = document.getElementById("brandField");
const yearField = document.getElementById("yearField");
const suggestPriceBtn = document.getElementById("suggestPriceBtn");
const suggestedPriceResult = document.getElementById("suggestedPriceResult");
const priceInput = document.getElementById("price");

let selectedType = null;

// --- EVENT LISTENERS ---
window.addEventListener("load", initializeForm);
if (productTypeSelector) {
  productTypeSelector.addEventListener("click", (e) => {
    const button = e.target.closest(".product-type-btn");
    if (!button) return;
    setActiveButton(button);
    selectedType = button.dataset.type;
    if (categoryInput) categoryInput.value = button.textContent.trim();
    updateFormVisibility(selectedType);
  });
}
if (sellForm) {
  sellForm.addEventListener("submit", handleFormSubmit);
}
if (imageUploadInput) {
  imageUploadInput.addEventListener("change", handleFileSelection);
}
if (imagePreview) {
  imagePreview.addEventListener("click", handleImageDeletion);
}
if (suggestPriceBtn) {
  suggestPriceBtn.addEventListener("click", handleSuggestPrice);
}
if (suggestedPriceResult) {
  suggestedPriceResult.addEventListener("click", (event) => {
    if (event.target.id === "applySuggestedPriceBtn") {
      applySuggestedPrice();
    } else if (
      event.target.id === "closeSuggestionBtn" ||
      event.target.closest("#closeSuggestionBtn")
    ) {
      closeSuggestionBox();
    }
  });
}

// --- CORE FUNCTIONS ---

async function uploadImages(imageFiles) {
  if (!imageFiles || imageFiles.length === 0) return [];
  const formData = new FormData();
  imageFiles.forEach((file) => formData.append("files", file));

  // <-- THÊM TOKEN VÀO HEADER
  const response = await fetch(`${API_BASE_URL}/files/upload`, {
    method: "POST",
    headers: {
      // KHÔNG set 'Content-Type' khi dùng FormData
      Authorization: `Bearer ${AUTH_TOKEN}`,
    },
    body: formData,
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => response.statusText);
    console.error("Upload error:", response.status, errorText);
    throw new Error(`Tải ảnh thất bại: ${errorText}`);
  }
  return response.json();
}

async function handleSuggestPrice() {
  if (!suggestPriceBtn) return;
  const originalBtnText = suggestPriceBtn.innerHTML;
  suggestPriceBtn.disabled = true;
  suggestPriceBtn.innerHTML = `<i class="fas fa-spinner fa-spin mr-1"></i> Đang...`;
  try {
    if (!sellForm) throw new Error("Form not found.");
    const formData = new FormData(sellForm);
    if (!selectedType) {
      showMessage("Vui lòng chọn Loại Sản Phẩm.", "warning");
      return;
    }
    const brand = formData.get("brand")?.trim();
    const year = parseInt(formData.get("year_of_manufacture"), 10);
    const conditionId = parseInt(formData.get("condition_id"), 10);
    if (!brand || isNaN(year) || isNaN(conditionId) || conditionId <= 0) {
      showMessage("Nhập Hãng, Năm SX, Tình Trạng để gợi ý giá.", "warning");
      return;
    }
    const requestData = {
      productType: selectedType,
      brand: brand,
      yearOfManufacture: year,
      conditionId: conditionId,
      warrantyPolicy: formData.get("warranty_status"),
      mileage: parseInt(formData.get("mileage"), 10) || null,
      rangePerCharge: parseInt(formData.get("range"), 10) || null,
      batteryCapacity:
        formData.get("battery_capacity") ||
        formData.get("battery_capacity_pin") ||
        null,
      batteryType: formData.get("battery_type") || null,
      chargeCycles: parseInt(formData.get("charge_cycles"), 10) || null,
      batteryLifespan: formData.get("usage_time")
        ? `${formData.get("usage_time")} tháng`
        : null,
      maxSpeed: parseInt(formData.get("max_speed"), 10) || null,
      color: formData.get("color")?.trim() || null,
      chargeTime: formData.get("charge_time")?.trim() || null,
      compatibleVehicle: formData.get("compatibility")?.trim() || null,
    };

    // Hàm callApi đã được cập nhật để gửi token
    const response = await callApi("/pricing/suggest", "POST", requestData);

    if (response && typeof response.suggestedPrice === "number") {
      currentSuggestedPrice = response.suggestedPrice;
      const formattedPrice = new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(response.suggestedPrice);
      if (suggestedPriceResult) {
        suggestedPriceResult.innerHTML = `
            <div class="p-3 suggestion-box-yellow border rounded-lg text-sm flex items-center justify-between animate__animated animate__fadeIn">
              <span class="flex items-center">
                <i class="fas fa-lightbulb mr-2 suggestion-icon"></i> Giá gợi ý: <strong class="text-lg ml-1" id="suggestedPriceValue">${formattedPrice}</strong>
              </span>
              <div class="flex items-center space-x-2">
                 <button type="button" id="applySuggestedPriceBtn" class="bg-green-500 hover:bg-green-600 text-white text-xs font-semibold py-1 px-3 rounded-md transition duration-200">
                    Áp dụng
                 </button>
                 <button type="button" id="closeSuggestionBtn" class="text-xl font-bold close-suggestion-btn" title="Đóng gợi ý">&times;</button> </div>
            </div>`;
        suggestedPriceResult.style.display = "block";
      }
      showMessage("Đã nhận được giá gợi ý!", "success");
    } else {
      throw new Error("Phản hồi gợi ý giá không hợp lệ.");
    }
  } catch (error) {
    console.error("Suggest Price Error:", error);
    showMessage(`Lỗi gợi ý giá: ${error.message || "Lỗi kết nối."}`, "error");
    if (suggestedPriceResult) suggestedPriceResult.style.display = "none";
    currentSuggestedPrice = null;
  } finally {
    if (suggestPriceBtn) {
      suggestPriceBtn.disabled = false;
      suggestPriceBtn.innerHTML = originalBtnText;
    }
  }
}

function applySuggestedPrice() {
  if (currentSuggestedPrice !== null && priceInput) {
    priceInput.value = currentSuggestedPrice;
    showMessage("Đã áp dụng giá gợi ý.", "success");
    closeSuggestionBox();
  } else {
    showMessage("Không có giá gợi ý.", "warning");
  }
}
function closeSuggestionBox() {
  if (suggestedPriceResult) {
    suggestedPriceResult.style.display = "none";
    suggestedPriceResult.innerHTML = "";
  }
}

async function handleFormSubmit(event) {
  event.preventDefault();

  // --- [CHẶN CỨNG] KIỂM TRA NGAY LẬP TỨC ---
  console.log("Đang kiểm tra số lượng ảnh:", allImageFiles.length);
  if (allImageFiles.length > 8) {
    // Dùng số 8 trực tiếp để tránh lỗi biến
    Swal.fire({
      icon: "error",
      title: "Quá nhiều ảnh",
      text: `Bạn đang cố gửi ${allImageFiles.length} ảnh. Hệ thống chỉ cho phép tối đa 8 ảnh.`,
      confirmButtonText: "Tôi sẽ xóa bớt",
      ...swalTheme,
    });
    return; // Dừng ngay lập tức
  }
  // -----------------------------------------

  if (isSubmitting || !submitButton) return;

  if (!selectedType) {
    showMessage("Chọn loại sản phẩm.", "warning");
    return;
  }

  if (allImageFiles.length === 0) {
    showMessage("Tải ít nhất 1 ảnh.", "warning");
    return;
  }

  const requiredInputs = sellForm.querySelectorAll("[required]");
  let firstInvalidField = null;
  for (const input of requiredInputs) {
    if (
      (input.tagName === "SELECT" && input.value === "") ||
      (input.tagName !== "SELECT" && !input.value.trim())
    ) {
      input.focus();
      firstInvalidField = input.labels?.[0]?.textContent || input.name;
      break;
    }
  }
  if (firstInvalidField) {
    showMessage(`Vui lòng điền: ${firstInvalidField}.`, "warning");
    return;
  }

  isSubmitting = true;
  const originalButtonText = submitButton.innerHTML;
  submitButton.disabled = true;
  submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Đang xử lý...`;

  try {
    const formData = new FormData(event.target);
    const imageFiles = allImageFiles;
    let imageUrls = [];
    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Tải ảnh (${imageFiles.length})...`;

    imageUrls = await uploadImages(imageFiles);

    const validImageUrls =
      imageUrls?.filter(
        (url) => typeof url === "string" && url.startsWith("/uploads/")
      ) ?? [];

    if (validImageUrls.length === 0 && imageFiles.length > 0) {
      throw new Error("Tải ảnh không thành công.");
    }

    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Lưu sản phẩm...`;
    const productData = {
      productName: formData.get("title")?.trim(),
      productType: selectedType,
      price: parseInt(formData.get("price")?.replace(/\D/g, ""), 10) || 0,
      description: formData.get("description")?.trim(),
      aiSuggestedPrice: currentSuggestedPrice,
    };

    if (
      !productData.productName ||
      !productData.productType ||
      productData.price <= 0
    ) {
      throw new Error("Thông tin SP cơ bản không hợp lệ.");
    }

    const createdProduct = await callApi("/products", "POST", productData);
    if (!createdProduct?.productId) {
      throw new Error("Không tạo được sản phẩm.");
    }
    const productId = createdProduct.productId;

    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Lưu chi tiết...`;
    const specData = {
      product: { productId },
      condition: { conditionId: parseInt(formData.get("condition_id"), 10) },
      brand: formData.get("brand")?.trim(),
      yearOfManufacture:
        parseInt(formData.get("year_of_manufacture"), 10) || null,
      warrantyPolicy: formData.get("warranty_status"),
      chargeTime: formData.get("charge_time") || null,
      chargeCycles: formData.get("charge_cycles")
        ? parseInt(formData.get("charge_cycles"), 10)
        : null,
      batteryType: formData.get("battery_type") || null,
    };

    if (selectedType !== "battery") {
      specData.mileage = parseInt(formData.get("mileage"), 10) || null;
      specData.batteryCapacity = formData.get("battery_capacity") || null;
      specData.maxSpeed = parseInt(formData.get("max_speed"), 10) || null;
      specData.rangePerCharge = parseInt(formData.get("range"), 10) || null;
      specData.color = formData.get("color") || null;
    } else {
      specData.batteryLifespan = formData.get("usage_time")
        ? `${formData.get("usage_time")} tháng`
        : null;
      specData.batteryCapacity = formData.get("battery_capacity_pin") || null;
      specData.compatibleVehicle = formData.get("compatibility") || null;
    }
    await callApi("/specifications", "POST", specData);

    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Lưu tin đăng...`;
    const listingData = {
      product: { productId },
      userId: LOGGED_IN_USER_ID,
      phone: formData.get("phone")?.trim(),
      location: formData.get("location")?.trim(),
    };
    if (!listingData.phone || !listingData.location) {
      throw new Error("Nhập SĐT và Khu vực.");
    }
    await callApi("/listings", "POST", listingData);

    submitButton.innerHTML = `<i class="fas fa-spinner fa-spin mr-2"></i> Liên kết ảnh...`;
    if (validImageUrls.length > 0) {
      const imagePromises = validImageUrls.map((url) =>
        callApi("/images", "POST", { product: { productId }, imageUrl: url })
      );
      await Promise.all(imagePromises);
    } else {
      console.warn("No valid image URLs to link.");
    }

    Swal.fire({
      icon: "success",
      title: "Đăng tin thành công!",
      text: "Tin của bạn sẽ sớm được duyệt.",
      confirmButtonText: "Tuyệt vời!",
      ...swalTheme,
    });
    resetForm();
  } catch (error) {
    console.error("Submit Error:", error);
    if (error.message.includes("401") || error.message.includes("403")) {
      Swal.fire({
        icon: "error",
        title: "Phiên đăng nhập hết hạn",
        text: "Vui lòng đăng nhập lại để tiếp tục.",
        confirmButtonText: "Đăng nhập",
        ...swalTheme,
      }).then(() => {
        localStorage.clear();
        window.location.href = "/login.html";
      });
    } else {
      Swal.fire({
        icon: "error",
        title: "Đăng tin thất bại",
        text: `Lỗi: ${error.message || "Lỗi không xác định."}. Thử lại.`,
        confirmButtonText: "Đã hiểu",
        ...swalTheme,
      });
    }
  } finally {
    isSubmitting = false;
    if (submitButton) {
      submitButton.disabled = false;
      submitButton.innerHTML = originalButtonText;
    }
  }
}

async function callApi(endpoint, method, body = null) {
  const options = {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${AUTH_TOKEN}`, // <-- THÊM TOKEN VÀO ĐÂY
    },
  };
  if (body) {
    options.body = JSON.stringify(body);
  }
  console.log(`API Call: ${method} ${API_BASE_URL}${endpoint}`, body || "");
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
    console.log(
      `API Resp Status: ${response.status} for ${method} ${endpoint}`
    );
    if (!response.ok) {
      let errorData = {
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
      try {
        const errorJson = await response.json();
        if (errorJson?.message) {
          errorData = errorJson;
        } else {
          errorData.message = (await response.text()) || response.statusText;
        }
      } catch (e) {
        errorData.message = `HTTP ${response.status}: ${response.statusText}`;
      }
      throw new Error(errorData.message);
    }
    if (response.status === 204) {
      return null;
    }
    const contentType = response.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      const d = await response.json();
      console.log(`API Resp JSON:`, d);
      return d;
    }
    const t = await response.text();
    console.log(`API Resp Text:`, t);
    return t;
  } catch (err) {
    console.error(`API Network Error: ${method} ${endpoint}:`, err);
    throw new Error(`Lỗi mạng: Không thể kết nối. (${err.message})`);
  }
}

// --- UTILITY & UI FUNCTIONS ---

function resetForm() {
  if (!sellForm) return;
  sellForm.reset();
  if (categoryInput) categoryInput.value = "Chưa chọn";
  if (imagePreview)
    imagePreview.innerHTML =
      '<p class="text-gray-400 text-sm self-center">Chưa có ảnh nào.</p>';
  allImageFiles = [];
  selectedType = null;
  setActiveButton(null);
  updateFormVisibility(null);
  currentSuggestedPrice = null;
  if (suggestedPriceResult) {
    suggestedPriceResult.style.display = "none";
    suggestedPriceResult.innerHTML = "";
  }
  initializeForm(); // Gọi lại để load SĐT/Địa chỉ
}

async function initializeForm() {
  // <-- SỬA HÀM NÀY ĐỂ DÙNG LOCALSTORAGE
  try {
    const user = LOGGED_IN_USER; // Dùng user đã load từ authInfo
    const ph = document.getElementById("phone");
    const loc = document.getElementById("location");

    // Tự động điền SĐT và địa chỉ nếu user đã có
    if (ph && !ph.value && user.phone) ph.value = user.phone;
    if (loc && !loc.value && user.address) loc.value = user.address;
  } catch (e) {
    console.error("Lỗi tải user data:", e.message);
    showMessage("Không thể tải thông tin SĐT/Địa chỉ.", "error");
  }

  // Phần còn lại giữ nguyên
  if (!selectedType) {
    const btn = document.querySelector('.product-type-btn[data-type="car"]');
    if (btn) {
      setActiveButton(btn);
      selectedType = "car";
      if (categoryInput) categoryInput.value = btn.textContent.trim();
      updateFormVisibility(selectedType);
    }
  }
  const nav = document.querySelector(".navbar");
  if (nav) {
    document.body.style.paddingTop = `${nav.offsetHeight}px`;
  }
}

function setActiveButton(button) {
  if (!productTypeSelector) return;
  productTypeSelector.querySelectorAll(".product-type-btn").forEach((b) => {
    b.classList.remove("bg-green-600", "text-white", "border-green-600");
    b.classList.add("bg-white", "text-gray-700", "border-gray-200");
  });
  if (button) {
    button.classList.add("bg-green-600", "text-white", "border-green-600");
    button.classList.remove("bg-white", "text-gray-700", "border-gray-200");
  }
}

function updateFormVisibility(type) {
  if (!specificFieldsContainer || !brandField || !yearField) {
    console.error("Elements missing for form update.");
    return;
  }
  specificFieldsContainer.innerHTML = "";
  const bi = document.getElementById("brand");
  const yi = document.getElementById("year");
  if (type) {
    brandField.classList.remove("hidden");
    yearField.classList.remove("hidden");
    bi?.setAttribute("required", "required");
    yi?.setAttribute("required", "required");
  } else {
    brandField.classList.add("hidden");
    yearField.classList.add("hidden");
    bi?.removeAttribute("required");
    yi?.removeAttribute("required");
  }
  let fieldsHTML = "";
  if (type === "car" || type === "motorbike" || type === "bike") {
    fieldsHTML = ` <div> <label for="range" class="block text-sm font-medium text-gray-700 mb-1">Quãng Đường / 1 Lần Sạc (Km) <span class="text-red-500">*</span></label> <input type="number" id="range" name="range" min="1" placeholder="Ví dụ: 300" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="mileage" class="block text-sm font-medium text-gray-700 mb-1">Số Km Đã Đi <span class="text-red-500">*</span></label> <input type="number" id="mileage" name="mileage" min="0" placeholder="Ví dụ: 15000" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="battery_capacity" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng Pin (kWh/Ah) <span class="text-red-500">*</span></label> <input type="text" id="battery_capacity" name="battery_capacity" placeholder="Ví dụ: 30 kWh hoặc 20 Ah" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="battery_type" class="block text-sm font-medium text-gray-700 mb-1">Loại Pin <span class="text-red-500">*</span></label> <input type="text" id="battery_type" name="battery_type" placeholder="Ví dụ: Lithium-ion, LFP..." required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành <span class="text-red-500">*</span></label> <select id="warranty_status" name="warranty_status" required class="form-select w-full p-3 border rounded-lg"> <option value="">-- Chọn --</option> <option value="Còn bảo hành chính hãng">Còn bảo hành chính hãng</option> <option value="Hết bảo hành">Hết bảo hành</option> </select> </div> <div> <label for="color" class="block text-sm font-medium text-gray-700 mb-1">Màu Sắc</label> <input type="text" id="color" name="color" placeholder="Ví dụ: Trắng, Đỏ" class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="max_speed" class="block text-sm font-medium text-gray-700 mb-1">Tốc Độ Tối Đa (Km/h) <span class="text-red-500">*</span></label> <input type="number" id="max_speed" name="max_speed" min="10" placeholder="Ví dụ: 120" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="charge_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Sạc</label> <input type="text" id="charge_time" name="charge_time" placeholder="Ví dụ: 6-8 giờ" class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="charge_cycles" class="block text-sm font-medium text-gray-700 mb-1">Số Lần Sạc (Ước tính)</label> <input type="number" id="charge_cycles" name="charge_cycles" min="0" placeholder="Ví dụ: 500" class="form-input w-full p-3 border rounded-lg"> </div> `;
  } else if (type === "battery") {
    fieldsHTML = ` <div> <label for="battery_type" class="block text-sm font-medium text-gray-700 mb-1">Loại Pin <span class="text-red-500">*</span></label> <input type="text" id="battery_type" name="battery_type" placeholder="Ví dụ: Lithium-ion, LFP, Ắc quy khô..." required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="battery_capacity_pin" class="block text-sm font-medium text-gray-700 mb-1">Dung Lượng (Ah/kWh) <span class="text-red-500">*</span></label> <input type="text" id="battery_capacity_pin" name="battery_capacity_pin" placeholder="Ví dụ: 60 Ah hoặc 4 kWh" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="usage_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Đã Dùng (Tháng) <span class="text-red-500">*</span></label> <input type="number" id="usage_time" name="usage_time" min="1" placeholder="Ví dụ: 18" required class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="warranty_status" class="block text-sm font-medium text-gray-700 mb-1">Tình Trạng Bảo Hành <span class="text-red-500">*</span></label> <select id="warranty_status" name="warranty_status" required class="form-select w-full p-3 border rounded-lg"> <option value="">-- Chọn --</option> <option value="Còn bảo hành chính hãng">Còn bảo hành chính hãng</option> <option value="Hết bảo hành">Hết bảo hành</option> </select> </div> <div> <label for="charge_time" class="block text-sm font-medium text-gray-700 mb-1">Thời Gian Sạc</label> <input type="text" id="charge_time" name="charge_time" placeholder="Ví dụ: 6-8 giờ" class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="charge_cycles" class="block text-sm font-medium text-gray-700 mb-1">Số Lần Sạc (Ước tính)</label> <input type="number" id="charge_cycles" name="charge_cycles" min="0" placeholder="Ví dụ: 500" class="form-input w-full p-3 border rounded-lg"> </div> <div> <label for="compatibility" class="block text-sm font-medium text-gray-700 mb-1">Tương Thích Với Xe</label> <input type="text" id="compatibility" name="compatibility" placeholder="Ví dụ: VinFast Klara S, Yadea G5,..." class="form-input w-full p-3 border rounded-lg"> </div> `;
  }
  specificFieldsContainer.innerHTML =
    fieldsHTML ||
    '<p class="text-gray-500 md:col-span-2">Chọn loại SP để nhập thông số.</p>';
}

function handleFileSelection(event) {
  if (!event.target.files) return;
  const newFiles = Array.from(event.target.files);
  if (newFiles.length === 0) return;

  // --- [LOGIC MỚI] KIỂM TRA SỐ LƯỢNG ẢNH ---
  const currentCount = allImageFiles.length;
  const newCount = newFiles.length;

  if (currentCount + newCount > MAX_IMAGE_COUNT) {
    Swal.fire({
      icon: "warning",
      title: "Quá giới hạn ảnh",
      text: `Bạn chỉ được đăng tối đa ${MAX_IMAGE_COUNT} ảnh. Bạn đang có ${currentCount} ảnh và vừa chọn thêm ${newCount} ảnh.`,
      confirmButtonText: "Đã hiểu",
      ...swalTheme,
    });
    // Reset input để người dùng chọn lại
    event.target.value = null;
    return;
  }
  // ------------------------------------------

  let addedFilesCount = 0;
  let oversizedFilesInfo = [];

  newFiles.forEach((file) => {
    if (
      !["image/jpeg", "image/png", "image/webp", "image/jpg"].includes(
        file.type
      )
    ) {
      showMessage(`Loại file không hợp lệ: ${file.name}.`, "warning");
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      oversizedFilesInfo.push({ name: file.name, size: file.size });
    } else {
      if (
        !allImageFiles.some((f) => f.name === file.name && f.size === file.size)
      ) {
        allImageFiles.push(file);
        addedFilesCount++;
      }
    }
  });

  if (oversizedFilesInfo.length > 0) {
    const mbf = (MAX_FILE_SIZE / (1024 * 1024)).toFixed(1);
    let msg = `Ảnh vượt quá ${mbf}MB:<br>`;
    oversizedFilesInfo.forEach((i) => {
      const fmb = (i.size / (1024 * 1024)).toFixed(1);
      msg += `- ${escapeHtml(i.name)} (${fmb}MB)<br>`;
    });
    Swal.fire({
      icon: "warning",
      title: "Ảnh Quá Lớn",
      html: msg,
      confirmButtonText: "Đã hiểu",
      ...swalTheme,
    });
  }

  if (addedFilesCount > 0) {
    renderImagePreviews();
  }

  // Reset value để cho phép chọn lại cùng 1 file nếu lỡ xóa nhầm
  event.target.value = null;
}

function renderImagePreviews() {
  if (!imagePreview) return;
  imagePreview.innerHTML = "";
  if (allImageFiles.length === 0) {
    imagePreview.innerHTML =
      '<p class="text-gray-400 text-sm self-center">Chưa có ảnh.</p>';
    return;
  }
  allImageFiles.forEach((file, index) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const pht = ` <div class="preview-image-container animate__animated animate__fadeIn"> <img src="${
        e.target.result
      }" alt="${escapeHtml(
        file.name
      )}"> <button type="button" class="delete-image-btn" data-index="${index}">&times;</button> </div> `;
      imagePreview.insertAdjacentHTML("beforeend", pht);
    };
    reader.onerror = (e) => {
      console.error("Lỗi đọc file:", file.name, e);
      showMessage(`Lỗi đọc ảnh: ${file.name}`, "error");
    };
    reader.readAsDataURL(file);
  });
}

function handleImageDeletion(event) {
  const btn = event.target.closest(".delete-image-btn");
  if (!btn) return;
  const idx = parseInt(btn.dataset.index, 10);
  if (!isNaN(idx) && idx >= 0 && idx < allImageFiles.length) {
    const cont = btn.closest(".preview-image-container");
    if (cont) {
      cont.classList.remove("animate__fadeIn");
      cont.classList.add("animate__zoomOut", "animate__faster");
      cont.addEventListener(
        "animationend",
        () => {
          if (allImageFiles[idx]) {
            allImageFiles.splice(idx, 1);
            renderImagePreviews();
          } else {
            renderImagePreviews();
          }
        },
        { once: true }
      );
    } else {
      if (allImageFiles[idx]) {
        allImageFiles.splice(idx, 1);
        renderImagePreviews();
      }
    }
  }
}

function showMessage(message, type = "info") {
  const iconType =
    type === "warning"
      ? "warning"
      : type === "error"
      ? "error"
      : type === "success"
      ? "success"
      : "info";
  Toast.fire({ icon: iconType, title: message });
}
function escapeHtml(unsafe) {
  if (!unsafe) return "";
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
