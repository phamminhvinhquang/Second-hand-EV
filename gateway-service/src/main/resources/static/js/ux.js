// js/ux.js
// Ripple + submit spinner + small helpers

document.addEventListener("DOMContentLoaded", () => {
  // Init AOS if present
  if (window.AOS) {
    AOS.init({ duration: 650, once: true, easing: "ease-out-cubic" });
  }

  // Make elements with .entrance visible after DOM to provide fallback animation
  document
    .querySelectorAll(".entrance")
    .forEach((el) => requestAnimationFrame(() => el.classList.add("visible")));

  // RIPPLE effect for clickable buttons/links with class .ripple
  document.querySelectorAll(".ripple").forEach((btn) => {
    btn.addEventListener("click", function (e) {
      const rect = this.getBoundingClientRect();
      const circle = document.createElement("span");
      const size = Math.max(rect.width, rect.height) * 1.2;
      const x = e.clientX - rect.left - size / 2;
      const y = e.clientY - rect.top - size / 2;
      circle.style.cssText = `
        position:absolute; left:${x}px; top:${y}px; width:${size}px; height:${size}px;
        border-radius:50%; background: rgba(255,255,255,0.18); transform:scale(0);
        pointer-events:none; transition: transform .5s ease, opacity .6s ease;
        z-index: 2;
      `;
      this.appendChild(circle);
      requestAnimationFrame(() => (circle.style.transform = "scale(1)"));
      setTimeout(() => {
        circle.style.opacity = "0";
        setTimeout(() => circle.remove(), 650);
      }, 250);
    });
  });

  // Submit button spinner helper:
  // Buttons that have attribute data-spinner-target="#formId" will show spinner while form is processing
  document.querySelectorAll("button[data-spinner-target]").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      const selector = btn.getAttribute("data-spinner-target");
      const form = document.querySelector(selector);
      if (!form) return;
      // show spinner inside button
      const spinner = document.createElement("span");
      spinner.className = "ux-spinner";
      spinner.style.marginLeft = "8px";
      btn.disabled = true;
      btn.appendChild(spinner);
      // if the form uses normal submit, spinner will be visible. For demo, auto remove after 1.5s
      setTimeout(() => {
        btn.disabled = false;
        spinner.remove();
      }, 1500);
    });
  });

  // Small enhancement: animate "save" button on profile update success
  window.uxFlash = function (el) {
    if (!el) return;
    el.animate(
      [
        {
          boxShadow: "0 8px 22px rgba(16,185,129,0.14)",
          transform: "translateY(-3px)",
        },
        {
          boxShadow: "0 6px 18px rgba(16,185,129,0.08)",
          transform: "translateY(0)",
        },
      ],
      { duration: 420, easing: "ease-out" }
    );
  };
});
