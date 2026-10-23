// API client connecting frontend to the Spring Boot + PostgreSQL backend

export interface CategoryOption {
  categoryId: number;
  categoryName: string;
  description: string;
  parentCategoryId?: number;
}

export interface LocationOption {
  locationId: number;
  locationName: string;
  locationType: string;
  description: string;
}

export interface FlightOption {
  flightId: number;
  flightNumber: string;
  airline: string;
}

export interface ReferenceData {
  categories: CategoryOption[];
  locations: LocationOption[];
  flights: FlightOption[];
}

export interface LostReport {
  lostReportId: number;
  passengerId: number;
  passengerName: string;
  itemId: number;
  itemName: string;
  categoryName: string;
  reportStatus: 'open' | 'resolved' | 'candidate_found' | 'closed';
  lostAt: string;
  lastSeenLocation?: string;
  flightNumber?: string;
  additionalDetails?: string;
  reportedAt: string;
  updatedAt: string;
  photoUrls?: string[];
}

export interface MatchRecord {
  lostReportId: number;
  passengerName: string;
  lostItemName: string;
  lostCategory: string;
  reportStatus: string;
  lostAt: string;
  foundItemId: number;
  foundItemName: string;
  foundCategory: string;
  foundLocation: string;
  foundAt: string;
  matchScore: number;
  matchStatus: 'candidate' | 'verified' | 'rejected';
  matchRemarks?: string;
  reviewedBy?: string;
  matchedAt: string;
  reviewedAt?: string;
}

export interface UserSessionProfile {
  passengerId?: number;
  staffId?: number;
  fullName: string;
  email: string;
  phone?: string;
  createdAt: string;
  userType?: 'passenger' | 'staff';
  staffRole?: string;
  staffRoleTitle?: string;
  authorities?: string[];
}

export type PassengerProfile = UserSessionProfile;

export interface PhotoAttachment {
  photoUrl: string;
  caption?: string;
  photoType?: 'overview' | 'serial_number' | 'damage_detail' | 'receipt' | 'tag' | 'general' | string;
}

export interface CreateLostReportPayload {
  categoryId: number;
  itemName: string;
  brand?: string;
  colour?: string;
  description?: string;
  estimatedValue?: number;
  itemCondition?: 'new' | 'good' | 'fair' | 'poor' | 'damaged' | 'unknown';
  lostAt: string;
  flightId?: number;
  lastSeenLocationId?: number;
  additionalDetails?: string;
  photos?: PhotoAttachment[];
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

// ---------------------------------------------------------------------------
// Staff Subrole Types & DTOs
// ---------------------------------------------------------------------------

export interface StaffFoundItem {
  foundItemId: number;
  itemId: number;
  itemName: string;
  categoryId: number;
  categoryName: string;
  brand?: string;
  colour?: string;
  description?: string;
  estimatedValue?: number;
  itemCondition?: string;
  itemStatus: 'awaiting_review' | 'stored' | 'claimed' | 'returned' | 'disposed';
  foundLocationId?: number;
  foundLocationName?: string;
  flightId?: number;
  flightNumber?: string;
  foundAt: string;
  receivedAt: string;
  foundByStaffId: number;
  foundByStaffName: string;
  storageName?: string;
  slotNumber?: number;
  photoUrls?: string[];
  daysInStorage?: number;
}

export interface CreateStaffFoundItemPayload {
  categoryId: number;
  itemName: string;
  brand?: string;
  colour?: string;
  description?: string;
  estimatedValue?: number;
  itemCondition?: string;
  flightId?: number;
  foundLocationId?: number;
  foundAt: string;
  photos?: PhotoAttachment[];
}

export interface StaffClaimItem {
  claimId: number;
  matchId: number;
  lostReportId: number;
  foundItemId: number;
  passengerName: string;
  passengerEmail: string;
  passengerPhone?: string;
  lostItemName: string;
  foundItemName: string;
  matchScore: number;
  matchStatus: string;
  claimStatus: 'submitted' | 'under_review' | 'approved' | 'rejected' | 'withdrawn';
  submittedAt: string;
  claimReviewedBy?: string;
  claimReviewedAt?: string;
  decisionNotes?: string;
  verificationMethod?: string;
  verificationResult?: string;
  verifiedAt?: string;
  returnMethod?: string;
  returnStatus?: string;
}

export interface ReviewClaimPayload {
  decision: 'approved' | 'rejected' | 'under_review';
  verificationMethod?: string;
  notes?: string;
}

export interface StorageSlotInfo {
  storageId: number;
  slotNumber: number;
  slotStatus: string;
  assignedFoundItemId?: number;
  assignedItemName?: string;
}

export interface StorageLocationInfo {
  storageId: number;
  storageName: string;
  locationName: string;
  description: string;
  capacity: number;
  availableSlots: number;
  occupiedSlots: number;
  slots: StorageSlotInfo[];
}

export interface AssignStoragePayload {
  foundItemId: number;
  storageId: number;
  slotNumber: number;
}

export interface StaffReturnItem {
  returnId: number;
  claimId: number;
  foundItemId: number;
  passengerName: string;
  passengerEmail: string;
  passengerPhone?: string;
  itemName: string;
  returnMethod: 'counter_handover' | 'delivery';
  returnStatus: 'initiated' | 'in_transit' | 'completed' | 'failed' | 'cancelled';
  handledByStaffId: number;
  handledByStaffName: string;
  deliveryNotes?: string;
  createdAt: string;
  completedAt?: string;
}

export interface ProcessReturnPayload {
  claimId: number;
  returnMethod: 'counter_handover' | 'delivery';
  returnStatus: 'initiated' | 'in_transit' | 'completed' | 'failed' | 'cancelled';
  deliveryNotes?: string;
}

export interface SupervisorStats {
  totalFoundItems: number;
  awaitingReviewCount: number;
  storedCount: number;
  claimedCount: number;
  returnedCount: number;
  totalLostReports: number;
  pendingClaimsCount: number;
  approvedClaimsCount: number;
  totalStorageSlots: number;
  occupiedStorageSlots: number;
  activeStaffCount: number;
  totalAuditsCount: number;
}

export interface StaffAuditLog {
  auditId: number;
  actorStaffId?: number;
  actorStaffName?: string;
  actionType: string;
  entityName: string;
  entityId?: number;
  details?: string;
  occurredAt: string;
}

export interface StaffMember {
  staffId: number;
  fullName: string;
  email: string;
  staffRole: string;
  staffRoleTitle: string;
  isActive: boolean;
  createdAt: string;
}

export interface CreateStaffPayload {
  fullName: string;
  email: string;
  password: string;
  staffRole: string;
}

interface CsrfResponse {
  headerName: string;
  parameterName: string;
  token: string;
}

let cachedCsrf: { headerName: string; token: string } | null = null;

export async function fetchCsrfToken(): Promise<{ headerName: string; token: string }> {
  try {
    const res = await fetch('/api/auth/csrf', {
      credentials: 'include',
    });
    if (!res.ok) {
      throw new Error(`Failed to fetch CSRF token: ${res.status}`);
    }
    const data: CsrfResponse = await res.json();
    cachedCsrf = { headerName: data.headerName, token: data.token };
    return cachedCsrf;
  } catch (err) {
    console.warn('Could not refresh CSRF token:', err);
    return cachedCsrf || { headerName: 'X-CSRF-TOKEN', token: '' };
  }
}

export async function getReferenceData(): Promise<ReferenceData> {
  const res = await fetch('/api/reference-data', {
    credentials: 'include',
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch reference data: ${res.status}`);
  }
  return res.json();
}

export async function getLostReports(params?: { status?: string; passengerId?: number }): Promise<LostReport[]> {
  const query = new URLSearchParams();
  if (params?.status && params.status !== 'All') {
    query.set('status', params.status);
  }
  if (params?.passengerId) {
    query.set('passengerId', String(params.passengerId));
  }

  const url = `/api/lost-reports${query.toString() ? `?${query.toString()}` : ''}`;
  const res = await fetch(url, {
    credentials: 'include',
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch lost reports: ${res.status}`);
  }
  return res.json();
}

export async function getMatches(params?: { status?: string; minScore?: number }): Promise<MatchRecord[]> {
  const query = new URLSearchParams();
  if (params?.status && params.status !== 'All') {
    query.set('status', params.status);
  }
  if (params?.minScore) {
    query.set('minScore', String(params.minScore));
  }

  const url = `/api/matches${query.toString() ? `?${query.toString()}` : ''}`;
  const res = await fetch(url, {
    credentials: 'include',
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch matches: ${res.status}`);
  }
  return res.json();
}

export async function getCurrentUser(): Promise<PassengerProfile | null> {
  try {
    const res = await fetch('/api/auth/me', {
      credentials: 'include',
    });
    if (res.status === 401 || res.status === 403) {
      return null;
    }
    if (!res.ok) {
      return null;
    }
    return res.json();
  } catch {
    return null;
  }
}

export async function login(payload: LoginPayload): Promise<PassengerProfile> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    if (res.status === 401) {
      throw new Error('Invalid email or password.');
    }
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Login failed. Please verify your credentials.');
  }

  // Refresh CSRF after session ID change
  await fetchCsrfToken();
  return res.json();
}

export async function register(payload: RegisterPayload): Promise<PassengerProfile> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/passengers', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Registration failed. An account with this email may already exist.');
  }

  return res.json();
}

export async function logout(): Promise<void> {
  const csrf = await fetchCsrfToken();
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: {
      [csrf.headerName]: csrf.token,
    },
  });
  cachedCsrf = null;
}

export async function createLostReport(payload: CreateLostReportPayload): Promise<LostReport> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/lost-reports', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to submit report (${res.status}). Please check all required fields.`);
  }

  return res.json();
}

// ---------------------------------------------------------------------------
// Staff Portal API Functions
// ---------------------------------------------------------------------------

export async function getStaffFoundItems(params?: { status?: string; search?: string }): Promise<StaffFoundItem[]> {
  const query = new URLSearchParams();
  if (params?.status && params.status !== 'All') {
    query.set('status', params.status);
  }
  if (params?.search) {
    query.set('search', params.search);
  }

  const url = `/api/staff/items${query.toString() ? `?${query.toString()}` : ''}`;
  const res = await fetch(url, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load found items: ${res.status}`);
  }
  return res.json();
}

export async function createStaffFoundItem(payload: CreateStaffFoundItemPayload): Promise<{ foundItemId: number; message: string }> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/staff/items', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to log found item (${res.status}).`);
  }
  return res.json();
}

export async function getStaffClaims(status?: string): Promise<StaffClaimItem[]> {
  const query = new URLSearchParams();
  if (status && status !== 'All') {
    query.set('status', status);
  }
  const url = `/api/staff/claims${query.toString() ? `?${query.toString()}` : ''}`;
  const res = await fetch(url, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load claims: ${res.status}`);
  }
  return res.json();
}

export async function reviewStaffClaim(claimId: number, payload: ReviewClaimPayload): Promise<{ message: string }> {
  const csrf = await fetchCsrfToken();
  const res = await fetch(`/api/staff/claims/${claimId}/review`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to update claim review (${res.status}).`);
  }
  return res.json();
}

export async function getStaffStorageLocations(): Promise<StorageLocationInfo[]> {
  const res = await fetch('/api/staff/inventory/locations', { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load storage inventory: ${res.status}`);
  }
  return res.json();
}

export async function assignStaffStorageSlot(payload: AssignStoragePayload): Promise<{ message: string }> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/staff/inventory/assign', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to assign storage slot (${res.status}).`);
  }
  return res.json();
}

export async function getStaffDispatches(status?: string): Promise<StaffReturnItem[]> {
  const query = new URLSearchParams();
  if (status && status !== 'All') {
    query.set('status', status);
  }
  const url = `/api/staff/dispatches${query.toString() ? `?${query.toString()}` : ''}`;
  const res = await fetch(url, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load dispatches: ${res.status}`);
  }
  return res.json();
}

export async function processStaffReturn(payload: ProcessReturnPayload): Promise<{ message: string }> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/staff/dispatches/process', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to process return dispatch (${res.status}).`);
  }
  return res.json();
}

export async function getSupervisorStats(): Promise<SupervisorStats> {
  const res = await fetch('/api/staff/supervisor/stats', { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load supervisor stats: ${res.status}`);
  }
  return res.json();
}

export async function getSupervisorAudits(limit = 50): Promise<StaffAuditLog[]> {
  const res = await fetch(`/api/staff/supervisor/audits?limit=${limit}`, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load audit logs: ${res.status}`);
  }
  return res.json();
}

export async function getAdminStaffMembers(): Promise<StaffMember[]> {
  const res = await fetch('/api/staff/admin/users', { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load staff directory: ${res.status}`);
  }
  return res.json();
}

export async function createAdminStaff(payload: CreateStaffPayload): Promise<StaffMember> {
  const csrf = await fetchCsrfToken();
  const res = await fetch('/api/staff/admin/users', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to create staff member (${res.status}).`);
  }
  return res.json();
}

export async function updateAdminStaffStatus(staffId: number, isActive: boolean): Promise<void> {
  const csrf = await fetchCsrfToken();
  const res = await fetch(`/api/staff/admin/users/${staffId}/status`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ isActive }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to update staff status (${res.status}).`);
  }
}

export async function updateAdminStaffRole(staffId: number, staffRole: string): Promise<void> {
  const csrf = await fetchCsrfToken();
  const res = await fetch(`/api/staff/admin/users/${staffId}/role`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ staffRole }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Failed to update staff role (${res.status}).`);
  }
}

// ---------------------------------------------------------------------------
// Photo Upload and Management API Functions
// ---------------------------------------------------------------------------

export interface UploadPhotoResult {
  photoUrl: string;
  caption: string;
  photoType: string;
  filename: string;
}

export async function uploadPhoto(
  file: File,
  caption?: string,
  photoType = 'overview'
): Promise<UploadPhotoResult> {
  const formData = new FormData();
  formData.append('file', file);
  if (caption) formData.append('caption', caption);
  if (photoType) formData.append('photoType', photoType);

  const res = await fetch('/api/photos/upload', {
    method: 'POST',
    credentials: 'include',
    body: formData,
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Photo upload failed (${res.status}). Please choose a valid image file.`);
  }

  return res.json();
}

export async function uploadPhotoBase64(
  base64Data: string,
  filename?: string,
  caption?: string,
  photoType = 'overview'
): Promise<UploadPhotoResult> {
  const res = await fetch('/api/photos/upload-base64', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ base64Data, filename, caption, photoType }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Photo upload failed (${res.status}).`);
  }

  return res.json();
}

export async function getItemPhotos(itemId: number): Promise<Array<{ photoNumber: number; photoUrl: string; caption?: string; photoType?: string }>> {
  const res = await fetch(`/api/items/${itemId}/photos`, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load photos for item ${itemId}`);
  }
  return res.json();
}

export async function getFoundItemPhotos(foundItemId: number): Promise<Array<{ photoNumber: number; photoUrl: string; caption?: string; photoType?: string }>> {
  const res = await fetch(`/api/staff/found-items/${foundItemId}/photos`, { credentials: 'include' });
  if (!res.ok) {
    throw new Error(`Failed to load photos for found item ${foundItemId}`);
  }
  return res.json();
}

