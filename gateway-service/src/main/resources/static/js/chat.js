// File: /js/chat.js
const CHAT_SERVICE_URL = "";
const BACKEND_ORIGIN = "";
const FRONTEND_ORIGIN = "";

let stompClient = null;
let currentUserId = localStorage.getItem("userId");
let currentUserName =
  localStorage.getItem("userName") || "User " + currentUserId;

let currentRecipientId = null;
let currentProductId = null;
let lastDateRendered = null;

let isBlockedByMe = false;
let isBlockedByOther = false;
let statusInterval = null;
let blockInterval = null; // [THÊM MỚI] Biến quản lý kiểm tra chặn
let selectedMessageIdForRecall = null;

// Modal elements
const mediaModal = document.getElementById("mediaModal");
const mediaViewer = document.getElementById("mediaViewer");
const modalCloseBtn = document.querySelector(".modal-close");

// === BỘ THÔNG BÁO TÙY CHỈNH (THAY THẾ ALERT/CONFIRM) ===

/**
 * Hiển thị thông báo (toast) thay vì alert()
 * @param {string} message Nội dung thông báo
 * @param {'info' | 'error' | 'success'} type Loại thông báo
 */
function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast-item ${type}`;

  let icon = "fas ";
  if (type === "error") icon += "fa-exclamation-circle";
  else if (type === "success") icon += "fa-check-circle";
  else icon += "fa-info-circle";

  toast.innerHTML = `<i class="${icon}"></i><span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 4000);
}

/**
 * Hiển thị modal xác nhận (thay thế confirm())
 * @param {string} message Câu hỏi xác nhận
 * @returns {Promise<boolean>} Trả về true nếu đồng ý, false nếu hủy
 */
function showConfirm(message) {
  const modal = document.getElementById("confirmModal");
  const msgEl = document.getElementById("confirmModalMessage");
  const btnOk = document.getElementById("confirmBtnOk");
  const btnCancel = document.getElementById("confirmBtnCancel");

  if (!modal || !msgEl || !btnOk || !btnCancel) return Promise.resolve(false);

  msgEl.textContent = message;
  modal.classList.add("visible");

  return new Promise((resolve) => {
    const onOk = () => {
      modal.classList.remove("visible");
      btnOk.removeEventListener("click", onOk);
      btnCancel.removeEventListener("click", onCancel);
      resolve(true);
    };
    const onCancel = () => {
      modal.classList.remove("visible");
      btnOk.removeEventListener("click", onOk);
      btnCancel.removeEventListener("click", onCancel);
      resolve(false);
    };

    btnOk.addEventListener("click", onOk);
    btnCancel.addEventListener("click", onCancel);
  });
}

// === KẾT THÚC BỘ THÔNG BÁO TÙY CHỈNH ===

document.addEventListener("DOMContentLoaded", () => {
  if (!currentUserId) {
    // [THAY ĐỔI] Sử dụng toast thay vì alert
    showToast("Vui lòng đăng nhập!", "error");
    window.location.href = "/login.html";
    return;
  }

  // Báo Online
  fetch(`${CHAT_SERVICE_URL}/api/chat/connect?userId=${currentUserId}`, {
    method: "POST",
  });

  resetChatUI();
  connectWebSocket();
  loadRecentConversations();
  parseUrlParams();

  // Gửi Text
  const msgForm = document.getElementById("messageForm");
  if (msgForm) {
    msgForm.addEventListener("submit", (e) => {
      e.preventDefault();
      sendMessage("TEXT");
    });
  }

  // Gửi Ảnh
  const btnSelectImage = document.getElementById("btnSelectImage");
  const imageInput = document.getElementById("imageInput");
  if (btnSelectImage && imageInput) {
    btnSelectImage.addEventListener("click", () => {
      if (!checkBlockAction(true)) imageInput.click();
    });
    imageInput.addEventListener("change", (e) => handleFileUpload(e, "IMAGE"));
  }

  // Gửi Video
  const btnSelectVideo = document.getElementById("btnSelectVideo");
  const videoInput = document.getElementById("videoInput");
  if (btnSelectVideo && videoInput) {
    btnSelectVideo.addEventListener("click", () => {
      if (!checkBlockAction(true)) videoInput.click();
    });
    videoInput.addEventListener("change", (e) => handleFileUpload(e, "VIDEO"));
  }

  // Gửi Link SP
  const btnLink = document.getElementById("sendProductLinkBtn");
  if (btnLink) {
    btnLink.onclick = function (e) {
      e.preventDefault();
      e.stopPropagation();
      sendProductMessage();
    };
  }

  // Tìm kiếm
  const searchInput = document.getElementById("searchInput");
  if (searchInput)
    searchInput.addEventListener("input", (e) =>
      filterConversations(e.target.value)
    );

  // Click Global: Đóng các menu
  document.addEventListener("click", (e) => {
    const msgMenu = document.getElementById("msgContextMenu");
    if (msgMenu && msgMenu.style.display === "block")
      msgMenu.style.display = "none";

    if (!e.target.closest(".sidebar-menu-btn")) {
      document
        .querySelectorAll(".sidebar-menu")
        .forEach((m) => m.classList.add("hidden"));
    }
  });

  const ctxRecallBtn = document.getElementById("ctxRecallBtn");
  if (ctxRecallBtn) {
    ctxRecallBtn.addEventListener("click", () => {
      if (selectedMessageIdForRecall) recallMessage(selectedMessageIdForRecall);
      document.getElementById("msgContextMenu").style.display = "none";
    });
  }

  if (modalCloseBtn)
    modalCloseBtn.addEventListener("click", () =>
      mediaModal.classList.add("hidden")
    );
  if (mediaModal)
    mediaModal.addEventListener("click", (e) => {
      if (e.target === mediaModal) mediaModal.classList.add("hidden");
    });

  setupMenuOptions();
});

// --- 1. SIDEBAR ITEM (ĐÃ NÂNG CẤP MENU 3 CHẤM) ---
function createSidebarItem(id, name, lastMsg, isActive, isUnread = false) {
  const div = document.createElement("div");
  div.setAttribute("data-id", id);

  let classes = `sidebar-item flex items-center p-3 hover:bg-gray-100 cursor-pointer border-b border-gray-100 transition relative group`;
  if (isActive) classes += " bg-green-50";
  if (isUnread) classes += " unread bg-green-50";

  div.className = classes;

  let preview = lastMsg || "";
  if (preview === "Tin nhắn đã bị thu hồi") {
    preview = `<i class="fas fa-undo-alt text-xs text-gray-400 mr-1"></i> Tin nhắn đã bị thu hồi`;
  }
  if (preview.length > 25 && !preview.startsWith("<i"))
    preview = preview.substring(0, 25) + "...";

  // [NÂNG CẤP] Thêm dấu chấm chưa đọc (unread-dot)
  div.innerHTML = `
      <img src="https://placehold.co/40x40/e0e0e0/909090?text=${name
        .charAt(0)
        .toUpperCase()}" class="w-10 h-10 rounded-full object-cover border">
      <div class="ml-3 flex-1 overflow-hidden">
           <h4 class="font-semibold text-sm text-gray-800 truncate">${name}</h4>
           <p class="text-xs text-gray-500 truncate font-medium preview-text ${
             isUnread ? "font-bold text-gray-800" : ""
           }">${preview}</p>
      </div>
      
      ${isUnread ? '<div class="unread-dot ml-2"></div>' : ""}
      
      <button class="sidebar-menu-btn absolute right-2 top-1/2 transform -translate-y-1/2 w-8 h-8 rounded-full hover:bg-gray-200 text-gray-400 hover:text-gray-600 opacity-0 group-hover:opacity-100 transition-all focus:outline-none flex items-center justify-center z-10">
          <i class="fas fa-ellipsis-v text-xs"></i>
      </button>

      <div class="sidebar-menu hidden absolute right-8 top-8 w-48 bg-white border rounded-lg shadow-lg z-20 overflow-hidden animate-fade-in">
          <div class="btn-delete-sidebar w-full text-left px-3 py-2 text-xs text-red-600 hover:bg-red-50 flex items-center transition cursor-pointer">
              <i class="fas fa-trash-alt mr-2"></i> Xóa cuộc trò chuyện
          </div>
      </div>
  `;

  div.onclick = (e) => {
    if (
      e.target.closest(".sidebar-menu-btn") ||
      e.target.closest(".sidebar-menu")
    )
      return;

    div.classList.remove("unread");
    div.classList.remove("bg-green-50");
    // [SỬA] Xóa dấu chấm khi click
    const dot = div.querySelector(".unread-dot");
    if (dot) dot.remove();

    window.location.href = `?to=${id}&name=${name}`;
  };

  const menuBtn = div.querySelector(".sidebar-menu-btn");
  const menu = div.querySelector(".sidebar-menu");

  menuBtn.onclick = (e) => {
    e.stopPropagation();
    document.querySelectorAll(".sidebar-menu").forEach((m) => {
      if (m !== menu) m.classList.add("hidden");
    });
    menu.classList.toggle("hidden");
  };

  const deleteBtn = div.querySelector(".btn-delete-sidebar");

  deleteBtn.onclick = async (e) => {
    e.stopPropagation();
    if (!(await showConfirm("Xóa cuộc trò chuyện này?"))) return;

    try {
      const res = await fetch(
        `${CHAT_SERVICE_URL}/api/chat/conversation?userId=${currentUserId}&partnerId=${id}`,
        { method: "DELETE" }
      );
      if (res.ok) {
        div.remove();

        // [THÊM MỚI] Kiểm tra xem danh sách có rỗng không
        const list = document.getElementById("chatList");
        if (list && list.querySelectorAll(".sidebar-item").length === 0) {
          renderEmptyChatList();
        }
        // [KẾT THÚC THÊM MỚI]

        if (id == currentRecipientId) {
          resetChatUI();
          window.history.pushState({}, "", window.location.pathname);
        }
      } else {
        showToast("Lỗi khi xóa.", "error");
      }
    } catch (err) {
      console.error(err);
      showToast("Lỗi kết nối.", "error");
    }
  };

  return div;
}

// --- CÁC HÀM KHÁC ---

function parseUrlParams() {
  const params = new URLSearchParams(window.location.search);
  const toId = params.get("to");
  const toName = params.get("name");

  const pid = params.get("pid");
  const pname = params.get("pname");
  const pprice = params.get("pprice");
  const pimg = params.get("pimg");

  if (toId) {
    loadChatWindow(toId, toName || `User ${toId}`);
    if (pid) {
      currentProductId = pid;
      showProductContext(pid, pname, pprice, pimg);
    }
  } else {
    resetChatUI();
  }
}

function sendProductMessage() {
  if (!currentProductId || !currentRecipientId) {
    showToast("Lỗi: Thiếu thông tin để gửi.", "error");
    return;
  }
  const nameEl = document.getElementById("ctxName");
  const priceEl = document.getElementById("ctxPrice");
  const imgEl = document.getElementById("ctxImg");
  const name = nameEl ? nameEl.textContent : "Sản phẩm";
  const price = priceEl ? priceEl.textContent : "";
  const img = imgEl ? imgEl.src : "";
  const productData = JSON.stringify({
    id: currentProductId,
    name: name,
    price: price,
    img: img,
  });
  sendMessage("PRODUCT", productData);
  const ctxBox = document.getElementById("productContext");
  if (ctxBox) ctxBox.classList.add("hidden");
}

function showProductContext(id, name, price, imgUrl) {
  const box = document.getElementById("productContext");
  if (!box) return;
  document.getElementById("ctxName").textContent = name || "Sản phẩm";
  if (price) {
    const rawPrice = String(price).replace(/\D/g, "");
    const numericPrice = parseInt(rawPrice);
    if (!isNaN(numericPrice)) {
      document.getElementById("ctxPrice").textContent = new Intl.NumberFormat(
        "vi-VN",
        { style: "currency", currency: "VND" }
      ).format(numericPrice);
    } else {
      document.getElementById("ctxPrice").textContent = price;
    }
  }
  if (imgUrl)
    document.getElementById("ctxImg").src = `${BACKEND_ORIGIN}${imgUrl}`;
  box.classList.remove("hidden");
}

function connectWebSocket() {
  const socket = new SockJS(
    `${CHAT_SERVICE_URL}/ws-chat/ws?userId=${currentUserId}`
  );
  stompClient = Stomp.over(socket);
  stompClient.debug = null;
  stompClient.connect(
    {},
    function (frame) {
      console.log("✅ WS Connected");
      stompClient.subscribe(
        `/queue/messages/${currentUserId}`,
        function (messageOutput) {
          const msg = JSON.parse(messageOutput.body);

          if (msg.type === "READ_RECEIPT") {
            if (currentRecipientId && msg.readerId == currentRecipientId)
              markAllMessagesAsSeenUI();
            return;
          }

          if (msg.type === "RECALL") {
            updateMessageAsRecalled(msg.messageId);
            updateSidebarOnRecall(msg.messageId);
            return;
          }

          handleIncomingMessage(msg);
        }
      );
    },
    function (error) {
      setTimeout(connectWebSocket, 5000);
    }
  );
}

function handleIncomingMessage(msg) {
  // [SỬA] Xử lý khi nhận tin nhắn báo lỗi từ Server
  if (msg.content === "BLOCKED_BY_USER" && parseInt(msg.senderId) === 0) {
    // Xóa tin nhắn "Đang gửi..." nếu có
    const pending = document.querySelector(".msg-pending");
    if (pending) pending.closest(".animate-fade-in").remove();

    // Cập nhật trạng thái và hiện thông báo đỏ duy nhất
    isBlockedByOther = true;
    toggleBlockMessage(true);
    return;
  }
  const msgSenderId = parseInt(msg.senderId);
  const myId = parseInt(currentUserId);
  const partnerId = currentRecipientId ? parseInt(currentRecipientId) : null;

  if (msgSenderId === myId) {
    const pendingMsg = document.querySelector(".msg-pending");
    if (pendingMsg) {
      const parentDiv = pendingMsg.closest(".animate-fade-in");
      if (parentDiv) parentDiv.remove();
    }
  }

  if (partnerId && (msgSenderId === partnerId || msgSenderId === myId)) {
    renderMessage(msg);
    scrollToBottom();
    if (msgSenderId === partnerId) markMessagesAsRead(partnerId);
  }
  updateSidebarOnIncoming(msg);
}

function sendMessage(type = "TEXT", contentOverride = null) {
  if (checkBlockAction()) return;
  const input = document.getElementById("messageInput");
  const content = type === "TEXT" ? input.value.trim() : contentOverride;
  if (!content || !currentRecipientId) return;

  const chatMessage = {
    senderId: parseInt(currentUserId),
    senderName: null,
    recipientId: parseInt(currentRecipientId),
    content: content,
    msgType: type,
    productId: currentProductId ? parseInt(currentProductId) : null,
    timestamp: new Date().toISOString(),
    read: false,
  };

  renderMessage(chatMessage, true);
  scrollToBottom();

  if (type === "TEXT") input.value = "";

  if (stompClient && stompClient.connected) {
    stompClient.send("/app/private-message", {}, JSON.stringify(chatMessage));
    updateSidebarOnIncoming({
      ...chatMessage,
      partnerName: document.getElementById("currentChatName").textContent,
    });
  } else {
    connectWebSocket();
  }
}

function renderMessage(msg, isPending = false) {
  const msgArea = document.getElementById("messageArea");

  // [SỬA LỖI] Đưa logic xóa placeholder lên ĐẦU HÀM
  // Xóa placeholder TRƯỚC khi render bất cứ thứ gì khác
  if (msgArea.querySelector(".fa-comments")) {
    msgArea.innerHTML = "";
  }
  const placeholder = msgArea.querySelector(".text-center");
  if (placeholder) {
    placeholder.remove();
  }
  // [KẾT THÚC SỬA LỖI]

  const msgDate = new Date(msg.timestamp);
  const dateStr = msgDate.toLocaleDateString("vi-VN");

  if (lastDateRendered !== dateStr) {
    const today = new Date().toLocaleDateString("vi-VN");
    const divider = document.createElement("div");
    divider.className = "date-divider";
    divider.innerHTML = `<span>${
      dateStr === today ? "Hôm nay" : dateStr
    }</span>`;
    msgArea.appendChild(divider);
    lastDateRendered = dateStr;
  }

  // Phần còn lại của hàm giữ nguyên
  const isMe = parseInt(msg.senderId) === parseInt(currentUserId);
  const time = msgDate.toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });

  const div = document.createElement("div");
  if (msg.id && !isPending) div.id = `msg-${msg.id}`;

  div.className = `flex flex-col ${
    isMe ? "items-end" : "items-start"
  } mb-3 animate-fade-in group`;

  let contentHtml = "";
  let bubbleClass = "";

  const isRecalled = msg.recalled || msg.content === "Tin nhắn đã bị thu hồi";

  if (isRecalled) {
    contentHtml = `<i class="fas fa-undo-alt text-xs mr-1.5 opacity-70"></i> Tin nhắn đã bị thu hồi`;
    bubbleClass =
      "msg-recalled-bubble not-italic text-gray-700 text-sm px-3 py-2 bg-gray-100 rounded-lg border border-gray-200";
  } else {
    if (msg.msgType === "IMAGE") {
      const fullImgUrl = `${CHAT_SERVICE_URL}${msg.content}`;
      contentHtml = `<img src="${fullImgUrl}" class="max-w-[200px] rounded-lg border bg-white cursor-pointer" onclick="showMediaModal('${fullImgUrl}', 'image')" />`;
      bubbleClass = "p-1";
    } else if (msg.msgType === "VIDEO") {
      const fullVideoUrl = `${CHAT_SERVICE_URL}${msg.content}`;
      contentHtml = ` <div class="relative max-w-[200px] cursor-pointer" onclick="showMediaModal('${fullVideoUrl}', 'video')"> <video class="w-full rounded-lg border bg-black"><source src="${fullVideoUrl}" type="video/mp4"></video> <div class="absolute inset-0 flex items-center justify-center pointer-events-none"><i class="fas fa-play-circle text-4xl text-white opacity-80"></i></div> </div> `;
      bubbleClass = "p-1 relative";
    } else if (msg.msgType === "PRODUCT") {
      try {
        const p = JSON.parse(msg.content);
        const link = `${FRONTEND_ORIGIN}/product_detail.html?id=${p.id}`;
        contentHtml = ` <div class="product-bubble" onclick="window.open('${link}', '_blank')"> <img src="${p.img}" alt="${p.name}"> <div class="p-info"> <h4>${p.name}</h4> <div class="price">${p.price}</div> <div class="text-xs text-blue-500 mt-1">Nhấn để xem chi tiết</div> </div> </div> `;
        bubbleClass = "p-0 bg-transparent border-none shadow-none";
      } catch (e) {
        contentHtml = `<p>Sản phẩm: ${msg.content}</p>`;
      }
    } else {
      contentHtml = `<p>${msg.content}</p>`;
      bubbleClass = `max-w-[75%] ${
        isMe
          ? "bg-green-600 text-white rounded-l-xl rounded-tr-xl"
          : "bg-white text-gray-800 border rounded-r-xl rounded-tl-xl"
      } p-3 shadow-sm text-sm break-words relative msg-bubble`;
    }
  }

  if (isPending) bubbleClass += " msg-pending opacity-70";

  div.innerHTML = `<div class="${bubbleClass}">${contentHtml}</div>`;

  if (isMe) {
    const statusText = isPending
      ? "Đang gửi..."
      : msg.read
      ? "Đã xem"
      : "Đã gửi";

    const metaHtml = isRecalled
      ? `<div class="text-[10px] text-gray-400 mt-1 text-right mr-1">${time}</div>`
      : `<div class="flex items-center justify-end mt-1 text-[10px] text-gray-400"> <span class="mr-1">${time}</span> <span class="msg-status italic">${statusText}</span> </div>`;

    div.innerHTML += metaHtml;

    if (!isPending && msg.id && !isRecalled) {
      const diffHours = Math.abs(new Date() - msgDate) / 36e5;
      if (diffHours < 24) {
        const targetSelector =
          msg.msgType === "TEXT"
            ? ".msg-bubble"
            : msg.msgType === "PRODUCT"
            ? ".product-bubble"
            : msg.msgType === "VIDEO"
            ? ".relative"
            : "img";
        const target = div.querySelector(targetSelector);

        if (target) {
          target.addEventListener("contextmenu", (e) =>
            showContextMenu(e, msg.id)
          );
        }
      }
    }
  } else {
    div.innerHTML += `<div class="text-gray-400 text-[10px] mt-1 ml-1">${time}</div>`;
  }

  msgArea.appendChild(div);
}
function showMediaModal(url, type) {
  if (!mediaModal || !mediaViewer) return;
  mediaViewer.innerHTML = "";
  if (type === "image") {
    const img = document.createElement("img");
    img.src = url;
    img.className = "max-h-[80vh] max-w-[80vw] object-contain";
    mediaViewer.appendChild(img);
  } else if (type === "video") {
    const video = document.createElement("video");
    video.src = url;
    video.controls = true;
    video.autoplay = true;
    video.className = "max-h-[80vh] max-w-[80vw] object-contain";
    mediaViewer.appendChild(video);
  }
  mediaModal.classList.remove("hidden");
}

async function handleFileUpload(e, fileType) {
  const f = e.target.files[0];
  if (!f) return;
  if (checkBlockAction(true)) {
    e.target.value = "";
    return;
  }
  const fd = new FormData();
  fd.append("file", f);
  try {
    const r = await fetch(`${CHAT_SERVICE_URL}/api/chat/upload`, {
      method: "POST",
      body: fd,
    });
    if (r.ok) sendMessage(fileType, await r.text());
  } catch (e) {
    showToast("Lỗi upload file.", "error");
  }
  e.target.value = "";
}

function updateMessageAsRecalled(messageId) {
  const msgDiv = document.getElementById(`msg-${messageId}`);
  if (!msgDiv) return;

  const contentWrapper = msgDiv.firstElementChild;

  if (contentWrapper) {
    // [NÂNG CẤP] Thay đổi class cho tin nhắn thu hồi
    contentWrapper.className =
      "msg-recalled-bubble not-italic text-gray-700 text-sm px-3 py-2 bg-gray-100 rounded-lg border border-gray-200";
    contentWrapper.innerHTML = `<i class="fas fa-undo-alt text-xs mr-1.5 opacity-70"></i> Tin nhắn đã bị thu hồi`;

    const newClone = contentWrapper.cloneNode(true);
    msgDiv.replaceChild(newClone, contentWrapper);

    const metaStatus = msgDiv.querySelector(".msg-status");
    if (metaStatus) {
      metaStatus.style.display = "none";
    }
  }
}

function updateSidebarOnRecall(messageId) {
  loadRecentConversations();
}

async function recallMessage(messageId) {
  try {
    const res = await fetch(
      `${CHAT_SERVICE_URL}/api/chat/recall/${messageId}?userId=${currentUserId}`,
      { method: "DELETE" }
    );
    if (!res.ok) {
      showToast("Không thể thu hồi.", "error");
    }
  } catch (e) {
    showToast("Lỗi kết nối.", "error");
  }
}

function showContextMenu(e, messageId) {
  e.preventDefault();
  selectedMessageIdForRecall = messageId;
  const menu = document.getElementById("msgContextMenu");
  menu.style.display = "block";

  const screenW = window.innerWidth;
  const screenH = window.innerHeight;
  const menuW = menu.offsetWidth;
  const menuH = menu.offsetHeight;

  let left = e.pageX;
  let top = e.pageY;

  if (left + menuW > screenW) {
    left = screenW - menuW - 10;
  }
  if (top + menuH > screenH) {
    top = screenH - menuH - 10;
  }

  menu.style.left = `${left}px`;
  menu.style.top = `${top}px`;
}

function updateSidebarOnIncoming(msg) {
  const list = document.getElementById("chatList");

  // [SỬA] Sửa lại cách kiểm tra placeholder
  const placeholder = list.querySelector(".flex-col.items-center");
  if (placeholder) {
    list.innerHTML = "";
  }
  // [KẾT THÚC SỬA]

  let targetId, displayName, content;

  if (msg.content === "Tin nhắn đã bị thu hồi" || msg.recalled) {
    content = `<i class="fas fa-undo-alt text-xs text-gray-400 mr-1"></i> Tin nhắn đã bị thu hồi`;
  } else if (msg.msgType === "IMAGE") content = "[Hình ảnh]";
  else if (msg.msgType === "VIDEO") content = "[Video]";
  else if (msg.msgType === "PRODUCT") content = "[Sản phẩm]";
  else content = msg.content;

  if (parseInt(msg.senderId) === parseInt(currentUserId)) {
    targetId = msg.recipientId;
    displayName =
      msg.partnerName || document.getElementById("currentChatName").textContent;
    content = "Bạn: " + content;
  } else {
    targetId = msg.senderId;
    displayName =
      msg.senderName && !msg.senderName.startsWith("User ")
        ? msg.senderName
        : msg.senderName || `User ${targetId}`;
  }

  const existingItem = list.querySelector(
    `div.sidebar-item[data-id='${targetId}']`
  );
  if (existingItem) existingItem.remove();

  const isActive = targetId == currentRecipientId;
  const isUnread =
    !isActive && parseInt(msg.senderId) !== parseInt(currentUserId);

  list.prepend(
    createSidebarItem(targetId, displayName, content, isActive, isUnread)
  );
}

async function deleteConversation() {
  if (!(await showConfirm("Xóa cuộc trò chuyện này?"))) return;
  try {
    await fetch(
      `${CHAT_SERVICE_URL}/api/chat/conversation?userId=${currentUserId}&partnerId=${currentRecipientId}`,
      { method: "DELETE" }
    );
    const item = document.querySelector(
      `div.sidebar-item[data-id='${currentRecipientId}']`
    );
    if (item) item.remove();

    // [THÊM MỚI] Kiểm tra xem danh sách có rỗng không
    const list = document.getElementById("chatList");
    if (list && list.querySelectorAll(".sidebar-item").length === 0) {
      renderEmptyChatList();
    }
    // [KẾT THÚC THÊM MỚI]

    resetChatUI();
    window.history.pushState({}, "", window.location.pathname);
  } catch (e) {
    showToast("Lỗi xóa chat.", "error");
  }
}

// --- Copy đoạn này đè lên hàm resetChatUI cũ ---
function resetChatUI() {
  // [FIX] Dừng kiểm tra chặn khi thoát chat
  if (blockInterval) {
    clearInterval(blockInterval);
    blockInterval = null;
  }

  currentRecipientId = null;
  document.getElementById("currentChatName").textContent = "Chọn người để chat";
  document.getElementById("currentChatAvatar").src =
    "https://placehold.co/40x40/f1f5f9/94a3b8?text=?";
  document.getElementById("currentChatStatus").textContent =
    "Chưa chọn người nhận";
  document.getElementById(
    "messageArea"
  ).innerHTML = `<div class="flex flex-col items-center justify-center h-full text-gray-400"><i class="fas fa-comments text-6xl text-gray-200 mb-4"></i><p>Chọn người chat</p></div>`;
  document.getElementById("messageInput").disabled = true;
  document.getElementById("messageInput").placeholder =
    "Chọn người chat để nhập tin...";
  document.getElementById("sendBtn").disabled = true;
  const chatOpt = document.getElementById("chatOptionsBtn");
  if (chatOpt) chatOpt.classList.add("hidden");
  const ctx = document.getElementById("productContext");
  if (ctx) ctx.classList.add("hidden");
}

function scrollToBottom() {
  const m = document.getElementById("messageArea");
  if (m) m.scrollTop = m.scrollHeight;
}

function renderSystemMessage(text, isError = false) {
  const msgArea = document.getElementById("messageArea");
  const div = document.createElement("div");
  div.className = "flex justify-center my-3 animate-fade-in";
  const bgColor = isError ? "bg-red-100" : "bg-gray-100";
  const textColor = isError ? "text-red-600" : "text-gray-500";
  const borderColor = isError ? "border-red-200" : "border-gray-200";
  div.innerHTML = `<span class="${bgColor} ${textColor} text-xs px-3 py-1 rounded-full border ${borderColor} text-center">${text}</span>`;
  msgArea.appendChild(div);
  scrollToBottom();
}

// [SỬA] Kiểm tra chặn khi người dùng cố bấm nút Gửi
function checkBlockAction(silent = false) {
  if (isBlockedByMe) {
    if (!silent) showToast("Bạn đã chặn người dùng này.", "error");
    return true;
  }

  if (isBlockedByOther) {
    // Thay vì render thêm dòng mới, ta chỉ cần làm dòng thông báo cũ rung lên
    const existingMsg = document.getElementById("unique-block-msg");

    if (existingMsg) {
      // Hiệu ứng rung nhẹ để nhắc nhở (nếu bạn có class animate-pulse của tailwind)
      existingMsg.classList.remove("animate-fade-in"); // Reset animation
      void existingMsg.offsetWidth; // Trigger reflow
      existingMsg.classList.add("animate-pulse");
      setTimeout(() => existingMsg.classList.remove("animate-pulse"), 500);
    } else {
      // Nếu chưa hiện thì hiện nó ra
      toggleBlockMessage(true);
    }

    if (!silent) showToast("Không thể gửi tin nhắn.", "error");
    return true;
  }

  return false;
}

// File: /js/chat.js

async function loadRecentConversations() {
  const list = document.getElementById("chatList");
  const params = new URLSearchParams(window.location.search);
  const activeId = params.get("to");

  const res = await fetch(
    `${CHAT_SERVICE_URL}/api/chat/conversations/${currentUserId}`
  );
  if (res.ok) {
    const d = await res.json();
    list.innerHTML = "";
    if (d.length === 0) {
      // [SỬA] Gọi hàm helper mới
      renderEmptyChatList();
    }

    // Vòng lặp forEach này sẽ tự động không chạy nếu d.length === 0
    d.forEach((c) => {
      let preview = c.lastMessage;
      if (c.msgType === "IMAGE") preview = "[Hình ảnh]";
      else if (c.msgType === "VIDEO") preview = "[Video]";
      else if (c.msgType === "PRODUCT") preview = "[Sản phẩm]";
      else if (c.recalled) preview = "Tin nhắn đã bị thu hồi";

      const isActive = c.partnerId == activeId;
      const showUnread = c.unread && !isActive;

      list.appendChild(
        createSidebarItem(
          c.partnerId,
          c.partnerName,
          preview,
          isActive,
          showUnread
        )
      );
    });
  }
}

// [THÊM MỚI] Hàm helper để hiển thị danh sách rỗng
/**
 * Hiển thị thông báo khi danh sách chat rỗng
 */
function renderEmptyChatList() {
  const list = document.getElementById("chatList");
  if (!list) return;

  list.innerHTML = `
    <div class='flex flex-col items-center justify-center h-full text-gray-400 mt-10 px-4'>
        <i class='fas fa-inbox text-5xl text-gray-200 mb-4'></i>
        <p class='text-sm font-medium text-center'>Không có tin nhắn nào</p>
        <p class='text-xs text-center text-gray-400 mt-1'>Bắt đầu trò chuyện để hiển thị tin nhắn của bạn ở đây.</p>
    </div>`;
}
// [KẾT THÚC THÊM MỚI]

// --- Copy đoạn này đè lên hàm loadChatWindow cũ ---
async function loadChatWindow(recipientId, recipientName) {
  currentRecipientId = recipientId;
  lastDateRendered = null;
  document.getElementById("currentChatName").textContent = recipientName;
  document.getElementById(
    "currentChatAvatar"
  ).src = `https://placehold.co/40x40/e0e0e0/909090?text=${recipientName
    .charAt(0)
    .toUpperCase()}`;
  const input = document.getElementById("messageInput");
  input.disabled = false;
  input.placeholder = "Nhập tin nhắn...";
  input.focus();
  document.getElementById("sendBtn").disabled = false;
  document.getElementById("chatOptionsBtn").classList.remove("hidden");

  startTrackingUserStatus(recipientId);
  markMessagesAsRead(recipientId);

  // --- [FIX BẮT ĐẦU] Logic Polling kiểm tra chặn ---
  // 1. Xóa interval cũ nếu đang chạy
  if (blockInterval) clearInterval(blockInterval);

  // 2. Kiểm tra ngay lập tức
  await checkBlockStatus();

  // 3. Thiết lập kiểm tra định kỳ mỗi 3 giây
  blockInterval = setInterval(checkBlockStatus, 3000);
  // --- [FIX KẾT THÚC] ---

  const msgArea = document.getElementById("messageArea");
  msgArea.innerHTML = `<div class="flex justify-center mt-10"><div class="loader"></div></div>`;

  try {
    const response = await fetch(
      `${CHAT_SERVICE_URL}/api/chat/history/${currentUserId}/${recipientId}`
    );
    msgArea.innerHTML = "";
    if (response.ok) {
      const history = await response.json();
      if (history.length === 0) {
        msgArea.innerHTML = `<div class="flex flex-col items-center justify-center h-full text-gray-400"><i class="fas fa-comments text-6xl text-gray-200 mb-4"></i><p>Bắt đầu cuộc trò chuyện với ${recipientName}</p></div>`;
      } else {
        history.forEach((msg) => renderMessage(msg));
        scrollToBottom();
      }
    }
  } catch (e) {
    msgArea.innerHTML = `<div class="text-center text-red-500 mt-10">Lỗi tải lịch sử chat</div>`;
  }
}

function startTrackingUserStatus(id) {
  if (statusInterval) clearInterval(statusInterval);
  updateUserStatus(id);
  statusInterval = setInterval(() => {
    if (currentRecipientId == id) updateUserStatus(id);
  }, 60000);
}

async function updateUserStatus(id) {
  try {
    const r = await fetch(`${CHAT_SERVICE_URL}/api/chat/user-status/${id}`);

    if (r.ok) {
      const u = await r.json();
      const statusEl = document.getElementById("currentChatStatus");
      if (statusEl) {
        statusEl.innerHTML = u.online
          ? `<span class="text-green-600 font-bold text-[11px]">● Đang hoạt động</span>`
          : `<span class="text-gray-400 text-[11px]">Truy cập lần cuối ${timeAgo(
              u.lastActiveAt
            )}</span>`;
      }
    }
  } catch (e) {}
}

function timeAgo(d) {
  if (!d) return "gần đây";
  const s = Math.floor((new Date() - new Date(d)) / 1000);
  if (s < 60) return "vài giây trước";
  if (s < 3600) return Math.floor(s / 60) + " phút trước";
  if (s < 86400) return Math.floor(s / 3600) + " giờ trước";
  return Math.floor(s / 86400) + " ngày trước";
}

function markMessagesAsRead(id) {
  fetch(
    `${CHAT_SERVICE_URL}/api/chat/mark-read?userId=${currentUserId}&partnerId=${id}`,
    { method: "POST" }
  );
}

function markAllMessagesAsSeenUI() {
  document.querySelectorAll(".msg-status").forEach((e) => {
    if (e.textContent === "Đã gửi") e.textContent = "Đã xem";
  });
}

function setupMenuOptions() {
  const btn = document.getElementById("chatOptionsBtn");
  const menu = document.getElementById("chatOptionsMenu");
  if (btn && menu) {
    btn.onclick = (e) => {
      e.stopPropagation();
      menu.classList.toggle("hidden");
    };
    document.onclick = (e) => {
      if (!btn.contains(e.target)) menu.classList.add("hidden");
    };
  }
  const btnBlock = document.getElementById("btnBlockUser");
  if (btnBlock) btnBlock.onclick = toggleBlockUser;
  const btnDel = document.getElementById("btnDeleteChat");
  if (btnDel) btnDel.onclick = deleteConversation;
}

async function toggleBlockUser() {
  const e = isBlockedByMe ? "unblock" : "block";
  const actionText = isBlockedByMe ? "BỎ CHẶN" : "CHẶN";

  if (!(await showConfirm(`${actionText} người dùng này?`))) return;

  await fetch(
    `${CHAT_SERVICE_URL}/api/chat/${e}?blockerId=${currentUserId}&blockedId=${currentRecipientId}`,
    { method: "POST" }
  );
  isBlockedByMe = !isBlockedByMe;
  updateBlockUI();
  showToast(
    isBlockedByMe ? "Đã chặn người dùng" : "Đã bỏ chặn người dùng",
    "success"
  );
}

// --- Copy đoạn này đè lên hàm checkBlockStatus cũ (nằm gần cuối file) ---
async function checkBlockStatus() {
  // [FIX] Thêm &_t=${Date.now()} để chống Cache trình duyệt (Chrome/Edge)
  const r = await fetch(
    `${CHAT_SERVICE_URL}/api/chat/check-block?user1=${currentUserId}&user2=${currentRecipientId}&_t=${Date.now()}`
  );
  if (r.ok) {
    const d = await r.json();

    // [FIX] Chỉ cập nhật UI khi trạng thái thực sự thay đổi để tránh nháy giao diện
    if (
      isBlockedByMe !== d.isBlockedByMe ||
      isBlockedByOther !== d.isBlockedByOther
    ) {
      isBlockedByMe = d.isBlockedByMe;
      isBlockedByOther = d.isBlockedByOther;
      updateBlockUI();
    }
  }
}

// [SỬA] Cập nhật giao diện khi trạng thái chặn thay đổi
function updateBlockUI() {
  const i = document.getElementById("messageInput");
  const b = document.getElementById("sendBtn");
  const s = document.querySelector("#btnBlockUser span");

  // Ẩn menu option để tránh rối
  const menu = document.getElementById("chatOptionsMenu");
  if (menu) menu.classList.add("hidden");

  if (isBlockedByMe) {
    // TRƯỜNG HỢP 1: Mình chặn họ
    if (s) s.textContent = "Bỏ chặn";
    if (i) {
      i.placeholder = "Đã chặn. Bạn không thể gửi tin nhắn.";
      i.disabled = true;
    }
    if (b) b.disabled = true;

    // Mình chặn họ thì không cần hiện thông báo đỏ "Người nhận chặn bạn"
    toggleBlockMessage(false);
  } else if (isBlockedByOther) {
    // TRƯỜNG HỢP 2: Họ chặn mình
    if (s) s.textContent = "Chặn";
    if (i) {
      // Vẫn cho nhập hoặc disable tùy bạn, ở đây mình để placeholder cảnh báo
      i.placeholder = "Bạn đã bị chặn.";
      i.disabled = false;
    }
    if (b) b.disabled = false;

    // -> HIỆN THÔNG BÁO ĐỎ
    toggleBlockMessage(true);
  } else {
    // TRƯỜNG HỢP 3: Bình thường
    if (s) s.textContent = "Chặn";
    if (i) {
      i.placeholder = "Nhập tin nhắn...";
      i.disabled = false;
    }
    if (b) b.disabled = false;

    // -> XÓA THÔNG BÁO ĐỎ (Nếu có)
    toggleBlockMessage(false);
  }
}
// [THÊM MỚI] Hàm quản lý hiển thị thông báo chặn (Chỉ hiện 1 cái duy nhất)
function toggleBlockMessage(show) {
  const msgArea = document.getElementById("messageArea");
  // ID này giúp chúng ta tìm lại chính xác thông báo cũ để xóa
  const existingMsg = document.getElementById("unique-block-msg");

  if (show) {
    // Chỉ tạo mới nếu chưa có thông báo nào
    if (!existingMsg && msgArea) {
      const div = document.createElement("div");
      div.id = "unique-block-msg";
      // Class 'sticky bottom-2' giúp nó luôn nổi lên dưới cùng dễ nhìn
      div.className =
        "flex justify-center my-3 animate-fade-in sticky bottom-2 z-10";
      div.innerHTML = `
        <span class="bg-red-100 text-red-600 text-xs px-4 py-2 rounded-full border border-red-200 text-center shadow-md font-medium select-none">
          <i class="fas fa-ban mr-1"></i> Tin nhắn không được gửi. Người nhận đã chặn bạn.
        </span>`;
      msgArea.appendChild(div);
      scrollToBottom();
    }
  } else {
    // Nếu cần ẩn mà đang có thông báo -> Xóa ngay
    if (existingMsg) {
      existingMsg.remove();
    }
  }
}
function filterConversations(k) {
  const i = document.querySelectorAll(".sidebar-item");
  i.forEach(
    (e) =>
      (e.style.display = e
        .querySelector("h4")
        .textContent.toLowerCase()
        .includes(k.toLowerCase().trim())
        ? "flex"
        : "none")
  );
}
