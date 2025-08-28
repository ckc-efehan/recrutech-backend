"use strict";

(function() {
  document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('resetPasswordForm');
    if (!form) {
      // No form rendered (invalid token case) -> nothing to wire up
      return;
    }

    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const submitBtn = document.getElementById('submitBtn');
    const passwordError = document.getElementById('passwordError');
    const confirmError = document.getElementById('confirmError');
    const passwordMatch = document.getElementById('passwordMatch');
    const successMessage = document.getElementById('successMessage');

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

    function validatePassword() {
      const password = newPassword.value;
      const isValid = passwordRegex.test(password);
      if (password.length > 0) {
        if (isValid) {
          newPassword.classList.remove('error');
          passwordError.style.display = 'none';
        } else {
          newPassword.classList.add('error');
          passwordError.style.display = 'block';
        }
      } else {
        newPassword.classList.remove('error');
        passwordError.style.display = 'none';
      }
      return isValid || password.length === 0;
    }

    function validateConfirmPassword() {
      const password = newPassword.value;
      const confirm = confirmPassword.value;
      if (confirm.length > 0) {
        if (password === confirm) {
          confirmPassword.classList.remove('error');
          confirmError.style.display = 'none';
          if (passwordMatch) passwordMatch.style.display = 'block';
          return true;
        } else {
          confirmPassword.classList.add('error');
          confirmError.style.display = 'block';
          if (passwordMatch) passwordMatch.style.display = 'none';
          return false;
        }
      } else {
        confirmPassword.classList.remove('error');
        confirmError.style.display = 'none';
        if (passwordMatch) passwordMatch.style.display = 'none';
        return false;
      }
    }

    function updateSubmitButton() {
      const passwordValid = passwordRegex.test(newPassword.value);
      const passwordsMatch = newPassword.value === confirmPassword.value && confirmPassword.value.length > 0;
      submitBtn.disabled = !(passwordValid && passwordsMatch);
    }

    newPassword.addEventListener('input', function() {
      validatePassword();
      if (confirmPassword.value.length > 0) {
        validateConfirmPassword();
      }
      updateSubmitButton();
    });

    confirmPassword.addEventListener('input', function() {
      validateConfirmPassword();
      updateSubmitButton();
    });

    form.addEventListener('submit', async function(e) {
      e.preventDefault();

      // Final client-side validation
      if (!passwordRegex.test(newPassword.value)) {
        alert('Das Passwort erfüllt nicht die Sicherheitsanforderungen.');
        return;
      }
      if (newPassword.value !== confirmPassword.value) {
        alert('Die Passwörter stimmen nicht überein.');
        return;
      }

      const token = form.querySelector('input[name="token"]').value;
      const payload = {
        token: token,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value
      };

      submitBtn.disabled = true;
      const originalText = submitBtn.textContent;
      submitBtn.textContent = 'Bitte warten...';

      try {
        const resp = await fetch('/api/auth/reset-password-confirm', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (resp.ok) {
          if (successMessage) successMessage.style.display = 'block';
          submitBtn.textContent = 'Erfolgreich!';
        } else {
          let errorText = '';
          try {
            const data = await resp.json();
            errorText = (data && (data.message || data.error || JSON.stringify(data))) || '';
          } catch (_) {
            errorText = await resp.text();
          }
          alert('Fehler beim Zurücksetzen: ' + (errorText || ('HTTP ' + resp.status)));
          submitBtn.disabled = false;
          submitBtn.textContent = originalText;
        }
      } catch (err) {
        alert('Netzwerkfehler: ' + (err && err.message ? err.message : err));
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
      }
    });
  });
})();