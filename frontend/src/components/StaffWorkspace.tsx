import { useState, useEffect, useCallback, type FormEvent } from 'react';
import {
  Luggage,
  ClipboardCheck,
  Warehouse,
  Truck,
  Activity,
  Users,
  Plus,
  Search,
  CheckCircle2,
  CircleAlert,
  ShieldCheck,
  RefreshCw,
  Lock,
  Boxes,
  X,
} from 'lucide-react';
import Dialog from './ui/Dialog';
import { ImageUploadWidget } from './ImageUploadWidget';
import {
  type PassengerProfile,
  type ReferenceData,
  type StaffFoundItem,
  type StaffClaimItem,
  type StorageLocationInfo,
  type StaffReturnItem,
  type SupervisorStats,
  type StaffAuditLog,
  type StaffMember,
  type PhotoAttachment,
  getStaffFoundItems,
  createStaffFoundItem,
  getStaffClaims,
  reviewStaffClaim,
  getStaffStorageLocations,
  assignStaffStorageSlot,
  getStaffDispatches,
  processStaffReturn,
  getSupervisorStats,
  getSupervisorAudits,
  getAdminStaffMembers,
  createAdminStaff,
  updateAdminStaffStatus,
  updateAdminStaffRole,
} from '../api/client';

interface StaffWorkspaceProps {
  currentUser: PassengerProfile;
  referenceData: ReferenceData;
  onRefreshPassengerData?: () => void;
}

type StaffTab = 'intake' | 'claims' | 'inventory' | 'dispatches' | 'supervisor' | 'admin';

export default function StaffWorkspace({
  currentUser,
  referenceData,
  onRefreshPassengerData,
}: StaffWorkspaceProps) {
  const authorities = currentUser.authorities || [];
  const staffRole = currentUser.staffRole || 'lost_found_agent';

  // Subrole identification helpers
  const isGroundStaff = staffRole === 'ground_staff' || staffRole === 'security_officer';
  const isLostFoundAgent = staffRole === 'lost_found_agent' || staffRole === 'lost_found_officer';

  // Permission checkers
  const hasIntakeAccess = authorities.includes('ROLE_STAFF_INTAKE') || authorities.includes('ROLE_SYSTEM_ADMIN');
  const hasClaimsAccess = authorities.includes('ROLE_CLAIMS_OFFICER') || authorities.includes('ROLE_SYSTEM_ADMIN');
  const hasInventoryAccess = authorities.includes('ROLE_INVENTORY_OFFICER') || authorities.includes('ROLE_SYSTEM_ADMIN');
  const hasDispatchAccess = authorities.includes('ROLE_DISPATCH_OFFICER') || authorities.includes('ROLE_SYSTEM_ADMIN');
  const hasSupervisorAccess = authorities.includes('ROLE_SUPERVISOR') || authorities.includes('ROLE_SYSTEM_ADMIN');
  const hasAdminAccess = authorities.includes('ROLE_SYSTEM_ADMIN');

  // Determine initial tab based on role
  const getDefaultTab = (): StaffTab => {
    if (staffRole === 'ground_staff' || staffRole === 'lost_found_agent') return 'intake';
    if (staffRole === 'claims_verification_officer') return 'claims';
    if (staffRole === 'inventory_storage_officer') return 'inventory';
    if (staffRole === 'return_dispatch_officer') return 'dispatches';
    if (staffRole === 'supervisor') return 'supervisor';
    if (staffRole === 'system_admin') return 'admin';
    return 'intake';
  };

  const [activeTab, setActiveTab] = useState<StaffTab>(getDefaultTab);

  // General Loading & Feedback State
  const [loading, setLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Tab 1: Intake State
  const [foundItems, setFoundItems] = useState<StaffFoundItem[]>([]);
  const [foundStatusFilter, setFoundStatusFilter] = useState('All');
  const [foundSearchTerm, setFoundSearchTerm] = useState('');
  const [newFoundModalOpen, setNewFoundModalOpen] = useState(false);
  const [newFoundForm, setNewFoundForm] = useState({
    categoryId: referenceData.categories[0]?.categoryId || 1,
    itemName: '',
    brand: '',
    colour: '',
    description: '',
    estimatedValue: '',
    itemCondition: 'good',
    flightId: '',
    foundLocationId: referenceData.locations[0]?.locationId || 1,
    photos: [] as PhotoAttachment[],
  });
  const [lightboxImg, setLightboxImg] = useState<string | null>(null);

  // Tab 2: Claims State
  const [claims, setClaims] = useState<StaffClaimItem[]>([]);
  const [claimsFilter, setClaimsFilter] = useState('All');
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [selectedClaim, setSelectedClaim] = useState<StaffClaimItem | null>(null);
  const [reviewDecision, setReviewDecision] = useState<'approved' | 'rejected'>('approved');
  const [verificationMethod, setVerificationMethod] = useState('in_person_document_check');
  const [reviewNotes, setReviewNotes] = useState('');

  // Tab 3: Inventory State
  const [storageLocations, setStorageLocations] = useState<StorageLocationInfo[]>([]);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignFoundItemId, setAssignFoundItemId] = useState<number | ''>('');
  const [assignStorageId, setAssignStorageId] = useState<number | ''>('');
  const [assignSlotNumber, setAssignSlotNumber] = useState<number | ''>(1);

  // Tab 4: Dispatches State
  const [dispatches, setDispatches] = useState<StaffReturnItem[]>([]);
  const [dispatchFilter, setDispatchFilter] = useState('All');
  const [processModalOpen, setProcessModalOpen] = useState(false);
  const [selectedDispatch, setSelectedDispatch] = useState<StaffReturnItem | null>(null);
  const [processStatus, setProcessStatus] = useState<'initiated' | 'in_transit' | 'completed'>('completed');
  const [processMethod, setProcessMethod] = useState<'counter_handover' | 'delivery'>('counter_handover');
  const [processNotes, setProcessNotes] = useState('');

  // Tab 5: Supervisor State
  const [stats, setStats] = useState<SupervisorStats | null>(null);
  const [auditLogs, setAuditLogs] = useState<StaffAuditLog[]>([]);

  // Tab 6: Admin State
  const [staffDirectory, setStaffDirectory] = useState<StaffMember[]>([]);
  const [newStaffModalOpen, setNewStaffModalOpen] = useState(false);
  const [newStaffForm, setNewStaffForm] = useState({
    fullName: '',
    email: '',
    password: '',
    staffRole: 'lost_found_agent',
  });

  const showToast = (type: 'success' | 'error', text: string) => {
    setToastMessage({ type, text });
    setTimeout(() => setToastMessage(null), 5000);
  };

  // -------------------------------------------------------------------------
  // Data Fetching per Tab
  // -------------------------------------------------------------------------

  const loadIntakeData = useCallback(async () => {
    if (!hasIntakeAccess) return;
    setLoading(true);
    try {
      const items = await getStaffFoundItems({
        status: foundStatusFilter,
        search: foundSearchTerm,
      });
      setFoundItems(items);
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load found items.');
    } finally {
      setLoading(false);
    }
  }, [hasIntakeAccess, foundStatusFilter, foundSearchTerm]);

  const loadClaimsData = useCallback(async () => {
    if (!hasClaimsAccess) return;
    setLoading(true);
    try {
      const data = await getStaffClaims(claimsFilter);
      setClaims(data);
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load claims.');
    } finally {
      setLoading(false);
    }
  }, [hasClaimsAccess, claimsFilter]);

  const loadInventoryData = useCallback(async () => {
    if (!hasInventoryAccess) return;
    setLoading(true);
    try {
      const locs = await getStaffStorageLocations();
      setStorageLocations(locs);
      if (locs.length > 0 && !assignStorageId) {
        setAssignStorageId(locs[0].storageId);
      }
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load storage inventory.');
    } finally {
      setLoading(false);
    }
  }, [hasInventoryAccess, assignStorageId]);

  const loadDispatchesData = useCallback(async () => {
    if (!hasDispatchAccess) return;
    setLoading(true);
    try {
      const data = await getStaffDispatches(dispatchFilter);
      setDispatches(data);
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load dispatches.');
    } finally {
      setLoading(false);
    }
  }, [hasDispatchAccess, dispatchFilter]);

  const loadSupervisorData = useCallback(async () => {
    if (!hasSupervisorAccess) return;
    setLoading(true);
    try {
      const [s, a] = await Promise.all([getSupervisorStats(), getSupervisorAudits(30)]);
      setStats(s);
      setAuditLogs(a);
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load supervisor telemetry.');
    } finally {
      setLoading(false);
    }
  }, [hasSupervisorAccess]);

  const loadAdminData = useCallback(async () => {
    if (!hasAdminAccess) return;
    setLoading(true);
    try {
      const users = await getAdminStaffMembers();
      setStaffDirectory(users);
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to load staff accounts.');
    } finally {
      setLoading(false);
    }
  }, [hasAdminAccess]);

  // Trigger loads on tab change
  useEffect(() => {
    if (activeTab === 'intake') loadIntakeData();
    else if (activeTab === 'claims') loadClaimsData();
    else if (activeTab === 'inventory') loadInventoryData();
    else if (activeTab === 'dispatches') loadDispatchesData();
    else if (activeTab === 'supervisor') loadSupervisorData();
    else if (activeTab === 'admin') loadAdminData();
  }, [activeTab, loadIntakeData, loadClaimsData, loadInventoryData, loadDispatchesData, loadSupervisorData, loadAdminData]);

  // -------------------------------------------------------------------------
  // Handlers
  // -------------------------------------------------------------------------

  const handleRegisterFoundItem = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await createStaffFoundItem({
        categoryId: Number(newFoundForm.categoryId),
        itemName: newFoundForm.itemName.trim(),
        brand: newFoundForm.brand.trim() || undefined,
        colour: newFoundForm.colour.trim() || undefined,
        description: newFoundForm.description.trim() || undefined,
        estimatedValue: newFoundForm.estimatedValue ? Number(newFoundForm.estimatedValue) : undefined,
        itemCondition: newFoundForm.itemCondition,
        flightId: newFoundForm.flightId ? Number(newFoundForm.flightId) : undefined,
        foundLocationId: newFoundForm.foundLocationId ? Number(newFoundForm.foundLocationId) : undefined,
        foundAt: new Date(Date.now() - 5 * 60000).toISOString(),
        photos: newFoundForm.photos && newFoundForm.photos.length > 0 ? newFoundForm.photos : undefined,
      });
      showToast('success', `Found item "${newFoundForm.itemName}" registered in custody register.`);
      setNewFoundModalOpen(false);
      setNewFoundForm({
        categoryId: referenceData.categories[0]?.categoryId || 1,
        itemName: '',
        brand: '',
        colour: '',
        description: '',
        estimatedValue: '',
        itemCondition: 'good',
        flightId: '',
        foundLocationId: referenceData.locations[0]?.locationId || 1,
        photos: [],
      });
      loadIntakeData();
      if (onRefreshPassengerData) onRefreshPassengerData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to register item.');
    } finally {
      setLoading(false);
    }
  };

  const handleReviewClaimSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedClaim) return;
    setLoading(true);
    try {
      await reviewStaffClaim(selectedClaim.claimId, {
        decision: reviewDecision,
        verificationMethod,
        notes: reviewNotes.trim() || undefined,
      });
      showToast('success', `Claim #${selectedClaim.claimId} has been marked as ${reviewDecision}.`);
      setReviewModalOpen(false);
      setSelectedClaim(null);
      setReviewNotes('');
      loadClaimsData();
      if (onRefreshPassengerData) onRefreshPassengerData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to update claim.');
    } finally {
      setLoading(false);
    }
  };

  const handleAssignStorageSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!assignFoundItemId || !assignStorageId || !assignSlotNumber) return;
    setLoading(true);
    try {
      await assignStaffStorageSlot({
        foundItemId: Number(assignFoundItemId),
        storageId: Number(assignStorageId),
        slotNumber: Number(assignSlotNumber),
      });
      showToast('success', `Item #${assignFoundItemId} assigned to locker slot #${assignSlotNumber}.`);
      setAssignModalOpen(false);
      setAssignFoundItemId('');
      loadInventoryData();
      loadIntakeData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to assign storage.');
    } finally {
      setLoading(false);
    }
  };

  const handleProcessReturnSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedDispatch) return;
    setLoading(true);
    try {
      await processStaffReturn({
        claimId: selectedDispatch.claimId,
        returnMethod: processMethod,
        returnStatus: processStatus,
        deliveryNotes: processNotes.trim() || undefined,
      });
      showToast('success', `Return dispatch #${selectedDispatch.returnId} updated to ${processStatus}.`);
      setProcessModalOpen(false);
      setSelectedDispatch(null);
      setProcessNotes('');
      loadDispatchesData();
      if (onRefreshPassengerData) onRefreshPassengerData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to update return dispatch.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateStaffSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const created = await createAdminStaff(newStaffForm);
      showToast('success', `Staff member ${created.fullName} (${created.staffRoleTitle}) created successfully.`);
      setNewStaffModalOpen(false);
      setNewStaffForm({
        fullName: '',
        email: '',
        password: '',
        staffRole: 'lost_found_agent',
      });
      loadAdminData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to create staff member.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStaffStatus = async (user: StaffMember) => {
    try {
      await updateAdminStaffStatus(user.staffId, !user.isActive);
      showToast('success', `Account for ${user.fullName} is now ${!user.isActive ? 'Active' : 'Deactivated'}.`);
      loadAdminData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to update status.');
    }
  };

  const handleRoleChange = async (user: StaffMember, newRole: string) => {
    try {
      await updateAdminStaffRole(user.staffId, newRole);
      showToast('success', `Role for ${user.fullName} updated to ${newRole}.`);
      loadAdminData();
    } catch (err) {
      showToast('error', err instanceof Error ? err.message : 'Failed to change role.');
    }
  };

  return (
    <div className="staff-workspace" id="staff-workspace-root">
      {/* Workspace Header */}
      <section className="staff-header-banner">
        <div className="staff-header-left">
          <div className="staff-role-pill">
            {isGroundStaff ? <ShieldCheck size={14} /> : <Luggage size={14} />}
            <span>{currentUser.staffRoleTitle || 'Airport Staff'}</span>
          </div>
          <h2>
            {isGroundStaff
              ? 'Airside Patrol & Custody Intake Console'
              : isLostFoundAgent
              ? 'Terminal Lost & Found Custody Desk'
              : 'Airport Custody & Operational Register'}
          </h2>
          <p className="staff-header-subtitle">
            {isGroundStaff
              ? 'Terminal airside discovery console for logging unattended items across security checkpoints, gates, and aircraft cabins to establish an immediate chain of custody.'
              : isLostFoundAgent
              ? 'Landside custody counter console for item cataloging, physical condition grading, photo evidence documentation, and cross-terminal registry searches.'
              : 'Restricted role-based terminal console for property intake, biometric/ID verification, physical vault custody, and property dispatch.'}
          </p>
        </div>

        <div className="staff-header-right">
          <div className="staff-actor-card">
            <span className="staff-actor-badge">
              {isGroundStaff ? 'Airside Clearance: Active' : isLostFoundAgent ? 'Custody Desk: Counter 1' : 'Active Duty'}
            </span>
            <strong>{currentUser.fullName}</strong>
            <small>{currentUser.email}</small>
          </div>
        </div>
      </section>

      {/* Floating Status Toast */}
      {toastMessage && (
        <div className={`status-toast ${toastMessage.type === 'success' ? 'status-toast-success' : 'status-toast-error'}`} role="status">
          {toastMessage.type === 'success' ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}
          <span>{toastMessage.text}</span>
        </div>
      )}

      {/* Subrole Tab Navigation Bar */}
      <div className="staff-tabs-bar" role="tablist">
        <button
          className={`staff-tab-btn ${activeTab === 'intake' ? 'staff-tab-btn-active' : ''} ${!hasIntakeAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasIntakeAccess && setActiveTab('intake')}
          disabled={!hasIntakeAccess}
        >
          {isGroundStaff ? <ShieldCheck size={16} /> : <Luggage size={16} />}
          <span>{isGroundStaff ? 'Airside Patrol Intake' : isLostFoundAgent ? 'Custody Desk Register' : 'Intake Desk'}</span>
          {!hasIntakeAccess && <Lock size={12} className="lock-icon" />}
        </button>

        <button
          className={`staff-tab-btn ${activeTab === 'claims' ? 'staff-tab-btn-active' : ''} ${!hasClaimsAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasClaimsAccess && setActiveTab('claims')}
          disabled={!hasClaimsAccess}
        >
          <ClipboardCheck size={16} />
          <span>Claims & Verification</span>
          {!hasClaimsAccess && <Lock size={12} className="lock-icon" />}
        </button>

        <button
          className={`staff-tab-btn ${activeTab === 'inventory' ? 'staff-tab-btn-active' : ''} ${!hasInventoryAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasInventoryAccess && setActiveTab('inventory')}
          disabled={!hasInventoryAccess}
        >
          <Warehouse size={16} />
          <span>Vault & Inventory</span>
          {!hasInventoryAccess && <Lock size={12} className="lock-icon" />}
        </button>

        <button
          className={`staff-tab-btn ${activeTab === 'dispatches' ? 'staff-tab-btn-active' : ''} ${!hasDispatchAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasDispatchAccess && setActiveTab('dispatches')}
          disabled={!hasDispatchAccess}
        >
          <Truck size={16} />
          <span>Return & Dispatch</span>
          {!hasDispatchAccess && <Lock size={12} className="lock-icon" />}
        </button>

        <button
          className={`staff-tab-btn ${activeTab === 'supervisor' ? 'staff-tab-btn-active' : ''} ${!hasSupervisorAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasSupervisorAccess && setActiveTab('supervisor')}
          disabled={!hasSupervisorAccess}
        >
          <Activity size={16} />
          <span>Supervisor Telemetry</span>
          {!hasSupervisorAccess && <Lock size={12} className="lock-icon" />}
        </button>

        <button
          className={`staff-tab-btn ${activeTab === 'admin' ? 'staff-tab-btn-active' : ''} ${!hasAdminAccess ? 'staff-tab-btn-disabled' : ''}`}
          onClick={() => hasAdminAccess && setActiveTab('admin')}
          disabled={!hasAdminAccess}
        >
          <Users size={16} />
          <span>Staff Directory</span>
          {!hasAdminAccess && <Lock size={12} className="lock-icon" />}
        </button>
      </div>

      {/* =====================================================================
          TAB 1: INTAKE DESK (Lost & Found Agent, Ground Staff, Supervisor, Admin)
         ===================================================================== */}
      {activeTab === 'intake' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>
                {isGroundStaff
                  ? 'Airside Discovery & Checkpoint Intake'
                  : isLostFoundAgent
                  ? 'Terminal Custody Desk & Inventory Register'
                  : 'Custody Intake & Item Register'}
              </h3>
              <p>
                {isGroundStaff
                  ? 'Log unattended baggage and property recovered during gate patrols, cabin sweeps, or security screening into the official chain of custody.'
                  : isLostFoundAgent
                  ? 'Catalog property handed over at the counter, record condition assessments, and search the active custody register for passenger inquiries.'
                  : 'Log newly discovered baggage, electronics, and documents handed over by terminal staff or passengers.'}
              </p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadIntakeData} title="Refresh item register">
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh
              </button>
              <button className="button button-primary" onClick={() => setNewFoundModalOpen(true)}>
                <Plus size={16} /> {isGroundStaff ? 'Log Patrol Discovery' : 'Intake Recovered Item'}
              </button>
            </div>
          </div>

          {/* Role Operational Context Banner */}
          {isGroundStaff && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', background: 'rgba(2, 132, 199, 0.08)', border: '1px solid rgba(2, 132, 199, 0.22)', borderRadius: 'var(--radius-xs)', marginBottom: '16px' }}>
              <ShieldCheck size={22} style={{ color: '#0284c7', flexShrink: 0 }} />
              <div>
                <strong style={{ fontSize: '13px', color: '#0369a1', display: 'block' }}>Field Patrol Mode — Airside Checkpoint Discovery</strong>
                <span style={{ fontSize: '12px', color: 'var(--muted)' }}>
                  You are authenticated with <strong>Airside Terminal Clearance</strong>. Log unattended items immediately upon recovery to record time and location proof. Custody handovers and vault storage assignments are completed at the landside desk.
                </span>
              </div>
            </div>
          )}

          {isLostFoundAgent && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', background: 'rgba(8, 127, 137, 0.08)', border: '1px solid rgba(8, 127, 137, 0.22)', borderRadius: 'var(--radius-xs)', marginBottom: '16px' }}>
              <Luggage size={22} style={{ color: 'var(--teal)', flexShrink: 0 }} />
              <div>
                <strong style={{ fontSize: '13px', color: 'var(--teal)', display: 'block' }}>Custody Counter Mode — Terminal 1 & 2 Intake Specialist</strong>
                <span style={{ fontSize: '12px', color: 'var(--muted)' }}>
                  You are authenticated as <strong>Lost & Found Agent</strong> stationed at the Landside Custody Counter. You have full authority to catalog condition assessments, upload photo evidence, and search cross-terminal inventory for walk-in passengers.
                </span>
              </div>
            </div>
          )}

          {/* Search & Filters */}
          <div className="staff-search-row">
            <div className="staff-search-input-wrap">
              <Search size={16} />
              <input
                type="text"
                placeholder="Search by item name, brand, or location..."
                value={foundSearchTerm}
                onChange={(e) => setFoundSearchTerm(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && loadIntakeData()}
              />
            </div>
            <div className="staff-filter-group">
              <label>Status:</label>
              <select value={foundStatusFilter} onChange={(e) => setFoundStatusFilter(e.target.value)}>
                <option value="All">All statuses</option>
                <option value="awaiting_review">Awaiting Review</option>
                <option value="stored">Stored in Vault</option>
                <option value="claimed">Claimed</option>
                <option value="returned">Returned to Owner</option>
              </select>
            </div>
          </div>

          {/* Items Table */}
          <div className="staff-table-wrap">
            <table className="staff-table">
              <thead>
                <tr>
                  <th>Item #</th>
                  <th>Property Details</th>
                  <th>Category</th>
                  <th>Discovery Location</th>
                  <th>Reported By</th>
                  <th>Custody Status</th>
                  <th>Vault Slot</th>
                  <th>Discovered At</th>
                </tr>
              </thead>
              <tbody>
                {foundItems.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="table-empty">
                      No found property items match your current filter.
                    </td>
                  </tr>
                ) : (
                  foundItems.map((item) => (
                    <tr key={item.foundItemId}>
                      <td>
                        <span className="mono-code">#{item.foundItemId}</span>
                      </td>
                      <td>
                        <strong>{item.itemName}</strong>
                        {item.brand && <small className="table-sub">{item.brand} {item.colour ? `· ${item.colour}` : ''}</small>}
                        {item.photoUrls && item.photoUrls.length > 0 && (
                          <div style={{ display: 'flex', gap: '6px', marginTop: '6px' }}>
                            {item.photoUrls.map((url, idx) => (
                              <img
                                key={idx}
                                src={url}
                                alt={`Custody photo ${idx + 1}`}
                                style={{
                                  width: '32px',
                                  height: '32px',
                                  borderRadius: '4px',
                                  objectFit: 'cover',
                                  border: '1px solid var(--line)',
                                  cursor: 'pointer'
                                }}
                                onClick={() => setLightboxImg(url)}
                                title="Click to view photo"
                              />
                            ))}
                          </div>
                        )}
                      </td>
                      <td>
                        <span className="table-tag">{item.categoryName}</span>
                      </td>
                      <td>
                        {item.foundLocationName || 'Terminal'}
                        {item.flightNumber && <small className="table-sub">Flight {item.flightNumber}</small>}
                      </td>
                      <td>
                        <span>{item.foundByStaffName}</span>
                      </td>
                      <td>
                        <span className={`status-badge status-${item.itemStatus}`}>
                          <span className="status-badge-dot" />
                          {item.itemStatus.replace('_', ' ')}
                        </span>
                      </td>
                      <td>
                        {item.storageName ? (
                          <div>
                            <span className="vault-tag">
                              <Warehouse size={12} /> {item.storageName} (Slot {item.slotNumber})
                            </span>
                            {item.daysInStorage !== undefined && item.daysInStorage !== null && (
                              <small className="table-sub" style={{ display: 'block', marginTop: '2px' }}>
                                {item.daysInStorage} day{item.daysInStorage === 1 ? '' : 's'} stored
                              </small>
                            )}
                          </div>
                        ) : (
                          <span className="vault-tag vault-tag-none">Unassigned</span>
                        )}
                      </td>
                      <td className="table-date">
                        {new Date(item.foundAt).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* =====================================================================
          TAB 2: CLAIMS VERIFICATION (Claims Officer, Supervisor, Admin)
         ===================================================================== */}
      {activeTab === 'claims' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>Passenger Claim Reviews & Identity Verification</h3>
              <p>Verify candidate matches, review identity verification documents, and approve custody transfers.</p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadClaimsData} title="Refresh claims">
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh
              </button>
            </div>
          </div>

          <div className="staff-search-row">
            <div className="staff-filter-group">
              <label>Claim Status:</label>
              <select value={claimsFilter} onChange={(e) => setClaimsFilter(e.target.value)}>
                <option value="All">All statuses</option>
                <option value="submitted">Submitted (Pending Review)</option>
                <option value="under_review">Under Review</option>
                <option value="approved">Approved</option>
                <option value="rejected">Rejected</option>
              </select>
            </div>
          </div>

          <div className="staff-table-wrap">
            <table className="staff-table">
              <thead>
                <tr>
                  <th>Claim #</th>
                  <th>Claimant Passenger</th>
                  <th>Lost Property vs Found Property</th>
                  <th>Match Confidence</th>
                  <th>Claim Status</th>
                  <th>ID Verification</th>
                  <th>Submitted</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {claims.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="table-empty">
                      No claims currently require action.
                    </td>
                  </tr>
                ) : (
                  claims.map((claim) => (
                    <tr key={claim.claimId}>
                      <td><span className="mono-code">#{claim.claimId}</span></td>
                      <td>
                        <strong>{claim.passengerName}</strong>
                        <small className="table-sub">{claim.passengerEmail}</small>
                        {claim.passengerPhone && <small className="table-sub">{claim.passengerPhone}</small>}
                      </td>
                      <td>
                        <div><strong>Lost:</strong> {claim.lostItemName}</div>
                        <div><strong>Found:</strong> {claim.foundItemName}</div>
                      </td>
                      <td>
                        <span className="match-score-pill">
                          {Number(claim.matchScore || 0).toFixed(1)}% Match
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-${claim.claimStatus}`}>
                          <span className="status-badge-dot" />
                          {claim.claimStatus.replace('_', ' ')}
                        </span>
                      </td>
                      <td>
                        {claim.verificationResult ? (
                          <span className="table-tag table-tag-verified">
                            <CheckCircle2 size={12} /> {claim.verificationResult} ({claim.verificationMethod || 'ID Check'})
                          </span>
                        ) : (
                          <span className="table-tag table-tag-pending">Pending Verification</span>
                        )}
                      </td>
                      <td className="table-date">
                        {new Date(claim.submittedAt).toLocaleDateString()}
                      </td>
                      <td>
                        <button
                          className="button button-small button-outline"
                          onClick={() => {
                            setSelectedClaim(claim);
                            setReviewDecision('approved');
                            setVerificationMethod(claim.verificationMethod || 'in_person_document_check');
                            setReviewNotes(claim.decisionNotes || '');
                            setReviewModalOpen(true);
                          }}
                        >
                          Review & Decide
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* =====================================================================
          TAB 3: VAULT & INVENTORY (Inventory Storage Officer, Supervisor, Admin)
         ===================================================================== */}
      {activeTab === 'inventory' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>Secure Vault & Locker Inventory</h3>
              <p>Manage physical storage locations, track locker slot occupancy, and assign newly processed items.</p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadInventoryData}>
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh
              </button>
              <button className="button button-primary" onClick={() => setAssignModalOpen(true)}>
                <Boxes size={16} /> Assign Item to Locker
              </button>
            </div>
          </div>

          <div className="vault-cards-grid">
            {storageLocations.map((loc) => {
              const occupancyPct = Math.round((loc.occupiedSlots / loc.capacity) * 100);
              return (
                <div key={loc.storageId} className="vault-location-card">
                  <div className="vault-card-header">
                    <div>
                      <h4>{loc.storageName}</h4>
                      <small><Warehouse size={13} /> {loc.locationName}</small>
                    </div>
                    <span className="vault-capacity-badge">
                      {loc.availableSlots} / {loc.capacity} Slots Free
                    </span>
                  </div>

                  <p className="vault-desc">{loc.description || 'Secure airport custodial locker facility.'}</p>

                  <div className="vault-meter">
                    <div className="vault-meter-fill" style={{ width: `${occupancyPct}%` }} />
                  </div>
                  <div className="vault-meter-labels">
                    <span>Occupancy: {occupancyPct}%</span>
                    <span>{loc.occupiedSlots} Occupied</span>
                  </div>

                  {/* Slot Grid */}
                  <div className="vault-slots-container">
                    <div className="slots-title">Locker Slots Visual Map:</div>
                    <div className="vault-slots-grid">
                      {loc.slots.map((slot) => {
                        const isOccupied = !!slot.assignedFoundItemId;
                        return (
                          <div
                            key={slot.slotNumber}
                            className={`slot-tile ${isOccupied ? 'slot-tile-occupied' : 'slot-tile-free'}`}
                            title={isOccupied ? `Slot ${slot.slotNumber}: ${slot.assignedItemName} (Item #${slot.assignedFoundItemId})` : `Slot ${slot.slotNumber}: Available`}
                          >
                            <span className="slot-num">{slot.slotNumber}</span>
                            <span className="slot-state">{isOccupied ? 'Occupied' : 'Free'}</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* =====================================================================
          TAB 4: RETURN & DISPATCH (Return Dispatch Officer, Supervisor, Admin)
         ===================================================================== */}
      {activeTab === 'dispatches' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>Counter Handover & Property Dispatch</h3>
              <p>Coordinate final property handover at the terminal desk or courier dispatch for verified claims.</p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadDispatchesData}>
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh
              </button>
            </div>
          </div>

          <div className="staff-search-row">
            <div className="staff-filter-group">
              <label>Dispatch Status:</label>
              <select value={dispatchFilter} onChange={(e) => setDispatchFilter(e.target.value)}>
                <option value="All">All statuses</option>
                <option value="initiated">Initiated</option>
                <option value="in_transit">In Transit (Courier)</option>
                <option value="completed">Completed / Handed Over</option>
              </select>
            </div>
          </div>

          <div className="staff-table-wrap">
            <table className="staff-table">
              <thead>
                <tr>
                  <th>Dispatch #</th>
                  <th>Claimant</th>
                  <th>Property</th>
                  <th>Handover Method</th>
                  <th>Dispatch Status</th>
                  <th>Handled By Officer</th>
                  <th>Notes</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {dispatches.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="table-empty">
                      No property handovers or dispatches recorded.
                    </td>
                  </tr>
                ) : (
                  dispatches.map((disp) => (
                    <tr key={disp.returnId}>
                      <td><span className="mono-code">#{disp.returnId}</span></td>
                      <td>
                        <strong>{disp.passengerName}</strong>
                        <small className="table-sub">{disp.passengerEmail}</small>
                      </td>
                      <td><strong>{disp.itemName}</strong></td>
                      <td>
                        <span className="table-tag">
                          {disp.returnMethod === 'counter_handover' ? 'Terminal Counter Handover' : 'Courier Delivery'}
                        </span>
                      </td>
                      <td>
                        <span className={`status-badge status-${disp.returnStatus}`}>
                          <span className="status-badge-dot" />
                          {disp.returnStatus.replace('_', ' ')}
                        </span>
                      </td>
                      <td>{disp.handledByStaffName}</td>
                      <td><span className="table-notes">{disp.deliveryNotes || '—'}</span></td>
                      <td>
                        <button
                          className="button button-small button-outline"
                          onClick={() => {
                            setSelectedDispatch(disp);
                            setProcessMethod(disp.returnMethod);
                            setProcessStatus(disp.returnStatus as 'initiated' | 'in_transit' | 'completed');
                            setProcessNotes(disp.deliveryNotes || '');
                            setProcessModalOpen(true);
                          }}
                        >
                          Update Status
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* =====================================================================
          TAB 5: SUPERVISOR TELEMETRY (Supervisor, Admin)
         ===================================================================== */}
      {activeTab === 'supervisor' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>Supervisor Telemetry & Operational KPIs</h3>
              <p>Executive metrics, custody lifecycle audit trails, and staff activity telemetry.</p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadSupervisorData}>
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh Telemetry
              </button>
            </div>
          </div>

          {stats && (
            <div className="kpi-grid">
              <div className="kpi-card">
                <span className="kpi-label">Total Found Property</span>
                <span className="kpi-value">{stats.totalFoundItems}</span>
                <small className="kpi-sub">{stats.awaitingReviewCount} awaiting review</small>
              </div>

              <div className="kpi-card">
                <span className="kpi-label">Stored in Vault</span>
                <span className="kpi-value">{stats.storedCount}</span>
                <small className="kpi-sub">{stats.occupiedStorageSlots} / {stats.totalStorageSlots} locker slots used</small>
              </div>

              <div className="kpi-card">
                <span className="kpi-label">Claims Verified</span>
                <span className="kpi-value">{stats.approvedClaimsCount}</span>
                <small className="kpi-sub">{stats.pendingClaimsCount} claims pending review</small>
              </div>

              <div className="kpi-card">
                <span className="kpi-label">Returned to Passenger</span>
                <span className="kpi-value">{stats.returnedCount}</span>
                <small className="kpi-sub">Fully closed lifecycle cases</small>
              </div>

              <div className="kpi-card">
                <span className="kpi-label">Active Staff Force</span>
                <span className="kpi-value">{stats.activeStaffCount}</span>
                <small className="kpi-sub">Across 7 specialized roles</small>
              </div>

              <div className="kpi-card">
                <span className="kpi-label">Audit Log Events</span>
                <span className="kpi-value">{stats.totalAuditsCount}</span>
                <small className="kpi-sub">Immutable audit entries logged</small>
              </div>
            </div>
          )}

          {/* Audit Logs Table */}
          <div className="supervisor-audit-section">
            <h4>Live Operational Audit Stream</h4>
            <div className="staff-table-wrap">
              <table className="staff-table">
                <thead>
                  <tr>
                    <th>Log #</th>
                    <th>Action</th>
                    <th>Actor Staff</th>
                    <th>Entity</th>
                    <th>Event Details</th>
                    <th>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="table-empty">
                        No audit events recorded yet.
                      </td>
                    </tr>
                  ) : (
                    auditLogs.map((log) => (
                      <tr key={log.auditId}>
                        <td><span className="mono-code">#{log.auditId}</span></td>
                        <td>
                          <span className={`table-tag action-tag-${log.actionType.toLowerCase()}`}>
                            {log.actionType}
                          </span>
                        </td>
                        <td><strong>{log.actorStaffName || 'System'}</strong></td>
                        <td><span className="mono-code">{log.entityName} #{log.entityId}</span></td>
                        <td><span className="table-notes mono-text">{log.details || '—'}</span></td>
                        <td className="table-date">{new Date(log.occurredAt).toLocaleString()}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      {/* =====================================================================
          TAB 6: STAFF ADMINISTRATION (System Admin Only)
         ===================================================================== */}
      {activeTab === 'admin' && (
        <section className="staff-panel">
          <div className="staff-panel-header">
            <div>
              <h3>Airport Staff Directory & Access Control</h3>
              <p>Provision staff accounts, assign subroles, and govern system security privileges.</p>
            </div>
            <div className="staff-panel-actions">
              <button className="button button-outline" onClick={loadAdminData}>
                <RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh Directory
              </button>
              <button className="button button-primary" onClick={() => setNewStaffModalOpen(true)}>
                <Plus size={16} /> Provision Staff Account
              </button>
            </div>
          </div>

          <div className="staff-table-wrap">
            <table className="staff-table">
              <thead>
                <tr>
                  <th>Staff ID</th>
                  <th>Full Name</th>
                  <th>Official Email</th>
                  <th>Assigned Subrole</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th>Privilege Actions</th>
                </tr>
              </thead>
              <tbody>
                {staffDirectory.map((user) => (
                  <tr key={user.staffId}>
                    <td><span className="mono-code">#{user.staffId}</span></td>
                    <td><strong>{user.fullName}</strong></td>
                    <td>{user.email}</td>
                    <td>
                      <select
                        className="role-selector-dropdown"
                        value={user.staffRole}
                        onChange={(e) => handleRoleChange(user, e.target.value)}
                      >
                        <option value="lost_found_agent">Lost & Found Agent</option>
                        <option value="claims_verification_officer">Claims Verification Officer</option>
                        <option value="inventory_storage_officer">Inventory & Storage Officer</option>
                        <option value="return_dispatch_officer">Return & Dispatch Officer</option>
                        <option value="supervisor">Lost Property Supervisor</option>
                        <option value="ground_staff">Ground Staff (Security/Cleaning)</option>
                        <option value="system_admin">System Administrator</option>
                      </select>
                    </td>
                    <td>
                      <span className={`status-badge ${user.isActive ? 'status-open' : 'status-rejected'}`}>
                        <span className="status-badge-dot" />
                        {user.isActive ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="table-date">{new Date(user.createdAt).toLocaleDateString()}</td>
                    <td>
                      <button
                        className={`button button-small ${user.isActive ? 'button-outline' : 'button-primary'}`}
                        onClick={() => handleToggleStaffStatus(user)}
                      >
                        {user.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* =====================================================================
          MODAL A: REGISTER FOUND ITEM (Intake Desk)
         ===================================================================== */}
      <Dialog
        open={newFoundModalOpen}
        onOpenChange={setNewFoundModalOpen}
        title={
          isGroundStaff
            ? 'Log Airside Patrol Discovery'
            : isLostFoundAgent
            ? 'Custody Desk Property Intake'
            : 'Log Discovered Property into Custody'
        }
        description={
          isGroundStaff
            ? 'Establish an official chain-of-custody record for property found during security sweeps, gate checks, or cabin cleaning.'
            : isLostFoundAgent
            ? 'Catalog property received at the Lost & Found reception counter with condition evaluation and photographic evidence.'
            : 'Register an item found in the terminal, security gate, or aircraft cabin into the official audit register.'
        }
        className="staff-modal"
      >
        <form onSubmit={handleRegisterFoundItem} className="dialog-form">
          <div className="form-grid-two">
            <label className="field">
              <span className="field-label">Category *</span>
              <select
                required
                value={newFoundForm.categoryId}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, categoryId: Number(e.target.value) }))}
              >
                {referenceData.categories.map((c) => (
                  <option key={c.categoryId} value={c.categoryId}>{c.categoryName}</option>
                ))}
              </select>
            </label>

            <label className="field">
              <span className="field-label">Item Name *</span>
              <input
                type="text"
                required
                placeholder="e.g. Black Leather Wallet"
                value={newFoundForm.itemName}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, itemName: e.target.value }))}
              />
            </label>
          </div>

          <div className="form-grid-two">
            <label className="field">
              <span className="field-label">Brand / Manufacturer</span>
              <input
                type="text"
                placeholder="e.g. Samsonite, Apple, Bose"
                value={newFoundForm.brand}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, brand: e.target.value }))}
              />
            </label>

            <label className="field">
              <span className="field-label">Dominant Colour</span>
              <input
                type="text"
                placeholder="e.g. Navy Blue, Silver"
                value={newFoundForm.colour}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, colour: e.target.value }))}
              />
            </label>
          </div>

          <div className="form-grid-two">
            <label className="field">
              <span className="field-label">Found Location *</span>
              <select
                required
                value={newFoundForm.foundLocationId}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, foundLocationId: Number(e.target.value) }))}
              >
                {referenceData.locations.map((loc) => (
                  <option key={loc.locationId} value={loc.locationId}>{loc.locationName}</option>
                ))}
              </select>
            </label>

            <label className="field">
              <span className="field-label">Associated Flight (if found in plane/gate)</span>
              <select
                value={newFoundForm.flightId}
                onChange={(e) => setNewFoundForm((p) => ({ ...p, flightId: e.target.value }))}
              >
                <option value="">None / Terminal Discovery</option>
                {referenceData.flights.map((fl) => (
                  <option key={fl.flightId} value={fl.flightId}>{fl.flightNumber} ({fl.airline})</option>
                ))}
              </select>
            </label>
          </div>

          <label className="field">
            <span className="field-label">Item Condition</span>
            <select
              value={newFoundForm.itemCondition}
              onChange={(e) => setNewFoundForm((p) => ({ ...p, itemCondition: e.target.value }))}
            >
              <option value="new">Pristine / New</option>
              <option value="good">Good / Minor Wear</option>
              <option value="fair">Fair</option>
              <option value="poor">Poor</option>
              <option value="damaged">Damaged</option>
            </select>
          </label>

          <label className="field">
            <span className="field-label">Description & Physical Marks</span>
            <textarea
              rows={3}
              placeholder="Notable stickers, initials, scratches, or contents inside..."
              value={newFoundForm.description}
              onChange={(e) => setNewFoundForm((p) => ({ ...p, description: e.target.value }))}
            />
          </label>

          <ImageUploadWidget
            photos={newFoundForm.photos}
            onChange={(photos) => setNewFoundForm((p) => ({ ...p, photos }))}
            maxPhotos={6}
            label="Visual Evidence & Custody Photos"
            hint="Upload custody photos: overall item, serial number plate, identifying marks, or luggage tags."
          />

          <button type="submit" className="button button-primary button-full" disabled={loading}>
            {loading
              ? 'Submitting to Register...'
              : isGroundStaff
              ? 'Submit Patrol Discovery to Custody'
              : isLostFoundAgent
              ? 'Register Item into Counter Custody'
              : 'Log Found Property'}
          </button>
        </form>
      </Dialog>

      {/* =====================================================================
          MODAL B: REVIEW CLAIM (Claims Verification)
         ===================================================================== */}
      <Dialog
        open={reviewModalOpen}
        onOpenChange={setReviewModalOpen}
        title={`Review Claim #${selectedClaim?.claimId}`}
        description="Verify claimant ownership against airport custody records and record formal review decision."
        className="staff-modal"
      >
        {selectedClaim && (
          <form onSubmit={handleReviewClaimSubmit} className="dialog-form">
            <div className="claim-summary-box">
              <div><strong>Claimant:</strong> {selectedClaim.passengerName} ({selectedClaim.passengerEmail})</div>
              <div><strong>Reported Lost:</strong> {selectedClaim.lostItemName}</div>
              <div><strong>Candidate Found:</strong> {selectedClaim.foundItemName}</div>
              <div><strong>Calculated Match Confidence:</strong> {Number(selectedClaim.matchScore || 0).toFixed(1)}%</div>
            </div>

            <label className="field">
              <span className="field-label">Review Decision *</span>
              <select
                value={reviewDecision}
                onChange={(e) => setReviewDecision(e.target.value as 'approved' | 'rejected')}
              >
                <option value="approved">Approve Claim (Valid Ownership Verified)</option>
                <option value="rejected">Reject Claim (Insufficient Proof / Mismatch)</option>
              </select>
            </label>

            {reviewDecision === 'approved' && (
              <label className="field">
                <span className="field-label">Identity Verification Method *</span>
                <select
                  value={verificationMethod}
                  onChange={(e) => setVerificationMethod(e.target.value)}
                >
                  <option value="in_person_document_check">In-Person Government Photo ID Check</option>
                  <option value="baggage_tag_receipt">Baggage Tag Claim Receipt Matching</option>
                  <option value="boarding_pass_verification">Boarding Pass & Itinerary Verification</option>
                  <option value="serial_number_invoice">Device Serial Number / Purchase Invoice</option>
                </select>
              </label>
            )}

            <label className="field">
              <span className="field-label">Review Notes / Audit Remarks</span>
              <textarea
                rows={3}
                placeholder="Enter justification, ID document reference, or reasons for decision..."
                value={reviewNotes}
                onChange={(e) => setReviewNotes(e.target.value)}
              />
            </label>

            <button type="submit" className="button button-primary button-full" disabled={loading}>
              {loading ? 'Submitting Decision...' : `Confirm Decision: ${reviewDecision.toUpperCase()}`}
            </button>
          </form>
        )}
      </Dialog>

      {/* =====================================================================
          MODAL C: ASSIGN STORAGE (Vault & Inventory)
         ===================================================================== */}
      <Dialog
        open={assignModalOpen}
        onOpenChange={setAssignModalOpen}
        title="Assign Found Property to Secure Storage"
        description="Allocate an unassigned property item to a specific locker slot in an airport storage cabinet."
        className="staff-modal"
      >
        <form onSubmit={handleAssignStorageSubmit} className="dialog-form">
          <label className="field">
            <span className="field-label">Found Item to Store *</span>
            <select
              required
              value={assignFoundItemId}
              onChange={(e) => setAssignFoundItemId(Number(e.target.value))}
            >
              <option value="">Select an item awaiting storage...</option>
              {foundItems
                .filter((item) => item.itemStatus === 'awaiting_review' || !item.storageName)
                .map((item) => (
                  <option key={item.foundItemId} value={item.foundItemId}>
                    #{item.foundItemId} - {item.itemName} ({item.categoryName})
                  </option>
                ))}
            </select>
          </label>

          <label className="field">
            <span className="field-label">Storage Facility *</span>
            <select
              required
              value={assignStorageId}
              onChange={(e) => setAssignStorageId(Number(e.target.value))}
            >
              {storageLocations.map((loc) => (
                <option key={loc.storageId} value={loc.storageId}>
                  {loc.storageName} ({loc.availableSlots} slots available)
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span className="field-label">Locker Slot Number *</span>
            <input
              type="number"
              min={1}
              max={100}
              required
              value={assignSlotNumber}
              onChange={(e) => setAssignSlotNumber(Number(e.target.value))}
            />
          </label>

          <button type="submit" className="button button-primary button-full" disabled={loading}>
            {loading ? 'Assigning Slot...' : 'Confirm Storage Assignment'}
          </button>
        </form>
      </Dialog>

      {/* =====================================================================
          MODAL D: PROCESS RETURN (Return & Dispatch)
         ===================================================================== */}
      <Dialog
        open={processModalOpen}
        onOpenChange={setProcessModalOpen}
        title="Update Dispatch / Handover Status"
        description="Record custody handover completion or courier transit state for approved property return."
        className="staff-modal"
      >
        {selectedDispatch && (
          <form onSubmit={handleProcessReturnSubmit} className="dialog-form">
            <div className="claim-summary-box">
              <div><strong>Claimant:</strong> {selectedDispatch.passengerName}</div>
              <div><strong>Property Item:</strong> {selectedDispatch.itemName}</div>
            </div>

            <label className="field">
              <span className="field-label">Handover Method</span>
              <select
                value={processMethod}
                onChange={(e) => setProcessMethod(e.target.value as 'counter_handover' | 'delivery')}
              >
                <option value="counter_handover">Terminal Counter Handover</option>
                <option value="delivery">Courier Delivery</option>
              </select>
            </label>

            <label className="field">
              <span className="field-label">Dispatch Status *</span>
              <select
                value={processStatus}
                onChange={(e) => setProcessStatus(e.target.value as 'initiated' | 'in_transit' | 'completed')}
              >
                <option value="initiated">Initiated (Preparing)</option>
                <option value="in_transit">In Transit (Dispatched via Courier)</option>
                <option value="completed">Completed (Released to Passenger)</option>
              </select>
            </label>

            <label className="field">
              <span className="field-label">Handover Notes / Tracking Details</span>
              <textarea
                rows={3}
                placeholder="Passenger signature confirmed, airway bill number, locker released..."
                value={processNotes}
                onChange={(e) => setProcessNotes(e.target.value)}
              />
            </label>

            <button type="submit" className="button button-primary button-full" disabled={loading}>
              {loading ? 'Updating Return...' : 'Save Dispatch Status'}
            </button>
          </form>
        )}
      </Dialog>

      {/* =====================================================================
          MODAL E: PROVISION STAFF (System Admin)
         ===================================================================== */}
      <Dialog
        open={newStaffModalOpen}
        onOpenChange={setNewStaffModalOpen}
        title="Provision Airport Staff Account"
        description="Create an official airport staff credential with designated subrole and system authorities."
        className="staff-modal"
      >
        <form onSubmit={handleCreateStaffSubmit} className="dialog-form">
          <label className="field">
            <span className="field-label">Full Name *</span>
            <input
              type="text"
              required
              placeholder="e.g. Airport Security Officer"
              value={newStaffForm.fullName}
              onChange={(e) => setNewStaffForm((p) => ({ ...p, fullName: e.target.value }))}
            />
          </label>

          <label className="field">
            <span className="field-label">Official Email Address *</span>
            <input
              type="email"
              required
              placeholder="e.g. security.officer@airport.test"
              value={newStaffForm.email}
              onChange={(e) => setNewStaffForm((p) => ({ ...p, email: e.target.value }))}
            />
          </label>

          <label className="field">
            <span className="field-label">Temporary Password *</span>
            <input
              type="password"
              required
              minLength={6}
              placeholder="At least 6 characters"
              value={newStaffForm.password}
              onChange={(e) => setNewStaffForm((p) => ({ ...p, password: e.target.value }))}
            />
          </label>

          <label className="field">
            <span className="field-label">Designated Staff Subrole *</span>
            <select
              required
              value={newStaffForm.staffRole}
              onChange={(e) => setNewStaffForm((p) => ({ ...p, staffRole: e.target.value }))}
            >
              <option value="lost_found_agent">Lost & Found Agent (Intake & Search)</option>
              <option value="claims_verification_officer">Claims Verification Officer (Claim Reviews)</option>
              <option value="inventory_storage_officer">Inventory & Storage Officer (Physical Vault)</option>
              <option value="return_dispatch_officer">Return & Dispatch Officer (Counter & Courier)</option>
              <option value="supervisor">Lost Property Supervisor (Oversight & Escalations)</option>
              <option value="ground_staff">Ground Staff (Restricted Discovery Intake)</option>
              <option value="system_admin">System Administrator (Full Management)</option>
            </select>
          </label>

          <button type="submit" className="button button-primary button-full" disabled={loading}>
            {loading ? 'Creating Account...' : 'Provision Staff Account'}
          </button>
        </form>
      </Dialog>

      {/* Lightbox for full-resolution photo viewing */}
      {lightboxImg && (
        <div className="lightbox-overlay" onClick={() => setLightboxImg(null)}>
          <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="lightbox-close-btn"
              onClick={() => setLightboxImg(null)}
              aria-label="Close"
            >
              <X size={20} />
            </button>
            <img src={lightboxImg} alt="Enlarged view" className="lightbox-img" />
          </div>
        </div>
      )}
    </div>
  );
}
