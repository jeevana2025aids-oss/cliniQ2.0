/**
 * CliniQ - Modern Doctor Appointment Booking System
 * Client-Side Application Logic
 */

const API_BASE = '/api';

// ==========================================================================
// Toast Notification System
// ==========================================================================
function showToast(type, title, message) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';

    toast.innerHTML = `
        <div style="font-size: 1.25rem; font-weight: bold; line-height: 1;">${icon}</div>
        <div style="flex: 1;">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'all 0.3s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

// ==========================================================================
// API Helper
// ==========================================================================
async function apiCall(endpoint, options = {}) {
    try {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        };

        const response = await fetch(`${API_BASE}${endpoint}`, config);

        // Handle 204 No Content
        if (response.status === 204) {
            return null;
        }

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            let errorMsg = 'An error occurred during request.';
            if (data) {
                if (data.validationErrors) {
                    const fields = Object.entries(data.validationErrors)
                        .map(([f, msg]) => `${f}: ${msg}`)
                        .join(' | ');
                    errorMsg = `${data.message} (${fields})`;
                } else if (data.message) {
                    errorMsg = data.message;
                }
            }
            throw new Error(errorMsg);
        }

        return data;
    } catch (error) {
        showToast('error', 'Action Failed', error.message);
        throw error;
    }
}

// ==========================================================================
// Modal Helpers
// ==========================================================================
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('open');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('open');
    }
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('open');
    }
});

// Format helpers
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
    return dateStr;
}

function formatTime(timeStr) {
    if (!timeStr) return '-';
    const parts = timeStr.split(':');
    const hour = parseInt(parts[0], 10);
    const min = parts[1];
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const h = hour % 12 || 12;
    return `${h}:${min} ${ampm}`;
}

function getTodayString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// ==========================================================================
// Dashboard Logic (index.html)
// ==========================================================================
async function initDashboard() {
    loadDashboardStats();
    loadDashboardAppointments();
    loadDashboardAvailableSlots();
}

async function loadDashboardStats() {
    try {
        const stats = await apiCall('/appointments/dashboard/stats');
        if (stats) {
            document.getElementById('stat-doctors').textContent = stats.totalDoctors || 0;
            document.getElementById('stat-patients').textContent = stats.totalPatients || 0;
            document.getElementById('stat-available-slots').textContent = stats.availableSlots || 0;
            document.getElementById('stat-today-appointments').textContent = stats.todayAppointments || 0;
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadDashboardAppointments() {
    const tbody = document.getElementById('today-appointments-tbody');
    if (!tbody) return;

    try {
        const today = getTodayString();
        const appointments = await apiCall(`/appointments?date=${today}`);

        if (!appointments || appointments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="empty-icon">📅</div>
                        <div class="empty-title">No appointments scheduled for today</div>
                        <p class="empty-desc">Any appointments booked for today will be listed here.</p>
                    </td>
                </tr>`;
            return;
        }

        tbody.innerHTML = appointments.map(a => `
            <tr>
                <td><strong>#${a.id}</strong></td>
                <td>
                    <div style="font-weight: 600;">${a.patient ? a.patient.name : 'Unknown'}</div>
                    <small style="color: var(--text-muted);">${a.patient ? a.patient.phone : ''}</small>
                </td>
                <td>
                    <div style="font-weight: 600;">${a.doctor ? a.doctor.name : 'Unknown'}</div>
                    <span class="badge badge-specialization">${a.doctor ? a.doctor.specialization : ''}</span>
                </td>
                <td>
                    <strong>${formatTime(a.slot.startTime)} - ${formatTime(a.slot.endTime)}</strong>
                </td>
                <td>
                    <span class="badge ${a.status === 'CONFIRMED' ? 'badge-confirmed' : 'badge-cancelled'}">
                        <span class="badge-dot"></span> ${a.status}
                    </span>
                </td>
                <td>
                    ${a.status === 'CONFIRMED' ? `
                        <button class="btn btn-danger btn-sm" onclick="cancelAppointmentDashboard(${a.id})">
                            Cancel
                        </button>
                    ` : '<span style="color: var(--text-muted); font-size: 0.85rem;">Cancelled</span>'}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--danger); padding: 1.5rem;">Failed to load today's appointments</td></tr>`;
    }
}

async function cancelAppointmentDashboard(id) {
    if (!confirm('Are you sure you want to cancel this appointment? The slot will immediately become available for rebooking.')) {
        return;
    }
    try {
        await apiCall(`/appointments/${id}/cancel`, { method: 'PUT' });
        showToast('success', 'Appointment Cancelled', 'Slot freed immediately for rebooking.');
        loadDashboardStats();
        loadDashboardAppointments();
        loadDashboardAvailableSlots();
    } catch (e) {
        console.error(e);
    }
}

async function loadDashboardAvailableSlots() {
    const tbody = document.getElementById('available-slots-tbody');
    if (!tbody) return;

    try {
        const slots = await apiCall('/slots/available');
        if (!slots || slots.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="empty-state">
                        <div class="empty-icon">⏰</div>
                        <div class="empty-title">No open slots available</div>
                        <p class="empty-desc">Doctors can publish slots using the Doctors page.</p>
                    </td>
                </tr>`;
            return;
        }

        // Show first 6 slots on dashboard
        const previewSlots = slots.slice(0, 6);
        tbody.innerHTML = previewSlots.map(s => `
            <tr>
                <td><strong>#${s.id}</strong></td>
                <td>
                    <div style="font-weight: 600;">${s.doctor ? s.doctor.name : 'Unknown'}</div>
                    <span class="badge badge-specialization">${s.doctor ? s.doctor.specialization : ''}</span>
                </td>
                <td>${formatDate(s.slotDate)}</td>
                <td><strong>${formatTime(s.startTime)} - ${formatTime(s.endTime)}</strong></td>
                <td>
                    <a href="appointments.html?slotId=${s.id}&doctorId=${s.doctor.id}" class="btn btn-primary btn-sm">
                        Book Slot
                    </a>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--danger); padding: 1.5rem;">Failed to load slots</td></tr>`;
    }
}

// ==========================================================================
// Doctors Management Logic (doctors.html)
// ==========================================================================
async function initDoctors() {
    loadDoctors();

    const searchInput = document.getElementById('doctor-search');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                loadDoctors(e.target.value);
            }, 300);
        });
    }

    const doctorForm = document.getElementById('add-doctor-form');
    if (doctorForm) {
        doctorForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const doctorData = {
                name: document.getElementById('doc-name').value,
                specialization: document.getElementById('doc-specialization').value,
                email: document.getElementById('doc-email').value,
                phone: document.getElementById('doc-phone').value
            };
            try {
                await apiCall('/doctors', {
                    method: 'POST',
                    body: JSON.stringify(doctorData)
                });
                showToast('success', 'Doctor Added', `${doctorData.name} has been added to the clinic.`);
                closeModal('add-doctor-modal');
                doctorForm.reset();
                loadDoctors();
            } catch (err) {
                console.error(err);
            }
        });
    }

    const publishForm = document.getElementById('publish-slots-form');
    if (publishForm) {
        // Set default dates
        const todayStr = getTodayString();
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 3);
        const tomorrowStr = tomorrow.toISOString().split('T')[0];

        document.getElementById('slot-start-date').value = todayStr;
        document.getElementById('slot-end-date').value = tomorrowStr;

        publishForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const slotRangeData = {
                doctorId: parseInt(document.getElementById('slot-doctor-id').value, 10),
                startDate: document.getElementById('slot-start-date').value,
                endDate: document.getElementById('slot-end-date').value,
                dailyStartTime: document.getElementById('slot-start-time').value,
                dailyEndTime: document.getElementById('slot-end-time').value,
                durationMinutes: parseInt(document.getElementById('slot-duration').value, 10)
            };

            try {
                const created = await apiCall('/slots/publish-range', {
                    method: 'POST',
                    body: JSON.stringify(slotRangeData)
                });
                showToast('success', 'Slots Published', `Published ${created ? created.length : 'recurring'} slots successfully.`);
                closeModal('publish-slots-modal');
                publishForm.reset();
            } catch (err) {
                console.error(err);
            }
        });
    }
}

async function loadDoctors(search = '') {
    const grid = document.getElementById('doctors-grid');
    if (!grid) return;

    try {
        const url = search ? `/doctors?search=${encodeURIComponent(search)}` : '/doctors';
        const doctors = await apiCall(url);

        if (!doctors || doctors.length === 0) {
            grid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-icon">👨‍⚕️</div>
                    <div class="empty-title">No doctors found</div>
                    <p class="empty-desc">Add a doctor to begin configuring clinic schedules.</p>
                </div>`;
            return;
        }

        grid.innerHTML = doctors.map(d => `
            <div class="card" style="margin-bottom: 0;">
                <div class="card-body">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                        <div>
                            <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-main);">${d.name}</h3>
                            <span class="badge badge-specialization" style="margin-top: 0.25rem;">${d.specialization}</span>
                        </div>
                        <div class="stat-icon icon-teal" style="width: 44px; height: 44px; font-size: 1.25rem;">
                            🩺
                        </div>
                    </div>
                    <div style="font-size: 0.875rem; color: var(--text-muted); display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 1.25rem;">
                        <div>📧 ${d.email}</div>
                        <div>📞 ${d.phone}</div>
                    </div>
                    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                        <button class="btn btn-secondary btn-sm" onclick="openPublishSlotsModal(${d.id}, '${d.name.replace(/'/g, "\\'")}')">
                            Publish Slots
                        </button>
                        <button class="btn btn-outline btn-sm" onclick="viewDoctorSlots(${d.id}, '${d.name.replace(/'/g, "\\'")}')">
                            View Slots
                        </button>
                        <button class="btn btn-danger btn-sm" style="margin-left: auto;" onclick="deleteDoctor(${d.id})">
                            Delete
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        grid.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; color: var(--danger); padding: 2rem;">Failed to load doctors</div>`;
    }
}

function openPublishSlotsModal(doctorId, doctorName) {
    const select = document.getElementById('slot-doctor-id');
    if (select) {
        select.innerHTML = `<option value="${doctorId}">${doctorName}</option>`;
    }
    openModal('publish-slots-modal');
}

async function viewDoctorSlots(doctorId, doctorName) {
    try {
        const slots = await apiCall(`/slots/doctor/${doctorId}`);
        const modal = document.getElementById('view-slots-modal');
        const title = document.getElementById('view-slots-title');
        const tbody = document.getElementById('view-slots-tbody');

        title.textContent = `Slots for ${doctorName}`;
        if (!slots || slots.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="empty-state">No slots published yet for this doctor.</td></tr>`;
        } else {
            tbody.innerHTML = slots.map(s => `
                <tr>
                    <td><strong>#${s.id}</strong></td>
                    <td>${formatDate(s.slotDate)}</td>
                    <td><strong>${formatTime(s.startTime)} - ${formatTime(s.endTime)}</strong></td>
                    <td>
                        <span class="badge ${s.booked ? 'badge-booked' : 'badge-available'}">
                            <span class="badge-dot"></span> ${s.booked ? 'Booked' : 'Available'}
                        </span>
                    </td>
                </tr>
            `).join('');
        }
        openModal('view-slots-modal');
    } catch (err) {
        console.error(err);
    }
}

async function deleteDoctor(id) {
    if (!confirm('Are you sure you want to remove this doctor? All associated slots and records will also be removed.')) {
        return;
    }
    try {
        await apiCall(`/doctors/${id}`, { method: 'DELETE' });
        showToast('success', 'Doctor Removed', 'Doctor removed successfully.');
        loadDoctors();
    } catch (e) {
        console.error(e);
    }
}

// ==========================================================================
// Patients Management Logic (patients.html)
// ==========================================================================
async function initPatients() {
    loadPatients();

    const searchInput = document.getElementById('patient-search');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                loadPatients(e.target.value);
            }, 300);
        });
    }

    const patientForm = document.getElementById('add-patient-form');
    if (patientForm) {
        patientForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const patientData = {
                name: document.getElementById('pat-name').value,
                email: document.getElementById('pat-email').value,
                phone: document.getElementById('pat-phone').value,
                age: parseInt(document.getElementById('pat-age').value, 10),
                gender: document.getElementById('pat-gender').value
            };
            try {
                await apiCall('/patients', {
                    method: 'POST',
                    body: JSON.stringify(patientData)
                });
                showToast('success', 'Patient Registered', `${patientData.name} has been registered.`);
                closeModal('add-patient-modal');
                patientForm.reset();
                loadPatients();
            } catch (err) {
                console.error(err);
            }
        });
    }
}

async function loadPatients(search = '') {
    const tbody = document.getElementById('patients-tbody');
    if (!tbody) return;

    try {
        const url = search ? `/patients?search=${encodeURIComponent(search)}` : '/patients';
        const patients = await apiCall(url);

        if (!patients || patients.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-state">
                        <div class="empty-icon">👥</div>
                        <div class="empty-title">No patients found</div>
                        <p class="empty-desc">Register a new patient using the button above.</p>
                    </td>
                </tr>`;
            return;
        }

        tbody.innerHTML = patients.map(p => `
            <tr>
                <td><strong>#${p.id}</strong></td>
                <td><div style="font-weight: 600;">${p.name}</div></td>
                <td>
                    <div>${p.email}</div>
                    <small style="color: var(--text-muted);">${p.phone}</small>
                </td>
                <td>${p.age} yrs (${p.gender})</td>
                <td>${formatDate(p.createdAt ? p.createdAt.split('T')[0] : '')}</td>
                <td>
                    <div style="display: flex; gap: 0.5rem;">
                        <button class="btn btn-outline btn-sm" onclick="viewPatientHistory(${p.id}, '${p.name.replace(/'/g, "\\'")}')">
                            History
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="deletePatient(${p.id})">
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--danger); padding: 1.5rem;">Failed to load patients</td></tr>`;
    }
}

async function viewPatientHistory(patientId, patientName) {
    try {
        const history = await apiCall(`/patients/${patientId}/appointments`);
        const title = document.getElementById('history-title');
        const tbody = document.getElementById('history-tbody');

        title.textContent = `Appointment History - ${patientName}`;
        if (!history || history.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No past appointments found.</td></tr>`;
        } else {
            tbody.innerHTML = history.map(a => `
                <tr>
                    <td><strong>#${a.id}</strong></td>
                    <td>${a.doctor ? a.doctor.name : 'Unknown'} (${a.doctor ? a.doctor.specialization : ''})</td>
                    <td>${formatDate(a.slot ? a.slot.slotDate : '')} (${formatTime(a.slot.startTime)} - ${formatTime(a.slot.endTime)})</td>
                    <td>
                        <span class="badge ${a.status === 'CONFIRMED' ? 'badge-confirmed' : 'badge-cancelled'}">
                            <span class="badge-dot"></span> ${a.status}
                        </span>
                    </td>
                    <td>${a.reason || '-'}</td>
                </tr>
            `).join('');
        }
        openModal('patient-history-modal');
    } catch (err) {
        console.error(err);
    }
}

async function deletePatient(id) {
    if (!confirm('Are you sure you want to delete this patient profile?')) return;
    try {
        await apiCall(`/patients/${id}`, { method: 'DELETE' });
        showToast('success', 'Patient Deleted', 'Patient removed successfully.');
        loadPatients();
    } catch (e) {
        console.error(e);
    }
}

// ==========================================================================
// Appointments Logic (appointments.html)
// ==========================================================================
async function initAppointments() {
    loadAppointments();
    populateBookingDropdowns();

    // Check URL params for preselected slot/doctor
    const urlParams = new URLSearchParams(window.location.search);
    const preselectedDoctor = urlParams.get('doctorId');
    const preselectedSlot = urlParams.get('slotId');

    if (preselectedSlot) {
        openModal('book-appointment-modal');
    }

    // Filter changes
    const doctorFilter = document.getElementById('filter-doctor');
    const dateFilter = document.getElementById('filter-date');
    const statusFilter = document.getElementById('filter-status');

    if (doctorFilter) doctorFilter.addEventListener('change', () => loadAppointments());
    if (dateFilter) dateFilter.addEventListener('change', () => loadAppointments());
    if (statusFilter) statusFilter.addEventListener('change', () => loadAppointments());

    // Dynamic slot loading for booking modal
    const bookDoctorSelect = document.getElementById('book-doctor-id');
    const bookDateInput = document.getElementById('book-date');

    if (bookDateInput) {
        bookDateInput.value = getTodayString();
    }

    async function refreshBookingSlots() {
        const docId = bookDoctorSelect ? bookDoctorSelect.value : '';
        const date = bookDateInput ? bookDateInput.value : '';
        const slotSelect = document.getElementById('book-slot-id');

        if (!slotSelect) return;

        if (!docId) {
            slotSelect.innerHTML = '<option value="">-- Select a doctor first --</option>';
            return;
        }

        try {
            let url = `/slots/available?doctorId=${docId}`;
            if (date) url += `&date=${date}`;

            const slots = await apiCall(url);
            if (!slots || slots.length === 0) {
                slotSelect.innerHTML = '<option value="">No available slots for this date</option>';
            } else {
                slotSelect.innerHTML = '<option value="">-- Choose an Open Slot --</option>' +
                    slots.map(s => `
                        <option value="${s.id}" ${preselectedSlot && preselectedSlot == s.id ? 'selected' : ''}>
                            ${formatDate(s.slotDate)} | ${formatTime(s.startTime)} - ${formatTime(s.endTime)}
                        </option>
                    `).join('');
            }
        } catch (e) {
            slotSelect.innerHTML = '<option value="">Error loading slots</option>';
        }
    }

    if (bookDoctorSelect) bookDoctorSelect.addEventListener('change', refreshBookingSlots);
    if (bookDateInput) bookDateInput.addEventListener('change', refreshBookingSlots);

    // Initial slot load if preselected
    if (preselectedDoctor && bookDoctorSelect) {
        bookDoctorSelect.value = preselectedDoctor;
        refreshBookingSlots();
    }

    // Book Form Submit
    const bookForm = document.getElementById('book-appointment-form');
    if (bookForm) {
        bookForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const bookingData = {
                patientId: parseInt(document.getElementById('book-patient-id').value, 10),
                slotId: parseInt(document.getElementById('book-slot-id').value, 10),
                reason: document.getElementById('book-reason').value
            };

            if (!bookingData.patientId || !bookingData.slotId) {
                showToast('error', 'Missing Information', 'Please select both patient and open slot.');
                return;
            }

            try {
                await apiCall('/appointments/book', {
                    method: 'POST',
                    body: JSON.stringify(bookingData)
                });
                showToast('success', 'Appointment Booked!', 'Slot has been reserved and marked unavailable.');
                closeModal('book-appointment-modal');
                bookForm.reset();
                loadAppointments();
            } catch (err) {
                console.error(err);
            }
        });
    }
}

async function populateBookingDropdowns() {
    try {
        const [doctors, patients] = await Promise.all([
            apiCall('/doctors'),
            apiCall('/patients')
        ]);

        // Filter doctor dropdown
        const filterDoc = document.getElementById('filter-doctor');
        if (filterDoc && doctors) {
            filterDoc.innerHTML = '<option value="">All Doctors</option>' +
                doctors.map(d => `<option value="${d.id}">${d.name} (${d.specialization})</option>`).join('');
        }

        // Booking modal doctor dropdown
        const bookDoc = document.getElementById('book-doctor-id');
        if (bookDoc && doctors) {
            bookDoc.innerHTML = '<option value="">-- Select Doctor --</option>' +
                doctors.map(d => `<option value="${d.id}">${d.name} (${d.specialization})</option>`).join('');
        }

        // Booking modal patient dropdown
        const bookPatient = document.getElementById('book-patient-id');
        if (bookPatient && patients) {
            bookPatient.innerHTML = '<option value="">-- Select Patient --</option>' +
                patients.map(p => `<option value="${p.id}">${p.name} (Phone: ${p.phone})</option>`).join('');
        }
    } catch (e) {
        console.error('Error populating dropdowns:', e);
    }
}

async function loadAppointments() {
    const tbody = document.getElementById('appointments-tbody');
    if (!tbody) return;

    const doctorId = document.getElementById('filter-doctor')?.value;
    const date = document.getElementById('filter-date')?.value;
    const status = document.getElementById('filter-status')?.value;

    let query = [];
    if (doctorId) query.push(`doctorId=${doctorId}`);
    if (date) query.push(`date=${date}`);
    if (status) query.push(`status=${status}`);

    const url = `/appointments${query.length ? '?' + query.join('&') : ''}`;

    try {
        const appointments = await apiCall(url);

        if (!appointments || appointments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="empty-state">
                        <div class="empty-icon">📑</div>
                        <div class="empty-title">No appointments found</div>
                        <p class="empty-desc">Try clearing filters or book a new appointment.</p>
                    </td>
                </tr>`;
            return;
        }

        tbody.innerHTML = appointments.map(a => `
            <tr>
                <td><strong>#${a.id}</strong></td>
                <td>
                    <div style="font-weight: 600;">${a.patient ? a.patient.name : 'Unknown'}</div>
                    <small style="color: var(--text-muted);">${a.patient ? a.patient.phone : ''}</small>
                </td>
                <td>
                    <div style="font-weight: 600;">${a.doctor ? a.doctor.name : 'Unknown'}</div>
                    <span class="badge badge-specialization">${a.doctor ? a.doctor.specialization : ''}</span>
                </td>
                <td>${formatDate(a.slot ? a.slot.slotDate : '')}</td>
                <td>
                    <strong>${formatTime(a.slot.startTime)} - ${formatTime(a.slot.endTime)}</strong>
                </td>
                <td>
                    <span class="badge ${a.status === 'CONFIRMED' ? 'badge-confirmed' : 'badge-cancelled'}">
                        <span class="badge-dot"></span> ${a.status}
                    </span>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                        ${a.reason ? a.reason : ''}
                    </div>
                </td>
                <td>
                    ${a.status === 'CONFIRMED' ? `
                        <button class="btn btn-danger btn-sm" onclick="cancelAppointment(${a.id})">
                            Cancel
                        </button>
                    ` : '<span style="color: var(--text-muted); font-size: 0.85rem;">Cancelled</span>'}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--danger); padding: 1.5rem;">Failed to load appointments</td></tr>`;
    }
}

async function setTodayFilter() {
    const dateInput = document.getElementById('filter-date');
    if (dateInput) {
        dateInput.value = getTodayString();
        loadAppointments();
    }
}

async function clearFilters() {
    const doc = document.getElementById('filter-doctor');
    const date = document.getElementById('filter-date');
    const status = document.getElementById('filter-status');
    if (doc) doc.value = '';
    if (date) date.value = '';
    if (status) status.value = '';
    loadAppointments();
}

async function cancelAppointment(id) {
    if (!confirm('Are you sure you want to cancel this appointment? Rule Enforced: The slot will be freed immediately for rebooking.')) {
        return;
    }

    try {
        await apiCall(`/appointments/${id}/cancel`, { method: 'PUT' });
        showToast('success', 'Appointment Cancelled', 'Slot freed immediately for rebooking.');
        loadAppointments();
    } catch (e) {
        console.error(e);
    }
}
