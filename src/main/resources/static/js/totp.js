let eventSource;
let countdownInterval;
let currentPeriod;
let validationError = false; // Track if we have validation errors

// Toast notification functions
function showToast(message, type = 'error') {
    const container = document.getElementById('toastContainer');

    if (!container) {
        return;
    }

    // Remove any existing toasts
    const existingToasts = container.querySelectorAll('.toast');
    existingToasts.forEach(toast => toast.remove());

    // Create new toast element
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        ${message}
        <button class="toast-close" onclick="closeToast(this)">&times;</button>
    `;

    // Add to container
    container.appendChild(toast);

    // Trigger animation
    setTimeout(() => toast.classList.add('show'), 10);

    // Auto-remove after 5 seconds
    setTimeout(() => closeToast(toast.querySelector('.toast-close')), 2000);
}

function closeToast(button) {
    const toast = button.parentElement;
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
}

// Parse URL parameters and set form values
function loadUrlParameters() {
    const urlParams = new URLSearchParams(window.location.search);

    if (urlParams.get('secret')) {
        document.getElementById('secret').value = urlParams.get('secret');
    }
    if (urlParams.get('digits')) {
        document.getElementById('digits').value = urlParams.get('digits');
    }
    if (urlParams.get('period')) {
        document.getElementById('period').value = urlParams.get('period');
    }
}

// Theme management
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
}

function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);

    const themeToggle = document.getElementById('themeToggle');
    themeToggle.textContent = theme === 'dark' ? '☀️' : '🌙';
    themeToggle.title = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    setTheme(newTheme);
}

function updateCountdownDisplay() {
    // Check if all inputs have valid values before updating countdown
    const secret = document.getElementById('secret').value;
    const digits = document.getElementById('digits').value;
    const period = parseInt(document.getElementById('period').value);

    if (!secret.trim() || !digits.trim() || !period || isNaN(period)) {
        // Stop countdown and reset display when inputs are empty/invalid
        document.getElementById('countdown').textContent = '30';
        document.getElementById('progressBar').style.width = '100%';
        return;
    }

    const now = Math.floor(Date.now() / 1000);
    const remainingTime = period - (now % period);
    const progressPercent = (remainingTime / period) * 100;

    document.getElementById('countdown').textContent = remainingTime;
    document.getElementById('progressBar').style.width = progressPercent + '%';
}

function startTOTPStream() {
    const secret = document.getElementById('secret').value;
    const digits = document.getElementById('digits').value;
    const period = parseInt(document.getElementById('period').value);

    // Reset validation error state when user makes changes
    validationError = false;

    if (!secret.trim() || !digits.trim() || !period) {
        document.getElementById('totpCode').textContent = '------';

        // Stop countdown when inputs are empty
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        // Close existing SSE connection
        if (eventSource) {
            eventSource.close();
        }

        return;
    }

    currentPeriod = period;

    // Close existing connections
    if (eventSource) {
        eventSource.close();
    }
    if (countdownInterval) {
        clearInterval(countdownInterval);
    }

    // Start new SSE connection
    const url = `/totp-stream?secret=${encodeURIComponent(secret)}&digits=${digits}&period=${period}`;
    eventSource = new EventSource(url);

    eventSource.onmessage = function (event) {
        try {
            // event.data already contains just the JSON part (without "data: " prefix)
            const data = JSON.parse(event.data);

            // Check if it's an error response from server validation
            if (data.error) {
                validationError = true;
                showToast(data.error);
                document.getElementById('totpCode').textContent = '------';

                // Close the connection to stop retrying
                eventSource.close();
                if (countdownInterval) {
                    clearInterval(countdownInterval);
                }
                return;
            }

            // Success - clear any validation errors and update display
            validationError = false;
            document.getElementById('totpCode').textContent = data.code;
            document.getElementById('countdown').textContent = data.remainingTime;
            document.getElementById('progressBar').style.width = data.progressPercent + '%';
        } catch (error) {
            document.getElementById('totpCode').textContent = '------';
        }
    };

    eventSource.onerror = function (error) {
        document.getElementById('totpCode').textContent = '000000';
        eventSource.close();
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        // Only retry if it's not a validation error
        if (!validationError) {
            // Network/connection error - retry after 2 seconds
            setTimeout(startTOTPStream, 2000);
        }
        // If it's a validation error, don't retry - wait for user input change
    };

    // Update countdown display every second client-side
    countdownInterval = setInterval(updateCountdownDisplay, 1000);
}

function copyToClipboard() {
    const totpCode = document.getElementById('totpCode').textContent;
    if (totpCode && totpCode !== '------' && totpCode !== 'ERROR') {
        navigator.clipboard.writeText(totpCode).then(() => {
            const button = document.getElementById('copyButton');
            const originalText = button.innerHTML;
            button.innerHTML = '✅ Copied!';
            setTimeout(() => {
                button.innerHTML = originalText;
            }, 2000);
        }).catch(() => {
            // Fallback for older browsers
            const textArea = document.createElement('textarea');
            textArea.value = totpCode;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);

            const button = document.getElementById('copyButton');
            const originalText = button.innerHTML;
            button.innerHTML = '✅ Copied!';
            setTimeout(() => {
                button.innerHTML = originalText;
            }, 2000);
        });
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);
    document.getElementById('copyButton').addEventListener('click', copyToClipboard);
    document.getElementById('secret').addEventListener('input', startTOTPStream);
    document.getElementById('digits').addEventListener('input', startTOTPStream);
    document.getElementById('period').addEventListener('input', startTOTPStream);

    initTheme();
    loadUrlParameters();
    startTOTPStream();
});