// Thay thế toàn bộ file: admin-listings.js
document.addEventListener("DOMContentLoaded", () => {
  // --- HÀM LẤY AUTH TỪ LOCALSTORAGE (THEO auth.js) ---
  function getAuthInfo() {
    const token = localStorage.getItem("token"); // DÙNG KEY TỪ auth.js
    const userString = localStorage.getItem("user"); // DÙNG KEY TỪ auth.js

    if (!token || !userString) {
      return null; // Không đăng nhập
    }
    try {
      const user = JSON.parse(userString);
      return {
        token: token,
        user: user,
        userId: user.id,
      };
    } catch (e) {
      return null; // Lỗi parse
    }
  }

  // Lấy thông tin Auth để dùng cho API
  const authInfo = getAuthInfo();
  const AUTH_TOKEN = authInfo ? authInfo.token : null;

  // Hiển thị nội dung trang ngay lập tức
  const authGate = document.getElementById("auth-gate");
  if (authGate) authGate.classList.remove("hidden");

  const GATEWAY_URL = ""; // URL của Gateway
  // --- CẤU HÌNH VÀ BIẾN ---
  const ADMIN_API_URL = `${GATEWAY_URL}/api/admin/listings`;
  const PUBLIC_API_URL = `${GATEWAY_URL}/api/listings`;
  const WS_URL = `${GATEWAY_URL}/ws-listing/ws`; // <-- THAY ĐỔI QUAN TRỌNG
  const BACKEND_ORIGIN = GATEWAY_URL;
  let currentPage = 0;
  let totalPages = 0;
  let currentStatus = "PENDING";
  let currentListingId = null;
  let debounceTimer;
  let stompClient = null;
  const PAGE_SIZE = 15;

  // --- DOM ELEMENTS ---
  const filterContainer = document.getElementById("filterContainer");
  const tableBody = document.getElementById("listingsTableBody");
  const tableStatus = document.getElementById("tableStatus");
  const paginationContainer = document.getElementById("paginationContainer");
  const rejectModal = document.getElementById("rejectModal");
  const rejectReasonInput = document.getElementById("rejectReason");
  const viewDetailsModal = document.getElementById("viewDetailsModal");
  const searchForm = document.getElementById("searchForm");
  const searchInput = document.getElementById("searchInput");
  const reasonChecklist = document.getElementById("reasonChecklist");

  const modalAnimation = {
    showClass: {
      popup: "animate__animated animate__zoomIn animate__faster",
    },
    hideClass: {
      popup: "animate__animated animate__zoomOut animate__faster",
    },
  };
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
    ...modalAnimation,
  });

  // --- HÀM CALL API (THÊM TOKEN) ---
  const callApi = async (url, method = "GET", body = null) => {
    const options = {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${AUTH_TOKEN}`, // <-- GỬI TOKEN
      },
    };
    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);

    if (response.status === 401 || response.status === 403) {
      // Token hết hạn hoặc không có quyền
      Toast.fire({
        icon: "error",
        title: "Phiên đăng nhập hết hạn hoặc không có quyền.",
      });
      localStorage.clear();
      window.location.href = "/login.html";
      throw new Error("Unauthorized");
    }

    if (!response.ok) {
      const errorData = await response
        .json()
        .catch(() => ({ message: `Lỗi HTTP: ${response.statusText}` }));
      throw new Error(
        errorData.message || `Lỗi không xác định (${response.status})`
      );
    }
    return response.status === 204 ? null : response.json();
  };

  // --- HÀM LẤY DỮ LIỆU ---
  const fetchListings = async (page) => {
    tableBody.innerHTML = "";
    tableStatus.textContent = "Đang tải dữ liệu...";
    tableStatus.style.display = "block";
    const searchQuery = searchInput.value.trim();
    const isSearching = searchQuery.length > 0;
    try {
      let data;
      let apiUrl = "";
      if (isSearching) {
        const sortParam = "sort=listingDate,desc";
        apiUrl = `${ADMIN_API_URL}/search?query=${searchQuery}&page=${page}&size=${PAGE_SIZE}&${sortParam}`;
      } else {
        let sortParam = "sort=listingDate,desc";
        if (currentStatus === "PENDING") {
          sortParam = "sort=product.createdAt,asc";
        }
        apiUrl = `${ADMIN_API_URL}?status=${currentStatus}&page=${page}&size=${PAGE_SIZE}&${sortParam}`;
      }
      data = await callApi(apiUrl); // <-- Đã gửi token
      renderListings(data.content);
      currentPage = data.number;
      totalPages = data.totalPages;
      renderPagination();
    } catch (error) {
      if (error.message !== "Unauthorized") {
        tableStatus.textContent = `Lỗi khi tải dữ liệu: ${error.message}`;
        Toast.fire({ icon: "error", title: "Không thể tải danh sách!" });
      }
    }
  };

  // --- HÀM RENDER ---
  const renderListings = (listings) => {
    if (!listings || listings.length === 0) {
      tableStatus.textContent = "Không có tin đăng nào phù hợp.";
      tableStatus.style.display = "block";
      tableBody.innerHTML = "";
      return;
    }
    tableStatus.style.display = "none";
    tableBody.innerHTML = listings
      .map((listing) => createListingRowHTML(listing))
      .join("");
  };
  const getStatusBadge = (status) => {
    const statuses = {
      PENDING: "bg-yellow-100 text-yellow-800",
      ACTIVE: "bg-green-100 text-green-800",
      SOLD: "bg-gray-100 text-gray-800",
      REJECTED: "bg-red-100 text-red-800",
    };
    return `<span class="px-2 py-1 text-xs font-semibold rounded-full ${
      statuses[status] || ""
    }">${status}</span>`;
  };

  const createListingRowHTML = (listing) => {
    const {
      listingId,
      product,
      userId,
      listingDate,
      listingStatus,
      verified,
      adminNotes,
    } = listing;
    const createdAt = product ? product.createdAt : listingDate;
    const productNameHTML = product?.productName
      ? product.productName
      : `<i class="text-gray-500">N/A</i>`;
    const verifiedBadgeHTML = verified
      ? `<span class="verified-badge-inline"><i class="fas fa-check-circle"></i>KĐ</span>`
      : "";

    const reasonButtonHTML =
      listingStatus === "REJECTED" && adminNotes
        ? `<button data-action="view-reason" data-reason="${String(
            adminNotes
          ).replace(
            /"/g,
            "&quot;"
          )}" class="btn-view-reason action-btn">Xem lý do</button>`
        : "";
    let actionsHTML = "";
    if (listingStatus === "PENDING") {
      actionsHTML = `<button data-id="${listingId}" data-action="approve" class="action-btn btn-approve text-xs">Duyệt</button> <button data-id="${listingId}" data-action="reject" class="action-btn btn-reject text-xs">Từ chối</button>`;
    } else if (listingStatus === "ACTIVE") {
      actionsHTML = verified
        ? `<span class="verified-badge-inline text-xs"><i class="fas fa-check-circle"></i> Đã kiểm định</span>`
        : `<button data-id="${listingId}" data-action="verify" class="action-btn btn-verify text-xs">Gắn nhãn KĐ</button>`;
    } else {
      actionsHTML = `<span class="text-gray-400 text-xs">---</span>`;
    }
    const viewButtonHTML = `<button data-id="${listingId}" data-action="view" class="action-btn btn-view text-xs mr-2">Xem</button>`;
    return `
        <tr class="listing-row border-b animate__animated animate__fadeInUp" data-listing-id="${listingId}">
            <td class="px-6 py-4 font-medium">${productNameHTML} ${verifiedBadgeHTML}</td>
            <td class="px-6 py-4">${userId}</td>
            <td class="px-6 py-4">${new Date(createdAt).toLocaleString(
              "vi-VN"
            )}</td>
            <td class="px-6 py-4 status-cell">
                ${getStatusBadge(listingStatus)}
                ${reasonButtonHTML}
            </td>
            <td class="px-6 py-4 text-center space-x-1 actions-cell">
                ${viewButtonHTML}
                ${actionsHTML}
            </td>
        </tr>
    `;
  };

  const renderPagination = () => {
    paginationContainer.innerHTML = "";
    if (totalPages <= 1) return;
    paginationContainer.innerHTML += `<button data-page="${
      currentPage - 1
    }" class="pagination-btn" ${
      currentPage === 0 ? "disabled" : ""
    }>Trước</button>`;
    const pagesToShow = getPaginationRange(currentPage, totalPages);
    pagesToShow.forEach((page) => {
      if (page === "...") {
        paginationContainer.innerHTML += `<span class="pagination-dots">...</span>`;
      } else {
        paginationContainer.innerHTML += `<button class="pagination-btn ${
          page === currentPage ? "active" : ""
        }" data-page="${page}">${page + 1}</button>`;
      }
    });
    paginationContainer.innerHTML += `<button data-page="${
      currentPage + 1
    }" class="pagination-btn" ${
      currentPage >= totalPages - 1 ? "disabled" : ""
    }>Sau</button>`;
  };

  const getPaginationRange = (currentPage, totalPages) => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i);
    }
    const pages = new Set();
    pages.add(0);
    if (currentPage > 2) {
      pages.add("...");
    }
    if (currentPage > 0) {
      pages.add(currentPage - 1);
    }
    pages.add(currentPage);
    if (currentPage < totalPages - 1) {
      pages.add(currentPage + 1);
    }
    if (currentPage < totalPages - 3) {
      pages.add("...");
    }
    pages.add(totalPages - 1);
    return Array.from(pages);
  };

  const productTypeMap = {
    car: "Ô Tô Điện",
    motorbike: "Xe Máy Điện",
    bike: "Xe Đạp Điện",
    battery: "Pin Đã Qua Sử Dụng",
  };
  const formatPrice = (price) => {
    if (!price || price === 0) return "Thương lượng";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };
  const populateViewModal = (listing) => {
    const { product, phone, location, listingStatus, listingDate, verified } =
      listing;
    if (!product) {
      Swal.fire({
        title: "Lỗi Dữ Liệu",
        text: "Không thể tải chi tiết: thiếu thông tin sản phẩm.",
        icon: "error",
        confirmButtonColor: "#dc2626",
      });
      return;
    }
    const spec = product.specification;
    const createdAt = product.createdAt || listingDate;
    const verifiedBadgeModal = verified
      ? `<span class="px-3 py-1 text-sm inline-flex items-center leading-5 font-semibold rounded-full bg-blue-100 text-blue-800 ml-2"><i class="fas fa-check-circle" style="font-size: 12px; margin-right: 6px;"></i>Đã Kiểm Định</span>`
      : "";
    document.getElementById("viewTitle").textContent = product.productName;
    document.getElementById("viewPrice").textContent = formatPrice(
      product.price
    );
    document.getElementById("viewProductType").textContent =
      productTypeMap[product.productType] || "Không xác định";
    document.getElementById("viewBrand").textContent = spec?.brand || "N/A";
    document.getElementById("viewLocation").textContent = location;
    document.getElementById("viewPhone").textContent = phone;
    document.getElementById("viewDate").textContent = new Date(
      createdAt
    ).toLocaleString("vi-VN");
    document.getElementById("viewStatus").innerHTML =
      getStatusBadge(listingStatus, true) + verifiedBadgeModal;
    document.getElementById("viewDescription").textContent =
      product.description;
    document.getElementById("viewImages").innerHTML =
      product.images && product.images.length > 0
        ? product.images
            .map(
              (img) =>
                `<div class="aspect-square overflow-hidden rounded-md"><img src="${BACKEND_ORIGIN}${img.imageUrl}" class="w-full h-full object-cover"></div>`
            )
            .join("")
        : '<p class="text-sm text-gray-500 col-span-full">Không có hình ảnh.</p>';
    const createSpecItem = (label, value) =>
      value
        ? `<div><strong class="text-gray-600">${label}:</strong> <span>${value}</span></div>`
        : "";
    let specsHTML = [
      createSpecItem("Bảo hành", spec?.warrantyPolicy),
      createSpecItem("Loại Pin", spec?.batteryType),
      createSpecItem("Thời gian sạc", spec?.chargeTime),
      createSpecItem("Số lần sạc", spec?.chargeCycles),
    ].join("");
    if (product.productType !== "battery") {
      specsHTML += [
        createSpecItem(
          "Quãng đường / 1 lần sạc ",
          spec?.rangePerCharge ? `${spec.rangePerCharge} km` : null
        ),
        createSpecItem(
          "Số km đã đi",
          spec?.mileage ? `${spec.mileage} km` : null
        ),
        createSpecItem("Dung lượng pin", spec?.batteryCapacity),
        createSpecItem("Màu sắc", spec?.color),
        createSpecItem(
          "Tốc độ tối đa",
          spec?.maxSpeed ? `${spec.maxSpeed} km/h` : null
        ),
      ].join("");
    } else {
      specsHTML += [
        createSpecItem("Dung lượng", spec?.batteryCapacity),
        createSpecItem("Thời gian đã dùng", spec?.batteryLifespan),
        createSpecItem("Tương thích xe", spec?.compatibleVehicle),
      ].join("");
    }
    document.getElementById("viewSpecs").innerHTML =
      specsHTML ||
      '<p class="text-sm text-gray-500">Không có thông số kỹ thuật.</p>';
    viewDetailsModal.classList.remove("hidden");
  };
  const updateTableRow = (listingData) => {
    const row = tableBody.querySelector(
      `tr[data-listing-id="${listingData.listingId}"]`
    );
    const isSearching = searchInput.value.trim().length > 0;
    const currentFilterStatus =
      filterContainer.querySelector(".active")?.dataset.status;
    const isPendingTab = currentStatus === "PENDING" && !isSearching;
    const statusMatchesFilter =
      isSearching ||
      !currentFilterStatus ||
      (listingData.listingStatus &&
        listingData.listingStatus.toUpperCase() ===
          currentFilterStatus.toUpperCase());
    if (row) {
      if (statusMatchesFilter) {
        const newRowHTML = createListingRowHTML(listingData);
        row.outerHTML = newRowHTML;
        const updatedRow = tableBody.querySelector(
          `tr[data-listing-id="${listingData.listingId}"]`
        );
        if (updatedRow) {
          updatedRow.classList.add("row-updated");
          setTimeout(() => updatedRow.classList.remove("row-updated"), 1200);
        }
      } else {
        removeTableRow(listingData.listingId);
      }
    } else {
      if (statusMatchesFilter && !isSearching) {
        const newRowHTML = createListingRowHTML(listingData);
        if (isPendingTab) {
          tableBody.insertAdjacentHTML("beforeend", newRowHTML);
        } else {
          tableBody.insertAdjacentHTML("afterbegin", newRowHTML);
        }
        const newRow = tableBody.querySelector(
          `tr[data-listing-id="${listingData.listingId}"]`
        );
        if (newRow) {
          newRow.classList.add("row-updated");
          setTimeout(() => newRow.classList.remove("row-updated"), 1200);
        }
        checkEmptyTable();
      }
    }
  };
  const removeTableRow = (listingId) => {
    const row = tableBody.querySelector(`tr[data-listing-id="${listingId}"]`);
    if (row) {
      console.log("Xóa hàng real-time:", listingId);
      row.classList.add(
        "animate__animated",
        "animate__fadeOut",
        "animate__faster"
      );
      row.addEventListener("animationend", () => {
        row.remove();
        checkEmptyTable();
      });
    } else {
      console.log("Không tìm thấy hàng để xóa:", listingId);
    }
  };
  const checkEmptyTable = () => {
    if (tableBody.children.length === 0) {
      tableStatus.textContent = "Không có tin đăng nào phù hợp.";
      tableStatus.style.display = "block";
    } else {
      tableStatus.style.display = "none";
    }
  };

  // --- XỬ LÝ SỰ KIỆN ---
  searchForm.addEventListener("submit", (e) => e.preventDefault());
  searchInput.addEventListener("input", () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const query = searchInput.value.trim();
      if (query.length > 0) {
        const activeFilter = filterContainer.querySelector(".active");
        if (activeFilter) activeFilter.classList.remove("active");
      } else {
        let activeFilter = filterContainer.querySelector(".active");
        if (!activeFilter) {
          activeFilter = filterContainer.querySelector(
            `[data-status="${currentStatus}"]`
          );
          if (activeFilter) activeFilter.classList.add("active");
          else {
            const firstFilter = filterContainer.querySelector(".filter-btn");
            if (firstFilter) {
              firstFilter.classList.add("active");
              currentStatus = firstFilter.dataset.status;
            }
          }
        } else {
          currentStatus = activeFilter.dataset.status;
        }
      }
      fetchListings(0);
    }, 400);
  });
  filterContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".filter-btn");
    if (!button) return;
    searchInput.value = "";
    const currentActive = filterContainer.querySelector(".active");
    if (currentActive) currentActive.classList.remove("active");
    button.classList.add("active");
    currentStatus = button.dataset.status;
    fetchListings(0);
  });
  paginationContainer.addEventListener("click", (e) => {
    const button = e.target.closest(".pagination-btn");
    if (button && !button.disabled) {
      fetchListings(parseInt(button.dataset.page));
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  });

  // <-- CÁC HÀM TRONG NÀY ĐÃ TỰ ĐỘNG DÙNG callApi (CÓ TOKEN)
  tableBody.addEventListener("click", async (e) => {
    const button = e.target.closest(".action-btn");
    if (!button || button.disabled) return;
    const id = button.dataset.id;
    const action = button.dataset.action;

    try {
      if (action === "view") {
        const listingDetails = await callApi(`${PUBLIC_API_URL}/${id}`);
        populateViewModal(listingDetails);
      } else if (action === "view-reason") {
        const rawReason =
          button.dataset.reason || "Không có lý do được cung cấp.";
        let formattedReason = rawReason;
        const noteSeparator = ". Ghi chú: ";
        const separatorIndex = rawReason.indexOf(noteSeparator);
        if (separatorIndex !== -1) {
          const presetPart = rawReason.substring(0, separatorIndex + 1);
          const customPart = rawReason.substring(
            separatorIndex + noteSeparator.length
          );
          formattedReason = `${presetPart}<br><br><strong>Ghi chú:</strong> ${customPart}`;
        }
        Swal.fire({
          title: "Lý do từ chối",
          html: `<div class="text-left p-2 bg-gray-50 rounded whitespace-pre-wrap">${formattedReason}</div>`,
          icon: "info",
          confirmButtonText: "Đã hiểu",
          confirmButtonColor: "#dc2626",
          ...modalAnimation,
        });
      } else if (action === "approve") {
        Swal.fire({
          title: "Xác nhận Duyệt?",
          text: "Bạn có chắc chắn muốn DUYỆT tin đăng này?",
          icon: "question",
          showCancelButton: true,
          confirmButtonText: "Duyệt",
          cancelButtonText: "Hủy",
          confirmButtonColor: "#16a34a",
          cancelButtonColor: "#6c757d",
          ...modalAnimation,
        }).then(async (result) => {
          if (result.isConfirmed) {
            button.textContent = "Đang...";
            button.disabled = true;
            try {
              const updatedListing = await callApi(
                `${ADMIN_API_URL}/${id}/approve`,
                "POST"
              );
              Toast.fire({ icon: "success", title: "Đã duyệt tin đăng!" });
              updateTableRow(updatedListing);
            } catch (error) {
              Swal.fire("Lỗi", `Duyệt thất bại: ${error.message}`, "error");
              button.textContent = "Duyệt";
              button.disabled = false;
            }
          }
        });
      } else if (action === "reject") {
        currentListingId = id;
        rejectModal.classList.remove("hidden");
        rejectReasonInput.value = "";
        reasonChecklist
          .querySelectorAll('input[type="checkbox"]')
          .forEach((cb) => (cb.checked = false));
        rejectReasonInput.focus();
      } else if (action === "verify") {
        Swal.fire({
          title: "Gắn nhãn Kiểm Định?",
          text: "Bạn có chắc chắn muốn GẮN NHÃN KIỂM ĐỊNH cho tin này?",
          icon: "question",
          showCancelButton: true,
          confirmButtonText: "Gắn nhãn",
          cancelButtonText: "Hủy",
          confirmButtonColor: "#2563eb",
          cancelButtonColor: "#6c757d",
          ...modalAnimation,
        }).then(async (result) => {
          if (result.isConfirmed) {
            button.textContent = "Đang...";
            button.disabled = true;
            try {
              const updatedListing = await callApi(
                `${ADMIN_API_URL}/${id}/verify`,
                "POST"
              );
              Toast.fire({ icon: "success", title: "Đã gắn nhãn kiểm định!" });
              updateTableRow(updatedListing);
            } catch (error) {
              Swal.fire("Lỗi", `Gắn nhãn thất bại: ${error.message}`, "error");
              const verifyButton = tableBody.querySelector(
                `button[data-id="${id}"][data-action="verify"]`
              );
              if (verifyButton) {
                verifyButton.textContent = "Gắn nhãn KĐ";
                verifyButton.disabled = false;
              }
            }
          }
        });
      }
    } catch (error) {
      if (error.message !== "Unauthorized") {
        Swal.fire("Lỗi", `Thao tác thất bại: ${error.message}`, "error");
      }
    }
  });

  document
    .getElementById("cancelRejectBtn")
    .addEventListener("click", () => rejectModal.classList.add("hidden"));

  document
    .getElementById("confirmRejectBtn")
    .addEventListener("click", async () => {
      const selectedReasons = Array.from(
        reasonChecklist.querySelectorAll('input[type="checkbox"]:checked')
      ).map((cb) => cb.value);
      const customReason = rejectReasonInput.value.trim();
      if (selectedReasons.length === 0 && !customReason) {
        Swal.fire({
          title: "Thiếu thông tin",
          text: "Vui lòng chọn ít nhất một lý do hoặc nhập ghi chú.",
          icon: "warning",
          confirmButtonColor: "#f97316",
          ...modalAnimation,
        });
        return;
      }
      let combinedReason = "";
      if (selectedReasons.length > 0) {
        combinedReason += "" + selectedReasons.join("; ");
      }
      if (customReason) {
        if (combinedReason) {
          combinedReason += ". ";
        }
        combinedReason += "Ghi chú: " + customReason;
      }
      const rejectButton = document.getElementById("confirmRejectBtn");
      const originalText = rejectButton.textContent;
      try {
        rejectButton.textContent = "Đang xử lý...";
        rejectButton.disabled = true;
        const updatedListing = await callApi(
          `${ADMIN_API_URL}/${currentListingId}/reject`,
          "POST",
          { reason: combinedReason }
        );
        rejectModal.classList.add("hidden");
        Toast.fire({ icon: "success", title: "Đã từ chối tin đăng." });
        updateTableRow(updatedListing);
      } catch (error) {
        Swal.fire("Lỗi", `Từ chối thất bại: ${error.message}`, "error");
      } finally {
        rejectButton.textContent = originalText;
        rejectButton.disabled = false;
      }
    });

  document
    .getElementById("closeViewModalBtn")
    .addEventListener("click", () => viewDetailsModal.classList.add("hidden"));
  document
    .getElementById("closeViewModalBtnFooter")
    .addEventListener("click", () => viewDetailsModal.classList.add("hidden"));

  // --- LOGIC WEBSOCKET (THÊM TOKEN) ---
  const onAdminUpdateReceived = (payload) => {
    try {
      const messageData = JSON.parse(payload.body);
      console.log("Nhận được cập nhật qua WebSocket:", messageData);
      if (messageData.action === "delete" && messageData.listingId) {
        removeTableRow(messageData.listingId);
      } else if (messageData.listingId && messageData.listingStatus) {
        if (
          !(messageData.product && messageData.product.productName) &&
          messageData.listingStatus === "PENDING"
        ) {
          console.warn("WS: Tin PENDING mới bị thiếu dữ liệu, gọi API...");
          setTimeout(() => {
            callApi(`${PUBLIC_API_URL}/${messageData.listingId}`) // callApi đã có token
              .then((fullListingData) => {
                if (fullListingData) {
                  console.log(
                    "API: Lấy full data cho tin PENDING thành công, cập nhật UI."
                  );
                  updateTableRow(fullListingData);
                }
              })
              .catch((err) => {
                console.error("API: Lỗi khi lấy full data cho WS update:", err);
                updateTableRow(messageData);
              });
          }, 500);
        } else {
          console.log("WS: Cập nhật UI với dữ liệu từ WebSocket.");
          updateTableRow(messageData);
        }
      } else {
        console.warn("Nhận được tin nhắn WebSocket không hợp lệ:", messageData);
      }
    } catch (e) {
      console.error("Lỗi xử lý thông báo WebSocket:", e);
      console.error("Nội dung thô (raw) gây lỗi:", payload.body);
    }
  };
  const connectWebSocketForAdmin = () => {
    try {
      const socket = new SockJS(WS_URL);
      stompClient = Stomp.over(socket);
      stompClient.debug = null;

      // <-- THÊM TOKEN VÀO HEADER KẾT NỐI
      const headers = {
        Authorization: `Bearer ${AUTH_TOKEN}`,
      };

      stompClient.connect(
        headers, // <-- GỬI HEADER
        (frame) => {
          console.log("Admin đã kết nối WebSocket:", frame);
          stompClient.subscribe(
            `/topic/admin/listingUpdate`,
            onAdminUpdateReceived
          );
        },
        (error) => {
          console.error("Lỗi WebSocket Admin:", error.toString());
          if (
            error.toString().includes("403") ||
            error.toString().includes("401")
          ) {
            console.error("WS Auth thất bại. Đang đăng xuất.");
            localStorage.clear();
            window.location.href = "/login.html";
          } else {
            setTimeout(connectWebSocketForAdmin, 7000 + Math.random() * 3000);
          }
        }
      );
    } catch (e) {
      console.error("Không thể khởi tạo SockJS cho Admin:", e);
      setTimeout(connectWebSocketForAdmin, 10000);
    }
  };

  // --- KHỞI CHẠY ---
  fetchListings(0);
  connectWebSocketForAdmin();
});
