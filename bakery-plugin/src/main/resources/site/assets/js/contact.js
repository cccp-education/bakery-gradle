document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    var form = document.getElementById('contact-form');
    if (!form) return;

    var submitButton = form.querySelector('button[type="submit"]');
    var successMessage = document.getElementById('contact-success-message');
    var errorMessage = document.getElementById('contact-error-message');

    var nameInput = form.querySelector('input[name="name"]');
    var emailInput = form.querySelector('input[name="email"]');
    var phoneInput = document.getElementById('phone');
    var subjectInput = form.querySelector('input[name="subject"]');
    var messageInput = form.querySelector('textarea[name="message"]');

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        event.stopPropagation();

        nameInput.setCustomValidity('');
        emailInput.setCustomValidity('');
        if (phoneInput) phoneInput.setCustomValidity('');
        subjectInput.setCustomValidity('');
        messageInput.setCustomValidity('');

        if (nameInput.value.trim().length < 1) {
            nameInput.setCustomValidity('Veuillez saisir au moins 1 caractère pour le nom.');
        }

        var emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(emailInput.value.trim())) {
            emailInput.setCustomValidity('Veuillez saisir une adresse email valide.');
        }

        if (phoneInput && phoneInput.value.trim() !== '') {
            var phonePattern = /^\d{10,15}$/;
            if (!phonePattern.test(phoneInput.value.trim())) {
                phoneInput.setCustomValidity('Veuillez saisir un numéro de téléphone valide (10 à 15 chiffres).');
            }
        }

        if (subjectInput.value.trim().length < 3) {
            subjectInput.setCustomValidity('Veuillez saisir au moins 3 caractères pour le sujet.');
        }

        if (messageInput.value.trim().length < 10) {
            messageInput.setCustomValidity('Veuillez saisir au moins 10 caractères pour votre message.');
        }

        form.classList.add('was-validated');

        if (!form.checkValidity()) {
            return;
        }

        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Envoi en cours...';

        successMessage.style.display = 'none';
        errorMessage.style.display = 'none';

        if (typeof firebase === 'undefined' || !window.FIREBASE_CONFIG) {
            errorMessage.style.display = 'block';
            submitButton.disabled = false;
            submitButton.innerHTML = '<i class="bi bi-send me-2"></i>Envoyer le Message';
            return;
        }

        try {
            if (!firebase.apps.length) {
                firebase.initializeApp(window.FIREBASE_CONFIG);
            }
            var db = firebase.firestore();

            db.collection('contacts').add({
                name: nameInput.value.trim(),
                email: emailInput.value.trim(),
                phone: phoneInput ? phoneInput.value.trim() : '',
                subject: subjectInput.value.trim(),
                message: messageInput.value.trim(),
                createdAt: firebase.firestore.FieldValue.serverTimestamp()
            }).then(function () {
                successMessage.style.display = 'block';
                form.reset();
                form.classList.remove('was-validated');
            }).catch(function (error) {
                console.error('Firestore write failed:', error);
                errorMessage.style.display = 'block';
            }).finally(function () {
                submitButton.disabled = false;
                submitButton.innerHTML = '<i class="bi bi-send me-2"></i>Envoyer le Message';
            });
        } catch (error) {
            console.error('Firebase init failed:', error);
            errorMessage.style.display = 'block';
            submitButton.disabled = false;
            submitButton.innerHTML = '<i class="bi bi-send me-2"></i>Envoyer le Message';
        }
    }, false);
});
