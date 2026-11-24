'use strict';
// Existing mail and QR links keep secrets in the fragment. Exchange them for server session state.
const fragment = new URLSearchParams(location.hash.slice(1));
const kind = fragment.has('group') ? 'group' : 'guest';
const access = fragment.get(kind);
if (access && /^[a-f0-9]{64}$/.test(access)) {
  history.replaceState(null, '', location.pathname + location.search);
  const form = document.createElement('form');
  form.method = 'post';
  form.action = '/access';
  const fields = { access, kind };
  const csrf = document.querySelector('meta[name="_csrf"]');
  const parameter = document.querySelector('meta[name="_csrf_parameter"]');
  if (csrf && parameter) fields[parameter.content] = csrf.content;
  for (const [name, value] of Object.entries(fields)) {
    const input = document.createElement('input');
    input.type = 'hidden'; input.name = name; input.value = value; form.append(input);
  }
  document.body.append(form);
  form.submit();
}
for (const form of document.querySelectorAll('form[data-confirm]')) {
  form.addEventListener('submit', event => {
    if (!confirm(form.dataset.confirm)) event.preventDefault();
  });
}
const scanButton = document.getElementById('scan-start');
if (scanButton) {
  let stream, timer;
  const video = document.getElementById('scanner');
  const message = document.getElementById('scan-message');
  function stop() {
    clearTimeout(timer); stream?.getTracks().forEach(track => track.stop());
    stream = undefined; video.hidden = true; scanButton.textContent = 'QR-Code mit Kamera scannen';
  }
  scanButton.addEventListener('click', async () => {
    if (stream) { stop(); return; }
    try {
      if (!window.BarcodeDetector) throw new Error('Kamerascan wird nicht unterstützt. Bitte den Reservierungslink eingeben.');
      const detector = new BarcodeDetector({ formats: ['qr_code'] });
      stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      video.srcObject = stream; video.hidden = false; await video.play();
      scanButton.textContent = 'Kamera schließen'; message.textContent = '';
      async function scan() {
        try {
          if (!stream) return;
          const results = await detector.detect(video);
          const found = results[0]?.rawValue.match(/guest=([a-f0-9]{64})/);
          if (found) {
            document.querySelector('#access-form input[name="access"]').value = found[1];
            stop(); document.getElementById('access-form').requestSubmit();
          } else timer = setTimeout(scan, 300);
        } catch (error) { stop(); message.textContent = error.message; }
      }
      await scan();
    } catch (error) { stop(); message.textContent = error.message; }
  });
  addEventListener('pagehide', stop);
}
// The summary supplements native forms; booking and validation remain on the server.
const bookingDate = document.querySelector('[data-booking-date]');
const bookingDuration = document.querySelector('[data-booking-duration]');
function updateBookingSummary() {
  const datePreview = document.querySelector('[data-booking-preview]');
  const durationPreview = document.querySelector('[data-duration-preview]');
  if (bookingDate && datePreview && bookingDate.value) {
    const date = new Date(bookingDate.value);
    if (!Number.isNaN(date.getTime())) datePreview.textContent = new Intl.DateTimeFormat('de-AT', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  }
  if (bookingDuration && durationPreview) durationPreview.textContent = bookingDuration.selectedOptions[0].textContent;
}
bookingDate?.addEventListener('input', updateBookingSummary);
bookingDuration?.addEventListener('change', updateBookingSummary);
updateBookingSummary();

// Progressive enhancement: subtle header elevation on scroll.
const header = document.querySelector('.app-header');
if (header) {
  const syncHeader = () => header.classList.toggle('is-scrolled', window.scrollY > 8);
  syncHeader();
  window.addEventListener('scroll', syncHeader, { passive: true });
}

// Progressive enhancement: keep active category tab synchronized on menu page.
const categoryNav = document.querySelector('.category-nav');
if (categoryNav) {
  const links = [...categoryNav.querySelectorAll('a[href^="#"]')];
  const setActive = (hash) => {
    const target = hash || links[0]?.getAttribute('href');
    categoryNav.classList.add('has-active');
    for (const link of links) {
      link.classList.toggle('is-active', link.getAttribute('href') === target);
    }
  };
  setActive(location.hash);
  for (const link of links) {
    link.addEventListener('click', () => setActive(link.getAttribute('href')));
  }
  window.addEventListener('hashchange', () => setActive(location.hash));
}
